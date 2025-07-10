package com.example.sh;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

public class LoginMqttActivity extends AppCompatActivity  {
    private EditText  editUsername, editPass;
    private Button btnLogin;
    private TextView txtRegister;
    private MqttHelper mqttHelper;
    private final String serverUri = "tcp://mqtt.pvl.com.vn:1883";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_mqtt);
        //-------------------------------------Ánh xạ view--------------------------------------
        editUsername = findViewById(R.id.editUsername);
        editPass = findViewById(R.id.editPass);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);

        mqttHelper = new MqttHelper(this);

        //Kiểm tra trạng thái đã đăng nhập
        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            String savedUsername = prefs.getString("username", "");
            String savedPassword = prefs.getString("password", "");
            String clientId = "AndroidClient_" + System.currentTimeMillis();

            MqttHelper mqttHelper = new MqttHelper(this);
            mqttHelper.connect("tcp://mqtt.pvl.com.vn:1883", clientId, savedUsername, savedPassword, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Lưu trạng thái đăng nhập
                    SharedPreferences.Editor editor = getSharedPreferences("loginPrefs", MODE_PRIVATE).edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.putString("username", savedUsername);
                    editor.putString("password", savedPassword);
                    editor.apply();

                    Toast.makeText(LoginMqttActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginMqttActivity.this, HomeActivity.class));
                    finish();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(LoginMqttActivity.this, "Tự động đăng nhập thất bại", Toast.LENGTH_SHORT).show();
                }
            });
        }

        //Hiển thị thông báo nếu có
        String message = getIntent().getStringExtra("success_message");
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }

        //Xử lý đăng nhập
        btnLogin.setOnClickListener(v -> {
            String user = editUsername.getText().toString().trim();
            String pass = editPass.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            String clientId = "AndroidClient_" + System.currentTimeMillis();

            mqttHelper.connect(serverUri, clientId, user, pass, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Lưu thông tin đăng nhập
                    SharedPreferences.Editor editor = getSharedPreferences("loginPrefs", MODE_PRIVATE).edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.putString("username", user);
                    editor.putString("password", pass);
                    editor.apply();

                    Toast.makeText(LoginMqttActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginMqttActivity.this, HomeActivity.class));
                    finish();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(LoginMqttActivity.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                    Log.e("MQTT", "Lỗi kết nối: " + exception.getMessage());
                }
            });
        });

        //Chuyển sang đăng ký
        txtRegister.setOnClickListener(v -> {
//            Intent intent = new Intent(LoginDbActivity.this, RegisterActivity.class);
//            startActivity(intent);
        });
    }
}
