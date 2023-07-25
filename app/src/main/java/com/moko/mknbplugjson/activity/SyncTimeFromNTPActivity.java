package com.moko.mknbplugjson.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivitySyncTimeFromNtpBinding;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.NTPParams;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class SyncTimeFromNTPActivity extends BaseActivity<ActivitySyncTimeFromNtpBinding> {
    private final String FILTER_ASCII = "[ -~]*";
    private MQTTConfig appMqttConfig;
    private MokoDevice mMokoDevice;
    private Handler mHandler;

    @Override
    protected void onCreate() {
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etNtpUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getSyncFromNTP();
    }

    @Override
    protected ActivitySyncTimeFromNtpBinding getViewBinding() {
        return ActivitySyncTimeFromNtpBinding.inflate(getLayoutInflater());
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
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_NTP_PARAMS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) return;
            Type type = new TypeToken<NTPParams>() {
            }.getType();
            NTPParams ntpParams = new Gson().fromJson(msgCommon.data, type);
            mBind.cbSyncSwitch.setChecked(ntpParams.ntp_switch == 1);
            mBind.etNtpUrl.setText(ntpParams.server);
            mBind.etSyncInterval.setText(String.valueOf(ntpParams.interval));
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_NTP_PARAMS) {
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
            if (overloadOccur.state == 1) finish();
        }
    }

    public void onBack(View view) {
        finish();
    }

    private void getSyncFromNTP() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadNTPParams(deviceParams);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, MQTTConstants.READ_MSG_ID_NTP_PARAMS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (isValid()) {
            String ntpUrlStr = mBind.etNtpUrl.getText().toString();
            String syncIntervalStr = mBind.etSyncInterval.getText().toString();
            int syncInterval = Integer.parseInt(syncIntervalStr);
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setSyncFromNTP(ntpUrlStr, syncInterval);
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private boolean isValid() {
        String syncIntervalStr = mBind.etSyncInterval.getText().toString();
        if (TextUtils.isEmpty(syncIntervalStr)) return false;
        int syncInterval = Integer.parseInt(syncIntervalStr);
        return syncInterval >= 1 && syncInterval <= 720;
    }

    private void setSyncFromNTP(String ntpServer, int interval) {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        NTPParams ntpParams = new NTPParams();
        ntpParams.ntp_switch = mBind.cbSyncSwitch.isChecked() ? 1 : 0;
        ntpParams.server = ntpServer;
        ntpParams.interval = interval;
        String message = MQTTMessageAssembler.assembleWriteNTPParams(deviceParams, ntpParams);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, MQTTConstants.CONFIG_MSG_ID_NTP_PARAMS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
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
