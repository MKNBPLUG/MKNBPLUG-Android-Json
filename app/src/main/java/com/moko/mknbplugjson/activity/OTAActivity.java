package com.moko.mknbplugjson.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityOtaBinding;
import com.moko.mknbplugjson.dialog.AlertMessageDialog;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.DeviceStatus;
import com.moko.support.json.entity.FirmwareOTA;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OTAResult;
import com.moko.support.json.entity.WorkMode;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class OTAActivity extends BaseActivity<ActivityOtaBinding> {
    private final String FILTER_ASCII = "[ -~]*";
    public static String TAG = OTAActivity.class.getSimpleName();
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private Handler mHandler;

    @Override
    protected void onCreate() {
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        }
        InputFilter inputFilter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etOtaUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), inputFilter});
        mHandler = new Handler(Looper.getMainLooper());
        String mqttConfigAppStr = SPUtils.getStringValue(OTAActivity.this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        //首先读取设备的状态
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        readDeviceStatus();
    }

    @Override
    protected ActivityOtaBinding getViewBinding() {
        return ActivityOtaBinding.inflate(getLayoutInflater());
    }

    private void readDeviceStatus() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        String message = MQTTMessageAssembler.assembleReadDeviceWorkMode(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_WORK_MODE, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
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
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_WORK_MODE) {
            //读取设备工作模式
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "get work mode fail");
                finish();
                return;
            }
            Type infoType = new TypeToken<WorkMode>() {
            }.getType();
            WorkMode workMode = new Gson().fromJson(msgCommon.data, infoType);
            if (workMode.work_mode == 1) {
                //debug mode
                AlertMessageDialog dialog = new AlertMessageDialog();
                dialog.setMessage("Device is in debug mode, OTA is unvailable!");
                dialog.setCancelGone();
                dialog.setConfirm("OK");
                dialog.setOnAlertConfirmListener(this::finish);
                dialog.show(getSupportFragmentManager());
            }
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_DEVICE_STATUS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) return;
            Type infoType = new TypeToken<DeviceStatus>() {
            }.getType();
            DeviceStatus deviceStatus = new Gson().fromJson(msgCommon.data, infoType);
            if (deviceStatus.status != 0) {
                ToastUtils.showToast(this, "Device is OTA, please wait");
                return;
            }
            XLog.i("升级固件");
            mHandler.postDelayed(() -> {
                dismissLoadingMessageDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 190 * 1000);
            showLoadingMessageDialog("waiting");
            setOTAFirmware();
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OTA_RESULT) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingMessageDialog();
                mHandler.removeMessages(0);
            }
            Type type = new TypeToken<OTAResult>() {
            }.getType();
            OTAResult result = new Gson().fromJson(msgCommon.data, type);
            if (result.result == 1) {
                ToastUtils.showToast(this, R.string.update_success);
            } else {
                ToastUtils.showToast(this, R.string.update_failed);
            }
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_OTA) {
            if (msgCommon.result_code != 0) {
                dismissLoadingMessageDialog();
                ToastUtils.showToast(this, "Set up failed");
            }
        }
    }

    public void onBack(View view) {
        finish();
    }

    public void startUpdate(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mMokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        if (TextUtils.isEmpty(mBind.etOtaUrl.getText())) {
            ToastUtils.showToast(this, "params error");
            return;
        }
        XLog.i("检查设备状态");
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed, please try it again!");
        }, 30 * 1000);
        showLoadingProgressDialog();
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        String message = MQTTMessageAssembler.assembleReadDeviceStatus(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_DEVICE_STATUS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setOTAFirmware() {
        String url = mBind.etOtaUrl.getText().toString();
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        FirmwareOTA params = new FirmwareOTA();
        params.url = url;
        String message = MQTTMessageAssembler.assembleWriteOTA(deviceParams, params);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_OTA, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
