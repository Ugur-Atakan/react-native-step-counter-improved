import React, { useEffect, useState } from 'react';
import { Text, TouchableOpacity, View } from 'react-native';
import {
  isStepCountingSupported,
  parseStepData,
  startStepCounterUpdate,
  stopStepCounterUpdate,
} from '@uguratakan/react-native-step-counter-improved';

export default function App() {
  const [steps, setSteps] = useState(0);
  const [supported, setSupported] = useState(false);
  const [granted, setGranted] = useState(false);

  const askPermission = async () => {
    isStepCountingSupported().then((result) => {
      console.debug('🚀 - isStepCountingSupported', result);
      setGranted(result.granted === true);
      setSupported(result.supported === true);
    });
  };

  const startStepCounter = () => {
    startStepCounterUpdate(new Date(), (data) => {
      console.log('🚀 yeni bir adım atıldı ');
      console.debug(parseStepData(data));
      setSteps(data.steps);
    });
  };

  useEffect(() => {
    askPermission();
    startStepCounter();
    return () => {
      stopStepCounterUpdate();
    };
  }, []);

  return (
    <View style={{ flex: 1 }}>
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <Text>StepCounter is Supported:{`${supported}`}</Text>
        <Text>StepCounter permission is Granted: {`${granted}`}</Text>
      </View>
      <Text style={{ alignSelf: 'center' }}>Current Steps:{steps}</Text>
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <TouchableOpacity
          style={{
            backgroundColor: '#ccc',
            justifyContent: 'center',
            alignItems: 'center',
            width: 300,
            height: 50,
          }}>
          <Text>Get current location</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}
