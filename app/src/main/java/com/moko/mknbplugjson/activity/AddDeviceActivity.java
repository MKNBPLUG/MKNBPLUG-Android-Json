package com.moko.mknbplugjson.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityAddDeviceBinding;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.SwitchInfo;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

/**
 * @author: jun.liu
 * @date: 2023/7/5 20:14
 * @des: add device
 */
public class AddDeviceActivity extends BaseActivity<ActivityAddDeviceBinding> {
    private final String FILTER_ASCII = "[ -~]*";
    private MQTTConfig appMqttConfig;
    private Handler mHandler;
    private String mac;

    @Override
    protected void onCreate() {
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etSubscribeTopic.setFilters(new InputFilter[]{filter, new InputFilter.LengthFilter(128)});
        mBind.etPublishTopic.setFilters(new InputFilter[]{filter, new InputFilter.LengthFilter(128)});
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected ActivityAddDeviceBinding getViewBinding() {
        return ActivityAddDeviceBinding.inflate(getLayoutInflater());
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
        if (!mac.equalsIgnoreCase(msgCommon.device_info.mac)) {
            return;
        }
        if (msgCommon.result_code != 0) return;
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_SWITCH_INFO) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            Type infoType = new TypeToken<SwitchInfo>() {
            }.getType();
            String mac = msgCommon.device_info.mac;
            SwitchInfo switchInfo = new Gson().fromJson(msgCommon.data, infoType);
            int switch_state = switchInfo.switch_state;
            if (switch_state == 1) {
                //设备添加成功
                Intent intent = new Intent(this, AddDeviceSuccessActivity.class);
                intent.putExtra("mac", mac);
                intent.putExtra("subscribe", mBind.etSubscribeTopic.getText().toString());
                intent.putExtra("publish", mBind.etPublishTopic.getText().toString());
                startActivity(intent);
                finish();
            } else {
                ToastUtils.showToast(this, "Add device failed！");
            }
        }
    }

    public void onBack(View view) {
        finish();
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (isValid()) {
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Add device failed！");
            }, 90 * 1000);
            DeviceParams deviceParams = new DeviceParams();
            mac = deviceParams.mac = mBind.etMac.getText().toString().toUpperCase();
            String message = MQTTMessageAssembler.assembleReadSwitchInfo(deviceParams);
            try {
                MQTTSupport.getInstance().publish(mBind.etSubscribeTopic.getText().toString(), message, MQTTConstants.READ_MSG_ID_SWITCH_INFO, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private boolean isValid() {
        if (TextUtils.isEmpty(mBind.etMac.getText()) || mBind.etMac.getText().length() != 12)
            return false;
        if (TextUtils.isEmpty(mBind.etSubscribeTopic.getText())) return false;
        return !TextUtils.isEmpty(mBind.etPublishTopic.getText());
    }
}
