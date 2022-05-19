package com.moko.mknbplugjson.entity;


import java.io.Serializable;

public class MQTTConfig implements Serializable {
    public String host = "";
    public String port = "";
    public boolean cleanSession = true;
    public int connectMode;
    public int qos = 1;
    public int keepAlive = 60;
    public String clientId = "";
    public String username = "";
    public String password = "";
    public String caPath;
    public String clientKeyPath;
    public String clientCertPath;
    public boolean lwtEnable;
    public boolean lwtRetain;
    public int lwtQos = 1;
    public String lwtTopic;
    public String lwtPayload;
    public String topicSubscribe;
    public String topicPublish;
    public String deviceId = "";
    public String ntpUrl;
    public int timeZone;
    public String apn;
    public String apnUsername;
    public String apnPassword;
    public int networkPriority = 0;
    public boolean debugModeEnable;
}
