package com.moko.support.json.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.support.json.MokoSupport;
import com.moko.support.json.entity.OrderCHAR;
import com.moko.support.json.entity.ParamsKeyEnum;

import java.io.File;
import java.io.FileInputStream;

import androidx.annotation.IntRange;

public class ParamsTask extends OrderTask {
    public byte[] data;

    public ParamsTask() {
        super(OrderCHAR.CHAR_PARAMS, OrderTask.RESPONSE_TYPE_WRITE);
    }

    @Override
    public byte[] assemble() {
        return data;
    }

    public void setData(ParamsKeyEnum key) {
        response.responseValue = data = new byte[]{
                (byte) 0xED,
                (byte) 0x00,
                (byte) key.getParamsKey(),
                (byte) 0x00
        };
    }

    public void setMqttHost(String host) {
        byte[] dataBytes = host.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_HOST.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setMqttPort(@IntRange(from = 1, to = 65535) int port) {
        byte[] dataBytes = MokoUtils.toByteArray(port, 2);
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_PORT.getParamsKey(),
                (byte) 0x02,
                dataBytes[0],
                dataBytes[1]
        };
    }

    public void setMqttUsername(String username) {
        byte[] dataBytes = username.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_USERNAME.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setMqttPassword(String password) {
        byte[] dataBytes = password.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_PASSWORD.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setMqttClientId(String clientId) {
        byte[] dataBytes = clientId.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_CLIENT_ID.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setMqttCleanSession(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_CLEAN_SESSION.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
    }

    public void setMqttKeepAlive(@IntRange(from = 10, to = 120) int keepAlive) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_KEEP_ALIVE.getParamsKey(),
                (byte) 0x01,
                (byte) keepAlive
        };
    }

    public void setMqttQos(@IntRange(from = 0, to = 2) int qos) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_QOS.getParamsKey(),
                (byte) 0x01,
                (byte) qos
        };
    }

    public void setMqttSubscribeTopic(String topic) {
        byte[] dataBytes = topic.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_SUBSCRIBE_TOPIC.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setMqttPublishTopic(String topic) {
        byte[] dataBytes = topic.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_PUBLISH_TOPIC.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setLwtEnable(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_LWT_ENABLE.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
    }

    public void setLwtQos(@IntRange(from = 0, to = 2) int qos) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_LWT_QOS.getParamsKey(),
                (byte) 0x01,
                (byte) qos
        };
    }

    public void setLwtRetain(@IntRange(from = 0, to = 1) int retain) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_LWT_RETAIN.getParamsKey(),
                (byte) 0x01,
                (byte) retain
        };
    }

    public void setLwtTopic(String topic) {
        byte[] dataBytes = topic.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_LWT_TOPIC.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setLwtPayload(String payload) {
        byte[] dataBytes = payload.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_LWT_PAYLOAD.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setMqttDeviceId(String deviceId) {
        byte[] dataBytes = deviceId.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_DEVICE_ID.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }


    public void setMqttConnectMode(@IntRange(from = 0, to = 2) int mode) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_CONNECT_MODE.getParamsKey(),
                (byte) 0x01,
                (byte) mode
        };
    }


    public void setNTPUrl(String url) {
        byte[] dataBytes = url.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_NTP_URL.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setNTPTimeZone(@IntRange(from = -24, to = 28) int timeZone) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_NTP_TIME_ZONE.getParamsKey(),
                (byte) 0x01,
                (byte) timeZone
        };
    }

    public void setApn(String apn) {
        byte[] dataBytes = apn.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_APN.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setApnUsername(String username) {
        byte[] dataBytes = username.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_APN_USERNAME.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setApnPassword(String password) {
        byte[] dataBytes = password.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_APN_PASSWORD.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
    }

    public void setNetworkPriority(@IntRange(from = 0, to = 10) int priority) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_NETWORK_PRIORITY.getParamsKey(),
                (byte) 0x01,
                (byte) priority
        };
    }

    public void setDataFormat(@IntRange(from = 0, to = 1) int format) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_DATA_FORMAT.getParamsKey(),
                (byte) 0x01,
                (byte) format
        };
    }

    public void testMode() {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_TEST_MODE.getParamsKey(),
                (byte) 0x01,
                (byte) 0x01
        };
    }

    public void setMode(@IntRange(from = 0, to = 1) int mode) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_CHANGE_MODE.getParamsKey(),
                (byte) 0x01,
                (byte) mode
        };
    }

    public void setFile(ParamsKeyEnum key, File file) throws Exception {
        FileInputStream inputSteam = new FileInputStream(file);
        dataBytes = new byte[(int) file.length()];
        inputSteam.read(dataBytes);
        dataLength = dataBytes.length;
        if (dataLength != 0) {
            if (dataLength % DATA_LENGTH_MAX > 0) {
                packetCount = dataLength / DATA_LENGTH_MAX + 1;
            } else {
                packetCount = dataLength / DATA_LENGTH_MAX;
            }
        } else {
            packetCount = 1;
        }
        remainPack = packetCount - 1;
        packetIndex = 0;
        delayTime = DEFAULT_DELAY_TIME + 500 * packetCount;
        if (packetCount > 1) {
            data = new byte[DATA_LENGTH_MAX + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) key.getParamsKey();
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) DATA_LENGTH_MAX;
            for (int i = 0; i < DATA_LENGTH_MAX; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        } else {
            data = new byte[dataLength + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) key.getParamsKey();
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) dataLength;
            for (int i = 0; i < dataLength; i++) {
                data[i + 6] = dataBytes[i];
            }
        }
    }

    public void setLongChar(ParamsKeyEnum key, String character) {
        dataBytes = character.getBytes();
        dataLength = dataBytes.length;
        if (dataLength != 0) {
            if (dataLength % DATA_LENGTH_MAX > 0) {
                packetCount = dataLength / DATA_LENGTH_MAX + 1;
            } else {
                packetCount = dataLength / DATA_LENGTH_MAX;
            }
        } else {
            packetCount = 1;
        }
        remainPack = packetCount - 1;
        packetIndex = 0;
        delayTime = DEFAULT_DELAY_TIME + 500 * packetCount;
        if (packetCount > 1) {
            data = new byte[DATA_LENGTH_MAX + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) key.getParamsKey();
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) DATA_LENGTH_MAX;
            for (int i = 0; i < DATA_LENGTH_MAX; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        } else {
            data = new byte[dataLength + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) key.getParamsKey();
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) dataLength;
            for (int i = 0; i < dataLength; i++) {
                data[i + 6] = dataBytes[i];
            }
        }
    }

    private int packetCount;
    private int packetIndex;
    private int remainPack;
    private int dataLength;
    private int dataOrigin;
    private byte[] dataBytes;
    private static final int DATA_LENGTH_MAX = 180;

    @Override
    public boolean parseValue(byte[] value) {
        final int header = value[0] & 0xFF;
        if (header == 0xED)
            return true;
        final int cmd = value[2] & 0xFF;
        if (value.length == 5) {
            final int result = value[4] & 0xFF;
            if (result == 1)
                return true;
        } else {
            remainPack--;
            packetIndex++;
            if (remainPack >= 0) {
                assembleRemainData(cmd);
                return false;
            }
        }
        return false;
    }

    private void assembleRemainData(int cmd) {
        int length = dataLength - dataOrigin;
        if (length > DATA_LENGTH_MAX) {
            data = new byte[DATA_LENGTH_MAX + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) cmd;
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) DATA_LENGTH_MAX;
            for (int i = 0; i < DATA_LENGTH_MAX; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        } else {
            data = new byte[length + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) cmd;
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) length;
            for (int i = 0; i < length; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        }
        MokoSupport.getInstance().sendDirectOrder(this);
    }
}
