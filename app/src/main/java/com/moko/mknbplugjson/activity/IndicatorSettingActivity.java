package com.moko.mknbplugjson.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityIndicatorSettingBinding;
import com.moko.mknbplugjson.dialog.BottomDialog;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.DeviceStandard;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.NetConnectedStatus;
import com.moko.support.json.entity.NetConnectingStatus;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.entity.PowerProtectStatus;
import com.moko.support.json.entity.PowerStatus;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class IndicatorSettingActivity extends BaseActivity<ActivityIndicatorSettingBinding> {
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
    protected void onCreate() {
        mServerConnectedValues = new ArrayList<>();
        mServerConnectedValues.add("OFF");
        mServerConnectedValues.add("Solid blue for 5 seconds");
        mServerConnectedValues.add("Solid blue");
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getServerConnectingStatus();
        getServerConnectingLedStatus();
        //电源输出指示灯开关状态
        getPowerOutputStatus();
        getPowerProtectStatus();
        getDeviceType();
    }

    @Override
    protected ActivityIndicatorSettingBinding getViewBinding() {
        return ActivityIndicatorSettingBinding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        final String message = event.getMessage();
        if (TextUtils.isEmpty(message)) return;
        MsgCommon<JsonObject> msgCommon;
        try {
            Type type = new TypeToken<MsgCommon<JsonObject>>() {
            }.getType();
            msgCommon = new Gson().fromJson(message, type);
        } catch (Exception e) {
            return;
        }
        if (!mMokoDevice.mac.equalsIgnoreCase(msgCommon.device_info.mac)) {
            return;
        }
        mMokoDevice.isOnline = true;
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_NET_CONNECTING_STATUS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) return;
            Type type = new TypeToken<NetConnectingStatus>() {
            }.getType();
            NetConnectingStatus connectingStatus = new Gson().fromJson(msgCommon.data, type);
            mServerConnectingStatus = connectingStatus.net_connecting == 1;
            mBind.ivServerConnecting.setImageResource(mServerConnectingStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_NET_CONNECTED_STATUS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) return;
            Type type = new TypeToken<NetConnectedStatus>() {
            }.getType();
            NetConnectedStatus connectedStatus = new Gson().fromJson(msgCommon.data, type);
            mServerConnectedSelected = connectedStatus.net_connected;
            mBind.tvServerConnected.setText(mServerConnectedValues.get(mServerConnectedSelected));
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_POWER_SWITCH_STATUS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) return;
            Type type = new TypeToken<PowerStatus>() {
            }.getType();
            PowerStatus powerStatus = new Gson().fromJson(msgCommon.data, type);
            mPowerStatus = powerStatus.power_switch == 1;
            mBind.ivPowerOutput.setImageResource(mPowerStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
            mBind.layoutIndicatorColor.setVisibility(mPowerStatus ? View.VISIBLE : View.GONE);
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_POWER_PROTECT) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) return;
            Type type = new TypeToken<PowerProtectStatus>() {
            }.getType();
            PowerProtectStatus powerProtectStatus = new Gson().fromJson(msgCommon.data, type);
            mPowerProtectStatus = powerProtectStatus.power_protect == 1;
            mBind.ivPowerInputStatus.setImageResource(mPowerProtectStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        }
        //deviceType
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_DEVICE_STANDARD) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) return;
            Type infoType = new TypeToken<DeviceStandard>() {
            }.getType();
            DeviceStandard deviceType = new Gson().fromJson(msgCommon.data, infoType);
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
                mBind.ivServerConnecting.setImageResource(mServerConnectingStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
            if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_NET_CONNECTED_STATUS)
                mBind.tvServerConnected.setText(mServerConnectedValues.get(mServerConnectedSelected));
            if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_POWER_SWITCH_STATUS) {
                mBind.ivPowerOutput.setImageResource(mPowerStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
                mBind.layoutIndicatorColor.setVisibility(mPowerStatus ? View.VISIBLE : View.GONE);
            }
            if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_POWER_PROTECT)
                mBind.ivPowerInputStatus.setImageResource(mPowerProtectStatus ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVERLOAD_OCCUR
                || msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVER_VOLTAGE_OCCUR
                || msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_UNDER_VOLTAGE_OCCUR
                || msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVER_CURRENT_OCCUR) {
            Type infoType = new TypeToken<OverloadOccur>() {
            }.getType();
            OverloadOccur overloadOccur = new Gson().fromJson(msgCommon.data, infoType);
            if (overloadOccur.state == 1) finish();
        }
    }

    public void onBack(View view) {
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
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadNetConnectingStatus(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_NET_CONNECTING_STATUS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getServerConnectingLedStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadNetConnectedStatus(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_NET_CONNECTED_STATUS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getPowerOutputStatus() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
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
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadDeviceType(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_DEVICE_STANDARD, appMqttConfig.qos);
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
        if (isWindowLocked()) return;
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
        if (isWindowLocked()) return;
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

    public void onIndicatorColor(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, IndicatorColorActivity.class);
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
        if (isWindowLocked()) return;
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
