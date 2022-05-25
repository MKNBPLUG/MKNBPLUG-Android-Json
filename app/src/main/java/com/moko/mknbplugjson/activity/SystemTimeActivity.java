package com.moko.mknbplugjson.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.ble.lib.utils.MokoUtils;
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
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.entity.SystemTime;
import com.moko.support.json.entity.TimeZone;
import com.moko.support.json.event.DeviceOnlineEvent;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SystemTimeActivity extends BaseActivity {

    @BindView(R2.id.tv_time_zone)
    TextView tvTimeZone;
    @BindView(R2.id.tv_device_time)
    TextView tvDeviceTime;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    public Handler mHandler;
    public Handler mSyncTimeHandler;

    private ArrayList<String> mTimeZones;
    private int mSelectedTimeZone;
    private String mShowTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_time);
        ButterKnife.bind(this);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mTimeZones = new ArrayList<>();
        for (int i = 0; i <= 52; i++) {
            if (i < 24) {
                mTimeZones.add(String.format("UTC-%02d:%02d", (24 - i) / 2, ((i % 2 == 1) ? 30 : 00)));
            } else if (i == 24) {
                mTimeZones.add("UTC+00:00");
            } else {
                mTimeZones.add(String.format("UTC+%02d:%02d", (i - 24) / 2, ((i % 2 == 1) ? 30 : 00)));
            }
        }
        mHandler = new Handler(Looper.getMainLooper());
        mSyncTimeHandler = new Handler(Looper.getMainLooper());
        Calendar calendar = Calendar.getInstance();
        mShowTime = MokoUtils.calendar2strDate(calendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        mSelectedTimeZone = 40;
        tvDeviceTime.setText(String.format("Device time:%s %s", mShowTime, mTimeZones.get(mSelectedTimeZone)));
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getTimeZone();
        getSystemTime();
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
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_TIMEZONE) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0)
                return;
            Type infoType = new TypeToken<TimeZone>() {
            }.getType();
            TimeZone timeZone = new Gson().fromJson(msgCommon.data, infoType);
            mSelectedTimeZone = timeZone.time_zone + 24;
            tvTimeZone.setText(mTimeZones.get(mSelectedTimeZone));
            tvDeviceTime.setText(String.format("Device time:%s %s", mShowTime, mTimeZones.get(mSelectedTimeZone)));
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_SYSTEM_TIME) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0)
                return;
            Type infoType = new TypeToken<SystemTime>() {
            }.getType();
            SystemTime systemTime = new Gson().fromJson(msgCommon.data, infoType);
            int time = systemTime.time;
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time * 1000L);
            mShowTime = MokoUtils.calendar2strDate(calendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
            tvDeviceTime.setText(String.format("Device time:%s %s", mShowTime, mTimeZones.get(mSelectedTimeZone)));
            if (mSyncTimeHandler.hasMessages(0))
                mSyncTimeHandler.removeMessages(0);
            mSyncTimeHandler.postDelayed(() -> {
                getSystemTime();
            }, 30 * 1000);
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_TIMEZONE
                || msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_SYSTEM_TIME) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            ToastUtils.showToast(this, "Set up succeed");
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

    public void onBack(View view) {
        finish();
    }

    private void getTimeZone() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadTimeZone(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_TIMEZONE, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setTimeZone() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        TimeZone timeZone = new TimeZone();
        timeZone.time_zone = mSelectedTimeZone - 24;
        String message = MQTTMessageAssembler.assembleWriteTimeZone(deviceParams, timeZone);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_TIMEZONE, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void getSystemTime() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadSystemTime(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_SYSTEM_TIME, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setSystemTime() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        Calendar calendar = Calendar.getInstance();
        SystemTime systemTime = new SystemTime();
        systemTime.time = (int) (calendar.getTimeInMillis() / 1000);
        mShowTime = MokoUtils.calendar2strDate(calendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM);
        tvDeviceTime.setText(String.format("Device time:%s %s", mShowTime, mTimeZones.get(mSelectedTimeZone)));
        String message = MQTTMessageAssembler.assembleWriteSystemTime(deviceParams, systemTime);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_SYSTEM_TIME, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSelectTimeZoneClick(View view) {
        if (isWindowLocked())
            return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mTimeZones, mSelectedTimeZone);
        dialog.setListener(value -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            mSelectedTimeZone = value;
            tvTimeZone.setText(mTimeZones.get(mSelectedTimeZone));
            tvDeviceTime.setText(String.format("Device time:%s %s", mShowTime, mTimeZones.get(mSelectedTimeZone)));
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setTimeZone();
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onSyncClick(View view) {
        if (isWindowLocked())
            return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setSystemTime();
    }

    public void onSyncTimeFromNTPClick(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, SyncTimeFromNTPActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSyncTimeHandler.hasMessages(0))
            mSyncTimeHandler.removeMessages(0);
        if (mHandler.hasMessages(0))
            mHandler.removeMessages(0);
    }

}
