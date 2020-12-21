package com.example.smartsocket;

public class Device {
    private String imei;
    private String name;
    private String deviceStatus_text;
    private boolean deviceStatus_switch;

    public Device(String m_imei,String device_name,String deviceStatus_t,boolean deviceStatus_s){
        this.imei = m_imei;
        this.name = device_name;
        this.deviceStatus_text = deviceStatus_t;
        this.deviceStatus_switch = deviceStatus_s;
    }

    public String getImei() {
        return imei;
    }

    public String getName(){
        return name;
    }

    public String getDeviceStatus_text() {
        return deviceStatus_text;
    }

    public void setDeviceStatus_text(String statusText){
        this.deviceStatus_text = statusText;
    }

    public void setDeviceStatus_switch(boolean statusSwitch){
        this.deviceStatus_switch = statusSwitch;
    }

    public boolean getDeviceStatus_switch() {
        return deviceStatus_switch;
    }
}
