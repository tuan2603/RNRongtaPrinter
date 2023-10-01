import * as React from 'react';
import { useState } from 'react';

import { StyleSheet, Text, View } from 'react-native';
import {
  CMD_TYPE,
  CONNECT_TYPE,
  connectDevice,
  deviceDiscovery,
} from 'react-native-rongta-printer';
import _ from 'lodash';

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
    connectDevice(CMD_TYPE.CMD_ESC, CONNECT_TYPE.CON_WIFI, ip, port)
      .then((_res) => {
        onSetDevice('end connect wifi success');
        console.log('connect', _res);
      })
      .catch((_er) => {
        onSetDevice('end connect wifi error');
        console.log('_er', _er);
      });
  };

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

  const discoveryUsb = () => {
    deviceDiscovery(CMD_TYPE.CMD_TSC, CONNECT_TYPE.CON_USB)
      .then((res) => {
        console.log('res', res);
        onSetDevice('end discovery success');
        // handleConnect(res);
      })
      .catch((er) => {
        onSetDevice('end discovery error');
        console.log('er', er);
      });
  };

  React.useEffect(() => {
    setTimeout(() => {
      onSetDevice('start discovery');
      // discoveryLan();
      discoveryUsb();
    }, 2000);
  }, []);

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
