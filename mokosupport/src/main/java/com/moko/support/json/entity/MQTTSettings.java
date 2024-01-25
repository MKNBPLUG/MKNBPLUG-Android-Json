package com.moko.support.json.entity;

public class MQTTSettings {
    public String host;
    public int port;
    public String username;
    public String password;
    public String client_id;
    public int clean_session = 1;
    public int keepalive = 60;
    public int qos = 1;
    public String subscribe_topic;
    public String publish_topic;
    public int encryption_type;
    public String ca_cert_url = "";
    public String client_cert_url = "";
    public String client_key_url = "";
}
