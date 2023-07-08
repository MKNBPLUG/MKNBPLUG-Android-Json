package com.moko.mknbplugjson.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityAddDeviceSuccessBinding;
import com.moko.mknbplugjson.db.DBTools;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.DeviceType;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

/**
 * @author: jun.liu
 * @date: 2023/7/5 20:37
 * @des: add device success
 */
public class AddDeviceSuccessActivity extends BaseActivity<ActivityAddDeviceSuccessBinding> {
    private String subscribe;
    private String publish;
    private String mac;
    private Handler mHandler;
    public static final String TAG = AddDeviceSuccessActivity.class.getSimpleName();

    @Override
    protected void onCreate() {
        mac = getIntent().getStringExtra("mac");
        subscribe = getIntent().getStringExtra("subscribe");
        publish = getIntent().getStringExtra("publish");
        mBind.etDeviceName.setText("MK117NB-" + mac.substring(mac.length() - 4).toUpperCase());
        mBind.etDeviceName.setSelection(mBind.etDeviceName.getText().length());
        Button btnDone = findViewById(R.id.btnDone);
        mHandler = new Handler(Looper.getMainLooper());
        btnDone.setOnClickListener(v -> {
            if (TextUtils.isEmpty(mBind.etDeviceName.getText())) {
                ToastUtils.showToast(this, "device name can not be null");
                return;
            }
            //查询设备信息
            showLoadingProgressDialog();
            DeviceParams deviceParams = new DeviceParams();
            deviceParams.mac = mac;
            String message = MQTTMessageAssembler.assembleReadDeviceType(deviceParams);
            String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
            MQTTConfig appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
            try {
                MQTTSupport.getInstance().publish(subscribe, message, MQTTConstants.READ_MSG_ID_DEVICE_TYPE, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "add fail!");
            }, 90 * 1000);
        });
    }

    @Override
    protected ActivityAddDeviceSuccessBinding getViewBinding() {
        return ActivityAddDeviceSuccessBinding.inflate(getLayoutInflater());
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
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_DEVICE_TYPE) {
            if (mHandler.hasMessages(0)) {
                mHandler.removeMessages(0);
            }
            Type infoType = new TypeToken<DeviceType>() {
            }.getType();
            DeviceType deviceType = new Gson().fromJson(msgCommon.data, infoType);
            insertDeviceToLocal(deviceType.device_type);
        }
    }

    @Override
    public void onBackPressed() {
    }

    private void insertDeviceToLocal(int deviceType) {
        //保存设备信息
        String MQTTConfigStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        MQTTConfig mqttDeviceConfig;
        if (TextUtils.isEmpty(MQTTConfigStr)) {
            mqttDeviceConfig = new MQTTConfig();
        } else {
            Gson gson = new Gson();
            mqttDeviceConfig = gson.fromJson(MQTTConfigStr, MQTTConfig.class);
            mqttDeviceConfig.connectMode = 0;
            mqttDeviceConfig.cleanSession = true;
            mqttDeviceConfig.qos = 1;
            mqttDeviceConfig.keepAlive = 60;
            mqttDeviceConfig.clientId = "";
            mqttDeviceConfig.username = "";
            mqttDeviceConfig.password = "";
            mqttDeviceConfig.caPath = "";
            mqttDeviceConfig.clientKeyPath = "";
            mqttDeviceConfig.clientCertPath = "";
            mqttDeviceConfig.lwtTopic = "{device_name}/{device_id}/device_to_app";
            mqttDeviceConfig.lwtPayload = "Offline";
            mqttDeviceConfig.apn = "";
            mqttDeviceConfig.apnUsername = "";
            mqttDeviceConfig.apnPassword = "";
            mqttDeviceConfig.topicPublish = publish;
            mqttDeviceConfig.topicSubscribe = subscribe;
            mqttDeviceConfig.timeZone = 0;
        }

        String mqttConfigStr = new Gson().toJson(mqttDeviceConfig, MQTTConfig.class);
        MokoDevice mokoDevice = new MokoDevice();
        mokoDevice.name = mBind.etDeviceName.getText().toString();
        mokoDevice.mac = mac;
        mokoDevice.mqttInfo = mqttConfigStr;
        mokoDevice.topicSubscribe = subscribe;
        mokoDevice.topicPublish = publish;
        mokoDevice.deviceType = deviceType;
        DBTools.getInstance(getApplicationContext()).insertDevice(mokoDevice);
        dismissLoadingProgressDialog();
        // 跳转首页，刷新数据
        Intent intent = new Intent(this, JSONMainActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_MAC, mokoDevice.mac);
        startActivity(intent);
    }
}
