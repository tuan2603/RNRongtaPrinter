package com.rongtaprinter;
import static com.rongtaprinter.utils.MapUtil.toWritableMap;

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
import com.rt.printerlibrary.bean.WiFiConfigBean;
import com.rt.printerlibrary.cmd.Cmd;
import com.rt.printerlibrary.cmd.EscFactory;
import com.rt.printerlibrary.connect.PrinterInterface;
import com.rt.printerlibrary.enumerate.ConnectStateEnum;
import com.rt.printerlibrary.factory.cmd.CmdFactory;
import com.rt.printerlibrary.factory.connect.PIFactory;
import com.rt.printerlibrary.factory.connect.WiFiFactory;
import com.rt.printerlibrary.factory.printer.PrinterFactory;
import com.rt.printerlibrary.factory.printer.UniversalPrinterFactory;
import com.rt.printerlibrary.ipscan.IpScanner;
import com.rt.printerlibrary.printer.RTPrinter;

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
                if(deviceBeanList.get(i).getDeviceIp() != null) {
                  info.put("IP", deviceBeanList.get(i).getDeviceIp());
                }
                if(deviceBeanList.get(i).getMacAddress() != null) {
                  info.put("MAC", deviceBeanList.get(i).getMacAddress());
                }
                info.put("PORT", deviceBeanList.get(i).getDevicePort());
                info.put("DHCP", deviceBeanList.get(i).isDHCPEnable());
                writableArray.pushMap(toWritableMap(info));
              }
              if (mConnectDevicePromise != null ) {
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
      if(mConnectIPPromise != null){
        mConnectIPPromise.resolve(true);
      }
    } catch (Exception e) {
      e.printStackTrace();
      if(mConnectIPPromise != null){
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
  public void cutAll( Promise promise) {
    try {
      if (rtPrinter != null) {
        CmdFactory cmdFactory = new EscFactory();
        Cmd cmd = cmdFactory.create();
        cmd.append(cmd.getAllCutCmd());
        rtPrinter.writeMsgAsync(cmd.getAppendCmds());
        if(promise != null){
          promise.resolve(true);
        }
      }
      if(promise != null){
        promise.resolve(false);
      }
    } catch (Exception e) {
      e.printStackTrace();
      if(promise != null){
        promise.resolve(false);
      }
    }
  }
}
