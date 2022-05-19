package com.moko.support.json.entity;

public class MsgCommon<T> {
    public int msg_id;
    public DeviceParams device_info;
    public T data;
    public int result_code;
    public int result_msg;
}
