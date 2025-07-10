package com.example.sh;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;
import java.util.Locale;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<Device> deviceList;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onClick(Device device);
        void onLongClick(Device device, View view);
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public DeviceAdapter(List<Device> deviceList) {
        this.deviceList = deviceList;
    }


    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);

        holder.tvDeviceName.setText(device.getdeviceName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(device);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(device, v);
            return true;
        });

        // Sự kiện nhấn nút
        holder.btnOff.setOnClickListener(v ->
                Toast.makeText(v.getContext(), device.getdeviceName() + ": OFF", Toast.LENGTH_SHORT).show());

        holder.btnOn.setOnClickListener(v ->
                Toast.makeText(v.getContext(), device.getdeviceName() + ": ON", Toast.LENGTH_SHORT).show());

        holder.btnAuto.setOnClickListener(v -> {
            boolean isVisible = holder.layoutAutoSettings.getVisibility() == View.VISIBLE;
            holder.layoutAutoSettings.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        });

        ChartEntry latest = device.getLatestEntry();

        if (latest != null) {
            Log.d("DeviceAdapter", "status=" + latest.getStatus() + ", current=" + latest.getCurrent());
            // Hiển thị thời gian bật/tắt
            holder.tvTimeRange.setText(String.format("%s\n%s", latest.gettimeOn(), latest.gettimeOff()));

            // Hiển thị thông số kỹ thuật
            holder.tvParams.setText(String.format(Locale.getDefault(),
                    "%.3fA\n%.0fV\n%.1f°C",
                    latest.getCurrent(), latest.getVoltage(), latest.getTemperature()));

            // Cập nhật trạng thái toggle + màu sắc
            String status = latest.getStatus();
            final ColorStateList red = ColorStateList.valueOf(Color.parseColor("#F44336"));
            final ColorStateList green = ColorStateList.valueOf(Color.parseColor("#4CAF50"));
            final ColorStateList orange = ColorStateList.valueOf(Color.parseColor("#FF9800"));
            final ColorStateList gray = ColorStateList.valueOf(Color.parseColor("#F5F5F5"));

            // Set màu và chọn trạng thái
            holder.btnOff.setBackgroundTintList("OFF".equals(status) ? red : gray);
            holder.btnOn.setBackgroundTintList("ON".equals(status) ? green : gray);
            holder.btnAuto.setBackgroundTintList("ON/OFF".equals(status) ? orange : gray);

            // Đổi trạng thái toggleGroup (không animation)
            holder.toggleGroup.clearChecked();  // Xóa trước tránh sai trạng thái tạm thời
            if ("OFF".equals(status)) holder.toggleGroup.check(holder.btnOff.getId());
            else if ("ON".equals(status)) holder.toggleGroup.check(holder.btnOn.getId());
            else if ("ON/OFF".equals(status)) holder.toggleGroup.check(holder.btnAuto.getId());
        }

    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName, tvTimeRange, tvParams;
        MaterialButton btnOn, btnOff, btnAuto;
        MaterialButtonToggleGroup toggleGroup;
        LinearLayout layoutAutoSettings;
        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvTimeRange = itemView.findViewById(R.id.tvTimeRange);
            tvParams = itemView.findViewById(R.id.tvParams);
            toggleGroup = itemView.findViewById(R.id.toggleGroup);
            btnOn = itemView.findViewById(R.id.btnOn);
            btnOff = itemView.findViewById(R.id.btnOff);
            btnAuto = itemView.findViewById(R.id.btnAuto);
            layoutAutoSettings = itemView.findViewById(R.id.layoutAutoSettings);
        }
    }
}
