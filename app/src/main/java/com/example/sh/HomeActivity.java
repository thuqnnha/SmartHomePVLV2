package com.example.sh;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    private static final String MQTT_SERVER = "tcp://mqtt.pvl.com.vn:1883";

    private MqttHelper mqttHelper;
    private RecyclerView recyclerRooms, recyclerCamera;
    private RoomAdapter roomAdapter;
    private List<Room> roomList = new ArrayList<>();
    private SpeechRecognizer speechRecognizer;
    private TextView txtVoiceResult, txtCameraTitle;
    private ImageButton btnAddRoom, btnInformation;
    private ImageButton btnMicro;
    private String savedUsername,savedPassword;
    List<Device> cameraList = new ArrayList<>();
    CameraAdapter cameraAdapter = new CameraAdapter(cameraList);
    private TextView txtTemperature, txtWeatherComment,txtWindDirection, txtWindSpeed;
    private ImageView imgWeatherIcon;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private final String API_KEY = "c9613e0d043fd1cd2322f73b15d5339f"; // Lấy từ openweathermap.org
    private FusedLocationProviderClient fusedLocationClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        //----------------------------Ánh xạ view--------------------------------------
        recyclerRooms = findViewById(R.id.recyclerRooms);
        btnAddRoom = findViewById(R.id.btnAddRoom);
        txtVoiceResult = findViewById(R.id.txtVoiceResult);
        btnMicro = findViewById(R.id.btnMicro);
        btnInformation = findViewById(R.id.btnInformation);
        recyclerCamera = findViewById(R.id.recyclerCamera);
        recyclerRooms.setLayoutManager(new LinearLayoutManager(this));
        txtCameraTitle = findViewById(R.id.txtCameraTitle);

        txtTemperature = findViewById(R.id.txtTemperature);
        txtWindSpeed = findViewById(R.id.txtWindSpeed);
        txtWindDirection = findViewById(R.id.txtWindDirection);

        txtWeatherComment = findViewById(R.id.txtWeatherComment);
        imgWeatherIcon = findViewById(R.id.imgWeatherIcon);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Kiểm tra và yêu cầu quyền vị trí nếu cần
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocation();
        }

        loadCameraList();
        updateCameraSection();

        // Mô phỏng danh sách phòng
        roomList = new ArrayList<>();
        roomList.add(new Room("1", "Phòng khách", R.drawable.ic_living_room));
        roomList.add(new Room("2", "Phòng ngủ", R.drawable.ic_bedroom));
        roomList.add(new Room("3", "Phòng bếp", R.drawable.ic_kitchen));
        roomList.add(new Room("4", "Phòng tắm", R.drawable.ic_room));

        roomAdapter = new RoomAdapter(this, roomList);
        recyclerRooms.setAdapter(roomAdapter);


//        mqttHelper = new MqttHelper(this);
//        String clientId = "AndroidClient_" + System.currentTimeMillis();

//        mqttHelper.connect(MQTT_SERVER, clientId, savedUsername, savedPassword, new IMqttActionListener() {
//            @Override
//            public void onSuccess(IMqttToken asyncActionToken) {
//                Log.d("MQTT", "Connected");
//
//                String topicCmd = "user/" + savedUsername + "/cmd";
//                String topicData = "user/" + savedUsername + "/data";
//
//                mqttHelper.subscribe(topicData, (topic, message) -> {
//                    String payload = new String(message.getPayload());
//                    Log.d("MQTT", "Received: " + payload);
//                    parseRoomList(payload);
//                });
//
//                mqttHelper.publish(topicCmd, "{\"action\": \"get_rooms\"}");
//            }
//
//            @Override
//            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                Log.e("MQTT", "Failed to connect: " + exception.getMessage());
//            }
//        });

        roomAdapter.setOnRoomClickListener(new RoomAdapter.OnRoomClickListener() {
            @Override
            public void onClick(Room room) {
                // Khi click phòng -> mở form mới
                Intent intent = new Intent(HomeActivity.this, RoomDetailActivity.class);
                intent.putExtra("room_id", room.getRoomId());
                intent.putExtra("room_name", room.getRoomName());
                startActivity(intent);
            }

            @Override
            public void onLongClick(Room room, View view) {
                // Hiện popup menu khi nhấn giữ
                PopupMenu popup = new PopupMenu(HomeActivity.this, view);
                popup.getMenuInflater().inflate(R.menu.longclick_options_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_edit) {
                        // Gọi sửa phòng
                        //showEditRoomDialog(room);
                        return true;
                    } else if (item.getItemId() == R.id.menu_delete) {
                        //deleteRoom(room);
                        return true;
                    }
                    return false;
                });
                popup.show();
            }
        });

        cameraAdapter.setOnCameraClickListener(new CameraAdapter.OnCameraClickListener() {
            @Override
            public void onClick(Device device) {
                Intent intent = new Intent(HomeActivity.this, CameraControllerActivity.class);
                intent.putExtra("cam_id", device.getId());
                intent.putExtra("cam_name", device.getdeviceName());
                intent.putExtra("cam_macid", device.getdeviceMacId());;
                startActivity(intent);
            }

            @Override
            public void onLongClick(Device device, View view) {
                // Hiện popup menu khi nhấn giữ
                PopupMenu popup = new PopupMenu(view.getContext(), view);
                popup.getMenuInflater().inflate(R.menu.longclick_options_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_edit) {
                        int position = cameraList.indexOf(device);
                        if (position != -1) {
                            editCamera(device, position);
                        }
                        return true;
                    } else if (item.getItemId() == R.id.menu_delete) {
                        new AlertDialog.Builder(HomeActivity.this)
                                .setTitle("Xoá camera")
                                .setMessage("Bạn có chắc muốn xoá camera này?")
                                .setPositiveButton("Xoá", (dialog, which) -> {
                                    removeCamera(device);
                                })
                                .setNegativeButton("Huỷ", null)
                                .show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            }
        });

        recyclerCamera.setAdapter(cameraAdapter);

        btnAddRoom.setOnClickListener(v -> {
            View popupView = LayoutInflater.from(this).inflate(R.layout.popup_add_menu, null);
            PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

            // Bắt click
            popupView.findViewById(R.id.menuAddRoom).setOnClickListener(view -> {
                popupWindow.dismiss();
                // Mở activity thêm thiết bị
            });

            popupView.findViewById(R.id.menuAddCamera).setOnClickListener(view -> {
                popupWindow.dismiss();
                // Gợi ý đặt trong phương thức hoặc sự kiện onClick
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_camera, null);
                EditText etCameraName = dialogView.findViewById(R.id.etCameraName);
                EditText etSerial = dialogView.findViewById(R.id.etSerial);
                EditText etCameraNo = dialogView.findViewById(R.id.etCameraNo);
                EditText etVerifyCode = dialogView.findViewById(R.id.etVerifyCode);

                new AlertDialog.Builder(this)
                        .setTitle("Thêm camera mới")
                        .setView(dialogView)
                        .setPositiveButton("Thêm", (dialog, which) -> {
                            String name = etCameraName.getText().toString().trim();
                            String serial = etSerial.getText().toString().trim();
                            String cameraNo = etCameraNo.getText().toString().trim();
                            String verify = etVerifyCode.getText().toString().trim();

                            if (name.isEmpty() || serial.isEmpty() || cameraNo.isEmpty() || verify.isEmpty()) {
                                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            String macFormat = serial + " " + cameraNo + " " + verify;
                            Device newDevice = new Device(); // hoặc constructor
                            newDevice.setdeviceName(name);
                            newDevice.setdeviceMacId(macFormat);
                            newDevice.setdeviceType(0);

                            // Thêm vào adapter
                            cameraList.add(newDevice);
                            cameraAdapter.notifyItemInserted(cameraList.size() - 1);

                            updateCameraSection();

                            // Lưu thông tin camera vào SharedPreferences
                            SharedPreferences prefsCam = getSharedPreferences("camera_prefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefsCam.edit();
                            Gson gson = new Gson();
                            String json = gson.toJson(cameraList); // Chuyển danh sách thành chuỗi JSON
                            editor.putString("cameraList", json);
                            editor.apply();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();

            });

            popupView.findViewById(R.id.menuAddDevice).setOnClickListener(view -> {
                popupWindow.dismiss();
                // Mở activity thêm quick toggle
            });

            // Hiển thị gần nút thêm
            popupWindow.setElevation(10);
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupWindow.showAsDropDown(btnAddRoom, -150, 20); // Điều chỉnh vị trí cho đẹp
            //showAddRoomDialog();
        });

        btnInformation.setOnClickListener(v -> {
            View popupView = LayoutInflater.from(this).inflate(R.layout.popup_log_menu, null);
            PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

            // ✅ Nếu đã đăng nhập thì cập nhật giao diện
            SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
            if (isLoggedIn) {
                String savedUsername = prefs.getString("username", "");
                updateMqttLoginUI(popupView, savedUsername);
            }

            popupView.findViewById(R.id.menuLogInDb).setOnClickListener(view -> {
                popupWindow.dismiss();

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_login_db, null);
                builder.setView(dialogView);
                AlertDialog dialog = builder.create();
                dialog.show();
            });

            popupView.findViewById(R.id.menuLogInMqtt).setOnClickListener(view -> {
                showMqttLoginDialog(popupView, popupWindow);
            });

            popupView.findViewById(R.id.menuLogOut).setOnClickListener(view -> {
                popupWindow.dismiss();
                SharedPreferences.Editor editor = getSharedPreferences("loginPrefs", MODE_PRIVATE).edit();
                editor.clear();
                editor.apply();
                Toast.makeText(this, "Đã đăng xuất MQTT", Toast.LENGTH_SHORT).show();
            });

            popupWindow.setElevation(10);
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupWindow.showAsDropDown(btnInformation, 0, 20);
        });


        //Xử lý sự kiện khi bấm nút micro
        btnMicro.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return;
            }

            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Toast.makeText(this, "Thiết bị không hỗ trợ giọng nói", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Đang chờ giọng nói...", Toast.LENGTH_SHORT).show();
            startSpeechRecognition();
        });

    }
    private void showMqttLoginDialog(View popupView, PopupWindow popupWindow) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_login_mqtt, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        TextInputEditText editMqttName = dialogView.findViewById(R.id.editMqttName);
        TextInputEditText editUsernameMqtt = dialogView.findViewById(R.id.editUsernameMqtt);
        TextInputEditText editPassMqtt = dialogView.findViewById(R.id.editPassMqtt);

        // Load thông tin cũ
        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        editMqttName.setText(prefs.getString("serverUri", ""));
        editUsernameMqtt.setText(prefs.getString("username", ""));

        Button btnLogin = new Button(this);
        btnLogin.setText("Đăng nhập");
        ((LinearLayout) dialogView).addView(btnLogin);

        btnLogin.setOnClickListener(v -> {
            String serverUri = "tcp://" + editMqttName.getText().toString().trim() + ":1883";
            String username = editUsernameMqtt.getText().toString().trim();
            String password = editPassMqtt.getText().toString().trim();

            if (serverUri.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            String clientId = "AndroidClient_" + System.currentTimeMillis();
            MqttHelper mqttHelper = new MqttHelper(this);
            mqttHelper.connect(serverUri, clientId, username, password, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // ✅ Lưu trạng thái
                    SharedPreferences.Editor editor = getSharedPreferences("loginPrefs", MODE_PRIVATE).edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.putString("username", username);
                    editor.putString("password", password);
                    editor.putString("serverUri", serverUri);
                    editor.apply();

                    Toast.makeText(HomeActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                    // ✅ Cập nhật trực tiếp UI popup đang hiển thị
                    LinearLayout menuLogInMqtt = popupView.findViewById(R.id.menuLogInMqtt);
                    menuLogInMqtt.setBackgroundColor(Color.parseColor("#4CAF50"));

                    TextView textView = (TextView) menuLogInMqtt.getChildAt(1);
                    textView.setText(username);

                    // ✅ Bây giờ mới dismiss popup nếu cần
                    popupWindow.dismiss();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(HomeActivity.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    private void updateMqttLoginUI(View popupView, String username) {
        LinearLayout menuLogInMqtt = popupView.findViewById(R.id.menuLogInMqtt);

        if (menuLogInMqtt != null && menuLogInMqtt.getChildCount() >= 2) {
            menuLogInMqtt.setBackgroundColor(Color.parseColor("#4CAF50")); // Màu xanh
            TextView textView = (TextView) menuLogInMqtt.getChildAt(1);
            textView.setText(username);
        }
    }


    private void getLocation() {
        Log.d("WeatherDebug", "Gọi getLocation()");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Log.d("WeatherDebug", "Lấy được vị trí: " + location.getLatitude() + ", " + location.getLongitude());
                        getWeather(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.d("WeatherDebug", "Không lấy được vị trí (location == null)");
                        Toast.makeText(this, "Không thể lấy vị trí. Hãy thử lại sau.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("WeatherDebug", "Lỗi lấy vị trí: " + e.getMessage());
                    Toast.makeText(this, "Lỗi khi lấy vị trí.", Toast.LENGTH_SHORT).show();
                });
    }

    private void getWeather(double lat, double lon) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                "&longitude=" + lon + "&current_weather=true";

        Log.d("WeatherDebug", "URL gọi Open-Meteo: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject current = response.getJSONObject("current_weather");
                        double temp = current.getDouble("temperature");
                        int weatherCode = current.getInt("weathercode");
                        double windSpeed = current.getDouble("windspeed");
                        int windDir = current.getInt("winddirection");
                        int isDay = current.getInt("is_day");

                        txtWindSpeed.setText("Gió: " + windSpeed + " km/h");
                        txtWindDirection.setText("Hướng gió: " + windDir + "°");

                        txtTemperature.setText(String.format("%.1f°C", temp));
                        txtWeatherComment.setText(getWeatherComment(weatherCode, temp));

                        // đổi icon
                        int iconRes = getWeatherIconRes(weatherCode, isDay);
                        imgWeatherIcon.setImageResource(iconRes);

                    } catch (JSONException e) {
                        Log.e("WeatherDebug", "Lỗi JSON: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e("WeatherDebug", "Lỗi Open-Meteo: " + error.toString());
                    Toast.makeText(this, "Không lấy được dữ liệu thời tiết.", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }
    private int getWeatherIconRes(int code, int isDay) {
        if (isDay == 0) {
            // icon ban đêm
            if (code == 0) return R.drawable.ic_clear_night;
            if (code == 1 || code == 2) return R.drawable.ic_night_cloudy;
            if (code == 3) return R.drawable.ic_night_overcast;
            if (code >= 61 && code <= 67) return R.drawable.ic_night_rain;
            if (code >= 95) return R.drawable.ic_night_thunder;
            return R.drawable.ic_night;
        } else {
            // icon ban ngày
            if (code == 0) return R.drawable.ic_sunny;
            if (code == 1 || code == 2) return R.drawable.ic_partly_cloudy;
            if (code == 3) return R.drawable.ic_cloudy;
            if (code >= 61 && code <= 67) return R.drawable.ic_rain;
            if (code >= 95) return R.drawable.ic_thunder;
            return R.drawable.ic_sunny;
        }
    }
    private String getWeatherComment(int code, double temp) {
        String desc = "Thời tiết hiện tại";

        if (code == 0) desc = "Trời quang đãng";
        else if (code == 1 || code == 2) desc = "Trời có mây nhẹ";
        else if (code == 3) desc = "Nhiều mây";
        else if (code == 45 || code == 48) desc = "Có sương mù";
        else if (code >= 51 && code <= 57) desc = "Mưa phùn";
        else if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) desc = "Trời đang mưa";
        else if ((code >= 71 && code <= 77) || (code >= 85 && code <= 86)) desc = "Tuyết rơi";
        else if (code >= 95) desc = "Có dông lốc";

        if (temp > 30) desc += ", khá oi nóng";
        else if (temp < 15) desc += ", khá lạnh";

        return desc;
    }



    // Xử lý kết quả xin quyền
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Ứng dụng cần quyền vị trí để hoạt động", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void editCamera(Device device, int position) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_camera, null);

        EditText etCameraName = dialogView.findViewById(R.id.etCameraName);
        EditText etSerial = dialogView.findViewById(R.id.etSerial);
        EditText etCameraNo = dialogView.findViewById(R.id.etCameraNo);
        EditText etVerifyCode = dialogView.findViewById(R.id.etVerifyCode);

        // Đổ dữ liệu cũ ra
        etCameraName.setText(device.getdeviceName());
        String[] parts = device.getdeviceMacId().split(" ");
        if (parts.length == 3) {
            etSerial.setText(parts[0]);
            etCameraNo.setText(parts[1]);
            etVerifyCode.setText(parts[2]);
        }

        new AlertDialog.Builder(this)
                .setTitle("Sửa thông tin camera")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etCameraName.getText().toString().trim();
                    String serial = etSerial.getText().toString().trim();
                    String camNo = etCameraNo.getText().toString().trim();
                    String verify = etVerifyCode.getText().toString().trim();

                    if (name.isEmpty() || serial.isEmpty() || camNo.isEmpty() || verify.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String macFormat = serial + " " + camNo + " " + verify;

                    // Cập nhật lại đối tượng
                    device.setdeviceName(name);
                    device.setdeviceMacId(macFormat);

                    // Cập nhật adapter
                    cameraAdapter.notifyItemChanged(position);

                    // Lưu lại danh sách
                    SharedPreferences prefs = getSharedPreferences("camera_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    String json = new Gson().toJson(cameraList);
                    editor.putString("cameraList", json);
                    editor.apply();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void removeCamera(Device device) {
        int index = cameraList.indexOf(device);
        if (index != -1) {
            cameraList.remove(index);
            cameraAdapter.notifyItemRemoved(index);

            // Cập nhật SharedPreferences
            SharedPreferences prefsCam = getSharedPreferences("camera_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefsCam.edit();
            String json = new Gson().toJson(cameraList);
            editor.putString("cameraList", json);
            editor.apply();

            // Cập nhật lại phần hiển thị tiêu đề
            updateCameraSection();
        }
    }

    private void loadCameraList() {
        SharedPreferences prefs = getSharedPreferences("camera_prefs", MODE_PRIVATE);
        String json = prefs.getString("cameraList", null);

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Device>>() {}.getType();
            List<Device> savedList = gson.fromJson(json, type);

            if (savedList != null) {
                cameraList.addAll(savedList);
                cameraAdapter.notifyDataSetChanged();
            }
        }
    }
    private void updateCameraSection() {
        if (cameraList.isEmpty()) {
            txtCameraTitle.setVisibility(View.GONE);
            recyclerCamera.setVisibility(View.GONE);
        } else {
            txtCameraTitle.setVisibility(View.VISIBLE);
            recyclerCamera.setVisibility(View.VISIBLE);

            String title = "Camera giám sát (" + cameraList.size() + ")";
            txtCameraTitle.setText(title);
        }
    }

    private void parseRoomList(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray roomsArray = root.getJSONArray("rooms");

            List<Room> tempList = new ArrayList<>();
            for (int i = 0; i < roomsArray.length(); i++) {
                JSONObject roomObj = roomsArray.getJSONObject(i);
                String roomId = roomObj.getString("room_id");
                String roomName = roomObj.getString("room_name");
                int icon = getIconForRoom(roomName);
                tempList.add(new Room(roomId, roomName, icon));
            }

            runOnUiThread(() -> {
                roomList.clear();
                roomList.addAll(tempList);
                roomAdapter.notifyDataSetChanged();
            });
        } catch (Exception e) {
            Log.e("ParseJSON", "Error parsing room list: " + e.getMessage());
        }
    }
    private int getIconForRoom(String name) {
        name = name.toLowerCase();
        if (name.contains("khách")) return R.drawable.ic_living_room;
        if (name.contains("ngủ")) return R.drawable.ic_bedroom;
        if (name.contains("bếp")) return R.drawable.ic_kitchen;
        return R.drawable.ic_room; // default icon
    }

    private void startSpeechRecognition() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) {
                    Toast.makeText(HomeActivity.this, "Lỗi nhận giọng nói", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        //Toast.makeText(getContext(), "Ghi âm: " + spokenText, Toast.LENGTH_LONG).show();
                        // TODO: Xử lí
                        txtVoiceResult.setText(spokenText);

                        txtVoiceResult.postDelayed(() -> txtVoiceResult.setText("Nói gì đó..."), 5000);
                    }
                }
                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (partial != null && !partial.isEmpty()) {
                        String preview = partial.get(0);
                        //Toast.makeText(getContext(), "Bạn đang nói: " + preview, Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

//        // Giảm độ trễ phản hồi
//        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
//        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
//        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500);

        speechRecognizer.startListening(intent);
    }
    private void showAddRoomDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_room, null);

        TextInputEditText etRoomName = dialogView.findViewById(R.id.etRoomName);

        new AlertDialog.Builder(this)
                .setTitle("Thêm phòng")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = etRoomName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        try {
                            // Tìm ID cao nhất hiện có
                            int maxId = 0;
                            for (Room room : roomList) {
                                try {
                                    int id = Integer.parseInt(room.getRoomId());
                                    if (id > maxId) {
                                        maxId = id;
                                    }
                                } catch (NumberFormatException ignored) {}
                            }

                            int newId = maxId + 1;

                            JSONObject json = new JSONObject();
                            json.put("action", "add_room");
                            json.put("room_id", String.valueOf(newId));
                            json.put("room_name", newName);

                            String topicCmd = "user/" + savedUsername + "/cmd";

                            mqttHelper.publish(topicCmd, json.toString());
                            Toast.makeText(this, "Đã gửi yêu cầu thêm phòng", Toast.LENGTH_SHORT).show();
                            //
                            requestRoomList();
                        } catch (Exception e) {
                            Log.e("AddRoom", "JSON Error: " + e.getMessage());
                        }
                    } else {
                        Toast.makeText(this, "Tên phòng không được để trống", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditRoomDialog(Room room) {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_room, null);

        TextInputEditText etRoomName = dialogView.findViewById(R.id.etRoomName);
        etRoomName.setText(room.getRoomName());

        new AlertDialog.Builder(this)
                .setTitle("Sửa phòng")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = etRoomName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        try {
                            JSONObject json = new JSONObject();
                            json.put("action", "update_room");
                            json.put("room_id", room.getRoomId());
                            json.put("room_name", newName);

//                            SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
//                            String savedUsername = prefs.getString("username", "");
                            String topicCmd = "user/" + savedUsername + "/cmd";

                            mqttHelper.publish(topicCmd, json.toString());
                            Toast.makeText(this, "Đã gửi yêu cầu sửa phòng", Toast.LENGTH_SHORT).show();
                            //
                            requestRoomList();
                        } catch (Exception e) {
                            Log.e("EditRoom", "JSON Error: " + e.getMessage());
                        }
                    } else {
                        Toast.makeText(this, "Tên phòng không được để trống", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    private void deleteRoom(Room room) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa phòng \"" + room.getRoomName() + "\" không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("action", "delete_room");
                        json.put("room_id", room.getRoomId());

                        String topicCmd = "user/" + savedUsername + "/cmd";
                        mqttHelper.publish(topicCmd, json.toString());

                        Toast.makeText(this, "Đã gửi yêu cầu xóa phòng", Toast.LENGTH_SHORT).show();
                        //
                        requestRoomList();
                    } catch (Exception e) {
                        Log.e("DeleteRoom", "Lỗi JSON: " + e.getMessage());
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    private void requestRoomList() {
        String topicCmd = "user/" + savedUsername + "/cmd";
        mqttHelper.publish(topicCmd, "{\"action\": \"get_rooms\"}");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mqttHelper.disconnect();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}

