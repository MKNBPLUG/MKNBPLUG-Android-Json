package com.moko.mknbplugjson.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.adapter.MQTTFragmentAdapter;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityMqttDeviceModifyBinding;
import com.moko.mknbplugjson.db.DBTools;
import com.moko.mknbplugjson.dialog.AlertMessageDialog;
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
import com.moko.support.json.entity.WorkMode;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class ModifyMQTTSettingsActivity extends BaseActivity<ActivityMqttDeviceModifyBinding> implements RadioGroup.OnCheckedChangeListener {
    public static String TAG = ModifyMQTTSettingsActivity.class.getSimpleName();
    private final String FILTER_ASCII = "[ -~]*";
    private GeneralDeviceFragment generalFragment;
    private UserDeviceFragment userFragment;
    private SSLDevicePathFragment sslFragment;
    private LWTFragment lwtFragment;
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

    @Override
    protected void onCreate() {
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMQTTSettings = new MQTTSettings();
        mLWTSettings = new LWTSettings();
        mAPNSettings = new APNSettings();
        mNetworkSettings = new NetworkSettings();
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etMqttHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etMqttClientId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etMqttSubscribeTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        mBind.etMqttPublishTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        mBind.etApn.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100), filter});
        mBind.etApnUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(127), filter});
        mBind.etApnPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(127), filter});
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
        MQTTFragmentAdapter adapter = new MQTTFragmentAdapter(this);
        adapter.setFragmentList(fragments);
        mBind.vpMqtt.setAdapter(adapter);
        mBind.vpMqtt.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    mBind.rbGeneral.setChecked(true);
                } else if (position == 1) {
                    mBind.rbUser.setChecked(true);
                } else if (position == 2) {
                    mBind.rbSsl.setChecked(true);
                } else if (position == 3) {
                    mBind.rbLwt.setChecked(true);
                }
            }
        });
        mBind.vpMqtt.setOffscreenPageLimit(4);
        mBind.rgMqtt.setOnCheckedChangeListener(this);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected ActivityMqttDeviceModifyBinding getViewBinding() {
        return ActivityMqttDeviceModifyBinding.inflate(getLayoutInflater());
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
//        generalFragment.setCleanSession(mMQTTSettings.clean_session == 1);
//        generalFragment.setQos(mMQTTSettings.qos);
//        generalFragment.setKeepAlive(mMQTTSettings.keepalive);
//        lwtFragment.setQos(mLWTSettings.lwt_qos);
//        lwtFragment.setTopic(mLWTSettings.lwt_topic);
//        lwtFragment.setPayload(mLWTSettings.lwt_message);
        //首先读取设备的状态
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        readDeviceStatus();
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
                dialog.setMessage("Device is in debug mode, \nthis function is unvailable!");
                dialog.setCancelGone();
                dialog.setConfirm("OK");
                dialog.setOnAlertConfirmListener(this::finish);
                dialog.show(getSupportFragmentManager());
            } else {
                //读取设备参数
                getMqttParams();
                getMqttLwtParams();
                getMqttApnParams();
                getNetworkPriority();
            }
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_DEVICE_SETTINGS) {
            //读取mqtt基本参数
            if (msgCommon.result_code != 0) return;
            Type infoType = new TypeToken<MQTTSettings>() {
            }.getType();
            MQTTSettings params = new Gson().fromJson(msgCommon.data, infoType);
            mMQTTSettings.host = params.host;
            mMQTTSettings.port = params.port;
            mMQTTSettings.client_id = params.client_id;
            mMQTTSettings.subscribe_topic = params.subscribe_topic;
            mMQTTSettings.publish_topic = params.publish_topic;
            mMQTTSettings.clean_session = params.clean_session;
            mMQTTSettings.qos = params.qos;
            mMQTTSettings.keepalive = params.keepalive;
            mMQTTSettings.username = params.username;
            mMQTTSettings.password = params.password;
            mMQTTSettings.encryption_type = params.encryption_type;
            mMQTTSettings.ca_cert_url = params.ca_cert_url;
            mMQTTSettings.client_cert_url = params.client_cert_url;
            mMQTTSettings.client_key_url = params.client_key_url;
            setMqttParams();
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_LWT_SETTINGS) {
            //获取mqtt遗嘱参数
            if (msgCommon.result_code != 0) return;
            Type infoType = new TypeToken<LWTSettings>() {
            }.getType();
            LWTSettings params = new Gson().fromJson(msgCommon.data, infoType);
            mLWTSettings.lwt_enable = params.lwt_enable;
            mLWTSettings.lwt_qos = params.lwt_qos;
            mLWTSettings.lwt_retain = params.lwt_retain;
            mLWTSettings.lwt_topic = params.lwt_topic;
            mLWTSettings.lwt_message = params.lwt_message;

            lwtFragment.setLwtEnable(mLWTSettings.lwt_enable == 1);
            lwtFragment.setLwtRetain(mLWTSettings.lwt_retain == 1);
            lwtFragment.setQos(mLWTSettings.lwt_qos);
            lwtFragment.setTopic(mLWTSettings.lwt_topic);
            lwtFragment.setPayload(mLWTSettings.lwt_message);
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_APN_SETTINGS) {
            //获取apn参数
            if (msgCommon.result_code != 0) return;
            Type infoType = new TypeToken<APNSettings>() {
            }.getType();
            APNSettings params = new Gson().fromJson(msgCommon.data, infoType);
            mAPNSettings.apn = params.apn;
            mAPNSettings.apn_username = params.apn_username;
            mAPNSettings.apn_password = params.apn_password;
            //apn信息
            mBind.etApn.setText(mAPNSettings.apn);
            mBind.etApn.setSelection(mBind.etApn.getText().length());
            mBind.etApnUsername.setText(mAPNSettings.apn_username);
            mBind.etApnUsername.setSelection(mBind.etApnUsername.getText().length());
            mBind.etApnPassword.setText(mAPNSettings.apn_password);
            mBind.etApnPassword.setSelection(mBind.etApnPassword.getText().length());
        }
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_NETWORK_PRIORITY) {
            //网络制式
            if (msgCommon.result_code != 0) return;
            Type infoType = new TypeToken<NetworkSettings>() {
            }.getType();
            NetworkSettings params = new Gson().fromJson(msgCommon.data, infoType);
            mSelectedNetworkPriority = params.network_priority;
            mBind.tvNetworkPriority.setText(mNetworkPriority.get(mSelectedNetworkPriority));
        }
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
            intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_MAC, mMokoDevice.mac);
            startActivity(intent);
        }
    }

    //设置页面参数
    private void setMqttParams() {
        mBind.etMqttHost.setText(mMQTTSettings.host);
        mBind.etMqttHost.setSelection(mBind.etMqttHost.getText().length());
        mBind.etMqttPort.setText(String.valueOf(mMQTTSettings.port));
        mBind.etMqttPort.setSelection(mBind.etMqttPort.getText().length());
        mBind.etMqttClientId.setText(mMQTTSettings.client_id);
        mBind.etMqttClientId.setSelection(mBind.etMqttClientId.getText().length());
        mBind.etMqttSubscribeTopic.setText(mMQTTSettings.subscribe_topic);
        mBind.etMqttSubscribeTopic.setSelection(mBind.etMqttSubscribeTopic.getText().length());
        mBind.etMqttPublishTopic.setText(mMQTTSettings.publish_topic);
        mBind.etMqttPublishTopic.setSelection(mBind.etMqttPublishTopic.getText().length());
        generalFragment.setCleanSession(mMQTTSettings.clean_session == 1);
        generalFragment.setQos(mMQTTSettings.qos);
        generalFragment.setKeepAlive(mMQTTSettings.keepalive);
        userFragment.setUserName(mMQTTSettings.username);
        userFragment.setPassword(mMQTTSettings.password);
        sslFragment.setConnectMode(mMQTTSettings.encryption_type);
        sslFragment.setCAPath(mMQTTSettings.ca_cert_url);
        sslFragment.setClientCertPath(mMQTTSettings.client_cert_url);
        sslFragment.setClientKeyPath(mMQTTSettings.client_key_url);
    }

    /**
     * 读取页面默认参数
     */
    private void getMqttParams() {
        XLog.i("读取mqtt参数");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadSettingsForDevice(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_DEVICE_SETTINGS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取mqtt遗嘱参数
     */
    private void getMqttLwtParams() {
        XLog.i("读取mqtt遗嘱参数");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadSettingsForLWT(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_LWT_SETTINGS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getMqttApnParams() {
        XLog.i("读取mqtt apn参数");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadSettingsForApn(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_APN_SETTINGS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getNetworkPriority() {
        XLog.i("读取网络制式配置参数");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadNetworkPriority(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_NETWORK_PRIORITY, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onBack(View view) {
        finish();
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
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
        if (isWindowLocked()) return;
        sslFragment.selectCertificate();
    }

    private void saveParams() {
        final String host = mBind.etMqttHost.getText().toString().trim();
        final String port = mBind.etMqttPort.getText().toString().trim();
        final String clientId = mBind.etMqttClientId.getText().toString().trim();
        String topicSubscribe = mBind.etMqttSubscribeTopic.getText().toString().trim();
        String topicPublish = mBind.etMqttPublishTopic.getText().toString().trim();
        String apn = mBind.etApn.getText().toString().trim();
        String apnUsername = mBind.etApnUsername.getText().toString().trim();
        String apnPassword = mBind.etApnPassword.getText().toString().trim();

        mMQTTSettings.host = host;
        mMQTTSettings.port = Integer.parseInt(port);
        mMQTTSettings.client_id = clientId;
//        if ("{device_name}/{device_id}/app_to_device".equals(topicSubscribe)) {
//            topicSubscribe = String.format("%s/%s/app_to_device", mMokoDevice.name, mMokoDevice.deviceId);
//        }
//        if ("{device_name}/{device_id}/device_to_app".equals(topicPublish)) {
//            topicPublish = String.format("%s/%s/device_to_app", mMokoDevice.name, mMokoDevice.deviceId);
//        }
        mMQTTSettings.subscribe_topic = topicSubscribe;
        mMQTTSettings.publish_topic = topicPublish;

        mMQTTSettings.clean_session = generalFragment.isCleanSession() ? 1 : 0;
        mMQTTSettings.qos = generalFragment.getQos();
        mMQTTSettings.keepalive = generalFragment.getKeepAlive();
        mMQTTSettings.username = userFragment.getUsername();
        mMQTTSettings.password = userFragment.getPassword();
        mMQTTSettings.encryption_type = sslFragment.getConnectMode();
        if (mMQTTSettings.encryption_type == 1) {
            mMQTTSettings.ca_cert_url = sslFragment.getCAPath();
        }
        if (mMQTTSettings.encryption_type == 2) {
            mMQTTSettings.ca_cert_url = sslFragment.getCAPath();
            mMQTTSettings.client_cert_url = sslFragment.getClientCerPath();
            mMQTTSettings.client_key_url = sslFragment.getClientKeyPath();
        }
        mLWTSettings.lwt_enable = lwtFragment.getLwtEnable() ? 1 : 0;
        mLWTSettings.lwt_retain = lwtFragment.getLwtRetain() ? 1 : 0;
        mLWTSettings.lwt_qos = lwtFragment.getQos();
        //        if ("{device_name}/{device_id}/device_to_app".equals(lwtTopic)) {
//            lwtTopic = String.format("%s/%s/device_to_app", mMokoDevice.name, mMokoDevice.deviceId);
//        }
        mLWTSettings.lwt_topic = lwtFragment.getTopic();
        mLWTSettings.lwt_message = lwtFragment.getPayload();
        mAPNSettings.apn = apn;
        mAPNSettings.apn_username = apnUsername;
        mAPNSettings.apn_password = apnPassword;
        mNetworkSettings.network_priority = mSelectedNetworkPriority;

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

    private void setMQTTSettings() {
        DeviceParams deviceParams = new DeviceParams();
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
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleWriteDeviceReconnect(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_MQTT_RECONNECT, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        String host = mBind.etMqttHost.getText().toString().trim();
        String port = mBind.etMqttPort.getText().toString().trim();
        String clientId = mBind.etMqttClientId.getText().toString().trim();
        String topicSubscribe = mBind.etMqttSubscribeTopic.getText().toString().trim();
        String topicPublish = mBind.etMqttPublishTopic.getText().toString().trim();
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
        return generalFragment.isValid() && sslFragment.isValid() && lwtFragment.isValid();
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        if (checkedId == R.id.rb_general)
            mBind.vpMqtt.setCurrentItem(0);
        else if (checkedId == R.id.rb_user)
            mBind.vpMqtt.setCurrentItem(1);
        else if (checkedId == R.id.rb_ssl)
            mBind.vpMqtt.setCurrentItem(2);
        else if (checkedId == R.id.rb_lwt)
            mBind.vpMqtt.setCurrentItem(3);
    }

    public void selectNetworkPriority(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mNetworkPriority, mSelectedNetworkPriority);
        dialog.setListener(value -> {
            mSelectedNetworkPriority = value;
            mBind.tvNetworkPriority.setText(mNetworkPriority.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }
}
