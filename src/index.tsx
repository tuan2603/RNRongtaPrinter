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
const CONNECT_TYPE = {
  CON_WIFI,
  CON_USB,
};

const CMD_TYPE = {
  CMD_ESC,
  CMD_TSC,
};

export { CONNECT_TYPE, CMD_TYPE };

export function multiply(a: number, b: number): Promise<number> {
  return RNRongtaPrinter.multiply(a, b);
}

export function deviceDiscovery(
  cmdType: number,
  connectType: number
): Promise<any> {
  return RNRongtaPrinter.deviceDiscovery(cmdType, connectType);
}

//String deviceIp, int devicePort
export function connectDevice(
  cmdType: number,
  connectType: number,
  deviceIp: string,
  devicePort: number
): Promise<any> {
  return RNRongtaPrinter.connect(cmdType, connectType, deviceIp, devicePort);
}

export function cutAllPage(): Promise<any> {
  return RNRongtaPrinter.cutAll();
}
export function printBase64(
  base64: string,
  width: number,
  cmdType: number
): Promise<any> {
  return RNRongtaPrinter.printBase64(base64, width, cmdType);
}
