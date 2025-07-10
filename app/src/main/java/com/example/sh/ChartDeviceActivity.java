package com.example.sh;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChartDeviceActivity extends AppCompatActivity {

    private LineChart voltageChart, currentChart, temperatureChart;
    private TextView voltageValue, currentValue, temperatureValue;

    private List<ChartEntry> chartData = new ArrayList<>();
    private MqttHelper mqttHelper;

    private String topicData;
    private String savedUsername, savedPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_device);

        // 1. Ánh xạ View
        voltageChart = findViewById(R.id.voltageChart);
        currentChart = findViewById(R.id.currentChart);
        temperatureChart = findViewById(R.id.temperatureChart);

        voltageValue = findViewById(R.id.voltageValue);
        currentValue = findViewById(R.id.currentValue);
        temperatureValue = findViewById(R.id.temperatureValue);

        // 2. Lấy username & password từ SharedPreferences
        savedUsername = getSharedPreferences("loginPrefs", MODE_PRIVATE).getString("username", "");
        savedPassword = getSharedPreferences("loginPrefs", MODE_PRIVATE).getString("password", "");

        topicData = "user/" + savedUsername + "/data";

        // 3. Kết nối MQTT và subscribe
        mqttHelper = new MqttHelper(this);
        String clientId = "AndroidClient_" + System.currentTimeMillis();
        mqttHelper.connect("tcp://mqtt.pvl.com.vn:1883", clientId, savedUsername, savedPassword, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d("MQTT", "Kết nối MQTT thành công");

                mqttHelper.subscribe(topicData, (topic, message) -> {
                    String payload = new String(message.getPayload());
                    Log.d("MQTT_RECEIVE", "Nhận được: " + payload);
                    handleMqttMessage(payload);
                });
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e("MQTT", "Kết nối MQTT thất bại: " + exception.getMessage());
            }
        });
    }

    private void handleMqttMessage(String payload) {
        String[] parts = payload.split(" ");
        if (parts.length == 6) {
            String timeOn = parts[0];
            String timeOff = parts[1];
            String status = parts[2];
            float current = Float.parseFloat(parts[3]);
            float voltage = Float.parseFloat(parts[4]);
            float temperature = Float.parseFloat(parts[5]);

            ChartEntry entry = new ChartEntry(timeOn, timeOff, status, current, voltage, temperature);

            runOnUiThread(() -> {
                chartData.add(entry);

                updateLineChart(voltageChart, chartData, "voltage");
                updateLineChart(currentChart, chartData, "current");
                updateLineChart(temperatureChart, chartData, "temperature");

                voltageValue.setText(String.format(Locale.getDefault(), "%.0f V", voltage));
                currentValue.setText(String.format(Locale.getDefault(), "%.3f A", current));
                temperatureValue.setText(String.format(Locale.getDefault(), "%.1f °C", temperature));
            });
        }
    }

    private void updateLineChart(LineChart chart, List<ChartEntry> data, String type) {
        List<Entry> entries = new ArrayList<>();
        int dataSize = data.size();
        int startIndex = Math.max(0, dataSize - 100);

        for (int i = startIndex; i < dataSize; i++) {
            float value = 0f;
            if (type.equals("voltage")) {
                value = data.get(i).getVoltage();
            } else if (type.equals("current")) {
                value = data.get(i).getCurrent();
            } else if (type.equals("temperature")) {
                value = data.get(i).getTemperature();
            }
            entries.add(new Entry(i - startIndex, value));
        }

        LineDataSet dataSet = new LineDataSet(entries, type.toUpperCase());
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(1f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        int color = R.color.black;  // mặc định
        if (type.equals("voltage")) {
            color = R.color.voltage_accent;
        } else if (type.equals("current")) {
            color = R.color.current_accent;
        } else if (type.equals("temperature")) {
            color = R.color.temperature_accent;
        }


        int resolvedColor = ContextCompat.getColor(this, color);
        dataSet.setColor(resolvedColor);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(100);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // Cấu hình chart
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setTouchEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawLabels(false);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(-1f);
        xAxis.setAxisLineWidth(1f);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setDrawGridLines(false);
        yAxis.setAxisLineWidth(1f);
        yAxis.setTextColor(Color.BLACK);
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.2f", value);
            }
        });

        float latestValue = entries.get(entries.size() - 1).getY();
        LimitLine limitLine = new LimitLine(latestValue);
        limitLine.setLineColor(resolvedColor);
        limitLine.setLineWidth(1.2f);
        limitLine.setLabel("");
        limitLine.enableDashedLine(8f, 4f, 0f);

        yAxis.removeAllLimitLines();
        yAxis.addLimitLine(limitLine);

        chart.setExtraBottomOffset(10f);
        chart.invalidate();
    }
}
