package com.moko.mknbplugjson.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.R2;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtiles;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.IndicatorStatus;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.event.DeviceOnlineEvent;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.carbswang.android.numberpickerview.library.NumberPickerView;

public class IndicatorStatusActivity extends BaseActivity implements NumberPickerView.OnValueChangeListener {

    @BindView(R2.id.npv_color_settings)
    NumberPickerView npvColorSettings;
    @BindView(R2.id.et_blue)
    EditText etBlue;
    @BindView(R2.id.et_green)
    EditText etGreen;
    @BindView(R2.id.et_yellow)
    EditText etYellow;
    @BindView(R2.id.et_orange)
    EditText etOrange;
    @BindView(R2.id.et_red)
    EditText etRed;
    @BindView(R2.id.et_purple)
    EditText etPurple;
    @BindView(R2.id.ll_color_settings)
    LinearLayout llColorSettings;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private Handler mHandler;
    private int deviceType;
    private int maxValue = 4416;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indicator_color);
        ButterKnife.bind(this);
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        }
        deviceType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_DEVICE_TYPE, 0);
        if (deviceType == 1) {
            maxValue = 2160;
        }
        if (deviceType == 2) {
            maxValue = 3588;
        }
        npvColorSettings.setMinValue(0);
        npvColorSettings.setMaxValue(8);
        npvColorSettings.setValue(0);
        npvColorSettings.setOnValueChangedListener(this);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
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
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_INDICATOR_STATUS_COLOR) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            Type infoType = new TypeToken<IndicatorStatus>() {
            }.getType();
            IndicatorStatus indicatorStatus = new Gson().fromJson(msgCommon.data, infoType);
            npvColorSettings.setValue(indicatorStatus.led_state);
            if (indicatorStatus.led_state > 1) {
                llColorSettings.setVisibility(View.GONE);
            } else {
                llColorSettings.setVisibility(View.VISIBLE);
            }
            etBlue.setText(String.valueOf(indicatorStatus.blue));
            etGreen.setText(String.valueOf(indicatorStatus.green));
            etYellow.setText(String.valueOf(indicatorStatus.yellow));
            etOrange.setText(String.valueOf(indicatorStatus.orange));
            etRed.setText(String.valueOf(indicatorStatus.red));
            etPurple.setText(String.valueOf(indicatorStatus.purple));
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

    private void getColorSettings() {
        XLog.i("读取颜色范围");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
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
            llColorSettings.setVisibility(View.GONE);
        } else {
            llColorSettings.setVisibility(View.VISIBLE);
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
        String blue = etBlue.getText().toString();
        String green = etGreen.getText().toString();
        String yellow = etYellow.getText().toString();
        String orange = etOrange.getText().toString();
        String red = etRed.getText().toString();
        String purple = etPurple.getText().toString();
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
        if (blueValue <= 0 || blueValue > (maxValue - 5)) {
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
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        IndicatorStatus indicatorStatus = new IndicatorStatus();
        indicatorStatus.led_state = npvColorSettings.getValue();
//        if (indicatorStatus.led_state < 2) {
            indicatorStatus.blue = blueValue;
            indicatorStatus.green = greenValue;
            indicatorStatus.yellow = yellowValue;
            indicatorStatus.orange = orangeValue;
            indicatorStatus.red = redValue;
            indicatorStatus.purple = purpleValue;
//        }
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
