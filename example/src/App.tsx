import * as React from 'react';
import { useState } from 'react';

import { Button, StyleSheet, Text, View } from 'react-native';
import Rongta, { CMD_TYPE, CONNECT_TYPE } from 'react-native-rongta-printer';
import _, { get } from 'lodash';
import { BILL_RECEIPT } from './data';

export default function App() {
  const [device, setDevice] = useState('open');

  const onSetDevice = (val = '') => {
    setDevice((_device) => `${_device}, ${val}`);
  };

  const handleConnect = (cmd: number = CMD_TYPE.CMD_TSC, res: any) => {
    const ip: string = _.get(res, '0.IP', '');
    const port: number = _.get(res, '0.PORT', 0);
    onSetDevice(`ip:${ip} - port:${port}`);
    onSetDevice(`start connect wifi`);
    Rongta.connectDevice(cmd, CONNECT_TYPE.CON_WIFI, ip, port, 0, 0)
      .then((_res) => {
        onSetDevice('end connect wifi success');
        console.log('connect', _res);
      })
      .catch((_er) => {
        onSetDevice('end connect wifi error');
        console.log('_er', _er);
      });
  };

  const handleConnectUsb = (cmd: number = CMD_TYPE.CMD_TSC, res: any) => {
    const deviceId: number = get(res, '0.DEVICE_ID');
    const vendorId: number = get(res, '0.VENDOR_ID');
    onSetDevice(`deviceId:${deviceId} - vendorId:${vendorId}`);
    onSetDevice(`start connect usb`);
    Rongta.connectDevice(cmd, CONNECT_TYPE.CON_USB, '', 0, deviceId, vendorId)
      .then((_res) => {
        onSetDevice('end connect usb success');
        console.log('connect', _res);
      })
      .catch((_er) => {
        onSetDevice('end connect wifi error');
        console.log('_er', _er);
      });
  };

  const discoveryLan = (cmd: number = CMD_TYPE.CMD_TSC) => {
    Rongta.deviceDiscovery(cmd, CONNECT_TYPE.CON_WIFI)
      .then((res) => {
        console.log('res', res);
        onSetDevice('end discovery success');
        handleConnect(cmd, res);
      })
      .catch((er) => {
        onSetDevice('end discovery error');
        console.log('er', er);
      });
  };

  const discoveryUsb = (cmd: number = CMD_TYPE.CMD_TSC) => {
    onSetDevice('start discovery');
    Rongta.deviceDiscovery(cmd, CONNECT_TYPE.CON_USB)
      .then((res) => {
        console.log('res', res);
        onSetDevice('end discovery success');
        handleConnectUsb(cmd, res);
      })
      .catch((er) => {
        onSetDevice('end discovery error');
        console.log('er', er);
      });
  };

  const printImageBase64 = (cmd: number = CMD_TYPE.CMD_ESC) => {
    Rongta.printBase64(BILL_RECEIPT, 600, cmd)
      .then((res) => {
        onSetDevice('print success: ' + res);
      })
      .catch((er) => {
        onSetDevice('print error: ' + JSON.stringify(er));
      });
  };

  React.useEffect(() => {
    setTimeout(() => {
      setDevice('start app rongta');
    }, 2000);
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {device || 'open'}</Text>
      <SizeBox />

      <Button
        title={'connect usb esc'}
        onPress={() => {
          discoveryUsb(CMD_TYPE.CMD_ESC);
        }}
      />
      <SizeBox />

      <Button
        title={'connect lan esc'}
        onPress={() => {
          discoveryLan(CMD_TYPE.CMD_ESC);
        }}
      />
      <SizeBox />

      <Button
        title={'print esc'}
        onPress={() => {
          printImageBase64(CMD_TYPE.CMD_ESC);
        }}
      />
      <SizeBox />

      <Button
        title={'print status'}
        onPress={() => {
          Rongta.getStatus()
            .then((res) => {
              console.log('res', res);
            })
            .catch((er) => {
              console.log('er', er);
            });
        }}
      />
      <SizeBox />
    </View>
  );
}

const SizeBox = ({ size = 16 }: { size?: number }) => {
  return <View style={{ height: size, width: size }} />;
};

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
