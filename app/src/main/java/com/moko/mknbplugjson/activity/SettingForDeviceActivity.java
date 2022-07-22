package com.moko.mknbplugjson.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.R2;
import com.moko.mknbplugjson.base.BaseActivity;
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

import butterknife.BindView;
import butterknife.ButterKnife;


public class SettingForDeviceActivity extends BaseActivity {

    public static String TAG = SettingForDeviceActivity.class.getSimpleName();
    @BindView(R2.id.tv_type)
    TextView tvType;
    @BindView(R2.id.tv_host)
    TextView tvHost;
    @BindView(R2.id.tv_port)
    TextView tvPort;
    @BindView(R2.id.tv_client_id)
    TextView tvClientId;
    @BindView(R2.id.tv_user_name)
    TextView tvUserName;
    @BindView(R2.id.tv_password)
    TextView tvPassword;
    @BindView(R2.id.tv_clean_session)
    TextView tvCleanSession;
    @BindView(R2.id.tv_qos)
    TextView tvQos;
    @BindView(R2.id.tv_keep_alive)
    TextView tvKeepAlive;
    @BindView(R2.id.tv_lwt)
    TextView tvLwt;
    @BindView(R2.id.tv_lwt_retain)
    TextView tvLwtRetain;
    @BindView(R2.id.tv_lwt_qos)
    TextView tvLwtQos;
    @BindView(R2.id.tv_lwt_topic)
    TextView tvLwtTopic;
    @BindView(R2.id.tv_lwt_payload)
    TextView tvLwtPayload;
    @BindView(R2.id.tv_device_id)
    TextView tvDeviceId;
    @BindView(R2.id.tv_subscribe_topic)
    TextView tvSubscribeTopic;
    @BindView(R2.id.tv_publish_topic)
    TextView tvPublishTopic;

    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    public Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_for_device);
        ButterKnife.bind(this);
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
            tvHost.setText(mqttSettings.host);
            tvPort.setText(String.valueOf(mqttSettings.port));
            tvUserName.setText(mqttSettings.username);
            tvPassword.setText(mqttSettings.password);
            tvClientId.setText(mqttSettings.client_id);
            tvCleanSession.setText(mqttSettings.clean_session == 0 ? "NO" : "YES");
            tvQos.setText(String.valueOf(mqttSettings.qos));
            tvKeepAlive.setText(String.valueOf(mqttSettings.keepalive));
            tvDeviceId.setText(mMokoDevice.deviceId);

            if (mqttSettings.encryption_type == 0) {
                tvType.setText(getString(R.string.mqtt_connct_mode_tcp));
            } else {
                tvType.setText("SSL");
            }
            tvSubscribeTopic.setText(mqttSettings.subscribe_topic);
            tvPublishTopic.setText(mqttSettings.publish_topic);
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
            tvLwt.setText(String.valueOf(lwtSettings.lwt_enable));
            tvLwtRetain.setText(String.valueOf(lwtSettings.lwt_retain));
            tvLwtQos.setText(String.valueOf(lwtSettings.lwt_qos));
            tvLwtTopic.setText(lwtSettings.lwt_topic);
            tvLwtPayload.setText(String.valueOf(lwtSettings.lwt_message));
        }
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
//        String deviceId = event.getDeviceId();
//        if (!mMokoDevice.deviceId.equals(deviceId)) {
//            return;
//        }
//        boolean online = event.isOnline();
//        if (!online) {
//            finish();
//        }
//    }

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
        deviceParams.device_id = mMokoDevice.deviceId;
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
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadSettingsForLWT(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_LWT_SETTINGS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
