package com.moko.mknbplugjson.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityPowerOnDefaultBinding;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.entity.PowerOnDefault;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class PowerOnDefaultActivity extends BaseActivity<ActivityPowerOnDefaultBinding> implements RadioGroup.OnCheckedChangeListener {
    private MQTTConfig appMqttConfig;
    private MokoDevice mMokoDevice;
    private Handler mHandler;

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
        getPowerOnDefault();
    }

    @Override
    protected ActivityPowerOnDefaultBinding getViewBinding() {
        return ActivityPowerOnDefaultBinding.inflate(getLayoutInflater());
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
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_POWER_ON_DEFAULT) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) return;
            int status;
            Type statusType = new TypeToken<PowerOnDefault>() {
            }.getType();
            PowerOnDefault powerOnDefault = new Gson().fromJson(msgCommon.data, statusType);
            status = powerOnDefault.default_status;
            switch (status) {
                case 0:
                    mBind.rbSwitchOff.setChecked(true);
                    break;
                case 1:
                    mBind.rbSwitchOn.setChecked(true);
                    break;
                case 2:
                    mBind.rbLastStatus.setChecked(true);
                    break;
            }
            mBind.rgPowerStatus.setOnCheckedChangeListener(this);
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_POWER_ON_DEFAULT) {
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

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        if (checkedId == R.id.rb_switch_off)
            setPowerStatus(0);
        else if (checkedId == R.id.rb_switch_on)
            setPowerStatus(1);
        else if (checkedId == R.id.rb_last_status)
            setPowerStatus(2);
    }

    private void getPowerOnDefault() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadPowerOnDefault(deviceParams);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, MQTTConstants.READ_MSG_ID_POWER_ON_DEFAULT, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setPowerStatus(int status) {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        PowerOnDefault powerOnDefault = new PowerOnDefault();
        powerOnDefault.default_status = status;
        String message = MQTTMessageAssembler.assembleWritePowerOnDefault(deviceParams, powerOnDefault);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, MQTTConstants.CONFIG_MSG_ID_POWER_ON_DEFAULT, appMqttConfig.qos);
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
