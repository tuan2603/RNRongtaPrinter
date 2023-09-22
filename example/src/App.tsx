import * as React from 'react';

import { StyleSheet, View, Text, Alert } from 'react-native';
import {
  CMD_TYPE,
  CONNECT_TYPE,
  connectDevice,
  cutAllPage,
  deviceDiscovery,
} from 'react-native-rongta-printer';
import _ from 'lodash';

export default function App() {
  const handleConnect = (res: any) => {
    const ip: string = _.get(res, '0.IP', '');
    const port: number = _.get(res, '0.PORT', 0);
    connectDevice(CMD_TYPE.CMD_ESC, CONNECT_TYPE.CON_WIFI, ip, port)
      .then((_res) => {
        Alert.alert('connect', JSON.stringify(_res));
        cutAllPage().then().catch();
      })
      .catch((_er) => {
        Alert.alert('_er', JSON.stringify(_er));
      });
  };

  React.useEffect(() => {
    setTimeout(() => {
      deviceDiscovery(CMD_TYPE.CMD_ESC, CONNECT_TYPE.CON_WIFI)
        .then((res) => {
          Alert.alert('res', JSON.stringify(res));
          handleConnect(res);
        })
        .catch((er) => {
          console.log('er', er);
        });
    }, 2000);
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {'test'}</Text>
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
