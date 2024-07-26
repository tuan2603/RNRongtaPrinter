import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-rongta-printer' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const RNRongtaPrinter = NativeModules.RNRongtaPrinter
  ? NativeModules.RNRongtaPrinter
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const { CON_WIFI, CON_USB, CMD_ESC, CMD_TSC } = RNRongtaPrinter.getConstants();
const CONNECT_TYPE: { CON_WIFI: number; CON_USB: number } = {
  CON_WIFI,
  CON_USB,
};

const CMD_TYPE: { CMD_ESC: number; CMD_TSC: number } = {
  CMD_ESC,
  CMD_TSC,
};

export { CONNECT_TYPE, CMD_TYPE };

const deviceDiscovery = (
  cmdType: number = CMD_TYPE.CMD_ESC,
  connectType: number = CONNECT_TYPE.CON_WIFI
): Promise<any> => {
  return RNRongtaPrinter.deviceDiscovery(cmdType, connectType);
};

//String deviceIp, int devicePort
const connectDevice = (
  cmdType: number = CMD_TYPE.CMD_ESC,
  connectType: number = CONNECT_TYPE.CON_WIFI,
  deviceIp: string = '',
  devicePort: number = 0,
  deviceId: number = 0,
  vendorId: number = 0
): Promise<any> => {
  return RNRongtaPrinter.connect(
    cmdType,
    connectType,
    deviceIp,
    devicePort,
    deviceId,
    vendorId
  );
};

const cutAllPage = (): Promise<any> => {
  return RNRongtaPrinter.cutAll();
};

const printBase64 = (
  base64: string,
  width: number,
  cmdType: number = CMD_TYPE.CMD_ESC,
  isCut: boolean = false
): Promise<any> => {
  return RNRongtaPrinter.printBase64(base64, width, cmdType, isCut);
};

const disconnect = (): Promise<any> => {
  return RNRongtaPrinter.doDisConnect();
};

const cashBox = () => {
  return RNRongtaPrinter.cashBox();
};

const getStatus = (): Promise<any> => {
  return RNRongtaPrinter.getPrintStatus();
};

export default {
  cashBox,
  disconnect,
  printBase64,
  cutAllPage,
  connectDevice,
  deviceDiscovery,
  getStatus,
};
