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
import com.moko.mknbplugjson.databinding.ActivityResetByButtonBinding;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.ResetByButton;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

/**
 * @author: jun.liu
 * @date: 2023/7/10 10:38
 * @des:
 */
public class ResetByButtonActivity extends BaseActivity<ActivityResetByButtonBinding> {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private Handler mHandler;

    @Override
    protected ActivityResetByButtonBinding getViewBinding() {
        return ActivityResetByButtonBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate() {
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        }
        mHandler = new Handler(Looper.getMainLooper());
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        //首先读取设备的状态
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getResetByButton();
        mBind.radio.setOnCheckedChangeListener((group, checkedId) -> {
            int type = checkedId == R.id.btnAnyTime ? 1 : 0;
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                finish();
            }, 30 * 1000);
            setResetByButton(type);
        });
    }

    private void setResetByButton(int type) {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        ResetByButton params = new ResetByButton();
        params.key_reset_type = type;
        String message = MQTTMessageAssembler.assembleWriteResetByButton(deviceParams, params);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_RESET_BY_BUTTON, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getResetByButton() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        String message = MQTTMessageAssembler.assembleReadResetByButton(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_RESET_BY_BUTTON, appMqttConfig.qos);
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
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_RESET_BY_BUTTON) {
            //读取设备工作模式
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "fail");
                finish();
                return;
            }
            Type infoType = new TypeToken<ResetByButton>() {
            }.getType();
            ResetByButton resetByButton = new Gson().fromJson(msgCommon.data, infoType);
            if (resetByButton.key_reset_type == 1) {
                mBind.btnAnyTime.setChecked(true);
            } else {
                mBind.btnMinute.setChecked(true);
            }
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_RESET_BY_BUTTON) {
            if (msgCommon.result_code != 0) {
                dismissLoadingMessageDialog();
                ToastUtils.showToast(this, "Set up failed");
            }
        }
    }

    public void onBack(View view) {
        finish();
    }
}
