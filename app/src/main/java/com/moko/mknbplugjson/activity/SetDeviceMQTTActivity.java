package com.moko.mknbplugjson.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.elvishew.xlog.XLog;
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
import com.moko.mknbplugjson.adapter.MQTTFragmentAdapter;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityMqttDeviceBinding;
import com.moko.mknbplugjson.db.DBTools;
import com.moko.mknbplugjson.dialog.BottomDialog;
import com.moko.mknbplugjson.dialog.CustomDialog;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.fragment.GeneralDeviceFragment;
import com.moko.mknbplugjson.fragment.LWTFragment;
import com.moko.mknbplugjson.fragment.SSLDeviceFragment;
import com.moko.mknbplugjson.fragment.UserDeviceFragment;
import com.moko.mknbplugjson.utils.FileUtils;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.mknbplugjson.utils.Utils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.MokoSupport;
import com.moko.support.json.OrderTaskAssembler;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OrderCHAR;
import com.moko.support.json.entity.ParamsKeyEnum;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetDeviceMQTTActivity extends BaseActivity<ActivityMqttDeviceBinding> implements RadioGroup.OnCheckedChangeListener {
    private final String FILTER_ASCII = "[ -~]*";
    private GeneralDeviceFragment generalFragment;
    private UserDeviceFragment userFragment;
    private SSLDeviceFragment sslFragment;
    private LWTFragment lwtFragment;
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
    private boolean savedParamsError;
    private CustomDialog mqttConnDialog;
    private DonutProgress donutProgress;
    private boolean isSettingSuccess;
    private boolean isDeviceConnectSuccess;
    private Handler mHandler;

    private String expertFilePath;
    private boolean isFileError;


    @Override
    protected void onCreate() {
        String MQTTConfigStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        mqttAppConfig = new Gson().fromJson(MQTTConfigStr, MQTTConfig.class);
        mSelectedDeviceName = getIntent().getStringExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_NAME);
        mSelectedDeviceMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_MAC).replace(":", "");
        mSelectedDeviceType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_TYPE, 0);
        mqttDeviceConfig = new MQTTConfig();
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
        mBind.etNtpUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etApn.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100), filter});
        mBind.etApnUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(127), filter});
        mBind.etApnPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(127), filter});
        createFragment();
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
        expertFilePath = JSONMainActivity.PATH_LOGCAT + File.separator + "export" + File.separator + "Settings for Device.xlsx";
    }

    @Override
    protected ActivityMqttDeviceBinding getViewBinding() {
        return ActivityMqttDeviceBinding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (isSettingSuccess) {
            EventBus.getDefault().cancelEventDelivery(event);
            return;
        }
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
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
            byte[] value = response.responseValue;
            if (orderCHAR == OrderCHAR.CHAR_PARAMS) {
                if (value.length >= 4) {
                    int header = value[0] & 0xFF;// 0xED
                    int flag = value[1] & 0xFF;// read or write
                    int cmd = value[2] & 0xFF;
                    if (header != 0xED) return;
                    ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                    if (configKeyEnum == null) return;
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
                            case KEY_MQTT_HOST:
                                if (length > 0) {
                                    String host = new String(Arrays.copyOfRange(value, 4, value.length));
                                    mBind.etMqttHost.setText(host);
                                    mBind.etMqttHost.setSelection(mBind.etMqttHost.getText().length());
                                    mqttDeviceConfig.host = host;
                                }
                                break;

                            case KEY_MQTT_PORT:
                                if (length == 2) {
                                    int port = MokoUtils.toInt(Arrays.copyOfRange(value, 4, value.length));
                                    mBind.etMqttPort.setText(String.valueOf(port));
                                    mBind.etMqttPort.setSelection(mBind.etMqttPort.getText().length());
                                    mqttDeviceConfig.port = String.valueOf(port);
                                }
                                break;

                            case KEY_MQTT_CLIENT_ID:
                                if (length > 0) {
                                    String clientId = new String(Arrays.copyOfRange(value, 4, value.length));
                                    mBind.etMqttClientId.setText(clientId);
                                    mBind.etMqttClientId.setSelection(mBind.etMqttClientId.getText().length());
                                    mqttDeviceConfig.clientId = clientId;
                                }
                                break;

                            case KEY_MQTT_SUBSCRIBE_TOPIC:
                                if (length > 0) {
                                    String subscribe = new String(Arrays.copyOfRange(value, 4, value.length));
                                    mBind.etMqttSubscribeTopic.setText(subscribe);
                                    mBind.etMqttSubscribeTopic.setSelection(mBind.etMqttSubscribeTopic.getText().length());
                                    mqttDeviceConfig.topicSubscribe = subscribe;
                                }
                                break;

                            case KEY_MQTT_PUBLISH_TOPIC:
                                if (length > 0) {
                                    String publish = new String(Arrays.copyOfRange(value, 4, value.length));
                                    mBind.etMqttPublishTopic.setText(publish);
                                    mBind.etMqttPublishTopic.setSelection(mBind.etMqttPublishTopic.getText().length());
                                    mqttDeviceConfig.topicPublish = publish;
                                }
                                break;

                            case KEY_MQTT_CLEAN_SESSION:
                                if (length == 1) {
                                    boolean cleanSession = (value[4] & 0xff) == 1;
                                    generalFragment.setCleanSession(cleanSession);
                                    mqttDeviceConfig.cleanSession = cleanSession;
                                }
                                break;

                            case KEY_MQTT_QOS:
                                if (length == 1) {
                                    generalFragment.setQos(value[4] & 0xff);
                                    mqttDeviceConfig.qos = value[4] & 0xff;
                                }
                                break;

                            case KEY_MQTT_KEEP_ALIVE:
                                if (length == 1) {
                                    generalFragment.setKeepAlive(value[4] & 0xff);
                                    mqttDeviceConfig.keepAlive = value[4] & 0xff;
                                }
                                break;

                            case KEY_MQTT_USERNAME:
                                if (length > 0) {
                                    String userName = new String(Arrays.copyOfRange(value, 4, value.length));
                                    userFragment.setUserName(userName);
                                    mqttDeviceConfig.username = userName;
                                } else {
                                    mqttDeviceConfig.username = "";
                                }
                                break;

                            case KEY_MQTT_PASSWORD:
                                if (length > 0) {
                                    String password = new String(Arrays.copyOfRange(value, 4, value.length));
                                    userFragment.setPassword(password);
                                    mqttDeviceConfig.password = password;
                                } else {
                                    mqttDeviceConfig.password = "";
                                }
                                break;

                            case KEY_MQTT_CONNECT_MODE:
                                if (length == 1) {
                                    int mode = value[4] & 0xff;
                                    sslFragment.setConnectMode(mode);
                                    mqttDeviceConfig.connectMode = mode;
                                }
                                break;

                            case KEY_MQTT_LWT_ENABLE:
                                if (length == 1) {
                                    boolean enable = (value[4] & 0xff) == 1;
                                    lwtFragment.setLwtEnable(enable);
                                    mqttDeviceConfig.lwtEnable = enable;
                                }
                                break;

                            case KEY_MQTT_LWT_RETAIN:
                                if (length == 1) {
                                    boolean enable = (value[4] & 0xff) == 1;
                                    lwtFragment.setLwtRetain(enable);
                                    mqttDeviceConfig.lwtRetain = enable;
                                }
                                break;

                            case KEY_MQTT_LWT_QOS:
                                if (length == 1) {
                                    int qos = value[4] & 0xff;
                                    lwtFragment.setQos(qos);
                                    mqttDeviceConfig.lwtQos = qos;
                                }
                                break;

                            case KEY_MQTT_LWT_TOPIC:
                                if (length > 0) {
                                    String topic = new String(Arrays.copyOfRange(value, 4, value.length));
                                    lwtFragment.setTopic(topic);
                                    mqttDeviceConfig.lwtTopic = topic;
                                }
                                break;

                            case KEY_MQTT_LWT_PAYLOAD:
                                if (length > 0) {
                                    String payload = new String(Arrays.copyOfRange(value, 4, value.length));
                                    lwtFragment.setPayload(payload);
                                    mqttDeviceConfig.lwtPayload = payload;
                                }
                                break;

                            case KEY_APN:
                                if (length > 0) {
                                    String apn = new String(Arrays.copyOfRange(value, 4, value.length));
                                    mBind.etApn.setText(apn);
                                    mBind.etApn.setSelection(mBind.etApn.getText().length());
                                    mqttDeviceConfig.apn = apn;
                                }
                                break;

                            case KEY_APN_USERNAME:
                                if (length > 0) {
                                    String userName = new String(Arrays.copyOfRange(value, 4, value.length));
                                    mBind.etApnUsername.setText(userName);
                                    mBind.etApnUsername.setSelection(mBind.etApnUsername.getText().length());
                                    mqttDeviceConfig.apnUsername = userName;
                                }
                                break;

                            case KEY_APN_PASSWORD:
                                if (length > 0) {
                                    String password = new String(Arrays.copyOfRange(value, 4, value.length));
                                    mBind.etApnPassword.setText(password);
                                    mBind.etApnPassword.setSelection(mBind.etApnPassword.getText().length());
                                    mqttDeviceConfig.apnPassword = password;
                                }
                                break;

                            case KEY_NETWORK_PRIORITY:
                                if (length == 1) {
                                    mSelectedNetworkPriority = value[4] & 0xff;
                                    mBind.tvNetworkPriority.setText(mNetworkPriority.get(mSelectedNetworkPriority));
                                    mqttDeviceConfig.networkPriority = mSelectedNetworkPriority;
                                }
                                break;

                            case KEY_NTP_URL:
                                if (length > 0) {
                                    String url = new String(Arrays.copyOfRange(value, 4, value.length));
                                    mBind.etNtpUrl.setText(url);
                                    mBind.etNtpUrl.setSelection(mBind.etNtpUrl.getText().length());
                                    mqttDeviceConfig.ntpUrl = url;
                                }
                                break;

                            case KEY_NTP_TIME_ZONE:
                                if (length == 1) {
                                    mSelectedTimeZone = value[4] + 24;
                                    mBind.tvTimeZone.setText(mTimeZones.get(mSelectedTimeZone));
                                    mqttDeviceConfig.timeZone = mSelectedTimeZone;
                                }
                                break;
                        }
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        XLog.i("333333*******************" + event.getTopic() + "//////" + event.getMessage());
        //47.104.81.55
        final String topic = event.getTopic();
        final String message = event.getMessage();
        if (TextUtils.isEmpty(topic) || isDeviceConnectSuccess) {
            return;
        }
        if (TextUtils.isEmpty(message)) return;
        MsgCommon<JsonObject> msgCommon;
        try {
            Type type = new TypeToken<MsgCommon<JsonObject>>() {
            }.getType();
            msgCommon = new Gson().fromJson(message, type);
        } catch (Exception e) {
            return;
        }
        if (!mSelectedDeviceMac.equalsIgnoreCase(msgCommon.device_info.mac)) {
            return;
        }
        if (msgCommon.msg_id != MQTTConstants.NOTIFY_MSG_ID_SWITCH_STATE) return;
        if (donutProgress == null) return;
        if (!isDeviceConnectSuccess) {
            isDeviceConnectSuccess = true;
            donutProgress.setProgress(100);
            donutProgress.setText(100 + "%");
            // 关闭进度条弹框，保存数据，跳转修改设备名称页面
            mBind.etMqttHost.postDelayed(() -> {
                dismissConnMqttDialog();
                MokoDevice mokoDevice = DBTools.getInstance(SetDeviceMQTTActivity.this).selectDeviceByMac(mSelectedDeviceMac);
                String mqttConfigStr = new Gson().toJson(mqttDeviceConfig, MQTTConfig.class);
                if (mokoDevice == null) {
                    mokoDevice = new MokoDevice();
                    mokoDevice.name = mSelectedDeviceName;
                    mokoDevice.mac = mSelectedDeviceMac.toLowerCase();
                    mokoDevice.mqttInfo = mqttConfigStr;
                    mokoDevice.topicSubscribe = mqttDeviceConfig.topicSubscribe;
                    mokoDevice.topicPublish = mqttDeviceConfig.topicPublish;
                    mokoDevice.deviceMode = mBind.cbDebugMode.isChecked() ? 2 : 1;
                    mokoDevice.deviceType = mSelectedDeviceType;
                    DBTools.getInstance(SetDeviceMQTTActivity.this).insertDevice(mokoDevice);
                } else {
                    mokoDevice.name = mSelectedDeviceName;
                    mokoDevice.mac = mSelectedDeviceMac.toLowerCase();
                    mokoDevice.mqttInfo = mqttConfigStr;
                    mokoDevice.topicSubscribe = mqttDeviceConfig.topicSubscribe;
                    mokoDevice.topicPublish = mqttDeviceConfig.topicPublish;
                    mokoDevice.deviceMode = mBind.cbDebugMode.isChecked() ? 2 : 1;
                    mokoDevice.deviceType = mSelectedDeviceType;
                    DBTools.getInstance(SetDeviceMQTTActivity.this).updateDevice(mokoDevice);
                }
                Intent modifyIntent = new Intent(this, ModifyNameActivity.class);
                modifyIntent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDevice);
                startActivity(modifyIntent);
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
        //读取参数
        showLoadingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.getMqttServer());
        orderTasks.add(OrderTaskAssembler.getMqttPort());
        orderTasks.add(OrderTaskAssembler.getMqttClientId());
        orderTasks.add(OrderTaskAssembler.getMqttSubscribe());
        orderTasks.add(OrderTaskAssembler.getMqttPublish());
        orderTasks.add(OrderTaskAssembler.getMqttCleanSession());
        orderTasks.add(OrderTaskAssembler.getMqttQos());
        orderTasks.add(OrderTaskAssembler.getMqttKeepAlive());
        orderTasks.add(OrderTaskAssembler.getMqttUserName());
        orderTasks.add(OrderTaskAssembler.getMqttPassword());
        orderTasks.add(OrderTaskAssembler.getMqttSSlMode());
        orderTasks.add(OrderTaskAssembler.getMqttLwtEnable());
        orderTasks.add(OrderTaskAssembler.getMqttLwtRetainEnable());
        orderTasks.add(OrderTaskAssembler.getMqttLwtQos());
        orderTasks.add(OrderTaskAssembler.getMqttLwtTopic());
        orderTasks.add(OrderTaskAssembler.getMqttLwtMsg());
        orderTasks.add(OrderTaskAssembler.getMqttApn());
        orderTasks.add(OrderTaskAssembler.getMqttApnUsername());
        orderTasks.add(OrderTaskAssembler.getMqttApnPassword());
        orderTasks.add(OrderTaskAssembler.getMqttNetworkPriority());
        orderTasks.add(OrderTaskAssembler.getMqttNtpHost());
        orderTasks.add(OrderTaskAssembler.getMqttTimezone());
//        orderTasks.add(OrderTaskAssembler.getMqttDebugMode());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void onBack(View view) {
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
            mBind.vpMqtt.setCurrentItem(0);
        else if (checkedId == R.id.rb_user)
            mBind.vpMqtt.setCurrentItem(1);
        else if (checkedId == R.id.rb_ssl)
            mBind.vpMqtt.setCurrentItem(2);
        else if (checkedId == R.id.rb_lwt)
            mBind.vpMqtt.setCurrentItem(3);
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (isVerify()) return;
        setMQTTDeviceConfig();
    }

    private boolean isVerify() {
        String host = mBind.etMqttHost.getText().toString().replaceAll(" ", "");
        String port = mBind.etMqttPort.getText().toString();
        String clientId = mBind.etMqttClientId.getText().toString().replaceAll(" ", "");
        String topicSubscribe = mBind.etMqttSubscribeTopic.getText().toString().replaceAll(" ", "");
        String topicPublish = mBind.etMqttPublishTopic.getText().toString().replaceAll(" ", "");
        String ntpUrl = mBind.etNtpUrl.getText().toString().replaceAll(" ", "");
        String apn = mBind.etApn.getText().toString().replaceAll(" ", "");
        String apnUsername = mBind.etApnUsername.getText().toString().replaceAll(" ", "");
        String apnPassword = mBind.etApnPassword.getText().toString().replaceAll(" ", "");

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
        if (TextUtils.isEmpty(topicSubscribe)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_topic_subscribe));
            return false;
        }
        if (TextUtils.isEmpty(topicPublish)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_topic_publish));
            return false;
        }
        if (!generalFragment.isValid() || !sslFragment.isValid() || !lwtFragment.isValid())
            return true;
        mqttDeviceConfig.host = host;
        mqttDeviceConfig.port = port;
        mqttDeviceConfig.clientId = clientId;
        mqttDeviceConfig.cleanSession = generalFragment.isCleanSession();
        mqttDeviceConfig.qos = generalFragment.getQos();
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
        mqttDeviceConfig.ntpUrl = ntpUrl;
        mqttDeviceConfig.timeZone = mSelectedTimeZone - 24;
        mqttDeviceConfig.apn = apn;
        mqttDeviceConfig.apnUsername = apnUsername;
        mqttDeviceConfig.apnPassword = apnPassword;
        mqttDeviceConfig.networkPriority = mSelectedNetworkPriority;
        mqttDeviceConfig.debugModeEnable = mBind.cbDebugMode.isChecked();

        if (!mqttDeviceConfig.topicPublish.isEmpty() && !mqttDeviceConfig.topicSubscribe.isEmpty()
                && mqttDeviceConfig.topicPublish.equals(mqttDeviceConfig.topicSubscribe)) {
            ToastUtils.showToast(this, "Subscribed and published topic can't be same !");
            return true;
        }
        return false;
    }

    private void setMQTTDeviceConfig() {
        try {
            showLoadingProgressDialog();
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
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
            if (!TextUtils.isEmpty(mqttDeviceConfig.username)) {
                orderTasks.add(OrderTaskAssembler.setMqttUserName(mqttDeviceConfig.username));
            }
            if (!TextUtils.isEmpty(mqttDeviceConfig.password)) {
                orderTasks.add(OrderTaskAssembler.setMqttPassword(mqttDeviceConfig.password));
            }
            orderTasks.add(OrderTaskAssembler.setMqttConnectMode(mqttDeviceConfig.connectMode));
            if (mqttDeviceConfig.connectMode == 2) {
                File file = new File(mqttDeviceConfig.caPath);
                orderTasks.add(OrderTaskAssembler.setCA(file));
            } else if (mqttDeviceConfig.connectMode == 3) {
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
            orderTasks.add(OrderTaskAssembler.setMode(mBind.cbDebugMode.isChecked() ? 1 : 0));
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } catch (Exception e) {
            ToastUtils.showToast(this, "File is missing");
        }
    }

    public void selectCertificate(View view) {
        if (isWindowLocked()) return;
        sslFragment.selectCertificate();
    }

    public void selectCAFile(View view) {
        if (isWindowLocked()) return;
        sslFragment.selectCAFile();
    }

    public void selectKeyFile(View view) {
        if (isWindowLocked()) return;
        sslFragment.selectKeyFile();
    }

    public void selectCertFile(View view) {
        if (isWindowLocked()) return;
        sslFragment.selectCertFile();
    }

    public void selectTimeZone(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mTimeZones, mSelectedTimeZone);
        dialog.setListener(value -> {
            mSelectedTimeZone = value;
            mBind.tvTimeZone.setText(mTimeZones.get(mSelectedTimeZone));
        });
        dialog.show(getSupportFragmentManager());
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
                setResult(RESULT_OK);
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
                XLog.i("333333publish=" + mqttDeviceConfig.topicPublish);
                MQTTSupport.getInstance().subscribe(mqttDeviceConfig.topicPublish, mqttAppConfig.qos);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onExportSettings(View view) {
        if (isWindowLocked()) return;
        mqttDeviceConfig.host = mBind.etMqttHost.getText().toString().replaceAll(" ", "");
        mqttDeviceConfig.port = mBind.etMqttPort.getText().toString();
        mqttDeviceConfig.clientId = mBind.etMqttClientId.getText().toString().replaceAll(" ", "");
        mqttDeviceConfig.topicSubscribe = mBind.etMqttSubscribeTopic.getText().toString().replaceAll(" ", "");
        mqttDeviceConfig.topicPublish = mBind.etMqttPublishTopic.getText().toString().replaceAll(" ", "");
        mqttDeviceConfig.ntpUrl = mBind.etNtpUrl.getText().toString().replaceAll(" ", "");
        mqttDeviceConfig.apn = mBind.etApn.getText().toString().replaceAll(" ", "");
        mqttDeviceConfig.apnUsername = mBind.etApnUsername.getText().toString().replaceAll(" ", "");
        mqttDeviceConfig.apnPassword = mBind.etApnPassword.getText().toString().replaceAll(" ", "");
        mqttDeviceConfig.cleanSession = generalFragment.isCleanSession();
        mqttDeviceConfig.qos = generalFragment.getQos();
        mqttDeviceConfig.keepAlive = generalFragment.getKeepAlive();
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
        mqttDeviceConfig.timeZone = mSelectedTimeZone - 24;
        mqttDeviceConfig.networkPriority = mSelectedNetworkPriority;
        mqttDeviceConfig.debugModeEnable = mBind.cbDebugMode.isChecked();
        showLoadingProgressDialog();
        final File expertFile = new File(expertFilePath);
        try {
            if (!expertFile.getParentFile().exists()) {
                expertFile.getParentFile().mkdirs();
            }
            if (!expertFile.exists()) {
                expertFile.delete();
                expertFile.createNewFile();
            }
            new Thread(() -> {
                XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
                XSSFSheet sheet = xssfWorkbook.createSheet();
                XSSFRow row0 = sheet.createRow(0);
                row0.createCell(0).setCellValue("Config_Item");
                row0.createCell(1).setCellValue("Config_value");
                row0.createCell(2).setCellValue("Remark");

                XSSFRow row1 = sheet.createRow(1);
                row1.createCell(0).setCellValue("Host");
                if (!TextUtils.isEmpty(mqttDeviceConfig.host))
                    row1.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.host));
                row1.createCell(2).setCellValue("1-64 characters");

                XSSFRow row2 = sheet.createRow(2);
                row2.createCell(0).setCellValue("Port");
                if (!TextUtils.isEmpty(mqttDeviceConfig.port))
                    row2.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.port));
                row2.createCell(2).setCellValue("Range: 1-65535");

                XSSFRow row3 = sheet.createRow(3);
                row3.createCell(0).setCellValue("Client id");
                if (!TextUtils.isEmpty(mqttDeviceConfig.clientId))
                    row3.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.clientId));
                row3.createCell(2).setCellValue("1-64 characters");

                XSSFRow row4 = sheet.createRow(4);
                row4.createCell(0).setCellValue("Subscribe Topic");
                if (!TextUtils.isEmpty(mqttDeviceConfig.topicSubscribe))
                    row4.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.topicSubscribe));
//                else
//                    row4.createCell(1).setCellValue("");
                row4.createCell(2).setCellValue("1-128 characters");

                XSSFRow row5 = sheet.createRow(5);
                row5.createCell(0).setCellValue("Publish Topic");
                if (!TextUtils.isEmpty(mqttDeviceConfig.topicPublish))
                    row5.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.topicPublish));
//                else
//                    row5.createCell(1).setCellValue("");
                row5.createCell(2).setCellValue("1-128 characters");

                XSSFRow row6 = sheet.createRow(6);
                row6.createCell(0).setCellValue("Clean Session");
                row6.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.cleanSession ? "1" : "0"));
                row6.createCell(2).setCellValue("Range: 0/1 0:NO 1:YES");

                XSSFRow row7 = sheet.createRow(7);
                row7.createCell(0).setCellValue("Qos");
                row7.createCell(1).setCellValue(String.format("value:%d", mqttDeviceConfig.qos));
                row7.createCell(2).setCellValue("Range: 0/1/2 0:qos0 1:qos1 2:qos2");

                XSSFRow row8 = sheet.createRow(8);
                row8.createCell(0).setCellValue("Keep Alive");
                row8.createCell(1).setCellValue(String.format("value:%d", mqttDeviceConfig.keepAlive));
                row8.createCell(2).setCellValue("Range: 10-120, unit: second");

                XSSFRow row9 = sheet.createRow(9);
                row9.createCell(0).setCellValue("MQTT Username");
                if (!TextUtils.isEmpty(mqttDeviceConfig.username))
                    row9.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.username));
//                else
//                    row9.createCell(1).setCellValue("");
                row9.createCell(2).setCellValue("0-128 characters");

                XSSFRow row10 = sheet.createRow(10);
                row10.createCell(0).setCellValue("MQTT Password");
                if (!TextUtils.isEmpty(mqttDeviceConfig.password))
                    row10.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.password));
//                else
//                    row10.createCell(1).setCellValue("");
                row10.createCell(2).setCellValue("0-128 characters");

                XSSFRow row11 = sheet.createRow(11);
                row11.createCell(0).setCellValue("SSL/TLS");
                XSSFRow row12 = sheet.createRow(12);
                row12.createCell(0).setCellValue("Certificate type");
                if (mqttDeviceConfig.connectMode > 0) {
                    row11.createCell(1).setCellValue("value:1");
                    row12.createCell(1).setCellValue(String.format("value:%d", mqttDeviceConfig.connectMode));
                } else {
                    row11.createCell(1).setCellValue(String.format("value:%d", mqttDeviceConfig.connectMode));
                    row12.createCell(1).setCellValue("value:1");
                }
                row11.createCell(2).setCellValue("Range: 0/1 0:Disable SSL (TCP mode) 1:Enable SSL");
                row12.createCell(2).setCellValue("Valid when SSL is enabled, range: 1/2 1: CA certificate file 2: Self signed certificates");

                XSSFRow row13 = sheet.createRow(13);
                row13.createCell(0).setCellValue("LWT");
                row13.createCell(1).setCellValue(mqttDeviceConfig.lwtEnable ? "value:1" : "value:0");
                row13.createCell(2).setCellValue("Range: 0/1 0:Disable 1:Enable");

                XSSFRow row14 = sheet.createRow(14);
                row14.createCell(0).setCellValue("LWT Retain");
                row14.createCell(1).setCellValue(mqttDeviceConfig.lwtRetain ? "value:1" : "value:0");
                row14.createCell(2).setCellValue("Range: 0/1 0:NO 1:YES");

                XSSFRow row15 = sheet.createRow(15);
                row15.createCell(0).setCellValue("LWT Qos");
                row15.createCell(1).setCellValue(String.format("value:%d", mqttDeviceConfig.lwtQos));
                row15.createCell(2).setCellValue("Range: 0/1/2 0:qos0 1:qos1 2:qos2");

                XSSFRow row16 = sheet.createRow(16);
                row16.createCell(0).setCellValue("LWT Topic");
                if (!TextUtils.isEmpty(mqttDeviceConfig.lwtTopic))
                    row16.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.lwtTopic));
//                else
//                    row16.createCell(1).setCellValue("");
                row16.createCell(2).setCellValue("1-128 characters (When LWT is enabled) ");

                XSSFRow row17 = sheet.createRow(17);
                row17.createCell(0).setCellValue("LWT Payload");
                if (!TextUtils.isEmpty(mqttDeviceConfig.lwtPayload))
                    row17.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.lwtPayload));
//                else
//                    row17.createCell(1).setCellValue("");
                row17.createCell(2).setCellValue("1-128 characters (When LWT is enabled) ");

                XSSFRow row18 = sheet.createRow(18);
                row18.createCell(0).setCellValue("NTP URL");
                if (!TextUtils.isEmpty(mqttDeviceConfig.ntpUrl))
                    row18.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.ntpUrl));
//                else
//                    row19.createCell(1).setCellValue("");
                row18.createCell(2).setCellValue("0-64 characters");

                XSSFRow row19 = sheet.createRow(19);
                row19.createCell(0).setCellValue("Timezone");
                row19.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.timeZone));
                row19.createCell(2).setCellValue("Range: -24~+28, step by half timezone 1 For example: 16- UTC+8");

                XSSFRow row20 = sheet.createRow(20);
                row20.createCell(0).setCellValue("APN");
                if (!TextUtils.isEmpty(mqttDeviceConfig.apn))
                    row20.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.apn));
//                else
//                    row21.createCell(1).setCellValue("");
                row20.createCell(2).setCellValue("0-100 characters");

                XSSFRow row21 = sheet.createRow(21);
                row21.createCell(0).setCellValue("APN Username");
                if (!TextUtils.isEmpty(mqttDeviceConfig.apnUsername))
                    row21.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.apnUsername));
//                else
//                    row22.createCell(1).setCellValue("");
                row21.createCell(2).setCellValue("0-127 characters");

                XSSFRow row22 = sheet.createRow(22);
                row22.createCell(0).setCellValue("APN Password");
                if (!TextUtils.isEmpty(mqttDeviceConfig.apnPassword))
                    row22.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.apnPassword));
//                else
//                    row23.createCell(1).setCellValue("");
                row22.createCell(2).setCellValue("0-127 characters");

                XSSFRow row23 = sheet.createRow(23);
                row23.createCell(0).setCellValue("Network Priority");
                row23.createCell(1).setCellValue(String.format("value:%d", mqttDeviceConfig.networkPriority));
                row23.createCell(2).setCellValue("Range: 0-10 0:eMTC->NB-IOT->GSM 1:eMTC-> GSM -> NB-IOT 2:NB-IOT->GSM-> eMTC 3:NB-IOT-> eMTC-> GSM 4:GSM -> NB-IOT-> eMTC 5:GSM -> eMTC->NB-IOT 6:eMTC->NB-IOT 7:NB-IOT-> eMTC 8:GSM 9:NB-IOT 10:eMTC");

                XSSFRow row24 = sheet.createRow(24);
                row24.createCell(0).setCellValue("Debug mode");
                row24.createCell(1).setCellValue(mqttDeviceConfig.debugModeEnable ? "value:1" : "value:0");
                row24.createCell(2).setCellValue("Range: 0/1 0:Disable 1:Enable");

                Uri uri = Uri.fromFile(expertFile);
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    xssfWorkbook.write(outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                    isFileError = true;
                }
                runOnUiThread(() -> {
                    dismissLoadingProgressDialog();
                    if (isFileError) {
                        isFileError = false;
                        ToastUtils.showToast(SetDeviceMQTTActivity.this, "Export error!");
                        return;
                    }
                    ToastUtils.showToast(SetDeviceMQTTActivity.this, "Export success!");
                    Utils.sendEmail(SetDeviceMQTTActivity.this, "", "", "Settings for Device", "Choose Email Client", expertFile);

                });
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showToast(this, "Export error!");
        }
    }

    public void onClearSettings(View view) {
        //清空所有输入框信息 选项恢复到默认值
        mBind.etMqttHost.setText("");
        mBind.etMqttPort.setText("");
        mBind.etMqttClientId.setText("");
        mBind.etMqttSubscribeTopic.setText("");
        mBind.etMqttPublishTopic.setText("");
        generalFragment.setCleanSession(true);
        generalFragment.setQos(1);
        generalFragment.setKeepAlive(60);
        userFragment.setUserName("");
        userFragment.setPassword("");
        sslFragment.setConnectMode(0);
        lwtFragment.setLwtEnable(false);
        lwtFragment.setLwtRetain(false);
        lwtFragment.setQos(1);
        lwtFragment.setPayload("");
        lwtFragment.setTopic("");
        mBind.etApn.setText("");
        mBind.etApnUsername.setText("");
        mBind.etApnPassword.setText("");
        mSelectedNetworkPriority = 0;
        mBind.tvNetworkPriority.setText(mNetworkPriority.get(0));
        mBind.etNtpUrl.setText("");
        mSelectedTimeZone = 0;
        mBind.tvTimeZone.setText(mTimeZones.get(0));
    }

    public void onImportSettings(View view) {
        if (isWindowLocked()) return;
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
                if (!paramFilePath.endsWith(".xlsx")) {
                    ToastUtils.showToast(this, "Please select the correct file!");
                    return;
                }
                final File paramFile = new File(paramFilePath);
                if (paramFile.exists()) {
                    showLoadingProgressDialog();
                    new Thread(() -> {
                        try {
                            Workbook workbook = WorkbookFactory.create(paramFile);
                            Sheet sheet = workbook.getSheetAt(0);
                            int rows = sheet.getLastRowNum();
                            int columns = sheet.getRow(0).getPhysicalNumberOfCells();
                            // 从第二行开始
                            if (rows < 24 || columns < 3) {
                                runOnUiThread(() -> {
                                    dismissLoadingProgressDialog();
                                    ToastUtils.showToast(SetDeviceMQTTActivity.this, "Please select the correct file!");
                                });
                                return;
                            }
                            Cell hostCell = sheet.getRow(1).getCell(1);
                            if (hostCell != null)
                                mqttDeviceConfig.host = hostCell.getStringCellValue().replaceAll("value:", "");
                            Cell postCell = sheet.getRow(2).getCell(1);
                            if (postCell != null)
                                mqttDeviceConfig.port = postCell.getStringCellValue().replaceAll("value:", "");
                            Cell clientCell = sheet.getRow(3).getCell(1);
                            if (clientCell != null)
                                mqttDeviceConfig.clientId = clientCell.getStringCellValue().replaceAll("value:", "");
                            Cell topicSubscribeCell = sheet.getRow(4).getCell(1);
                            if (topicSubscribeCell != null) {
                                mqttDeviceConfig.topicSubscribe = topicSubscribeCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell topicPublishCell = sheet.getRow(5).getCell(1);
                            if (topicPublishCell != null) {
                                mqttDeviceConfig.topicPublish = topicPublishCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell cleanSessionCell = sheet.getRow(6).getCell(1);
                            if (cleanSessionCell != null)
                                mqttDeviceConfig.cleanSession = "1".equals(cleanSessionCell.getStringCellValue().replaceAll("value:", ""));
                            Cell qosCell = sheet.getRow(7).getCell(1);
                            if (qosCell != null)
                                mqttDeviceConfig.qos = Integer.parseInt(qosCell.getStringCellValue().replaceAll("value:", ""));
                            Cell keepAliveCell = sheet.getRow(8).getCell(1);
                            if (keepAliveCell != null)
                                mqttDeviceConfig.keepAlive = Integer.parseInt(keepAliveCell.getStringCellValue().replaceAll("value:", ""));
                            Cell usernameCell = sheet.getRow(9).getCell(1);
                            if (usernameCell != null) {
                                mqttDeviceConfig.username = usernameCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell passwordCell = sheet.getRow(10).getCell(1);
                            if (passwordCell != null) {
                                mqttDeviceConfig.password = passwordCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell connectModeCell = sheet.getRow(11).getCell(1);
                            if (connectModeCell != null) {
                                // 0/1
                                mqttDeviceConfig.connectMode = Integer.parseInt(connectModeCell.getStringCellValue().replaceAll("value:", ""));
                                if (mqttDeviceConfig.connectMode > 0) {
                                    Cell cell = sheet.getRow(12).getCell(1);
                                    if (cell != null)
                                        // 1/2
                                        mqttDeviceConfig.connectMode = Integer.parseInt(cell.getStringCellValue().replaceAll("value:", ""));
                                }
                            }
                            Cell lwtEnableCell = sheet.getRow(13).getCell(1);
                            if (lwtEnableCell != null)
                                mqttDeviceConfig.lwtEnable = "1".equals(lwtEnableCell.getStringCellValue().replaceAll("value:", ""));
                            Cell lwtRetainCell = sheet.getRow(14).getCell(1);
                            if (lwtRetainCell != null)
                                mqttDeviceConfig.lwtRetain = "1".equals(lwtRetainCell.getStringCellValue().replaceAll("value:", ""));
                            Cell lwtQosCell = sheet.getRow(15).getCell(1);
                            if (lwtQosCell != null)
                                mqttDeviceConfig.lwtQos = Integer.parseInt(lwtQosCell.getStringCellValue().replaceAll("value:", ""));
                            Cell topicCell = sheet.getRow(16).getCell(1);
                            if (topicCell != null) {
                                mqttDeviceConfig.lwtTopic = topicCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell payloadCell = sheet.getRow(17).getCell(1);
                            if (payloadCell != null) {
                                mqttDeviceConfig.lwtPayload = payloadCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell ntpUrlCell = sheet.getRow(18).getCell(1);
                            if (ntpUrlCell != null) {
                                mqttDeviceConfig.ntpUrl = ntpUrlCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell timeZoneCell = sheet.getRow(19).getCell(1);
                            if (timeZoneCell != null)
                                mqttDeviceConfig.timeZone = Integer.parseInt(timeZoneCell.getStringCellValue().replaceAll("value:", ""));
                            Cell apnCell = sheet.getRow(20).getCell(1);
                            if (apnCell != null) {
                                mqttDeviceConfig.apn = apnCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell apnUsernameCell = sheet.getRow(21).getCell(1);
                            if (apnUsernameCell != null) {
                                mqttDeviceConfig.apnUsername = apnUsernameCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell apnPasswordCell = sheet.getRow(22).getCell(1);
                            if (apnPasswordCell != null) {
                                mqttDeviceConfig.apnPassword = apnPasswordCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell networkPriorityCell = sheet.getRow(23).getCell(1);
                            if (networkPriorityCell != null)
                                mqttDeviceConfig.networkPriority = Integer.parseInt(networkPriorityCell.getStringCellValue().replaceAll("value:", ""));
                            Cell debugModeEnableCell = sheet.getRow(24).getCell(1);
                            if (debugModeEnableCell != null)
                                mqttDeviceConfig.debugModeEnable = "1".equals(debugModeEnableCell.getStringCellValue().replaceAll("value:", ""));
                            runOnUiThread(() -> {
                                dismissLoadingProgressDialog();
                                if (isFileError) {
                                    ToastUtils.showToast(SetDeviceMQTTActivity.this, "Import failed!");
                                    return;
                                }
                                ToastUtils.showToast(SetDeviceMQTTActivity.this, "Import success!");
                                initData();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            isFileError = true;
                        }
                    }).start();
                } else {
                    ToastUtils.showToast(this, "File is not exists!");
                }
            }
        }
    }
}
