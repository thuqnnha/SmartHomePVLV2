package com.example.sh;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;

public class MqttHelper {
    private static final String TAG = "MqttHelper";

    private final Context context;
    public MqttAndroidClient mqttClient;

    public MqttHelper(Context context) {
        this.context = context;
    }

    public void connect(String serverUri, String clientId, String username, String password, IMqttActionListener callback) {
        mqttClient = new MqttAndroidClient(context, serverUri, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true);

        try {
            mqttClient.connect(options, null, callback);
        } catch (MqttException e) {
            Log.e(TAG, "MQTT Connect Exception: " + e.getMessage());
        }
    }

    public void publish(String topic, String message) {
        try {
            mqttClient.publish(topic, new MqttMessage(message.getBytes()));
        } catch (Exception e) {
            Log.e(TAG, "Publish error: " + e.getMessage());
        }
    }

    public void subscribe(String topic, IMqttMessageListener listener) {
        try {
            mqttClient.subscribe(topic, 1, listener);
        } catch (Exception e) {
            Log.e(TAG, "Subscribe error: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    public void disconnect() {
        try {
            if (mqttClient != null) mqttClient.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Disconnect error: " + e.getMessage());
        }
    }
}

