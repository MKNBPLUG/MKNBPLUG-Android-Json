package com.moko.mknbplugjson.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.R2;
import com.moko.mknbplugjson.adapter.MQTTFragmentAdapter;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.db.DBTools;
import com.moko.mknbplugjson.dialog.BottomDialog;
import com.moko.mknbplugjson.dialog.CustomDialog;
import com.moko.mknbplugjson.entity.MQTTConfig;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.fragment.GeneralDeviceFragment;
import com.moko.mknbplugjson.fragment.LWTFragment;
import com.moko.mknbplugjson.fragment.SSLDeviceFragment;
import com.moko.mknbplugjson.fragment.UserDeviceFragment;
import com.moko.mknbplugjson.utils.FileUtils;
import com.moko.mknbplugjson.utils.SPUtiles;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.mknbplugjson.utils.Utils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.MokoSupport;
import com.moko.support.json.OrderTaskAssembler;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OrderCHAR;
import com.moko.support.json.entity.ParamsKeyEnum;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import butterknife.BindView;
import butterknife.ButterKnife;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class SetDeviceMQTTActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {
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
    @BindView(R2.id.vp_mqtt)
    ViewPager2 vpMqtt;
    @BindView(R2.id.rg_mqtt)
    RadioGroup rgMqtt;
    @BindView(R2.id.et_device_id)
    EditText etDeviceId;
    @BindView(R2.id.et_ntp_url)
    EditText etNtpUrl;
    @BindView(R2.id.tv_time_zone)
    TextView tvTimeZone;
    @BindView(R2.id.rb_lwt)
    RadioButton rbLwt;
    @BindView(R2.id.et_apn)
    EditText etApn;
    @BindView(R2.id.et_apn_username)
    EditText etApnUsername;
    @BindView(R2.id.et_apn_password)
    EditText etApnPassword;
    @BindView(R2.id.tv_network_priority)
    TextView tvNetworkPriority;
    @BindView(R2.id.cb_debug_mode)
    CheckBox cbDebugMode;
    private GeneralDeviceFragment generalFragment;
    private UserDeviceFragment userFragment;
    private SSLDeviceFragment sslFragment;
    private LWTFragment lwtFragment;
    private MQTTFragmentAdapter adapter;
    private ArrayList<Fragment> fragments;

    private MQTTConfig mqttAppConfig;
    private MQTTConfig mqttDeviceConfig;

    private ArrayList<String> mTimeZones;
    private int mSelectedTimeZone;
    private ArrayList<String> mNetworkPriority;
    private int mSelectedNetworkPriority;
    private String mSelectedDeviceName;
    private String mSelectedDeviceMac;
    private int mSelectedDeviceType;
    private int mSelectedDeviceMode;
    private boolean savedParamsError;
    private CustomDialog mqttConnDialog;
    private DonutProgress donutProgress;
    private boolean isSettingSuccess;
    private boolean isDeviceConnectSuccess;
    private Handler mHandler;
    private InputFilter filter;

    private String importFilePath;
    private String expertFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt_device);
        ButterKnife.bind(this);
        String MQTTConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        mqttAppConfig = new Gson().fromJson(MQTTConfigStr, MQTTConfig.class);
        mSelectedDeviceName = getIntent().getStringExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_NAME);
        mSelectedDeviceMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_MAC);
        mSelectedDeviceMode = getIntent().getIntExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_MODE, 0);
        mSelectedDeviceType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_TYPE, 0);
        if (TextUtils.isEmpty(MQTTConfigStr)) {
            mqttDeviceConfig = new MQTTConfig();
        } else {
            Gson gson = new Gson();
            mqttDeviceConfig = gson.fromJson(MQTTConfigStr, MQTTConfig.class);
            mqttDeviceConfig.connectMode = 0;
            mqttDeviceConfig.qos = 1;
            mqttDeviceConfig.keepAlive = 60;
            mqttDeviceConfig.clientId = "";
            mqttDeviceConfig.username = "";
            mqttDeviceConfig.password = "";
            mqttDeviceConfig.caPath = "";
            mqttDeviceConfig.clientKeyPath = "";
            mqttDeviceConfig.clientCertPath = "";
            mqttDeviceConfig.lwtTopic = "";
            mqttDeviceConfig.lwtPayload = "Offline";
            mqttDeviceConfig.apn = "";
            mqttDeviceConfig.apnUsername = "";
            mqttDeviceConfig.apnPassword = "";
            mqttDeviceConfig.topicPublish = "";
            mqttDeviceConfig.topicSubscribe = "";
            mqttDeviceConfig.timeZone = 24;
        }
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
        etDeviceId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
        etNtpUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        etApn.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100), filter});
        etApnUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(127), filter});
        etApnPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(127), filter});
        createFragment();
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
        mTimeZones = new ArrayList<>();
        for (int i = -24; i <= 28; i++) {
            if (i < 0) {
                if (i % 2 == 0) {
                    int j = Math.abs(i / 2);
                    mTimeZones.add(String.format("UTC-%02d:00", j));
                } else {
                    int j = Math.abs((i + 1) / 2);
                    mTimeZones.add(String.format("UTC-%02d:30", j));
                }
            } else if (i == 0) {
                mTimeZones.add("UTC");
            } else {
                if (i % 2 == 0) {
                    mTimeZones.add(String.format("UTC+%02d:00", i / 2));
                } else {
                    mTimeZones.add(String.format("UTC+%02d:30", (i - 1) / 2));
                }
            }
        }
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
        initData();
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            if (isSettingSuccess) {
                EventBus.getDefault().cancelEventDelivery(event);
                return;
            }
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                finish();
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
            dismissLoadingProgressDialog();
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            int responseType = response.responseType;
            byte[] value = response.responseValue;
            switch (orderCHAR) {
                case CHAR_PARAMS:
                    if (value.length >= 4) {
                        int header = value[0] & 0xFF;// 0xED
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header != 0xED)
                            return;
                        ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                        if (configKeyEnum == null) {
                            return;
                        }
                        int length = value[3] & 0xFF;
                        if (flag == 0x01) {
                            // write
                            int result = value[4] & 0xFF;
                            switch (configKeyEnum) {
                                case KEY_MQTT_HOST:
                                case KEY_MQTT_PORT:
                                case KEY_MQTT_USERNAME:
                                case KEY_MQTT_PASSWORD:
                                case KEY_MQTT_CLIENT_ID:
                                case KEY_MQTT_CLEAN_SESSION:
                                case KEY_MQTT_KEEP_ALIVE:
                                case KEY_MQTT_QOS:
                                case KEY_MQTT_SUBSCRIBE_TOPIC:
                                case KEY_MQTT_PUBLISH_TOPIC:
                                case KEY_MQTT_LWT_ENABLE:
                                case KEY_MQTT_LWT_QOS:
                                case KEY_MQTT_LWT_RETAIN:
                                case KEY_MQTT_LWT_TOPIC:
                                case KEY_MQTT_LWT_PAYLOAD:
                                case KEY_MQTT_DEVICE_ID:
                                case KEY_MQTT_CONNECT_MODE:
                                case KEY_MQTT_CA:
                                case KEY_MQTT_CLIENT_CERT:
                                case KEY_MQTT_CLIENT_KEY:
                                case KEY_NTP_URL:
                                case KEY_NTP_TIME_ZONE:
                                case KEY_APN:
                                case KEY_APN_USERNAME:
                                case KEY_APN_PASSWORD:
                                case KEY_NETWORK_PRIORITY:
                                case KEY_DATA_FORMAT:
                                    if (result != 1) {
                                        savedParamsError = true;
                                    }
                                    break;
                                case KEY_CHANGE_MODE:
                                    if (result != 1) {
                                        savedParamsError = true;
                                    }
                                    if (savedParamsError) {
                                        ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
                                    } else {
                                        isSettingSuccess = true;
                                        showConnMqttDialog();
                                        subscribeTopic();
                                    }
                                    break;
                            }
                        }
                        if (flag == 0x00) {
                            // read
                            switch (configKeyEnum) {
                                case KEY_DEVICE_NAME:
                                    if (length > 0) {
                                        byte[] data = Arrays.copyOfRange(value, 4, 4 + length);
                                        String name = new String(data);
                                        mSelectedDeviceName = name;
                                    }
                                    break;
                                case KEY_DEVICE_MAC:
                                    if (length > 0) {
                                        byte[] data = Arrays.copyOfRange(value, 4, 4 + length);
                                        String mac = MokoUtils.bytesToHexString(data);
                                        mSelectedDeviceMac = mac.toUpperCase();
                                    }
                                    break;
                            }
                        }
                    }
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        final String topic = event.getTopic();
        final String message = event.getMessage();
        if (TextUtils.isEmpty(topic) || isDeviceConnectSuccess) {
            return;
        }
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
        if (!mqttDeviceConfig.deviceId.equals(msgCommon.device_info.device_id)) {
            return;
        }
        if (msgCommon.msg_id != MQTTConstants.NOTIFY_MSG_ID_SWITCH_STATE)
            return;
        if (donutProgress == null)
            return;
        if (!isDeviceConnectSuccess) {
            isDeviceConnectSuccess = true;
            donutProgress.setProgress(100);
            donutProgress.setText(100 + "%");
            // 关闭进度条弹框，保存数据，跳转修改设备名称页面
            etMqttHost.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dismissConnMqttDialog();
                    MokoDevice mokoDevice = DBTools.getInstance(SetDeviceMQTTActivity.this).selectDeviceByMac(mSelectedDeviceMac);
                    String mqttConfigStr = new Gson().toJson(mqttDeviceConfig, MQTTConfig.class);
                    if (mokoDevice == null) {
                        mokoDevice = new MokoDevice();
                        mokoDevice.name = mSelectedDeviceName;
                        mokoDevice.mac = mSelectedDeviceMac;
                        mokoDevice.mqttInfo = mqttConfigStr;
                        mokoDevice.topicSubscribe = mqttDeviceConfig.topicSubscribe;
                        mokoDevice.topicPublish = mqttDeviceConfig.topicPublish;
                        mokoDevice.deviceId = mqttDeviceConfig.deviceId;
                        mokoDevice.deviceMode = mSelectedDeviceMode;
                        mokoDevice.deviceType= mSelectedDeviceType;
                        DBTools.getInstance(SetDeviceMQTTActivity.this).insertDevice(mokoDevice);
                    } else {
                        mokoDevice.name = mSelectedDeviceName;
                        mokoDevice.mac = mSelectedDeviceMac;
                        mokoDevice.mqttInfo = mqttConfigStr;
                        mokoDevice.topicSubscribe = mqttDeviceConfig.topicSubscribe;
                        mokoDevice.topicPublish = mqttDeviceConfig.topicPublish;
                        mokoDevice.deviceId = mqttDeviceConfig.deviceId;
                        mokoDevice.deviceMode = mSelectedDeviceMode;
                        mokoDevice.deviceType = mSelectedDeviceType;
                        DBTools.getInstance(SetDeviceMQTTActivity.this).updateDevice(mokoDevice);
                    }
                    Intent modifyIntent = new Intent(SetDeviceMQTTActivity.this, ModifyNameActivity.class);
                    modifyIntent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDevice);
                    startActivity(modifyIntent);
                }
            }, 500);
        }
    }

    private void createFragment() {
        fragments = new ArrayList<>();
        generalFragment = GeneralDeviceFragment.newInstance();
        userFragment = UserDeviceFragment.newInstance();
        sslFragment = SSLDeviceFragment.newInstance();
        lwtFragment = LWTFragment.newInstance();
        fragments.add(generalFragment);
        fragments.add(userFragment);
        fragments.add(sslFragment);
        fragments.add(lwtFragment);
    }

    private void initData() {
        etMqttHost.setText(mqttDeviceConfig.host);
        etMqttPort.setText(mqttDeviceConfig.port);
        etMqttClientId.setText(mqttDeviceConfig.clientId);
        if (!TextUtils.isEmpty(mqttDeviceConfig.topicSubscribe))
            etMqttSubscribeTopic.setText(mqttDeviceConfig.topicSubscribe);
        if (!TextUtils.isEmpty(mqttDeviceConfig.topicPublish))
            etMqttPublishTopic.setText(mqttDeviceConfig.topicPublish);

        generalFragment.setCleanSession(mqttDeviceConfig.cleanSession);
        generalFragment.setQos(mqttDeviceConfig.qos);
        generalFragment.setKeepAlive(mqttDeviceConfig.keepAlive);
        userFragment.setUserName(mqttDeviceConfig.username);
        userFragment.setPassword(mqttDeviceConfig.password);
        lwtFragment.setLwtEnable(mqttDeviceConfig.lwtEnable);
        lwtFragment.setLwtRetain(mqttDeviceConfig.lwtRetain);
        lwtFragment.setQos(mqttDeviceConfig.lwtQos);
        lwtFragment.setTopic(mqttDeviceConfig.lwtTopic);
        lwtFragment.setPayload(mqttDeviceConfig.lwtPayload);
        sslFragment.setConnectMode(mqttDeviceConfig.connectMode);
        sslFragment.setCAPath(mqttDeviceConfig.caPath);
        sslFragment.setClientKeyPath(mqttDeviceConfig.clientKeyPath);
        sslFragment.setClientCertPath(mqttDeviceConfig.clientCertPath);

        etDeviceId.setText(mqttDeviceConfig.deviceId);
        etNtpUrl.setText(mqttDeviceConfig.ntpUrl);
        mSelectedTimeZone = mqttDeviceConfig.timeZone;
        tvTimeZone.setText(mTimeZones.get(mSelectedTimeZone));
        etApn.setText(mqttDeviceConfig.apn);
        etApnUsername.setText(mqttDeviceConfig.apnUsername);
        etApnPassword.setText(mqttDeviceConfig.apnPassword);
        mSelectedNetworkPriority = mqttDeviceConfig.networkPriority;
        tvNetworkPriority.setText(mNetworkPriority.get(mSelectedNetworkPriority));
        cbDebugMode.setChecked(mqttDeviceConfig.debugModeEnable);
    }

    public void back(View view) {
        back();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        MokoSupport.getInstance().disConnectBle();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        if (checkedId == R.id.rb_general)
            vpMqtt.setCurrentItem(0);
        else if (checkedId == R.id.rb_user)
            vpMqtt.setCurrentItem(1);
        else if (checkedId == R.id.rb_ssl)
            vpMqtt.setCurrentItem(2);
        else if (checkedId == R.id.rb_lwt)
            vpMqtt.setCurrentItem(3);
    }

    public void onSave(View view) {
        if (isWindowLocked())
            return;
        if (isVerify()) return;
        if ("{device_name}/{device_id}/app_to_device".equals(mqttDeviceConfig.topicSubscribe)) {
            mqttDeviceConfig.topicSubscribe = String.format("%s/%s/app_to_device", mSelectedDeviceName, mqttDeviceConfig.deviceId);
        }
        if ("{device_name}/{device_id}/device_to_app".equals(mqttDeviceConfig.topicPublish)) {
            mqttDeviceConfig.topicPublish = String.format("%s/%s/device_to_app", mSelectedDeviceName, mqttDeviceConfig.deviceId);
        }
        setMQTTDeviceConfig();
    }

    private boolean isVerify() {
        String host = etMqttHost.getText().toString().replaceAll(" ", "");
        String port = etMqttPort.getText().toString();
        String clientId = etMqttClientId.getText().toString().replaceAll(" ", "");
        String deviceId = etDeviceId.getText().toString().replaceAll(" ", "");
        String topicSubscribe = etMqttSubscribeTopic.getText().toString().replaceAll(" ", "");
        String topicPublish = etMqttPublishTopic.getText().toString().replaceAll(" ", "");
        String ntpUrl = etNtpUrl.getText().toString().replaceAll(" ", "");
        String apn = etApn.getText().toString().replaceAll(" ", "");
        String apnUsername = etApnUsername.getText().toString().replaceAll(" ", "");
        String apnPassword = etApnPassword.getText().toString().replaceAll(" ", "");

        if (TextUtils.isEmpty(host)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_host));
            return true;
        }
        if (TextUtils.isEmpty(port)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port_empty));
            return true;
        }
        if (Integer.parseInt(port) < 1 || Integer.parseInt(port) > 65535) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port));
            return true;
        }
        if (TextUtils.isEmpty(clientId)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_client_id_empty));
            return true;
        }
        if (TextUtils.isEmpty(deviceId)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_device_id_empty));
            return true;
        }
        if (!generalFragment.isValid() || !sslFragment.isValid() || !lwtFragment.isValid())
            return true;
        mqttDeviceConfig.host = host;
        mqttDeviceConfig.port = port;
        mqttDeviceConfig.clientId = clientId;
        mqttDeviceConfig.cleanSession = generalFragment.isCleanSession();
        mqttDeviceConfig.qos = generalFragment.getQos();
        mqttDeviceConfig.keepAlive = generalFragment.getKeepAlive();
        mqttDeviceConfig.keepAlive = generalFragment.getKeepAlive();
        mqttDeviceConfig.topicSubscribe = topicSubscribe;
        mqttDeviceConfig.topicPublish = topicPublish;
        mqttDeviceConfig.username = userFragment.getUsername();
        mqttDeviceConfig.password = userFragment.getPassword();
        mqttDeviceConfig.connectMode = sslFragment.getConnectMode();
        mqttDeviceConfig.caPath = sslFragment.getCaPath();
        mqttDeviceConfig.clientKeyPath = sslFragment.getClientKeyPath();
        mqttDeviceConfig.clientCertPath = sslFragment.getClientCertPath();
        mqttDeviceConfig.lwtEnable = lwtFragment.getLwtEnable();
        mqttDeviceConfig.lwtRetain = lwtFragment.getLwtRetain();
        mqttDeviceConfig.lwtQos = lwtFragment.getQos();
        mqttDeviceConfig.lwtTopic = lwtFragment.getTopic();
        mqttDeviceConfig.lwtPayload = lwtFragment.getPayload();
        mqttDeviceConfig.deviceId = deviceId;
        mqttDeviceConfig.ntpUrl = ntpUrl;
        mqttDeviceConfig.timeZone = mSelectedTimeZone - 24;
        mqttDeviceConfig.apn = apn;
        mqttDeviceConfig.apnUsername = apnUsername;
        mqttDeviceConfig.apnPassword = apnPassword;
        mqttDeviceConfig.networkPriority = mSelectedNetworkPriority;
        mqttDeviceConfig.debugModeEnable = cbDebugMode.isChecked();

        if (!mqttDeviceConfig.topicPublish.isEmpty() && !mqttDeviceConfig.topicSubscribe.isEmpty()
                && mqttDeviceConfig.topicPublish.equals(mqttDeviceConfig.topicSubscribe)) {
            ToastUtils.showToast(this, "Subscribed and published topic can't be same !");
            return true;
        }
        return false;
    }

//    private void showWifiInputDialog() {
//        View wifiInputView = LayoutInflater.from(this).inflate(R.layout.wifi_input_content, null);
//        final EditText etSSID = wifiInputView.findViewById(R.id.et_ssid);
//        final EditText etPassword = wifiInputView.findViewById(R.id.et_password);
//        etSSID.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
//        etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
//
//        CustomDialog dialog = new CustomDialog.Builder(this)
//                .setContentView(wifiInputView)
//                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                })
//                .setNegativeButton(R.string.confirm, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        mWifiSSID = etSSID.getText().toString();
//                        // 获取WIFI后，连接成功后发给设备
//                        if (TextUtils.isEmpty(mWifiSSID)) {
//                            ToastUtils.showToast(SetDeviceMQTTActivity.this, getString(R.string.wifi_verify_empty));
//                            return;
//                        }
//                        dialog.dismiss();
//                        mWifiPassword = etPassword.getText().toString();
//                    }
//                })
//                .create();
//        dialog.show();
//    }

    private void setMQTTDeviceConfig() {
        try {
            showLoadingProgressDialog();
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getDeviceMac());
            orderTasks.add(OrderTaskAssembler.getDeviceName());
            orderTasks.add(OrderTaskAssembler.setMqttHost(mqttDeviceConfig.host));
            orderTasks.add(OrderTaskAssembler.setMqttPort(Integer.parseInt(mqttDeviceConfig.port)));
            orderTasks.add(OrderTaskAssembler.setMqttClientId(mqttDeviceConfig.clientId));
            orderTasks.add(OrderTaskAssembler.setMqttCleanSession(mqttDeviceConfig.cleanSession ? 1 : 0));
            orderTasks.add(OrderTaskAssembler.setMqttKeepAlive(mqttDeviceConfig.keepAlive));
            orderTasks.add(OrderTaskAssembler.setMqttQos(mqttDeviceConfig.qos));
            orderTasks.add(OrderTaskAssembler.setMqttSubscribeTopic(mqttDeviceConfig.topicSubscribe));
            orderTasks.add(OrderTaskAssembler.setMqttPublishTopic(mqttDeviceConfig.topicPublish));
            orderTasks.add(OrderTaskAssembler.setLwtEnable(mqttDeviceConfig.lwtEnable ? 1 : 0));
            orderTasks.add(OrderTaskAssembler.setLwtQos(mqttDeviceConfig.lwtQos));
            orderTasks.add(OrderTaskAssembler.setLwtRetain(mqttDeviceConfig.lwtRetain ? 1 : 0));
            orderTasks.add(OrderTaskAssembler.setLwtTopic(mqttDeviceConfig.lwtTopic));
            orderTasks.add(OrderTaskAssembler.setLwtPayload(mqttDeviceConfig.lwtPayload));
            orderTasks.add(OrderTaskAssembler.setMqttDeivceId(mqttDeviceConfig.deviceId));
            if (!TextUtils.isEmpty(mqttDeviceConfig.username)) {
                orderTasks.add(OrderTaskAssembler.setMqttUserName(mqttDeviceConfig.username));
            }
            if (!TextUtils.isEmpty(mqttDeviceConfig.password)) {
                orderTasks.add(OrderTaskAssembler.setMqttPassword(mqttDeviceConfig.password));
            }
            orderTasks.add(OrderTaskAssembler.setMqttConnectMode(mqttDeviceConfig.connectMode));
            if (mqttDeviceConfig.connectMode == 1) {
                File file = new File(mqttDeviceConfig.caPath);
                orderTasks.add(OrderTaskAssembler.setCA(file));
            } else if (mqttDeviceConfig.connectMode == 2) {
                File caFile = new File(mqttDeviceConfig.caPath);
                orderTasks.add(OrderTaskAssembler.setCA(caFile));
                File clientKeyFile = new File(mqttDeviceConfig.clientKeyPath);
                orderTasks.add(OrderTaskAssembler.setClientKey(clientKeyFile));
                File clientCertFile = new File(mqttDeviceConfig.clientCertPath);
                orderTasks.add(OrderTaskAssembler.setClientCert(clientCertFile));
            }
            if (!TextUtils.isEmpty(mqttDeviceConfig.ntpUrl)) {
                orderTasks.add(OrderTaskAssembler.setNTPUrl(mqttDeviceConfig.ntpUrl));
            }
            orderTasks.add(OrderTaskAssembler.setNTPTimezone(mqttDeviceConfig.timeZone));
            if (!TextUtils.isEmpty(mqttDeviceConfig.apn)) {
                orderTasks.add(OrderTaskAssembler.setApn(mqttDeviceConfig.apn));
            }
            if (!TextUtils.isEmpty(mqttDeviceConfig.apnUsername)) {
                orderTasks.add(OrderTaskAssembler.setApnUsername(mqttDeviceConfig.apnUsername));
            }
            if (!TextUtils.isEmpty(mqttDeviceConfig.apnPassword)) {
                orderTasks.add(OrderTaskAssembler.setApnPassword(mqttDeviceConfig.apnPassword));
            }
            orderTasks.add(OrderTaskAssembler.setNetworkPriority(mqttDeviceConfig.networkPriority));
            // 0:JSON
            orderTasks.add(OrderTaskAssembler.setDataFormat(0));
            orderTasks.add(OrderTaskAssembler.setMode(cbDebugMode.isChecked() ? 1 : 0));
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } catch (Exception e) {
            ToastUtils.showToast(this, "File is missing");
        }
    }

    public void selectCertificate(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCertificate();
    }

    public void selectCAFile(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCAFile();
    }

    public void selectKeyFile(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectKeyFile();
    }

    public void selectCertFile(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCertFile();
    }

    public void selectTimeZone(View view) {
        if (isWindowLocked())
            return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mTimeZones, mSelectedTimeZone);
        dialog.setListener(value -> {
            mSelectedTimeZone = value;
            tvTimeZone.setText(mTimeZones.get(mSelectedTimeZone));
        });
        dialog.show(getSupportFragmentManager());
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

    private int progress;

    private void showConnMqttDialog() {
        isDeviceConnectSuccess = false;
        View view = LayoutInflater.from(this).inflate(R.layout.mqtt_conn_content, null);
        donutProgress = view.findViewById(R.id.dp_progress);
        mqttConnDialog = new CustomDialog.Builder(this)
                .setContentView(view)
                .create();
        mqttConnDialog.setCancelable(false);
        mqttConnDialog.show();
        new Thread(() -> {
            progress = 0;
            while (progress <= 100 && !isDeviceConnectSuccess) {
                runOnUiThread(() -> {
                    donutProgress.setProgress(progress);
                    donutProgress.setText(progress + "%");
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                progress++;
            }
        }).start();
        mHandler.postDelayed(() -> {
            if (!isDeviceConnectSuccess) {
                isDeviceConnectSuccess = true;
                isSettingSuccess = false;
                dismissConnMqttDialog();
                ToastUtils.showToast(SetDeviceMQTTActivity.this, getString(R.string.mqtt_connecting_timeout));
                finish();
            }
        }, 90 * 1000);
    }

    private void dismissConnMqttDialog() {
        if (mqttConnDialog != null && !isFinishing() && mqttConnDialog.isShowing()) {
            isDeviceConnectSuccess = true;
            isSettingSuccess = false;
            mqttConnDialog.dismiss();
            mHandler.removeMessages(0);
        }
    }

    private void subscribeTopic() {
        // 订阅
        try {
            if (TextUtils.isEmpty(mqttAppConfig.topicSubscribe)) {
                MQTTSupport.getInstance().subscribe(mqttDeviceConfig.topicPublish, mqttAppConfig.qos);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onExportSettings(View view) {
        if (isWindowLocked())
            return;
        if (isVerify()) return;
        if ("{device_name}/{device_id}/app_to_device".equals(mqttDeviceConfig.topicSubscribe)) {
            mqttDeviceConfig.topicSubscribe = "";
        }
        if ("{device_name}/{device_id}/device_to_app".equals(mqttDeviceConfig.topicPublish)) {
            mqttDeviceConfig.topicPublish = "";
        }
        final File expertFile = new File(expertFilePath);
        final File importFile = new File(importFilePath);
        try {
            Workbook workbook = Workbook.getWorkbook(importFile);
            WritableWorkbook wwb = Workbook.createWorkbook(expertFile, workbook);

            WritableSheet ws = wwb.getSheet(0);
            ws.addCell(new Label(1, 1, String.format("value:%s", mqttDeviceConfig.host)));
            ws.addCell(new Label(1, 2, String.format("value:%s", mqttDeviceConfig.port)));
            ws.addCell(new Label(1, 3, String.format("value:%s", mqttDeviceConfig.clientId)));
            if (!TextUtils.isEmpty(mqttDeviceConfig.topicSubscribe))
                ws.addCell(new Label(1, 4, String.format("value:%s", mqttDeviceConfig.topicSubscribe)));
            if (!TextUtils.isEmpty(mqttDeviceConfig.topicPublish))
                ws.addCell(new Label(1, 5, String.format("value:%s", mqttDeviceConfig.topicPublish)));
            ws.addCell(new Label(1, 6, String.format("value:%s", mqttDeviceConfig.cleanSession ? "1" : "0")));
            ws.addCell(new Label(1, 7, String.format("value:%d", mqttDeviceConfig.qos)));
            ws.addCell(new Label(1, 8, String.format("value:%d", mqttDeviceConfig.keepAlive)));
            if (!TextUtils.isEmpty(mqttDeviceConfig.username))
                ws.addCell(new Label(1, 9, String.format("value:%s", mqttDeviceConfig.username)));
            if (!TextUtils.isEmpty(mqttDeviceConfig.password))
                ws.addCell(new Label(1, 10, String.format("value:%s", mqttDeviceConfig.password)));
            if (mqttDeviceConfig.connectMode > 0) {
                ws.addCell(new Label(1, 11, String.format("value:%d", 1)));
                ws.addCell(new Label(1, 12, String.format("value:%d", mqttDeviceConfig.connectMode)));
            } else {
                ws.addCell(new Label(1, 11, String.format("value:%d", mqttDeviceConfig.connectMode)));
                ws.addCell(new Label(1, 12, String.format("value:%d", 1)));
            }
            ws.addCell(new Label(1, 13, String.format("value:%d", mqttDeviceConfig.lwtEnable ? "1" : "0")));
            ws.addCell(new Label(1, 14, String.format("value:%d", mqttDeviceConfig.lwtRetain ? "1" : "0")));
            ws.addCell(new Label(1, 15, String.format("value:%d", mqttDeviceConfig.lwtQos)));
            ws.addCell(new Label(1, 16, String.format("value:%s", mqttDeviceConfig.lwtTopic)));
            ws.addCell(new Label(1, 17, String.format("value:%s", mqttDeviceConfig.lwtPayload)));
            ws.addCell(new Label(1, 18, String.format("value:%s", mqttDeviceConfig.deviceId)));
            if (!TextUtils.isEmpty(mqttDeviceConfig.ntpUrl))
                ws.addCell(new Label(1, 19, String.format("value:%s", mqttDeviceConfig.ntpUrl)));
            ws.addCell(new Label(1, 20, String.format("value:%s", mqttDeviceConfig.timeZone)));
            if (!TextUtils.isEmpty(mqttDeviceConfig.apn))
                ws.addCell(new Label(1, 21, String.format("value:%s", mqttDeviceConfig.apn)));
            if (!TextUtils.isEmpty(mqttDeviceConfig.apnUsername))
                ws.addCell(new Label(1, 22, String.format("value:%s", mqttDeviceConfig.apnUsername)));
            if (!TextUtils.isEmpty(mqttDeviceConfig.apnPassword))
                ws.addCell(new Label(1, 23, String.format("value:%s", mqttDeviceConfig.apnPassword)));
            ws.addCell(new Label(1, 24, String.format("value:%s", mqttDeviceConfig.networkPriority)));
            ws.addCell(new Label(1, 25, String.format("value:%s", mqttDeviceConfig.debugModeEnable ? "1" : "0")));


            // 从内存中写入文件中
            workbook.close();
            wwb.write();
            wwb.close();
            Utils.sendEmail(this, "", "", "Settings for Device", "Choose Email Client", expertFile);
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showToast(this, "export error！");
        }
    }

    public void onImportSettings(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), AppConstants.REQUEST_CODE_OPEN_DEVICE_SETTINGS_FILE);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(this, "install file manager app");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_OPEN_DEVICE_SETTINGS_FILE) {
            if (resultCode == RESULT_OK) {
                //得到uri，后面就是将uri转化成file的过程。
                Uri uri = data.getData();
                String paramFilePath = FileUtils.getPath(this, uri);
                if (TextUtils.isEmpty(paramFilePath)) {
                    return;
                }
                if (!paramFilePath.endsWith(".xls") && !paramFilePath.endsWith(".xlsx")) {
                    ToastUtils.showToast(this, "Please select the correct file!");
                    return;
                }
                final File paramFile = new File(paramFilePath);
                if (paramFile.exists()) {
                    importFilePath = paramFilePath;
//                    String name = paramFilePath.substring(0, paramFilePath.lastIndexOf("."));
//                    String suffix = paramFilePath.substring(paramFilePath.lastIndexOf("."));
                    expertFilePath = paramFile.getParent() + File.separator + "export" + File.separator + "settings_for_app.xlsx";
                    try {
                        Workbook workbook = Workbook.getWorkbook(paramFile);
                        Sheet sheet = workbook.getSheet(0);
                        int rows = sheet.getRows();
                        int columns = sheet.getColumns();
                        // 从第二行开始
                        if (rows != 26 && columns != 3) {
                            ToastUtils.showToast(this, "Please select the correct file!");
                            return;
                        }
                        mqttDeviceConfig.host = sheet.getCell(1, 1).getContents().replaceAll("value:", "");
                        mqttDeviceConfig.port = sheet.getCell(2, 1).getContents().replaceAll("value:", "");
                        mqttDeviceConfig.clientId = sheet.getCell(3, 1).getContents().replaceAll("value:", "");
                        String topicSubscribe = sheet.getCell(4, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(topicSubscribe)) {
                            mqttDeviceConfig.topicSubscribe = topicSubscribe;
                        }
                        String topicPublish = sheet.getCell(5, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(topicPublish)) {
                            mqttDeviceConfig.topicPublish = topicPublish;
                        }
                        mqttDeviceConfig.cleanSession = "1".equals(sheet.getCell(6, 1).getContents().replaceAll("value:", ""));
                        mqttDeviceConfig.qos = Integer.parseInt(sheet.getCell(7, 1).getContents().replaceAll("value:", ""));
                        mqttDeviceConfig.keepAlive = Integer.parseInt(sheet.getCell(8, 1).getContents().replaceAll("value:", ""));
                        String username = sheet.getCell(9, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(username)) {
                            mqttDeviceConfig.username = username;
                        }
                        String password = sheet.getCell(10, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(password)) {
                            mqttDeviceConfig.password = password;
                        }
                        // 0/1
                        mqttDeviceConfig.connectMode = Integer.parseInt(sheet.getCell(11, 1).getContents().replaceAll("value:", ""));
                        if (mqttDeviceConfig.connectMode > 0) {
                            // 1/2
                            mqttDeviceConfig.connectMode = Integer.parseInt(sheet.getCell(12, 1).getContents().replaceAll("value:", ""));
                        }
                        mqttDeviceConfig.lwtEnable = "1".equals(sheet.getCell(13, 1).getContents().replaceAll("value:", ""));
                        mqttDeviceConfig.lwtRetain = "1".equals(sheet.getCell(14, 1).getContents().replaceAll("value:", ""));
                        mqttDeviceConfig.lwtQos = Integer.parseInt(sheet.getCell(15, 1).getContents().replaceAll("value:", ""));
                        String topic = sheet.getCell(16, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(topic)) {
                            mqttDeviceConfig.lwtTopic = topic;
                        }
                        String payload = sheet.getCell(17, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(payload)) {
                            mqttDeviceConfig.lwtPayload = payload;
                        }
                        String deviceId = sheet.getCell(18, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(deviceId)) {
                            mqttDeviceConfig.deviceId = deviceId;
                        }
                        String ntpUrl = sheet.getCell(19, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(ntpUrl)) {
                            mqttDeviceConfig.ntpUrl = ntpUrl;
                        }
                        mqttDeviceConfig.timeZone = Integer.parseInt(sheet.getCell(20, 1).getContents().replaceAll("value:", ""));
                        String apn = sheet.getCell(21, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(apn)) {
                            mqttDeviceConfig.apn = apn;
                        }
                        String apnUsername = sheet.getCell(22, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(apnUsername)) {
                            mqttDeviceConfig.apnUsername = apnUsername;
                        }
                        String apnPassword = sheet.getCell(23, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(apnPassword)) {
                            mqttDeviceConfig.apnPassword = apnPassword;
                        }
                        mqttDeviceConfig.networkPriority = Integer.parseInt(sheet.getCell(24, 1).getContents().replaceAll("value:", ""));
                        mqttDeviceConfig.debugModeEnable = "1".equals(sheet.getCell(25, 1).getContents().replaceAll("value:", ""));
                        initData();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ToastUtils.showToast(this, "Import success!");
                } else {
                    Toast.makeText(this, "file is not exists!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
