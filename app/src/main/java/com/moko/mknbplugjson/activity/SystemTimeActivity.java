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
import com.moko.mknbplugjson.databinding.ActivitySystemTimeBinding;
import com.moko.mknbplugjson.dialog.BottomDialog;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.mknbplugjson.utils.Utils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.DeviceTimeZone;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.entity.SystemTime;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;

public class SystemTimeActivity extends BaseActivity<ActivitySystemTimeBinding> {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    public Handler mHandler;
    public Handler mSyncTimeHandler;
    private ArrayList<String> mTimeZones;
    private int mSelectedTimeZone;

    @Override
    protected void onCreate() {
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
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
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getTimeZone();
    }

    @Override
    protected ActivitySystemTimeBinding getViewBinding() {
        return ActivitySystemTimeBinding.inflate(getLayoutInflater());
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
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_TIMEZONE) {
            if (msgCommon.result_code != 0) return;
            Type infoType = new TypeToken<DeviceTimeZone>() {
            }.getType();
            DeviceTimeZone timeZone = new Gson().fromJson(msgCommon.data, infoType);
            mSelectedTimeZone = timeZone.time_zone + 24;
            mBind.tvTimeZone.setText(mTimeZones.get(mSelectedTimeZone));
            getSystemTime();
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_SYSTEM_TIME) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) return;
            Type infoType = new TypeToken<SystemTime>() {
            }.getType();
            SystemTime systemTime = new Gson().fromJson(msgCommon.data, infoType);
            int time = systemTime.time;
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time * 1000L);
            String timeZoneId = mTimeZones.get(mSelectedTimeZone);
            String showTime = Utils.calendar2strDate(calendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM, timeZoneId.replaceAll("UTC", "GMT"));
            mBind.tvDeviceTime.setText(String.format("Device time:%s %s", showTime, timeZoneId));
            if (mSyncTimeHandler.hasMessages(0))
                mSyncTimeHandler.removeMessages(0);
            mSyncTimeHandler.postDelayed(this::getTimeZone, 60 * 1000);
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_TIMEZONE
                || msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_SYSTEM_TIME) {
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            ToastUtils.showToast(this, "Set up succeed");
            getTimeZone();
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

    private void getTimeZone() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadTimeZone(deviceParams);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, MQTTConstants.READ_MSG_ID_TIMEZONE, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setTimeZone() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        DeviceTimeZone timeZone = new DeviceTimeZone();
        timeZone.time_zone = mSelectedTimeZone - 24;
        String message = MQTTMessageAssembler.assembleWriteTimeZone(deviceParams, timeZone);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, MQTTConstants.CONFIG_MSG_ID_TIMEZONE, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getSystemTime() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadSystemTime(deviceParams);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, MQTTConstants.READ_MSG_ID_SYSTEM_TIME, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setSystemTime() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        Calendar calendar = Calendar.getInstance();
        SystemTime systemTime = new SystemTime();
        systemTime.time = (int) (calendar.getTimeInMillis() / 1000);
        String message = MQTTMessageAssembler.assembleWriteSystemTime(deviceParams, systemTime);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, MQTTConstants.CONFIG_MSG_ID_SYSTEM_TIME, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSelectTimeZoneClick(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mTimeZones, mSelectedTimeZone);
        dialog.setListener(value -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            mSelectedTimeZone = value;
            mBind.tvTimeZone.setText(mTimeZones.get(mSelectedTimeZone));
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
        if (isWindowLocked()) return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setSystemTime();
    }

    public void onSyncTimeFromNTPClick(View view) {
        if (isWindowLocked()) return;
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

    private String getTopic() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        return appTopic;
    }
}
