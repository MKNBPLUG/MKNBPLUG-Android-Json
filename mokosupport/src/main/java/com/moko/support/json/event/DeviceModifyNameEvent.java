package com.moko.support.json.event;

public class DeviceModifyNameEvent {

    private String mac;
    private String name;

    public DeviceModifyNameEvent(String mac) {
        this.mac = mac;
    }

    public String getMac() {
        return mac;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
