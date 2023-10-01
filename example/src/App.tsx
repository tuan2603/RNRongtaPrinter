import * as React from 'react';
import { useState } from 'react';

import { StyleSheet, Text, View } from 'react-native';
import {
  CMD_TYPE,
  CONNECT_TYPE,
  connectDevice,
  cutAllPage,
  deviceDiscovery,
} from 'react-native-rongta-printer';
import _, { get } from 'lodash';

export default function App() {
  const [device, setDevice] = useState('open');

  const onSetDevice = (val = '') => {
    setDevice((_device) => `${_device}, ${val}`);
  };

  const handleConnect = (res: any) => {
    const ip: string = _.get(res, '0.IP', '');
    const port: number = _.get(res, '0.PORT', 0);
    onSetDevice(`ip:${ip} - port:${port}`);
    onSetDevice(`start connect wifi`);
    connectDevice(CMD_TYPE.CMD_ESC, CONNECT_TYPE.CON_WIFI, ip, port, 0, 0)
      .then((_res) => {
        onSetDevice('end connect wifi success');
        console.log('connect', _res);
      })
      .catch((_er) => {
        onSetDevice('end connect wifi error');
        console.log('_er', _er);
      });
  };

  const handleConnectUsb = (res: any) => {
    const deviceId: number = get(res, '0.DEVICE_ID');
    const vendorId: number = get(res, '0.VENDOR_ID');
    onSetDevice(`deviceId:${deviceId} - vendorId:${vendorId}`);
    onSetDevice(`start connect usb`);
    connectDevice(
      CMD_TYPE.CMD_TSC,
      CONNECT_TYPE.CON_USB,
      '',
      0,
      deviceId,
      vendorId
    )
      .then((_res) => {
        onSetDevice('end connect usb success');
        console.log('connect', _res);
        cutAllPage().then(() => {});
      })
      .catch((_er) => {
        onSetDevice('end connect wifi error');
        console.log('_er', _er);
      });
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const discoveryLan = () => {
    deviceDiscovery(CMD_TYPE.CMD_ESC, CONNECT_TYPE.CON_WIFI)
      .then((res) => {
        console.log('res', res);
        onSetDevice('end discovery success');
        handleConnect(res);
      })
      .catch((er) => {
        onSetDevice('end discovery error');
        console.log('er', er);
      });
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const discoveryUsb = () => {
    deviceDiscovery(CMD_TYPE.CMD_TSC, CONNECT_TYPE.CON_USB)
      .then((res) => {
        console.log('res', res);
        onSetDevice('end discovery success');
        handleConnectUsb(res);
      })
      .catch((er) => {
        onSetDevice('end discovery error');
        console.log('er', er);
      });
  };

  React.useEffect(() => {
    setTimeout(() => {
      onSetDevice('start discovery');
      discoveryLan();
      discoveryUsb();
    }, 2000);
  }, [discoveryLan, discoveryUsb]);

  return (
    <View style={styles.container}>
      <Text>Result: {device || 'open'}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
