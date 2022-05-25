package com.moko.support.json.entity;

public class MQTTSettings {

    public String host;
    public int port;
    public String username;
    public String password;
    public String client_id;
    public int clean_session = 1;
    public int keepalive = 60;
    public int qos = 0;
    public String subscribe_topic;
    public String publish_topic;
    public int encryption_type;
    public String cert_host = "";
    public int cert_port;
    public String ca_cert_path = "";
    public String client_cert_path = "";
    public String client_key_path = "";
}
