package com.moko.mknbplugjson.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.R2;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.dialog.BottomDialog;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtiles;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.DeviceType;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.NetConnectedStatus;
import com.moko.support.json.entity.NetConnectingStatus;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.entity.PowerProtectStatus;
import com.moko.support.json.entity.PowerStatus;
import com.moko.support.json.event.DeviceOnlineEvent;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;


public class IndicatorSettingActivity extends BaseActivity {


    @BindView(R2.id.iv_server_connecting)
    ImageView ivServerConnecting;
    @BindView(R2.id.tv_server_connected)
    TextView tvServerConnected;
    @BindView(R2.id.iv_indicator_status)
    ImageView ivIndicatorStatus;
    @BindView(R2.id.tv_indicator_color)
    TextView tvIndicatorColor;
    @BindView(R2.id.iv_protection_signal)
    ImageView ivProtectionSignal;
    private MQTTConfig appMqttConfig;
    private MokoDevice mMokoDevice;
    private Handler mHandler;
    private boolean mServerConnectingStatus;
    private int mServerConnectedSelected;
    private boolean mPowerStatus;
    private boolean mPowerProtectStatus;
    private ArrayList<String> mServerConnectedValues;
    private int mDeviceType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indicator_setting);
        ButterKnife.bind(this);
        mServerConnectedValues = new ArrayList<>();
        mServerConnectedValues.add("OFF");
        mServerConnectedValues.add("Solid blue for 5 seconds");
        mServerConnectedValues.add("Solid blue");
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getServerConnectingStatus();
        getServerConnectedStatus();
        getPowerStatus();
        getPowerProtectStatus();
        getPowerProtectStatus();
        getDeviceType();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        final String topic = event.getTopic();
        final String message = event.getMessage();
        if (TextUtils.isEmpty(message))
            return;
        MsgCommon<JsonObject> msgCommon;
        try {
            Type type = new TypeToken<MsgCommon<JsonObject>>() {
            }.getType();
            msgCommon = new Gson().fromJson(message, type);
        } catch (Exception e) {
            return;
        }
        if (!mMokoDevice.deviceId.equals(msgCommon.device_info.device_id)) {
            return;
        }
        mMokoDevice.isOnline = true;
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_NET_CONNECTING_STATUS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0)
                return;
            Type type = new TypeToken<NetConnectingStatus>() {
            }.getType();
            NetConnectingStatus connectingStatus = new Gson().fromJson(msgCommon.data, type);
            mServerConnectingStatus = connectingStatus.net_connecting == 1;
            ivServerConnecting.setImageResource(mServerConnectingStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_NET_CONNECTED_STATUS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0)
                return;
            Type type = new TypeToken<NetConnectedStatus>() {
            }.getType();
            NetConnectedStatus connectedStatus = new Gson().fromJson(msgCommon.data, type);
            mServerConnectedSelected = connectedStatus.net_connected;
            tvServerConnected.setText(mServerConnectedValues.get(mServerConnectedSelected));
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_POWER_SWITCH_STATUS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0)
                return;
            Type type = new TypeToken<PowerStatus>() {
            }.getType();
            PowerStatus powerStatus = new Gson().fromJson(msgCommon.data, type);
            mPowerStatus = powerStatus.power_switch == 1;
            ivIndicatorStatus.setImageResource(mPowerStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
            tvIndicatorColor.setVisibility(mPowerStatus ? View.VISIBLE : View.GONE);
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_POWER_PROTECT) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0)
                return;
            Type type = new TypeToken<PowerProtectStatus>() {
            }.getType();
            PowerProtectStatus powerProtectStatus = new Gson().fromJson(msgCommon.data, type);
            mPowerProtectStatus = powerProtectStatus.power_protect == 1;
            ivProtectionSignal.setImageResource(mPowerProtectStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_DEVICE_TYPE) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0)
                return;
            Type infoType = new TypeToken<DeviceType>() {
            }.getType();
            DeviceType deviceType = new Gson().fromJson(msgCommon.data, infoType);
            mDeviceType = deviceType.type;
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_NET_CONNECTING_STATUS
                || msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_NET_CONNECTED_STATUS
                || msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_POWER_SWITCH_STATUS
                || msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_POWER_PROTECT) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            ToastUtils.showToast(this, "Set up succeed");
            if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_NET_CONNECTING_STATUS)
                ivServerConnecting.setImageResource(mServerConnectingStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
            if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_NET_CONNECTED_STATUS)
                tvServerConnected.setText(mServerConnectedValues.get(mServerConnectedSelected));
            if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_POWER_SWITCH_STATUS) {
                ivIndicatorStatus.setImageResource(mPowerStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
                tvIndicatorColor.setVisibility(mPowerStatus ? View.VISIBLE : View.GONE);
            }
            if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_POWER_PROTECT)
                ivProtectionSignal.setImageResource(mPowerProtectStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVERLOAD_OCCUR
                || msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVER_VOLTAGE_OCCUR
                || msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_UNDER_VOLTAGE_OCCUR
                || msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVER_CURRENT_OCCUR) {
            Type infoType = new TypeToken<OverloadOccur>() {
            }.getType();
            OverloadOccur overloadOccur = new Gson().fromJson(msgCommon.data, infoType);
            if (overloadOccur.state == 1)
                finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        String deviceId = event.getDeviceId();
        if (!mMokoDevice.deviceId.equals(deviceId)) {
            return;
        }
        boolean online = event.isOnline();
        if (!online)
            finish();
    }

    public void back(View view) {
        finish();
    }


    private void getServerConnectingStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadNetConnectingStatus(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_NET_CONNECTING_STATUS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getServerConnectedStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadNetConnectedStatus(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_NET_CONNECTED_STATUS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getPowerStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadPowerStatus(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_POWER_SWITCH_STATUS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getPowerProtectStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadPowerProtectStatus(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_POWER_PROTECT, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    private void getDeviceType() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadDeviceType(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_DEVICE_TYPE, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setServerConnectingStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        NetConnectingStatus connectingStatus = new NetConnectingStatus();
        connectingStatus.net_connecting = mServerConnectingStatus ? 1 : 0;
        String message = MQTTMessageAssembler.assembleWriteNetConnectingStatus(deviceParams, connectingStatus);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_NET_CONNECTING_STATUS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setServerConnectedStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        NetConnectedStatus connectedStatus = new NetConnectedStatus();
        connectedStatus.net_connected = mServerConnectedSelected;
        String message = MQTTMessageAssembler.assembleWriteNetConnectedStatus(deviceParams, connectedStatus);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_NET_CONNECTED_STATUS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setPowerStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        PowerStatus powerStatus = new PowerStatus();
        powerStatus.power_switch = mPowerStatus ? 1 : 0;
        String message = MQTTMessageAssembler.assembleWritePowerStatus(deviceParams, powerStatus);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_POWER_SWITCH_STATUS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setPowerProtectStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        PowerProtectStatus powerProtectStatus = new PowerProtectStatus();
        powerProtectStatus.power_protect = mPowerProtectStatus ? 1 : 0;
        String message = MQTTMessageAssembler.assembleWritePowerProtectStatus(deviceParams, powerProtectStatus);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_POWER_PROTECT, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void onServerConnecting(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        mServerConnectingStatus = !mServerConnectingStatus;
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        setServerConnectingStatus();
    }

    public void onSelectServerConnected(View view) {
        if (isWindowLocked())
            return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mServerConnectedValues, mServerConnectedSelected);
        dialog.setListener(value -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            mServerConnectedSelected = value;
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            setServerConnectedStatus();
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onIndicatorStatus(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, IndicatorStatusActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE_TYPE, mDeviceType);
        startActivity(i);
    }


    public void onPowerStatus(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        mPowerStatus = !mPowerStatus;
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        setPowerStatus();
    }

    public void onProtectionSignal(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        mPowerProtectStatus = !mPowerProtectStatus;
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        setPowerProtectStatus();
    }
}
