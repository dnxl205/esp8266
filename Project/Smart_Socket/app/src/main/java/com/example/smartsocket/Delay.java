package com.example.smartsocket;

public class Delay {
    private boolean delay_status;//是定时开启还是关闭
    private boolean isEffective;//是否有效
    private long timestamp;//时间戳
    private String delayDate;//预约日期
    private String delayTime;//预约时间

    public Delay(boolean delay_status,boolean isEffective,String delayDate,String delayTime,long timestamp){
        this.delay_status = delay_status;
        this.delayTime = delayTime;
        this.isEffective = isEffective;
        this.delayDate = delayDate;
        this.timestamp = timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setDelay_status(boolean delay_status) {
        this.delay_status = delay_status;
    }

    public void setDelayTime(String delayTime) {
        this.delayTime = delayTime;
    }

    public void setEffective(boolean effective) {
        isEffective = effective;
    }

    public void setDelayDate(String delayDate) {
        this.delayDate = delayDate;
    }

    public String getDelayTime() {
        return delayTime;
    }

    public boolean isDelay_status() {
        return delay_status;
    }

    public boolean isEffective() {
        return isEffective;
    }

    public String getDelayDate() {
        return delayDate;
    }
}
