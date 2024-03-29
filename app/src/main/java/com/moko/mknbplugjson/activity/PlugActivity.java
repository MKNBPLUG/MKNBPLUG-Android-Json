package com.moko.mknbplugjson.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityPlugBinding;
import com.moko.mknbplugjson.dialog.AlertMessageDialog;
import com.moko.mknbplugjson.dialog.TimerDialog;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.CountdownInfo;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.LoadInsertion;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.entity.SetCountdown;
import com.moko.support.json.entity.SwitchInfo;
import com.moko.support.json.entity.SwitchState;
import com.moko.support.json.event.DeviceModifyNameEvent;
import com.moko.support.json.event.DeviceOnlineEvent;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class PlugActivity extends BaseActivity<ActivityPlugBinding> {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private Handler mHandler;
    private boolean mIsOver;

    @Override
    protected void onCreate() {
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mBind.tvTitle.setText(mMokoDevice.name);
        mHandler = new Handler(Looper.getMainLooper());
        changeSwitchState();
        if (mMokoDevice.isOverload
                || mMokoDevice.isOverVoltage
                || mMokoDevice.isOverCurrent
                || mMokoDevice.isUnderVoltage) {
            showOverDialog();
            return;
        }
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            ToastUtils.showToast(this, "Read device status failed！");
            EventBus.getDefault().post(new DeviceOnlineEvent(mMokoDevice.mac, false));
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getSwitchInfo();
    }

    @Override
    protected ActivityPlugBinding getViewBinding() {
        return ActivityPlugBinding.inflate(getLayoutInflater());
    }

    String mOverStatus;

    private void showOverDialog() {
        if (mIsOver) return;
        if (mMokoDevice.isOverload)
            mOverStatus = "overload";
        if (mMokoDevice.isOverVoltage)
            mOverStatus = "overvoltage";
        if (mMokoDevice.isOverCurrent)
            mOverStatus = "overcurrent";
        if (mMokoDevice.isUnderVoltage)
            mOverStatus = "undervoltage";
        String message = String.format("Detect the socket %s, please confirm whether to exit the %s status?", mOverStatus, mOverStatus);
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage(message);
        dialog.setOnAlertConfirmListener(this::showClearOverStatusDialog);
        dialog.show(getSupportFragmentManager());
        mIsOver = true;
    }

    private void showClearOverStatusDialog() {
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage(String.format("If YES, the socket will exit %s status, and please make sure it is within the protection threshold. If NO, you need manually reboot it to exit this status.", mOverStatus));
        dialog.setOnAlertCancelListener(() -> {
            ToastUtils.showToast(PlugActivity.this, "Socket is" + mOverStatus + ",please check it!");
            PlugActivity.this.finish();
        });
        dialog.setOnAlertConfirmListener(() -> {
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                EventBus.getDefault().post(new DeviceOnlineEvent(mMokoDevice.mac, false));
                dismissLoadingProgressDialog();
                finish();
            }, 90 * 1000);
            clearOverStatus();
        });
        dialog.show(getSupportFragmentManager());
    }

    private void clearOverStatus() {
        XLog.i("清除过载状态");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        if (mMokoDevice.isOverload) {
            String message = MQTTMessageAssembler.assembleConfigClearOverloadStatus(deviceParams);
            try {
                MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_CLEAR_OVERLOAD_PROTECTION, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            return;
        }
        if (mMokoDevice.isOverVoltage) {
            String message = MQTTMessageAssembler.assembleConfigClearOverVoltageStatus(deviceParams);
            try {
                MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_CLEAR_OVER_VOLTAGE_PROTECTION, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            return;
        }
        if (mMokoDevice.isOverCurrent) {
            String message = MQTTMessageAssembler.assembleConfigClearOverCurrentStatus(deviceParams);
            try {
                MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_CLEAR_OVER_CURRENT_PROTECTION, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            return;
        }
        if (mMokoDevice.isUnderVoltage) {
            String message = MQTTMessageAssembler.assembleConfigClearUnderVoltageStatus(deviceParams);
            try {
                MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_CLEAR_UNDER_VOLTAGE_PROTECTION, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        final String topic = event.getTopic();
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
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_SWITCH_STATE
                || msgCommon.msg_id == MQTTConstants.READ_MSG_ID_SWITCH_INFO) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            Type infoType = new TypeToken<SwitchInfo>() {
            }.getType();
            SwitchInfo switchInfo = new Gson().fromJson(msgCommon.data, infoType);
            int switch_state = switchInfo.switch_state;
            mMokoDevice.on_off = switch_state == 1;
            mMokoDevice.isOverload = switchInfo.overload_state == 1;
            mMokoDevice.isOverCurrent = switchInfo.overcurrent_state == 1;
            mMokoDevice.isOverVoltage = switchInfo.overvoltage_state == 1;
            mMokoDevice.isUnderVoltage = switchInfo.undervoltage_state == 1;
            mMokoDevice.csq = Integer.parseInt(switchInfo.CSQ);
            changeSwitchState();
            if (mMokoDevice.isOverload
                    || mMokoDevice.isOverVoltage
                    || mMokoDevice.isUnderVoltage
                    || mMokoDevice.isOverCurrent) {
                showOverDialog();
            }
            return;
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_COUNTDOWN_INFO) {
            Type infoType = new TypeToken<CountdownInfo>() {
            }.getType();
            CountdownInfo timerInfo = new Gson().fromJson(msgCommon.data, infoType);
            int countdown = timerInfo.countdown;
            int switch_state = timerInfo.switch_state;
            if (countdown == 0) {
                mBind.tvTimerState.setVisibility(View.GONE);
            } else {
                int hour = countdown / 3600;
                int minute = (countdown % 3600) / 60;
                int second = (countdown % 3600) % 60;
                mBind.tvTimerState.setVisibility(View.VISIBLE);
                String timer = String.format("Device will turn %s after %02d:%02d:%02d", switch_state == 1 ? "on" : "off", hour, minute, second);
                mBind.tvTimerState.setText(timer);
            }
            return;
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVERLOAD_OCCUR) {
            Type infoType = new TypeToken<OverloadOccur>() {
            }.getType();
            OverloadOccur overloadOccur = new Gson().fromJson(msgCommon.data, infoType);
            mMokoDevice.isOverload = overloadOccur.state == 1;
            mMokoDevice.on_off = false;
            if (mMokoDevice.isOverload) {
                showOverDialog();
            } else {
                mIsOver = false;
            }
            return;
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVER_VOLTAGE_OCCUR) {
            Type infoType = new TypeToken<OverloadOccur>() {
            }.getType();
            OverloadOccur overloadOccur = new Gson().fromJson(msgCommon.data, infoType);
            mMokoDevice.isOverVoltage = overloadOccur.state == 1;
            mMokoDevice.on_off = false;
            if (mMokoDevice.isOverVoltage) {
                showOverDialog();
            } else {
                mIsOver = false;
            }
            return;
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_UNDER_VOLTAGE_OCCUR) {
            Type infoType = new TypeToken<OverloadOccur>() {
            }.getType();
            OverloadOccur overloadOccur = new Gson().fromJson(msgCommon.data, infoType);
            mMokoDevice.isUnderVoltage = overloadOccur.state == 1;
            mMokoDevice.on_off = false;
            if (mMokoDevice.isUnderVoltage) {
                showOverDialog();
            } else {
                mIsOver = false;
            }
            return;
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVER_CURRENT_OCCUR) {
            Type infoType = new TypeToken<OverloadOccur>() {
            }.getType();
            OverloadOccur overloadOccur = new Gson().fromJson(msgCommon.data, infoType);
            mMokoDevice.isOverCurrent = overloadOccur.state == 1;
            mMokoDevice.on_off = false;
            if (mMokoDevice.isOverCurrent) {
                showOverDialog();
            } else {
                mIsOver = false;
            }
            return;
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_LOAD_STATUS_NOTIFY) {
            Type infoType = new TypeToken<LoadInsertion>() {
            }.getType();
            LoadInsertion loadInsertion = new Gson().fromJson(msgCommon.data, infoType);
            ToastUtils.showToast(PlugActivity.this, loadInsertion.load == 1 ? "Load starts work！" : "Load stops work！");
            return;
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_CLEAR_OVERLOAD_PROTECTION
                || msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_CLEAR_OVER_VOLTAGE_PROTECTION
                || msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_CLEAR_UNDER_VOLTAGE_PROTECTION
                || msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_CLEAR_OVER_CURRENT_PROTECTION) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            mIsOver = false;
            ToastUtils.showToast(this, "Set up succeed");
            return;
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_SWITCH_STATE
                || msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_COUNTDOWN) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "Set up failed");
            } else {
                if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_SWITCH_STATE)
                    getSwitchInfo();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        String deviceMac = event.getMac();
        if (deviceMac.equalsIgnoreCase(mMokoDevice.mac)) {
            mMokoDevice.name = event.getName();
            mBind.tvTitle.setText(mMokoDevice.name);
        }
    }

    private void changeSwitchState() {
        mBind.rlTitle.setBackgroundColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.blue_0188cc : R.color.black_303a4b));
        mBind.llBg.setBackgroundColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.grey_f2f2f2 : R.color.black_303a4b));
        mBind.ivSwitchState.setImageDrawable(ContextCompat.getDrawable(this, mMokoDevice.on_off ? R.drawable.plug_switch_on : R.drawable.plug_switch_off));
        String switchState = "";
        if (!mMokoDevice.isOnline) {
            switchState = getString(R.string.plug_switch_offline);
        } else if (mMokoDevice.on_off) {
            switchState = getString(R.string.plug_switch_on);
        } else {
            switchState = getString(R.string.plug_switch_off);
        }
        mBind.tvSwitchState.setText(switchState);
        mBind.tvSwitchState.setTextColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.blue_0188cc : R.color.grey_808080));

        Drawable drawablePower = ContextCompat.getDrawable(this, mMokoDevice.on_off ? R.drawable.power_on : R.drawable.power_off);
        drawablePower.setBounds(0, 0, drawablePower.getMinimumWidth(), drawablePower.getMinimumHeight());
        mBind.tvDevicePower.setCompoundDrawables(null, drawablePower, null, null);
        mBind.tvDevicePower.setTextColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.blue_0188cc : R.color.grey_808080));
        Drawable drawableTimer = ContextCompat.getDrawable(this, mMokoDevice.on_off ? R.drawable.timer_on : R.drawable.timer_off);
        drawableTimer.setBounds(0, 0, drawableTimer.getMinimumWidth(), drawableTimer.getMinimumHeight());
        mBind.tvDeviceTimer.setCompoundDrawables(null, drawableTimer, null, null);
        mBind.tvDeviceTimer.setTextColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.blue_0188cc : R.color.grey_808080));
        Drawable drawableEnergy = ContextCompat.getDrawable(this, mMokoDevice.on_off ? R.drawable.energy_on : R.drawable.energy_off);
        drawableEnergy.setBounds(0, 0, drawableEnergy.getMinimumWidth(), drawableEnergy.getMinimumHeight());
        mBind.tvDeviceEnergy.setCompoundDrawables(null, drawableEnergy, null, null);
        mBind.tvDeviceEnergy.setTextColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.blue_0188cc : R.color.grey_808080));
        mBind.tvTimerState.setTextColor(ContextCompat.getColor(this, mMokoDevice.on_off ? R.color.blue_0188cc : R.color.grey_808080));
    }

    public void onBack(View view) {
        finish();
    }

    public void onPlugSetting(View view) {
        if (isWindowLocked()) return;
        // Energy
        Intent intent = new Intent(this, PlugSettingActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(intent);
    }

    public void onTimerClick(View view) {
        if (isWindowLocked()) return;
        if (mIsOver) {
            ToastUtils.showToast(this, "Socket is" + mOverStatus + ",please check it!");
            finish();
            return;
        }
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(PlugActivity.this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(PlugActivity.this, R.string.device_offline);
            return;
        }
        TimerDialog timerDialog = new TimerDialog();
        timerDialog.setOnoff(mMokoDevice.on_off);
        timerDialog.setListener(dialog -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(PlugActivity.this, R.string.network_error);
                return;
            }
            if (!mMokoDevice.isOnline) {
                ToastUtils.showToast(PlugActivity.this, R.string.device_offline);
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(PlugActivity.this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setTimer(dialog.getWvHour(), dialog.getWvMinute());
            dialog.dismiss();
        });
        timerDialog.show(getSupportFragmentManager());
    }

    private void setTimer(int hour, int minute) {
        SetCountdown setCountdown = new SetCountdown();
        setCountdown.countdown = hour * 3600 + minute * 60;
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleWriteTimer(deviceParams, setCountdown);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, MQTTConstants.CONFIG_MSG_ID_COUNTDOWN, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onPowerClick(View view) {
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(PlugActivity.this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(PlugActivity.this, R.string.device_offline);
            return;
        }
        // Power
        Intent intent = new Intent(this, ElectricityActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(intent);
    }

    public void onEnergyClick(View view) {
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        // Energy
        Intent intent = new Intent(this, EnergyActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(intent);
    }

    public void onSwitchClick(View view) {
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        XLog.i("切换开关");
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        changeSwitch();
    }

    private void changeSwitch() {
        mMokoDevice.on_off = !mMokoDevice.on_off;
        SwitchState switchState = new SwitchState();
        switchState.switch_state = mMokoDevice.on_off ? 1 : 0;
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleWriteSwitchInfo(deviceParams, switchState);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, MQTTConstants.CONFIG_MSG_ID_SWITCH_STATE, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getSwitchInfo() {
        XLog.i("读取开关状态");
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadSwitchInfo(deviceParams);
        try {
            MQTTSupport.getInstance().publish(getTopic(), message, MQTTConstants.READ_MSG_ID_SWITCH_INFO, appMqttConfig.qos);
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
