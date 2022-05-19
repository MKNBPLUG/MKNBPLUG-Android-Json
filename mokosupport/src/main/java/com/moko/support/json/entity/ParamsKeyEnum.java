package com.moko.support.json.entity;


import java.io.Serializable;

public enum ParamsKeyEnum implements Serializable {

    KEY_MQTT_HOST(0x31),
    KEY_MQTT_PORT(0x32),
    KEY_MQTT_USERNAME(0x33),
    KEY_MQTT_PASSWORD(0x34),
    KEY_MQTT_CLIENT_ID(0x35),
    KEY_MQTT_CLEAN_SESSION(0x36),
    KEY_MQTT_KEEP_ALIVE(0x37),
    KEY_MQTT_QOS(0x38),
    KEY_MQTT_SUBSCRIBE_TOPIC(0x39),
    KEY_MQTT_PUBLISH_TOPIC(0x3A),
    KEY_MQTT_LWT_ENABLE(0x3B),
    KEY_MQTT_LWT_QOS(0x3C),
    KEY_MQTT_LWT_RETAIN(0x3D),
    KEY_MQTT_LWT_TOPIC(0x3E),
    KEY_MQTT_LWT_PAYLOAD(0x3F),
    KEY_MQTT_DEVICE_ID(0x40),
    KEY_MQTT_CONNECT_MODE(0x41),
    KEY_MQTT_CA(0x42),
    KEY_MQTT_CLIENT_CERT(0x43),
    KEY_MQTT_CLIENT_KEY(0x44),
    KEY_NTP_URL(0x45),
    KEY_NTP_TIME_ZONE(0x46),
    KEY_APN(0x47),
    KEY_APN_USERNAME(0x48),
    KEY_APN_PASSWORD(0x49),
    KEY_NETWORK_PRIORITY(0x4A),
    KEY_DATA_FORMAT(0x4B),
    KEY_TEST_MODE(0x4C),
    KEY_CHANGE_MODE(0x4D),

    KEY_DEVICE_NAME(0x4E),
    KEY_DEVICE_MAC(0x4F),
    ;

    private int paramsKey;

    ParamsKeyEnum(int paramsKey) {
        this.paramsKey = paramsKey;
    }


    public int getParamsKey() {
        return paramsKey;
    }

    public static ParamsKeyEnum fromParamKey(int paramsKey) {
        for (ParamsKeyEnum paramsKeyEnum : ParamsKeyEnum.values()) {
            if (paramsKeyEnum.getParamsKey() == paramsKey) {
                return paramsKeyEnum;
            }
        }
        return null;
    }
}
