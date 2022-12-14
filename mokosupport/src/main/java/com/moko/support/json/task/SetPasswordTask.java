package com.moko.support.json.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.json.entity.OrderCHAR;

public class SetPasswordTask extends OrderTask {
    public byte[] data;

    public SetPasswordTask() {
        super(OrderCHAR.CHAR_PASSWORD, OrderTask.RESPONSE_TYPE_WRITE);
    }

    public void setData(String password) {
        this.data = new byte[12];
        byte[] passwordBytes = password.getBytes();
        int length = passwordBytes.length;
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) 0x01;
        data[3] = (byte) length;
        for (int i = 0; i < length; i++) {
            data[i + 4] = passwordBytes[i];
        }
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
