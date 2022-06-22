package com.moko.mknbplugjson.activity;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.R2;
import com.moko.mknbplugjson.adapter.MQTTFragmentAdapter;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.db.DBTools;
import com.moko.mknbplugjson.dialog.BottomDialog;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.fragment.GeneralDeviceFragment;
import com.moko.mknbplugjson.fragment.LWTFragment;
import com.moko.mknbplugjson.fragment.SSLDevicePathFragment;
import com.moko.mknbplugjson.fragment.UserDeviceFragment;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.APNSettings;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.DeviceStatus;
import com.moko.support.json.entity.LWTSettings;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MQTTSettings;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.NetworkSettings;
import com.moko.support.json.entity.ReadyResult;
import com.moko.support.json.event.DeviceOnlineEvent;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ModifyMQTTSettingsActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {
    public static String TAG = ModifyMQTTSettingsActivity.class.getSimpleName();
    private final String FILTER_ASCII = "[ -~]*";
    @BindView(R2.id.et_mqtt_host)
    EditText etMqttHost;
    @BindView(R2.id.et_mqtt_port)
    EditText etMqttPort;
    @BindView(R2.id.et_mqtt_client_id)
    EditText etMqttClientId;
    @BindView(R2.id.et_mqtt_subscribe_topic)
    EditText etMqttSubscribeTopic;
    @BindView(R2.id.et_mqtt_publish_topic)
    EditText etMqttPublishTopic;
    @BindView(R2.id.rb_general)
    RadioButton rbGeneral;
    @BindView(R2.id.rb_user)
    RadioButton rbUser;
    @BindView(R2.id.rb_ssl)
    RadioButton rbSsl;
    @BindView(R2.id.rb_lwt)
    RadioButton rbLwt;
    @BindView(R2.id.vp_mqtt)
    ViewPager2 vpMqtt;
    @BindView(R2.id.rg_mqtt)
    RadioGroup rgMqtt;
    @BindView(R2.id.et_apn)
    EditText etApn;
    @BindView(R2.id.et_apn_username)
    EditText etApnUsername;
    @BindView(R2.id.et_apn_password)
    EditText etApnPassword;
    @BindView(R2.id.tv_network_priority)
    TextView tvNetworkPriority;

    private GeneralDeviceFragment generalFragment;
    private UserDeviceFragment userFragment;
    private SSLDevicePathFragment sslFragment;
    private LWTFragment lwtFragment;
    private MQTTFragmentAdapter adapter;
    private ArrayList<Fragment> fragments;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private MQTTSettings mMQTTSettings;
    private LWTSettings mLWTSettings;
    private APNSettings mAPNSettings;
    private NetworkSettings mNetworkSettings;
    private ArrayList<String> mNetworkPriority;
    private int mSelectedNetworkPriority;

    public Handler mHandler;

    private InputFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt_device_modify);
        ButterKnife.bind(this);

        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMQTTSettings = new MQTTSettings();
        mLWTSettings = new LWTSettings();
        mAPNSettings = new APNSettings();
        mNetworkSettings = new NetworkSettings();
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        etMqttHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        etMqttClientId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        etMqttSubscribeTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        etMqttPublishTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        etApn.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100), filter});
        etApnUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(127), filter});
        etApnPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(127), filter});
        mNetworkPriority = new ArrayList<>();
        mNetworkPriority.add("eMTC->NB-IOT->GSM");
        mNetworkPriority.add("eMTC-> GSM -> NB-IOT");
        mNetworkPriority.add("NB-IOT->GSM-> eMTC");
        mNetworkPriority.add("NB-IOT-> eMTC-> GSM");
        mNetworkPriority.add("GSM -> NB-IOT-> eMTC");
        mNetworkPriority.add("GSM -> eMTC->NB-IOT");
        mNetworkPriority.add("eMTC->NB-IOT");
        mNetworkPriority.add("NB-IOT-> eMTC");
        mNetworkPriority.add("GSM");
        mNetworkPriority.add("NB-IOT");
        mNetworkPriority.add("eMTC");
        createFragment();
        initData();
        adapter = new MQTTFragmentAdapter(this);
        adapter.setFragmentList(fragments);
        vpMqtt.setAdapter(adapter);
        vpMqtt.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    rbGeneral.setChecked(true);
                } else if (position == 1) {
                    rbUser.setChecked(true);
                } else if (position == 2) {
                    rbSsl.setChecked(true);
                } else if (position == 3) {
                    rbLwt.setChecked(true);
                }
            }
        });
        vpMqtt.setOffscreenPageLimit(4);
        rgMqtt.setOnCheckedChangeListener(this);
        mHandler = new Handler(Looper.getMainLooper());
    }

    private void createFragment() {
        fragments = new ArrayList<>();
        generalFragment = GeneralDeviceFragment.newInstance();
        userFragment = UserDeviceFragment.newInstance();
        sslFragment = SSLDevicePathFragment.newInstance();
        lwtFragment = LWTFragment.newInstance();
        fragments.add(generalFragment);
        fragments.add(userFragment);
        fragments.add(sslFragment);
        fragments.add(lwtFragment);
    }

    private void initData() {
        generalFragment.setCleanSession(mMQTTSettings.clean_session == 1);
        generalFragment.setQos(mMQTTSettings.qos);
        generalFragment.setKeepAlive(mMQTTSettings.keepalive);
        lwtFragment.setQos(mLWTSettings.lwt_qos);
        lwtFragment.setTopic(mLWTSettings.lwt_topic);
        lwtFragment.setPayload(mLWTSettings.lwt_message);
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
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_DEVICE_STATUS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            Type infoType = new TypeToken<DeviceStatus>() {
            }.getType();
            DeviceStatus deviceStatus = new Gson().fromJson(msgCommon.data, infoType);
            if (deviceStatus.status != 0) {
                ToastUtils.showToast(this, "Device is OTA, please wait");
                return;
            }
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }, 30 * 1000);
            setMQTTSettings();
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_MQTT_SETTINGS) {
            if (msgCommon.result_code != 0) {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            setLWTSettings();
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_LWT_SETTINGS) {
            if (msgCommon.result_code != 0) {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            setAPNSettings();
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_APN_SETTINGS) {
            if (msgCommon.result_code != 0) {
                if (mHandler.hasMessages(0)) {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                }
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            setNetworkSettings();
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_NETWORK_PRIORITY) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }, 30 * 1000);
            // MQTT配置发送完成
            setMQTTConfigFinish();
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_MQTT_CONFIG_FINISH) {
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_RECONNECT_READY_RESULT) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            Type infoType = new TypeToken<ReadyResult>() {
            }.getType();
            ReadyResult readyResult = new Gson().fromJson(msgCommon.data, infoType);
            if (readyResult.result == 0) {
                ToastUtils.showToast(this, "Setup failed, please try it again!");
                return;
            }
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed, please try it again!");
            }, 30 * 1000);
            // 切换服务器
            setDeviceReconnect();
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_MQTT_RECONNECT) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed, please try it again!");
                return;
            }
            MQTTConfig mqttConfig = new Gson().fromJson(mMokoDevice.mqttInfo, MQTTConfig.class);
            mqttConfig.topicPublish = mMQTTSettings.publish_topic;
            mqttConfig.topicSubscribe = mMQTTSettings.subscribe_topic;
            mMokoDevice.topicPublish = mMQTTSettings.publish_topic;
            mMokoDevice.topicSubscribe = mMQTTSettings.subscribe_topic;
            mMokoDevice.mqttInfo = new Gson().toJson(mqttConfig, MQTTConfig.class);
            DBTools.getInstance(this).updateDevice(mMokoDevice);
            // 跳转首页，刷新数据
            Intent intent = new Intent(this, JSONMainActivity.class);
            intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
            intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_ID, mMokoDevice.deviceId);
            startActivity(intent);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        String deviceId = event.getDeviceId();
        if (!mMokoDevice.deviceId.equals(deviceId)) {
            return;
        }
        boolean online = event.isOnline();
        if (!online) {
            finish();
        }
    }

    public void onBack(View view) {
        finish();
    }

    public void onSave(View view) {
        if (isWindowLocked())
            return;
        if (isValid()) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            saveParams();
        }
    }


    public void onSelectCertificate(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCertificate();
    }


    private void saveParams() {
        final String host = etMqttHost.getText().toString().trim();
        final String port = etMqttPort.getText().toString().trim();
        final String clientId = etMqttClientId.getText().toString().trim();
        String topicSubscribe = etMqttSubscribeTopic.getText().toString().trim();
        String topicPublish = etMqttPublishTopic.getText().toString().trim();
        String apn = etApn.getText().toString().trim();
        String apnUsername = etApnUsername.getText().toString().trim();
        String apnPassword = etApnPassword.getText().toString().trim();

        mMQTTSettings.host = host;
        mMQTTSettings.port = Integer.parseInt(port);
        mMQTTSettings.client_id = clientId;
        if ("{device_name}/{device_id}/app_to_device".equals(topicSubscribe)) {
            topicSubscribe = String.format("%s/%s/app_to_device", mMokoDevice.name, mMokoDevice.deviceId);
        }
        if ("{device_name}/{device_id}/device_to_app".equals(topicPublish)) {
            topicPublish = String.format("%s/%s/device_to_app", mMokoDevice.name, mMokoDevice.deviceId);
        }
        mMQTTSettings.subscribe_topic = topicSubscribe;
        mMQTTSettings.publish_topic = topicPublish;

        mMQTTSettings.clean_session = generalFragment.isCleanSession() ? 1 : 0;
        mMQTTSettings.qos = generalFragment.getQos();
        mMQTTSettings.keepalive = generalFragment.getKeepAlive();
        mMQTTSettings.username = userFragment.getUsername();
        mMQTTSettings.password = userFragment.getPassword();
        mMQTTSettings.encryption_type = sslFragment.getConnectMode();
        if (mMQTTSettings.encryption_type > 0) {
            mMQTTSettings.cert_host = sslFragment.getSSLHost();
            mMQTTSettings.cert_port = sslFragment.getSSLPort();
        }
        if (mMQTTSettings.encryption_type == 1) {
            mMQTTSettings.ca_cert_path = sslFragment.getCAPath();
        }
        if (mMQTTSettings.encryption_type == 2) {
            mMQTTSettings.ca_cert_path = sslFragment.getCAPath();
            mMQTTSettings.client_cert_path = sslFragment.getClientCerPath();
            mMQTTSettings.client_key_path = sslFragment.getClientKeyPath();
        }
        mLWTSettings.lwt_enable = lwtFragment.getLwtEnable() ? 1 : 0;
        mLWTSettings.lwt_retain = lwtFragment.getLwtRetain() ? 1 : 0;
        mLWTSettings.lwt_qos = lwtFragment.getQos();
        String lwtTopic = lwtFragment.getTopic();
        if ("{device_name}/{device_id}/device_to_app".equals(lwtTopic)) {
            lwtTopic = String.format("%s/%s/device_to_app", mMokoDevice.name, mMokoDevice.deviceId);
        }
        mLWTSettings.lwt_topic = lwtTopic;
        mLWTSettings.lwt_message = lwtFragment.getPayload();
        mAPNSettings.apn = apn;
        mAPNSettings.apn_username = apnUsername;
        mAPNSettings.apn_password = apnPassword;
        mNetworkSettings.network_priority = mSelectedNetworkPriority;

        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
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


    private void setMQTTSettings() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        String message = MQTTMessageAssembler.assembleWriteMQTTSettings(deviceParams, mMQTTSettings);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_MQTT_SETTINGS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setLWTSettings() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        String message = MQTTMessageAssembler.assembleWriteLWTSettings(deviceParams, mLWTSettings);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_LWT_SETTINGS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setAPNSettings() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        String message = MQTTMessageAssembler.assembleWriteAPNSettings(deviceParams, mAPNSettings);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_APN_SETTINGS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setNetworkSettings() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        String message = MQTTMessageAssembler.assembleWriteNetworkSettings(deviceParams, mNetworkSettings);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_NETWORK_PRIORITY, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMQTTConfigFinish() {
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        String message = MQTTMessageAssembler.assembleWriteMQTTConfigFinish(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_MQTT_CONFIG_FINISH, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setDeviceReconnect() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleWriteDeviceReconnect(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_MQTT_RECONNECT, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private boolean isValid() {
        String host = etMqttHost.getText().toString().trim();
        String port = etMqttPort.getText().toString().trim();
        String clientId = etMqttClientId.getText().toString().trim();
        String topicSubscribe = etMqttSubscribeTopic.getText().toString().trim();
        String topicPublish = etMqttPublishTopic.getText().toString().trim();
        if (TextUtils.isEmpty(host)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_host));
            return false;
        }
        if (TextUtils.isEmpty(port)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port_empty));
            return false;
        }
        if (Integer.parseInt(port) < 1 || Integer.parseInt(port) > 65535) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port));
            return false;
        }
        if (TextUtils.isEmpty(clientId)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_client_id_empty));
            return false;
        }
        if (TextUtils.isEmpty(topicSubscribe)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_topic_subscribe));
            return false;
        }
        if (TextUtils.isEmpty(topicPublish)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_topic_publish));
            return false;
        }
        if (topicPublish.equals(topicSubscribe)) {
            ToastUtils.showToast(this, "Subscribed and published topic can't be same !");
            return false;
        }
        if (!generalFragment.isValid() || !sslFragment.isValid() || !lwtFragment.isValid())
            return false;
        return true;
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        if (checkedId == R.id.rb_general)
            vpMqtt.setCurrentItem(0);
        else if (checkedId == R.id.rb_user)
            vpMqtt.setCurrentItem(1);
        else if (checkedId == R.id.rb_ssl)
            vpMqtt.setCurrentItem(2);
        else if (checkedId == R.id.rb_lwt)
            vpMqtt.setCurrentItem(3);
    }

    public void selectNetworkPriority(View view) {
        if (isWindowLocked())
            return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mNetworkPriority, mSelectedNetworkPriority);
        dialog.setListener(value -> {
            mSelectedNetworkPriority = value;
            tvNetworkPriority.setText(mNetworkPriority.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }
}
