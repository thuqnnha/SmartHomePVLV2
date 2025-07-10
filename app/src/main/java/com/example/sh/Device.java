package com.example.sh;
import java.io.Serializable;

public class Device {
    private int deviceId;
    private String deviceMacId;
    private String deviceName;
    private int deviceType;
    private ChartEntry latestEntry;
    public Device() {

    }

    public ChartEntry getLatestEntry() {
        return latestEntry;
    }

    public void setLatestEntry(ChartEntry latestEntry) {
        this.latestEntry = latestEntry;
    }
    // Constructor
    public Device(int deviceId, String deviceMacId, String deviceName, int deviceType) {
        this.deviceId = deviceId;
        this.deviceMacId = deviceMacId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
    }

    // Getter
    public int getId() {
        return deviceId;
    }

    public String getdeviceMacId() {
        return deviceMacId;
    }

    public String getdeviceName() {
        return deviceName;
    }
    public int getdeviceType() {
        return deviceType;
    }

    // Setter (tùy chọn nếu bạn cần chỉnh sửa)
    public void setId(int deviceId) {
        this.deviceId = deviceId;
    }

    public void setdeviceMacId(String deviceMacId) {
        this.deviceMacId = deviceMacId;
    }

    public void setdeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    public void setdeviceType(int deviceType) {
        this.deviceType = deviceType;
    }


}

