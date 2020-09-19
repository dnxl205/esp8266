package com.example.smartsocket;

public class Device {
    private String name;
    private String deviceStatus_text;
    private boolean deviceStatus_switch;

    public Device(String device_name,String deviceStatus_t,boolean deviceStatus_s){
        this.name = device_name;
        this.deviceStatus_text = deviceStatus_t;
        this.deviceStatus_switch = deviceStatus_s;
    }
    public String getName(){
        return name;
    }

    public String getDeviceStatus_text() {
        return deviceStatus_text;
    }

    public boolean getDeviceStatus_switch() {
        return deviceStatus_switch;
    }
}
