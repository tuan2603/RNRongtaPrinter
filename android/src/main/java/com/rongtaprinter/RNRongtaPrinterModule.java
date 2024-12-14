package com.rongtaprinter;


import static com.rongtaprinter.utils.MapUtil.toWritableMap;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.module.annotations.ReactModule;
import com.rongtaprinter.utils.BaseEnum;
import com.rt.printerlibrary.bean.UsbConfigBean;
import com.rt.printerlibrary.bean.WiFiConfigBean;
import com.rt.printerlibrary.cmd.Cmd;
import com.rt.printerlibrary.cmd.EscFactory;
import com.rt.printerlibrary.connect.PrinterInterface;
import com.rt.printerlibrary.enumerate.BmpPrintMode;
import com.rt.printerlibrary.enumerate.CommonEnum;
import com.rt.printerlibrary.enumerate.ConnectStateEnum;
import com.rt.printerlibrary.enumerate.Print80StatusCmd;
import com.rt.printerlibrary.exception.SdkException;
import com.rt.printerlibrary.factory.cmd.CmdFactory;
import com.rt.printerlibrary.factory.connect.PIFactory;
import com.rt.printerlibrary.factory.connect.UsbFactory;
import com.rt.printerlibrary.factory.connect.WiFiFactory;
import com.rt.printerlibrary.factory.printer.PrinterFactory;
import com.rt.printerlibrary.factory.printer.UniversalPrinterFactory;
import com.rt.printerlibrary.ipscan.IpScanner;
import com.rt.printerlibrary.observer.PrinterObserver;
import com.rt.printerlibrary.observer.PrinterObserverManager;
import com.rt.printerlibrary.printer.RTPrinter;
import com.rt.printerlibrary.setting.BitmapSetting;
import com.rt.printerlibrary.setting.CommonSetting;
import com.rt.printerlibrary.utils.FuncUtils;
import com.rt.printerlibrary.utils.PrintStatusCmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@ReactModule(name = RNRongtaPrinterModule.NAME)
public class RNRongtaPrinterModule extends ReactContextBaseJavaModule implements PrinterObserver {

  private static final String TAG = "RNRongtaPrinterModule";

  private RTPrinter rtPrinter = null;


  private int currentCmdType = BaseEnum.CMD_ESC;


  public void setCurrentCmdType(int currentCmdType) {
    this.currentCmdType = currentCmdType;
  }

  private int currentConnectType = BaseEnum.CON_WIFI;


  public int getCurrentConnectType() {
    return currentConnectType;
  }

  public void setCurrentConnectType(int currentConnectType) {
    switch (currentConnectType) {
      case BaseEnum.CMD_ESC:
        printStatusCmd = PrintStatusCmd.cmd_print_100402;
        break;
      case BaseEnum.CMD_TSC:
        printStatusCmd = PrintStatusCmd.cmd_Normal;
        break;
    }
    this.currentConnectType = currentConnectType;
  }

  private ArrayList<PrinterInterface> printerInterfaceArrayList = new ArrayList<>();

  private PrintStatusCmd printStatusCmd = PrintStatusCmd.cmd_print_100402;

  private boolean isEnable = false;

  private Promise mConnectDevicePromise;
  private Promise mConnectIPPromise;
  private Promise mPrintPromise;
  private Promise mPrintStatusPromise;

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

  private void setPrintEnable(boolean isEnable) {
    this.isEnable = isEnable;
  }


  public void init() {
    PrinterFactory printerFactory = new UniversalPrinterFactory();
    rtPrinter = printerFactory.create();
    if(rtPrinter != null) {
      PrinterObserverManager.getInstance().add(this);
    }
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




  private void handleConnectType() {
    switch (getCurrentConnectType()) {
      case BaseEnum.CON_WIFI:
        new IpScanner() {
          @Override
          public void onSearchStart() {
            Log.d(TAG, "SCAN_START-------------------");
          }

          @Override
          public void onSearchFinish(List devicesList) {
            Log.d(TAG, "SCAN_FINISH-------------------");
            List<DeviceBean> deviceBeanList = (List<DeviceBean>) devicesList;
            WritableArray writableArray = Arguments.createArray();
            if (deviceBeanList != null && !deviceBeanList.isEmpty()) {
              for (int i = 0; i < deviceBeanList.size(); i++) {
                Map<String, Object> info = getStringObjectMap(deviceBeanList, i);
                writableArray.pushMap(toWritableMap(info));
              }
            }
            if (mConnectDevicePromise != null) {
              mConnectDevicePromise.resolve(writableArray);
            }
          }

          @Override
          public void onSearchError(String msg) {
            Log.d(TAG, "SCAN_ERROR-------------------");
            if (mConnectDevicePromise != null) {
              mConnectDevicePromise.reject("SCAN_ERROR", msg);
            }
          }
        }.start();
        break;

      case BaseEnum.CON_USB:
        List<UsbDevice> mList = new ArrayList<>();
        UsbManager mUsbManager = (UsbManager) getReactApplicationContext().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Log.d(TAG, "deviceList size = " + deviceList.size());
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
          UsbDevice device = deviceIterator.next();
          Log.d(TAG, "device getDeviceName" + device.getDeviceName());
          Log.d(TAG, "device getVendorId" + device.getVendorId());
          Log.d(TAG, "device getProductId" + device.getProductId());
          mList.add(device);
        }
        if (mConnectDevicePromise != null) {
          WritableArray writableArray = Arguments.createArray();
          for (int i = 0; i < mList.size(); i++) {
            Map<String, Object> info = new HashMap<>();
            info.put("DEVICE_NAME", mList.get(i).getDeviceName());
            info.put("VENDOR_ID", mList.get(i).getVendorId());
            info.put("PRODUCT_ID", mList.get(i).getProductId());
            info.put("DEVICE_ID", mList.get(i).getDeviceId());
            writableArray.pushMap(toWritableMap(info));
          }
          mConnectDevicePromise.resolve(writableArray);
        }
        break;

      default:
        if(mConnectDevicePromise != null) {
          mConnectDevicePromise.reject("SCAN_ERROR", "Not find connect type");
        }
    }
  }

  private static @NonNull Map<String, Object> getStringObjectMap(List<IpScanner.DeviceBean> deviceBeanList, int i) {
    Map<String, Object> info = new HashMap<>();
    if (deviceBeanList.get(i).getDeviceIp() != null) {
      info.put("IP", deviceBeanList.get(i).getDeviceIp());
    }
    if (deviceBeanList.get(i).getMacAddress() != null) {
      info.put("MAC", deviceBeanList.get(i).getMacAddress());
    }
    info.put("PORT", deviceBeanList.get(i).getDevicePort());
    info.put("DHCP", deviceBeanList.get(i).isDHCPEnable());
    return info;
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
      Log.d(TAG, "isConfigPrintEnable: true");
      setPrintEnable(true);
    } else {
      Log.d(TAG, "isConfigPrintEnable: false");
      setPrintEnable(false);
    }
  }


  private void connectWifi(WiFiConfigBean wiFiConfigBean) {
    PIFactory piFactory = new WiFiFactory();
    PrinterInterface printerInterface = piFactory.create();
    printerInterface.setConfigObject(wiFiConfigBean);
    if(rtPrinter != null) {
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
  }


  @ReactMethod
  public void connect(int cmdType, int connectType, String deviceIp, int devicePort, int deviceId, int vendorId,  Promise promise) {
    setCurrentCmdType(cmdType);
    setCurrentConnectType(connectType);
    mConnectIPPromise = promise;

    switch (connectType) {
      case BaseEnum.CON_WIFI:
        Object configObj = new WiFiConfigBean(deviceIp, devicePort);
        isConfigPrintEnable(configObj);
        WiFiConfigBean wiFiConfigBean = (WiFiConfigBean) configObj;
        connectWifi(wiFiConfigBean);
        break;
      case BaseEnum.CON_USB:
        UsbManager mUsbManager = (UsbManager) getReactApplicationContext().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        UsbDevice myUsbDevice = null;
        while (deviceIterator.hasNext()) {
          UsbDevice device = deviceIterator.next();
          if(device.getVendorId() == vendorId || device.getDeviceId() == deviceId){
            myUsbDevice = device;
          }
        }
        if(myUsbDevice != null) {
          PendingIntent mPermissionIntent = PendingIntent.getBroadcast(
            getReactApplicationContext(),
            0,
            new Intent(this.getReactApplicationContext().getPackageName()),
            PendingIntent.FLAG_IMMUTABLE);
          configObj = new UsbConfigBean(getReactApplicationContext(), myUsbDevice, mPermissionIntent);
          UsbConfigBean usbConfigBean = (UsbConfigBean) configObj;
          connectUSB(usbConfigBean);
        }
        break;
    }
  }

  private void connectUSB(UsbConfigBean usbConfigBean) {
    UsbManager mUsbManager = (UsbManager) getReactApplicationContext().getSystemService(Context.USB_SERVICE);
    PIFactory piFactory = new UsbFactory();
    PrinterInterface printerInterface = piFactory.create();
    printerInterface.setConfigObject(usbConfigBean);
    if(rtPrinter != null) {
      rtPrinter.setPrinterInterface(printerInterface);
      if (mUsbManager.hasPermission(usbConfigBean.usbDevice)) {
        try {
          rtPrinter.connect(usbConfigBean);
          if (mConnectIPPromise != null) {
            mConnectIPPromise.resolve(true);
          }
        } catch (Exception e) {
          e.printStackTrace();
          if (mConnectIPPromise != null) {
            mConnectIPPromise.resolve(false);
          }
        }
      } else {
        mUsbManager.requestPermission(usbConfigBean.usbDevice, usbConfigBean.pendingIntent);
      }
    }
  }

  @ReactMethod
  public void cutAll(Boolean isCutAll, Promise promise) {
    try {
      if (rtPrinter != null) {
        CmdFactory cmdFactory = new EscFactory();
        Cmd cmd = cmdFactory.create();
        if(isCutAll) {
          cmd.append(cmd.getAllCutCmd());
        } else {
          cmd.append(cmd.getHalfCutCmd());
        }
        rtPrinter.writeMsgAsync(cmd.getAppendCmds());
        if (promise != null) {
          promise.resolve(true);
          return;
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
      Log.d(TAG, "escPrint: base64ToBitmap");
      e.printStackTrace();
      if(mPrintPromise != null) {
        mPrintPromise.reject(e);
      }
    }
    return bitmap;
  }

  private void escPrint(Bitmap bitmap, int width, boolean isCut) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        CmdFactory cmdFactory = new EscFactory();
        Cmd cmd = cmdFactory.create();
        cmd.append(cmd.getHeaderCmd());
        CommonSetting commonSetting = new CommonSetting();
        commonSetting.setAlign(CommonEnum.ALIGN_MIDDLE);
        int mHeight = bitmap.getHeight();
        int mWidth = bitmap.getWidth();
        cmd.append(cmd.getCommonSettingCmd(commonSetting));
        BitmapSetting bitmapSetting = new BitmapSetting();
        BmpPrintMode mode = BmpPrintMode.MODE_SINGLE_COLOR;
        bitmapSetting.setBmpPrintMode(mode);
        bitmapSetting.setBimtapLimitWidth(width);
        Bitmap mBitmap = Bitmap.createScaledBitmap(bitmap, width, width * mHeight / mWidth, false);
        try {
          cmd.append(cmd.getBitmapCmd(bitmapSetting, mBitmap));
        } catch (SdkException e) {
          e.printStackTrace();
          if(mPrintPromise != null) {
            mPrintPromise.reject(e);
          }
          return;
        }
        cmd.append(cmd.getLFCRCmd());
        if(isCut) {
          cmd.append(cmd.getCmdCutNew());
        }
        if (rtPrinter != null) {
          rtPrinter.writeMsgAsync(cmd.getAppendCmds());
          if(mPrintPromise != null) {
            mPrintPromise.resolve(true);
          }
        } else {
          if(mPrintPromise != null) {
            mPrintPromise.reject(new Throwable("printer fail"));
          }
        }
      }
    }).
      start();
  }



  @ReactMethod
  public void printBase64(String base64, int width, int cmdType, boolean isCut, Promise promise) {
    mPrintPromise = promise;
    try {
      Bitmap bitmap = base64ToBitmap(base64);
      if (bitmap != null && cmdType == BaseEnum.CMD_ESC) {
        escPrint(bitmap, width, isCut);
      } else {
        if(mPrintPromise != null) {
          mPrintPromise.reject(new Throwable("Not found data printer"));
        }
      }
    } catch (Exception e) {
      Log.d(TAG, "escPrint: printBase64");
      if(mPrintPromise != null) {
        mPrintPromise.reject(e);
      }
    }
  }

  @ReactMethod
  private void doDisConnect(Promise promise) {
    if (rtPrinter != null && rtPrinter.getPrinterInterface() != null) {
      try {
        rtPrinter.disConnect();
        setPrintEnable(false);
        if(promise != null) {
          promise.resolve(true);
        }
      } catch (Exception e) {
        if(promise != null) {
          promise.reject(e);
        }
      }
    }
  }

  @Override
  public void printerObserverCallback(PrinterInterface printerInterface, int state) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        switch (state) {
          case CommonEnum.CONNECT_STATE_SUCCESS:
            Log.i(TAG, "printerObserverCallback: enable: true");
            setPrintEnable(true);
            if (mConnectIPPromise != null) {
              mConnectIPPromise.resolve(true);
            }
            break;
          case CommonEnum.CONNECT_STATE_INTERRUPTED:
            Log.i(TAG, "printerObserverCallback: enable: false");
            setPrintEnable(false);
            if (mConnectIPPromise != null) {
              mConnectIPPromise.reject(new Throwable("connect fail"));
            }
            break;
        }
      }
    });
  }

  @Override
  public void printerReadMsgCallback(PrinterInterface printerInterface, byte[] bytes) {
    Log.i(TAG, "printerReadMsgCallback: "+ FuncUtils.ByteArrToHex(bytes));
    mPrintStatusPromise.resolve(FuncUtils.ByteArrToHex(bytes));
  }

  @ReactMethod
  public void cashBox() {
    if (rtPrinter != null) {
      CmdFactory cmdFactory = new EscFactory();
      Cmd cmd = cmdFactory.create();
      cmd.append(cmd.getOpenMoneyBoxCmd());//Open cashbox use default setting[0x00,0x20,0x01]
      rtPrinter.writeMsgAsync(cmd.getAppendCmds());
    }
  }

  @ReactMethod
  public void getPrintStatus(Promise promise) {
    mPrintStatusPromise = promise;
    if (rtPrinter == null || !isEnable) {
      if(mPrintStatusPromise != null) {
        mPrintStatusPromise.resolve(false);
      }
    } else {
      if(rtPrinter.getConnectState() != ConnectStateEnum.Connected) {
        if(mPrintStatusPromise != null) {
          mPrintStatusPromise.resolve(false);
        }
      } else {
        CmdFactory cmdFactory = new EscFactory();
        Cmd cmd = cmdFactory.create();
        rtPrinter.writeMsgAsync(cmd.getPrint80StausCmd(Print80StatusCmd.cmd_IsPrinting));
        if(mPrintStatusPromise != null) {
          mPrintStatusPromise.resolve(true);
        }
      }
    }
  }

}
