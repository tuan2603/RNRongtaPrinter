package com.rongtaprinter;

import static com.rongtaprinter.utils.MapUtil.toWritableMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbManager;
import android.media.metrics.LogSessionId;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.module.annotations.ReactModule;
import com.rongtaprinter.utils.BaseEnum;
import com.rt.printerlibrary.bean.LableSizeBean;
import com.rt.printerlibrary.bean.UsbConfigBean;
import com.rt.printerlibrary.bean.WiFiConfigBean;
import com.rt.printerlibrary.cmd.Cmd;
import com.rt.printerlibrary.cmd.EscFactory;
import com.rt.printerlibrary.connect.PrinterInterface;
import com.rt.printerlibrary.enumerate.BmpPrintMode;
import com.rt.printerlibrary.enumerate.CommonEnum;
import com.rt.printerlibrary.enumerate.ConnectStateEnum;
import com.rt.printerlibrary.exception.SdkException;
import com.rt.printerlibrary.factory.cmd.CmdFactory;
import com.rt.printerlibrary.factory.connect.PIFactory;
import com.rt.printerlibrary.factory.connect.UsbFactory;
import com.rt.printerlibrary.factory.connect.WiFiFactory;
import com.rt.printerlibrary.factory.printer.PrinterFactory;
import com.rt.printerlibrary.factory.printer.UniversalPrinterFactory;
import com.rt.printerlibrary.ipscan.IpScanner;
import com.rt.printerlibrary.printer.RTPrinter;
import com.rt.printerlibrary.setting.BitmapSetting;
import com.rt.printerlibrary.setting.CommonSetting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ReactModule(name = RNRongtaPrinterModule.NAME)
public class RNRongtaPrinterModule extends ReactContextBaseJavaModule {

  private Object configObj;

  private RTPrinter rtPrinter;

  private PrinterFactory printerFactory;

  public RTPrinter getRtPrinter() {
    return rtPrinter;
  }


  public void setRtPrinter(RTPrinter rtPrinter) {
    this.rtPrinter = rtPrinter;
  }


  private int currentCmdType = BaseEnum.CMD_ESC;

  public int getCurrentCmdType() {
    return currentCmdType;
  }

  public void setCurrentCmdType(int currentCmdType) {
    this.currentCmdType = currentCmdType;
  }

  private int currentConnectType = BaseEnum.CON_WIFI;


  public int getCurrentConnectType() {
    return currentConnectType;
  }

  public void setCurrentConnectType(int currentConnectType) {
    this.currentConnectType = currentConnectType;
  }

  private ArrayList<PrinterInterface> printerInterfaceArrayList = new ArrayList<>();


  private Promise mConnectDevicePromise;
  private Promise mConnectIPPromise;
  private Promise mPrintPromise;

  public static final String NAME = "RNRongtaPrinter";

  public RNRongtaPrinterModule(ReactApplicationContext reactContext) {
    super(reactContext);
    init();
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }


  public void init() {
    printerFactory = new UniversalPrinterFactory();
    rtPrinter = printerFactory.create();
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("CON_BLUETOOTH", BaseEnum.CON_BLUETOOTH);
    constants.put("CON_WIFI", BaseEnum.CON_WIFI);
    constants.put("CON_USB", BaseEnum.CON_USB);
    constants.put("CMD_ESC", BaseEnum.CMD_ESC);
    constants.put("CMD_TSC", BaseEnum.CMD_TSC);
    constants.put("CMD_CPCL", BaseEnum.CMD_CPCL);
    constants.put("CMD_PIN", BaseEnum.CMD_PIN);
    return constants;
  }


  //device discovery printer
  @ReactMethod
  public void deviceDiscovery(int cmdType, int connectType, Promise promise) {
    mConnectDevicePromise = promise;
    setCurrentCmdType(cmdType);
    setCurrentConnectType(connectType);
    handleConnectType();
  }

  private void connectUSB(UsbConfigBean usbConfigBean) {
    UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    PIFactory piFactory = new UsbFactory();
    PrinterInterface printerInterface = piFactory.create();
    printerInterface.setConfigObject(usbConfigBean);
    rtPrinter.setPrinterInterface(printerInterface);

    if (mUsbManager.hasPermission(usbConfigBean.usbDevice)) {
      try {
        rtPrinter.connect(usbConfigBean);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      mUsbManager.requestPermission(usbConfigBean.usbDevice, usbConfigBean.pendingIntent);
    }

  }


  private void handleConnectType() {
    switch (getCurrentConnectType()) {
      case BaseEnum.CON_WIFI:
        new IpScanner() {
          @Override
          public void onSearchStart() {
            Log.d("rongta", "SCAN_START-------------------");
          }

          @Override
          public void onSearchFinish(List devicesList) {
            Log.d("rongta", "SCAN_FINISH-------------------");
            List<DeviceBean> deviceBeanList = (List<DeviceBean>) devicesList;
            if (deviceBeanList != null && deviceBeanList.size() != 0) {
              WritableArray writableArray = Arguments.createArray();
              for (int i = 0; i < deviceBeanList.size(); i++) {
                Map<String, Object> info = new HashMap<>();
                if (deviceBeanList.get(i).getDeviceIp() != null) {
                  info.put("IP", deviceBeanList.get(i).getDeviceIp());
                }
                if (deviceBeanList.get(i).getMacAddress() != null) {
                  info.put("MAC", deviceBeanList.get(i).getMacAddress());
                }
                info.put("PORT", deviceBeanList.get(i).getDevicePort());
                info.put("DHCP", deviceBeanList.get(i).isDHCPEnable());
                writableArray.pushMap(toWritableMap(info));
              }
              if (mConnectDevicePromise != null) {
                mConnectDevicePromise.resolve(writableArray);
              }
            }
          }

          @Override
          public void onSearchError(String msg) {
            Log.d("rongta", "SCAN_ERROR-------------------");
            if (mConnectDevicePromise != null) {
              mConnectDevicePromise.reject("SCAN_ERROR", msg);
            }
          }
        }.start();
        break;

      case BaseEnum.CON_USB:
        UsbConfigBean usbConfigBean = (UsbConfigBean) configObj;
        connectUSB(usbConfigBean);
        break;

      default:
        mConnectDevicePromise.reject("SCAN_ERROR", "Not find connect type");
    }
  }

  private boolean isInConnectList(Object configObj) {
    boolean isInList = false;
    for (int i = 0; i < printerInterfaceArrayList.size(); i++) {
      PrinterInterface printerInterface = printerInterfaceArrayList.get(i);
      if (configObj.toString().equals(printerInterface.getConfigObject().toString())) {
        if (printerInterface.getConnectState() == ConnectStateEnum.Connected) {
          isInList = true;
          break;
        }
      }
    }
    return isInList;
  }

  private void isConfigPrintEnable(Object configObj) {
    if (isInConnectList(configObj)) {
      Log.d("rongta", "isConfigPrintEnable: true");
    } else {
      Log.d("rongta", "isConfigPrintEnable: false");
    }
  }


  private void connectWifi(WiFiConfigBean wiFiConfigBean) {
    PIFactory piFactory = new WiFiFactory();
    PrinterInterface printerInterface = piFactory.create();
    printerInterface.setConfigObject(wiFiConfigBean);
    rtPrinter.setPrinterInterface(printerInterface);
    try {
      rtPrinter.connect(wiFiConfigBean);
      if (mConnectIPPromise != null) {
        mConnectIPPromise.resolve(true);
      }
    } catch (Exception e) {
      e.printStackTrace();
      if (mConnectIPPromise != null) {
        mConnectIPPromise.resolve(false);
      }
    }
  }


  @ReactMethod
  public void connect(int cmdType, int connectType, String deviceIp, int devicePort, Promise promise) {
    setCurrentCmdType(cmdType);
    setCurrentConnectType(connectType);
    mConnectIPPromise = promise;
    configObj = new WiFiConfigBean(deviceIp, devicePort);
    isConfigPrintEnable(configObj);
    switch (connectType) {
      case BaseEnum.CON_WIFI:
        WiFiConfigBean wiFiConfigBean = (WiFiConfigBean) configObj;
        connectWifi(wiFiConfigBean);
        break;
    }
  }


  @ReactMethod
  public void cutAll(Promise promise) {
    try {
      if (rtPrinter != null) {
        CmdFactory cmdFactory = new EscFactory();
        Cmd cmd = cmdFactory.create();
        cmd.append(cmd.getAllCutCmd());
        rtPrinter.writeMsgAsync(cmd.getAppendCmds());
        if (promise != null) {
          promise.resolve(true);
        }
      }
      if (promise != null) {
        promise.resolve(false);
      }
    } catch (Exception e) {
      e.printStackTrace();
      if (promise != null) {
        promise.resolve(false);
      }
    }
  }

  private Bitmap base64ToBitmap(String base64) {
    Bitmap bitmap = null;
    try {
      byte[] byteArr = Base64.decode(base64, 0);
      bitmap = BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length);
    } catch (Exception e) {
      Log.d("rongta", "escPrint: base64ToBitmap");
      e.printStackTrace();
    }
    return bitmap;
  }

  private void escPrint(Bitmap bitmap, int width) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        Log.d("rongta", "escPrint: start");
        CmdFactory cmdFactory = new EscFactory();
        Cmd cmd = cmdFactory.create();
        cmd.append(cmd.getHeaderCmd());
        CommonSetting commonSetting = new CommonSetting();
        commonSetting.setAlign(CommonEnum.ALIGN_MIDDLE);
        int mHeight = bitmap.getHeight();
        int mWidth = bitmap.getWidth();
        cmd.append(cmd.getCommonSettingCmd(commonSetting));
        BitmapSetting bitmapSetting = new BitmapSetting();
        BmpPrintMode mode = BmpPrintMode.MODE_SINGLE_FAST;
        bitmapSetting.setBmpPrintMode(mode);
        bitmapSetting.setBimtapLimitWidth(width);
        Bitmap mBitmap = Bitmap.createScaledBitmap(bitmap, width, width * mHeight / mWidth, false);
        try {
          cmd.append(cmd.getBitmapCmd(bitmapSetting, mBitmap));
        } catch (SdkException e) {
          mPrintPromise.reject(e);
          e.printStackTrace();
        }
        cmd.append(cmd.getLFCRCmd());
        cmd.append(cmd.getCmdCutNew());
        if (rtPrinter != null) {
          rtPrinter.writeMsgAsync(cmd.getAppendCmds());//Sync Write
          mPrintPromise.resolve(true);
        }
      }
    }).
      start();
  }

  @ReactMethod
  public void printBase64(String base64, int width, int cmdType, Promise promise) {
    mPrintPromise = promise;
    try {
      Bitmap bitmap = base64ToBitmap(base64);
      if (bitmap != null) {
        switch (cmdType) {
          case BaseEnum.CMD_ESC:
            escPrint(bitmap, width);
            break;
          default:
            break;
        }
      } else {
        mPrintPromise.reject("Not found data printer");
      }
    } catch (Exception e) {
      Log.d("rongta", "escPrint: printBase64");
      mPrintPromise.reject(e);
    }
  }
}
