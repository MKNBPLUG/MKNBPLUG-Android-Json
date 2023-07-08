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
import com.moko.mknbplugjson.dialog.BottomDialog;
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
import com.moko.support.json.entity.OTABothWayParams;
import com.moko.support.json.entity.OTAOneWayParams;
import com.moko.support.json.entity.OTAResult;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class OTAActivity extends BaseActivity<ActivityOtaBinding> {
    private final String FILTER_ASCII = "[ -~]*";
    public static String TAG = OTAActivity.class.getSimpleName();
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private ArrayList<String> mValues;
    private int mSelected;
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
        mBind.etMasterHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), inputFilter});
        mBind.etOneWayHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), inputFilter});
        mBind.etBothWayHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), inputFilter});
        mBind.etMasterFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), inputFilter});
        mBind.etOneWayCaFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), inputFilter});
        mBind.etBothWayCaFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), inputFilter});
        mBind.etBothWayClientKeyFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), inputFilter});
        mBind.etBothWayClientCertFilePath.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), inputFilter});
        mHandler = new Handler(Looper.getMainLooper());
        String mqttConfigAppStr = SPUtils.getStringValue(OTAActivity.this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mValues = new ArrayList<>();
        mValues.add("Firmware");
        mValues.add("CA certificate");
        mValues.add("Self signed server certificates");
        mBind.tvUpdateType.setText(mValues.get(mSelected));
    }

    @Override
    protected ActivityOtaBinding getViewBinding() {
        return ActivityOtaBinding.inflate(getLayoutInflater());
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
            if (mSelected == 0) {
                setOTAFirmware();
            }
            if (mSelected == 1) {
                setOTAOneWay();
            }
            if (mSelected == 2) {
                setOTABothWay();
            }
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OTA_RESULT) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingMessageDialog();
                mHandler.removeMessages(0);
            }
            Type type = new TypeToken<OTAResult>() {
            }.getType();
            OTAResult result = new Gson().fromJson(msgCommon.data, type);
            if (result.type != mSelected)
                return;
            if (result.result == 1) {
                ToastUtils.showToast(this, R.string.update_success);
            } else {
                ToastUtils.showToast(this, R.string.update_failed);
            }
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_OTA
                || msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_OTA_ONE_WAY
                || msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_OTA_BOTH_WAY) {
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
        if (mSelected == 0) {
            String hostStr = mBind.etMasterHost.getText().toString();
            String portStr = mBind.etMasterPort.getText().toString();
            String masterStr = mBind.etMasterFilePath.getText().toString();
            if (TextUtils.isEmpty(hostStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_host);
                return;
            }
            if (TextUtils.isEmpty(portStr) || Integer.parseInt(portStr) < 1 || Integer.parseInt(portStr) > 65535) {
                ToastUtils.showToast(this, R.string.mqtt_verify_port_empty);
                return;
            }
            if (TextUtils.isEmpty(masterStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_file_path);
                return;
            }
        }
        if (mSelected == 1) {
            String hostStr = mBind.etOneWayHost.getText().toString();
            String portStr = mBind.etOneWayPort.getText().toString();
            String oneWayStr = mBind.etOneWayCaFilePath.getText().toString();
            if (TextUtils.isEmpty(hostStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_host);
                return;
            }
            if (TextUtils.isEmpty(portStr) || Integer.parseInt(portStr) < 1 || Integer.parseInt(portStr) > 65535) {
                ToastUtils.showToast(this, R.string.mqtt_verify_port_empty);
                return;
            }
            if (TextUtils.isEmpty(oneWayStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_file_path);
                return;
            }
        }
        if (mSelected == 2) {
            String hostStr = mBind.etBothWayHost.getText().toString();
            String portStr = mBind.etBothWayPort.getText().toString();
            String bothWayCaStr = mBind.etBothWayCaFilePath.getText().toString();
            String bothWayClientKeyStr = mBind.etBothWayClientKeyFilePath.getText().toString();
            String bothWayClientCertStr = mBind.etBothWayClientCertFilePath.getText().toString();
            if (TextUtils.isEmpty(hostStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_host);
                return;
            }
            if (TextUtils.isEmpty(portStr) || Integer.parseInt(portStr) < 1 || Integer.parseInt(portStr) > 65535) {
                ToastUtils.showToast(this, R.string.mqtt_verify_port_empty);
                return;
            }
            if (TextUtils.isEmpty(bothWayCaStr)
                    || TextUtils.isEmpty(bothWayClientKeyStr)
                    || TextUtils.isEmpty(bothWayClientCertStr)) {
                ToastUtils.showToast(this, R.string.mqtt_verify_file_path);
                return;
            }
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

    public void onSelectUpdateType(View view) {
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mValues, mSelected);
        dialog.setListener(value -> {
            mSelected = value;
            switch (value) {
                case 0:
                    mBind.llMasterFirmware.setVisibility(View.VISIBLE);
                    mBind.llOneWay.setVisibility(View.GONE);
                    mBind.llBothWay.setVisibility(View.GONE);
                    break;
                case 1:
                    mBind.llMasterFirmware.setVisibility(View.GONE);
                    mBind.llOneWay.setVisibility(View.VISIBLE);
                    mBind.llBothWay.setVisibility(View.GONE);
                    break;
                case 2:
                    mBind.llMasterFirmware.setVisibility(View.GONE);
                    mBind.llOneWay.setVisibility(View.GONE);
                    mBind.llBothWay.setVisibility(View.VISIBLE);
                    break;
            }
            mBind.tvUpdateType.setText(mValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }

    private void setOTAFirmware() {
        String host = mBind.etMasterHost.getText().toString();
        String portStr = mBind.etMasterPort.getText().toString();
        String filePath = mBind.etMasterFilePath.getText().toString();

        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        FirmwareOTA params = new FirmwareOTA();
        params.host = host;
        params.port = Integer.parseInt(portStr);
        params.file_path = filePath;
        String message = MQTTMessageAssembler.assembleWriteOTA(deviceParams, params);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_OTA, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setOTAOneWay() {
        String hostStr = mBind.etOneWayHost.getText().toString();
        String portStr = mBind.etOneWayPort.getText().toString();
        String oneWayStr = mBind.etOneWayCaFilePath.getText().toString();
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        OTAOneWayParams params = new OTAOneWayParams();
        params.host = hostStr;
        params.port = Integer.parseInt(portStr);
        params.file_path = oneWayStr;
        String message = MQTTMessageAssembler.assembleWriteOTAOneWay(deviceParams, params);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_OTA_ONE_WAY, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setOTABothWay() {
        String hostStr = mBind.etBothWayHost.getText().toString();
        String portStr = mBind.etBothWayPort.getText().toString();
        String bothWayCaStr = mBind.etBothWayCaFilePath.getText().toString();
        String bothWayClientKeyStr = mBind.etBothWayClientKeyFilePath.getText().toString();
        String bothWayClientCertStr = mBind.etBothWayClientCertFilePath.getText().toString();
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        OTABothWayParams params = new OTABothWayParams();
        params.host = hostStr;
        params.port = Integer.parseInt(portStr);
        params.ca_file_path = bothWayCaStr;
        params.client_cert_file_path = bothWayClientCertStr;
        params.client_key_file_path = bothWayClientKeyStr;
        String message = MQTTMessageAssembler.assembleWriteOTABothWay(deviceParams, params);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_OTA_BOTH_WAY, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
