package com.moko.support.json.entity;

public class LWTSettings {
    public int lwt_enable;
    public int lwt_qos = 1;
    public int lwt_retain;
    public String lwt_topic = "{device_name}/{device_id}/device_to_app";
    public String lwt_message = "Offline";
}
