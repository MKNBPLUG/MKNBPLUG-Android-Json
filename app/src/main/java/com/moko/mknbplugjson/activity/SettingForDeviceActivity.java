package com.moko.mknbplugjson.activity;

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
import com.moko.mknbplugjson.databinding.ActivitySettingForDeviceBinding;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.LWTSettings;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MQTTSettings;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class SettingForDeviceActivity extends BaseActivity<ActivitySettingForDeviceBinding> {
    public static String TAG = SettingForDeviceActivity.class.getSimpleName();
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    public Handler mHandler;

    @Override
    protected void onCreate() {
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);

        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getSettingForDevice();
        getSettingForLWT();
    }

    @Override
    protected ActivitySettingForDeviceBinding getViewBinding() {
        return ActivitySettingForDeviceBinding.inflate(getLayoutInflater());
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
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_DEVICE_SETTINGS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                return;
            }
            Type infoType = new TypeToken<MQTTSettings>() {
            }.getType();
            MQTTSettings mqttSettings = new Gson().fromJson(msgCommon.data, infoType);
            mBind.tvHost.setText(mqttSettings.host);
            mBind.tvPort.setText(String.valueOf(mqttSettings.port));
            mBind.tvUserName.setText(mqttSettings.username);
            mBind.tvPassword.setText(mqttSettings.password);
            mBind.tvClientId.setText(mqttSettings.client_id);
            mBind.tvCleanSession.setText(mqttSettings.clean_session == 0 ? "NO" : "YES");
            mBind.tvQos.setText(String.valueOf(mqttSettings.qos));
            mBind.tvKeepAlive.setText(String.valueOf(mqttSettings.keepalive));
            mBind.tvDeviceId.setText(mMokoDevice.deviceId);

            if (mqttSettings.encryption_type == 0) {
                mBind.tvType.setText(getString(R.string.mqtt_connct_mode_tcp));
            } else {
                mBind.tvType.setText("SSL");
            }
            mBind.tvSubscribeTopic.setText(mqttSettings.subscribe_topic);
            mBind.tvPublishTopic.setText(mqttSettings.publish_topic);
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_LWT_SETTINGS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                return;
            }
            Type infoType = new TypeToken<LWTSettings>() {
            }.getType();
            LWTSettings lwtSettings = new Gson().fromJson(msgCommon.data, infoType);
            mBind.tvLwt.setText(String.valueOf(lwtSettings.lwt_enable));
            mBind.tvLwtRetain.setText(String.valueOf(lwtSettings.lwt_retain));
            mBind.tvLwtQos.setText(String.valueOf(lwtSettings.lwt_qos));
            mBind.tvLwtTopic.setText(lwtSettings.lwt_topic);
            mBind.tvLwtPayload.setText(String.valueOf(lwtSettings.lwt_message));
        }
    }

    public void onBack(View view) {
        finish();
    }

    private void getSettingForDevice() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadSettingsForDevice(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_DEVICE_SETTINGS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getSettingForLWT() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadSettingsForLWT(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_LWT_SETTINGS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
