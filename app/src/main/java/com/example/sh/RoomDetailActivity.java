package com.example.sh;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RoomDetailActivity extends AppCompatActivity {

    private TextView txtRoomName, txtDeviceTitle, txtCameraTitle;
    private RecyclerView recyclerDevices, recyclerCameras;
    private MqttHelper mqttHelper;
    private String savedUsername, savedPassword;
    private String topicCmd;
    private String topicData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);

        // 1. Ánh xạ View
        txtRoomName = findViewById(R.id.txtRoomName);
        txtDeviceTitle = findViewById(R.id.txtDeviceTitle);
        txtCameraTitle = findViewById(R.id.txtCameraTitle);
        recyclerDevices = findViewById(R.id.recyclerDevices);
        recyclerCameras = findViewById(R.id.recyclerCameras);

        recyclerDevices.setLayoutManager(new LinearLayoutManager(this));
        recyclerCameras.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        // 2. Nhận room_id & room_name từ Intent
        Intent intent = getIntent();
        String roomId = intent.getStringExtra("room_id");
        String roomName = intent.getStringExtra("room_name");
        txtRoomName.setText(roomName);

        // 3. Lấy username từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        savedUsername = prefs.getString("username", "");
        savedPassword = prefs.getString("password", "");

        topicCmd = "user/" + savedUsername + "/cmd";
        topicData = "user/" + savedUsername + "/data";

        List<Device> allDevices = new ArrayList<>();
        allDevices.add(new Device(1, "F69721360 1 RVNRNT", "Camera hành lang", 0));
        allDevices.add(new Device(2, "MAC2", "Đèn trần", 1));
        allDevices.add(new Device(3, "MAC3", "Camera cửa", 0));
//        allDevices.add(new Device(4, "MAC4", "Máy lạnh", 1));

        // Lọc theo loại thiết bị
        List<Device> cameraList = new ArrayList<>();
        List<Device> otherDevices = new ArrayList<>();

        for (Device d : allDevices) {
            if (d.getdeviceType() == 0)
                cameraList.add(d);
            else
                otherDevices.add(d);
        }

        // Gán adapter
        recyclerCameras.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerDevices.setLayoutManager(new LinearLayoutManager(this));

        CameraAdapter cameraAdapter = new CameraAdapter(cameraList);
        cameraAdapter.setOnCameraClickListener(new CameraAdapter.OnCameraClickListener() {
            @Override
            public void onClick(Device device) {
                Intent intent = new Intent(RoomDetailActivity.this, CameraControllerActivity.class);
                intent.putExtra("cam_id", device.getId());
                intent.putExtra("cam_name", device.getdeviceName());
                intent.putExtra("cam_macid", device.getdeviceMacId());
                startActivity(intent);
            }

            @Override
            public void onLongClick(Device device, View view) {
                // Hiện popup menu khi nhấn giữ
                PopupMenu popup = new PopupMenu(view.getContext(), view);
                popup.getMenuInflater().inflate(R.menu.longclick_options_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_edit) {

                        return true;
                    } else if (item.getItemId() == R.id.menu_delete) {

                        return true;
                    }
                    return false;
                });
                popup.show();
            }
        });
        recyclerCameras.setAdapter(cameraAdapter);


        DeviceAdapter deviceAdapter = new DeviceAdapter(otherDevices);
        deviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onClick(Device device) {
                Intent intent = new Intent(RoomDetailActivity.this, ChartDeviceActivity.class);
                intent.putExtra("device_id", device.getId());
                intent.putExtra("device_name", device.getdeviceName());
                intent.putExtra("device_macid", device.getdeviceMacId());
                startActivity(intent);
            }

            @Override
            public void onLongClick(Device device, View view) {
                // Hiện popup menu khi nhấn giữ
                PopupMenu popup = new PopupMenu(view.getContext(), view);
                popup.getMenuInflater().inflate(R.menu.longclick_options_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_edit) {

                        return true;
                    } else if (item.getItemId() == R.id.menu_delete) {

                        return true;
                    }
                    return false;
                });
                popup.show();
            }
        });
//        user\admin@pvl.com.vn\data
        recyclerDevices.setAdapter(deviceAdapter);
        mqttHelper = new MqttHelper(this);
        String clientId = "AndroidClient_" + System.currentTimeMillis();
        mqttHelper.connect("tcp://mqtt.pvl.com.vn:1883", clientId, savedUsername, savedPassword, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d("MQTT", "Kết nối MQTT thành công");

                // ✅ Sau khi kết nối mới được phép subscribe
                mqttHelper.subscribe(topicData, (topic, message) -> {
                    String payload = new String(message.getPayload());
                    Log.d("MQTT_RECEIVE", "Nhận được: " + payload);

                    String[] parts = payload.split(" ");
                    if (parts.length == 6) {
                        String timeOn = parts[0];
                        String timeOff = parts[1];
                        String status = parts[2];
                        float current = Float.parseFloat(parts[3]);
                        float voltage = Float.parseFloat(parts[4]);
                        float temperature = Float.parseFloat(parts[5]);

                        runOnUiThread(() -> {
                            for (int i = 0; i < otherDevices.size(); i++) {
                                Device device = otherDevices.get(i);
                                if (device.getId() == 2) {
                                    ChartEntry entry = new ChartEntry(timeOn, timeOff, status, current, voltage, temperature);
                                    device.setLatestEntry(entry);
                                    deviceAdapter.notifyItemChanged(i, "updateDataOnly");
                                    break;
                                }
                            }
                        });
                    }
                });
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Toast.makeText(RoomDetailActivity.this, "Kết nối MQTT thất bại", Toast.LENGTH_SHORT).show();
            }
        });



//        // 3. Lấy username từ SharedPreferences
//        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
//        savedUsername = prefs.getString("username", "");
//
//        topicCmd = "user/" + savedUsername + "/cmd";
//        topicData = "user/" + savedUsername + "/data";
//
//        // 4. Khởi tạo mqttHelper
//        mqttHelper = new MqttHelper(this);
//
//        // 5. Subscribe và xử lý phản hồi thiết bị
//        mqttHelper.subscribe(topicData, (topic, message) -> {
//            String payload = new String(message.getPayload());
//            Log.d("MQTT", "Received: " + payload);
//
//            try {
//                JSONObject json = new JSONObject(payload);
//                if (json.has("action") && json.getString("action").equals("get_devices")) {
//                    String resRoomId = json.getString("room_id");
//                    if (resRoomId.equals(roomId)) {
//                        JSONArray devicesArray = json.getJSONArray("devices");
//
//                        List<Device> normalDevices = new ArrayList<>();
//                        List<Device> cameraDevices = new ArrayList<>();
//
//                        for (int i = 0; i < devicesArray.length(); i++) {
//                            JSONObject obj = devicesArray.getJSONObject(i);
//                            Device device = new Device(
//                                    obj.getInt("device_id"),
//                                    obj.getString("device_macid"),
//                                    obj.getString("device_name"),
//                                    obj.getString("type")
//                            );
//
//                            if (device.getdeviceType().equals("camera")) {
//                                cameraDevices.add(device);
//                            } else {
//                                normalDevices.add(device);
//                            }
//                        }
//
//                        runOnUiThread(() -> {
//                            recyclerDevices.setAdapter(new DeviceAdapter(normalDevices));
//                            recyclerCameras.setAdapter(new CameraAdapter(cameraDevices));
//                            txtDeviceTitle.setText("Thiết bị trong phòng (" + normalDevices.size() + ")");
//                            txtCameraTitle.setText("Danh sách camera (" + cameraDevices.size() + ")");
//                        });
//                    }
//                }
//            } catch (JSONException e) {
//                Log.e("RoomDetail", "Parse Error: " + e.getMessage());
//            }
//        });
//
//        // 6. Gửi yêu cầu get_devices
//        JSONObject request = new JSONObject();
//        try {
//            request.put("action", "get_devices");
//            request.put("room_id", roomId);
//            mqttHelper.publish(topicCmd, request.toString());
//        } catch (JSONException e) {
//            Log.e("RoomDetail", "JSON Error: " + e.getMessage());
//        }
    }
}
