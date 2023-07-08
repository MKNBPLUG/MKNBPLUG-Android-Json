package com.moko.mknbplugjson.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityIndicatorColorBinding;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.IndicatorStatus;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

import cn.carbswang.android.numberpickerview.library.NumberPickerView;

public class IndicatorColorActivity extends BaseActivity<ActivityIndicatorColorBinding> implements NumberPickerView.OnValueChangeListener {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private Handler mHandler;
    private int maxValue = 4416;

    @Override
    protected void onCreate() {
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        }
        int deviceType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_DEVICE_TYPE, 0);
        if (deviceType == 1) {
            maxValue = 2160;
        }
        if (deviceType == 2) {
            maxValue = 3588;
        }
        mBind.npvColorSettings.setMinValue(0);
        mBind.npvColorSettings.setMaxValue(8);
        mBind.npvColorSettings.setValue(0);
        mBind.npvColorSettings.setOnValueChangedListener(this);
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mHandler = new Handler(Looper.getMainLooper());
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getColorSettings();
    }

    @Override
    protected ActivityIndicatorColorBinding getViewBinding() {
        return ActivityIndicatorColorBinding.inflate(getLayoutInflater());
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
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_INDICATOR_STATUS_COLOR) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            Type infoType = new TypeToken<IndicatorStatus>() {
            }.getType();
            IndicatorStatus indicatorStatus = new Gson().fromJson(msgCommon.data, infoType);
            mBind.npvColorSettings.setValue(indicatorStatus.led_state);
            if (indicatorStatus.led_state > 1) {
                mBind.llColorSettings.setVisibility(View.GONE);
            } else {
                mBind.llColorSettings.setVisibility(View.VISIBLE);
            }
            mBind.etBlue.setText(String.valueOf(indicatorStatus.blue));
            mBind.etGreen.setText(String.valueOf(indicatorStatus.green));
            mBind.etYellow.setText(String.valueOf(indicatorStatus.yellow));
            mBind.etOrange.setText(String.valueOf(indicatorStatus.orange));
            mBind.etRed.setText(String.valueOf(indicatorStatus.red));
            mBind.etPurple.setText(String.valueOf(indicatorStatus.purple));
            mBind.etBlue.setSelection(mBind.etBlue.getText().length());
            mBind.etGreen.setSelection(mBind.etGreen.getText().length());
            mBind.etYellow.setSelection(mBind.etYellow.getText().length());
            mBind.etOrange.setSelection(mBind.etOrange.getText().length());
            mBind.etRed.setSelection(mBind.etRed.getText().length());
            mBind.etPurple.setSelection(mBind.etPurple.getText().length());
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_INDICATOR_STATUS_COLOR) {
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

    private void getColorSettings() {
        XLog.i("读取颜色范围");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadIndicatorColor(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_INDICATOR_STATUS_COLOR, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {
        if (newVal > 1) {
            mBind.llColorSettings.setVisibility(View.GONE);
        } else {
            mBind.llColorSettings.setVisibility(View.VISIBLE);
        }
    }

    public void onSave(View view) {
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        setLEDColor();
    }

    private void setLEDColor() {
        String blue = mBind.etBlue.getText().toString();
        String green = mBind.etGreen.getText().toString();
        String yellow = mBind.etYellow.getText().toString();
        String orange = mBind.etOrange.getText().toString();
        String red = mBind.etRed.getText().toString();
        String purple = mBind.etPurple.getText().toString();
        if (TextUtils.isEmpty(blue)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        if (TextUtils.isEmpty(green)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        if (TextUtils.isEmpty(yellow)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        if (TextUtils.isEmpty(orange)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        if (TextUtils.isEmpty(red)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        if (TextUtils.isEmpty(purple)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        int blueValue = Integer.parseInt(blue);
        if (blueValue < 2 || blueValue > (maxValue - 5)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }

        int greenValue = Integer.parseInt(green);
        if (greenValue <= blueValue || greenValue > (maxValue - 4)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }

        int yellowValue = Integer.parseInt(yellow);
        if (yellowValue <= greenValue || yellowValue > (maxValue - 3)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }

        int orangeValue = Integer.parseInt(orange);
        if (orangeValue <= yellowValue || orangeValue > (maxValue - 2)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }

        int redValue = Integer.parseInt(red);
        if (redValue <= orangeValue || redValue > (maxValue - 1)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }

        int purpleValue = Integer.parseInt(purple);
        if (purpleValue <= redValue || purpleValue > maxValue) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        XLog.i("设置颜色范围");
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        IndicatorStatus indicatorStatus = new IndicatorStatus();
        indicatorStatus.led_state = mBind.npvColorSettings.getValue();
        indicatorStatus.blue = blueValue;
        indicatorStatus.green = greenValue;
        indicatorStatus.yellow = yellowValue;
        indicatorStatus.orange = orangeValue;
        indicatorStatus.red = redValue;
        indicatorStatus.purple = purpleValue;
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        String message = MQTTMessageAssembler.assembleWriteIndicatorColor(deviceParams, indicatorStatus);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_INDICATOR_STATUS_COLOR, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
