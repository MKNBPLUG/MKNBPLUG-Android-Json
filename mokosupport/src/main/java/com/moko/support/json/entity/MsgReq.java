package com.moko.support.json.entity;

public class MsgReq<T> {
    public int msg_id;
    public DeviceParams device_info;
    public T data;
}
