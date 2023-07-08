package com.moko.mknbplugjson.entity;


import java.io.Serializable;

public class MokoDevice implements Serializable {

    public int id;
    public String name;
    public String mac;
    public String mqttInfo;
    public String topicPublish;
    public String topicSubscribe;
    public boolean isOnline;
    public int deviceMode;
    public int deviceType;
    public boolean on_off;
    public boolean isOverload;
    public boolean isOverCurrent;
    public boolean isOverVoltage;
    public boolean isUnderVoltage;
    public int csq;//信号强度
}
