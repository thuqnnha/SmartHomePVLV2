package com.example.sh;

public class Room {
    private String roomId;
    private String roomName;
    private int iconResId;

    public Room(String roomId, String roomName,int iconResId) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.iconResId = iconResId;
    }

    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public int getIconResId() { return iconResId; }
}

