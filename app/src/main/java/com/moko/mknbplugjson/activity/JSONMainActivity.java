package com.moko.mknbplugjson.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.BuildConfig;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.adapter.DeviceAdapter;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityMainJsonBinding;
import com.moko.mknbplugjson.db.DBTools;
import com.moko.mknbplugjson.dialog.AlertMessageDialog;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.mknbplugjson.utils.Utils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.MokoSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.entity.SwitchInfo;
import com.moko.support.json.entity.SwitchState;
import com.moko.support.json.event.DeviceDeletedEvent;
import com.moko.support.json.event.DeviceModifyNameEvent;
import com.moko.support.json.event.DeviceOnlineEvent;
import com.moko.support.json.event.MQTTConnectionCompleteEvent;
import com.moko.support.json.event.MQTTConnectionFailureEvent;
import com.moko.support.json.event.MQTTConnectionLostEvent;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;

public class JSONMainActivity extends BaseActivity<ActivityMainJsonBinding> implements BaseQuickAdapter.OnItemChildClickListener,
        BaseQuickAdapter.OnItemClickListener, BaseQuickAdapter.OnItemLongClickListener {
    private ArrayList<MokoDevice> devices;
    private DeviceAdapter adapter;
    public Handler mHandler;
    public String MQTTAppConfigStr;
    private MQTTConfig appMqttConfig;
    public static String PATH_LOGCAT;

    @Override
    protected void onCreate() {
        // 初始化Xlog
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 优先保存到SD卡中
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PATH_LOGCAT = getExternalFilesDir(null).getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MKNBPLUG" : "MKNBPLUGJSON");
            } else {
                PATH_LOGCAT = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MKNBPLUG" : "MKNBPLUGJSON");
            }
        } else {
            // 如果SD卡不存在，就保存到本应用的目录下
            PATH_LOGCAT = getFilesDir().getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MKNBPLUG" : "MKNBPLUGJSON");
        }
        MokoSupport.getInstance().init(getApplicationContext());
        MQTTSupport.getInstance().init(getApplicationContext());

        devices = DBTools.getInstance(this).selectAllDevice();
        adapter = new DeviceAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(devices);
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);
        adapter.setOnItemChildClickListener(this);
        mBind.rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvDeviceList.setAdapter(adapter);
        if (devices.isEmpty()) {
            mBind.rlEmpty.setVisibility(View.VISIBLE);
            mBind.rvDeviceList.setVisibility(View.GONE);
        } else {
            mBind.rvDeviceList.setVisibility(View.VISIBLE);
            mBind.rlEmpty.setVisibility(View.GONE);
        }
        mHandler = new Handler(Looper.getMainLooper());
        MQTTAppConfigStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        if (!TextUtils.isEmpty(MQTTAppConfigStr)) {
            appMqttConfig = new Gson().fromJson(MQTTAppConfigStr, MQTTConfig.class);
            mBind.tvTitle.setText(getString(R.string.mqtt_connecting));
        }
        StringBuilder buffer = new StringBuilder();
        // 记录机型
        buffer.append("机型：");
        buffer.append(android.os.Build.MODEL);
        buffer.append("=====");
        // 记录版本号
        buffer.append("手机系统版本：");
        buffer.append(android.os.Build.VERSION.RELEASE);
        buffer.append("=====");
        // 记录APP版本
        buffer.append("APP版本：");
        buffer.append(Utils.getVersionInfo(this));
        XLog.d(buffer.toString());
        try {
            MQTTSupport.getInstance().connectMqtt(MQTTAppConfigStr);
        } catch (FileNotFoundException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ToastUtils.showToast(this, "Please select your SSL certificates again, otherwise the APP can't use normally.");
                startActivityForResult(new Intent(this, SetAppMQTTActivity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
            }
            // 读取stacktrace信息
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            e.printStackTrace(printWriter);
            StringBuilder errorReport = new StringBuilder();
            errorReport.append(result.toString());
            XLog.e(errorReport.toString());
        }
    }

    @Override
    protected ActivityMainJsonBinding getViewBinding() {
        return ActivityMainJsonBinding.inflate(getLayoutInflater());
    }

    ///////////////////////////////////////////////////////////////////////////
    // connect event
    ///////////////////////////////////////////////////////////////////////////
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
        mBind.tvTitle.setText(getString(R.string.app_name));
        // 订阅所有设备的Topic
        subscribeAllDevices();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionLostEvent(MQTTConnectionLostEvent event) {
        mBind.tvTitle.setText(getString(R.string.mqtt_connecting));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionFailureEvent(MQTTConnectionFailureEvent event) {
        mBind.tvTitle.setText(getString(R.string.mqtt_connect_failed));
    }

    ///////////////////////////////////////////////////////////////////////////
    // topic message event
    ///////////////////////////////////////////////////////////////////////////
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        updateDeviceNetworkStatus(event);
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onMQTTUnSubscribeSuccessEvent(MQTTUnSubscribeSuccessEvent event) {
//        dismissLoadingProgressDialog();
//    }
//
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onMQTTUnSubscribeFailureEvent(MQTTUnSubscribeFailureEvent event) {
//        dismissLoadingProgressDialog();
//    }

    ///////////////////////////////////////////////////////////////////////////
    // device event
    ///////////////////////////////////////////////////////////////////////////

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        if (!devices.isEmpty()) {
            for (MokoDevice device : devices) {
                if (device.mac.equalsIgnoreCase(event.getMac())) {
                    device.name = event.getName();
                    break;
                }
            }
        }
        adapter.replaceData(devices);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceDeletedEvent(DeviceDeletedEvent event) {
        // 删除了设备
        int id = event.getId();
        Iterator<MokoDevice> iterator = devices.iterator();
        while (iterator.hasNext()) {
            MokoDevice device = iterator.next();
            if (id == device.id) {
                iterator.remove();
                break;
            }
        }
        adapter.replaceData(devices);
        if (devices.isEmpty()) {
            mBind.rlEmpty.setVisibility(View.VISIBLE);
            mBind.rvDeviceList.setVisibility(View.GONE);
        }
    }
    // TODO: 2023/7/7  未发现该方法有用 暂时先注释掉
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onDeviceUpdateEvent(DeviceUpdateEvent event) {
//        String deviceId = event.getDeviceId();
//        if (TextUtils.isEmpty(deviceId)) return;
//        MokoDevice mokoDevice = DBTools.getInstance(this).selectDevice(deviceId);
//        if (!devices.isEmpty()) {
//            Iterator<MokoDevice> iterator = devices.iterator();
//            while (iterator.hasNext()) {
//                MokoDevice device = iterator.next();
//                if (deviceId.equals(device.deviceId)) {
//                    iterator.remove();
//                    break;
//                }
//            }
//        }
//        devices.add(mokoDevice);
//    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        String mac = event.getMac();
        if (devices == null || devices.size() == 0 || event.isOnline()) return;
        for (MokoDevice mokoDevice : devices) {
            if (mac.equalsIgnoreCase(mokoDevice.mac)) {
                mokoDevice.isOnline = false;
                mokoDevice.on_off = false;
                XLog.i(mokoDevice.mac + "离线");
                adapter.replaceData(devices);
                break;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        XLog.i("onNewIntent...");
        setIntent(intent);
        if (getIntent().getExtras() != null) {
            String from = getIntent().getStringExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY);
            String deviceMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_DEVICE_MAC);
            if (ModifyNameActivity.TAG.equals(from)
                    || PlugSettingActivity.TAG.equals(from) || AddDeviceSuccessActivity.TAG.equals(from)) {
                devices.clear();
                devices.addAll(DBTools.getInstance(this).selectAllDevice());
                if (!TextUtils.isEmpty(deviceMac)) {
                    MokoDevice mokoDevice = DBTools.getInstance(this).selectDeviceByMac(deviceMac);
                    if (mokoDevice == null) return;
                    for (final MokoDevice device : devices) {
                        if (deviceMac.equals(device.mac)) {
                            device.isOnline = true;
                            break;
                        }
                    }
                }
                adapter.replaceData(devices);
                if (!devices.isEmpty()) {
                    mBind.rvDeviceList.setVisibility(View.VISIBLE);
                    mBind.rlEmpty.setVisibility(View.GONE);
                } else {
                    mBind.rvDeviceList.setVisibility(View.GONE);
                    mBind.rlEmpty.setVisibility(View.VISIBLE);
                }
            }
            if (ModifyMQTTSettingsActivity.TAG.equals(from)) {
                if (!TextUtils.isEmpty(deviceMac)) {
                    MokoDevice mokoDevice = DBTools.getInstance(this).selectDeviceByMac(deviceMac);
                    for (final MokoDevice device : devices) {
                        if (deviceMac.equalsIgnoreCase(device.mac)) {
                            if (!device.topicPublish.equals(mokoDevice.topicPublish)) {
                                // 取消订阅
                                try {
                                    MQTTSupport.getInstance().unSubscribe(device.topicPublish);
                                } catch (MqttException e) {
                                    e.printStackTrace();
                                }
                            }
                            device.mqttInfo = mokoDevice.mqttInfo;
                            device.topicPublish = mokoDevice.topicPublish;
                            device.topicSubscribe = mokoDevice.topicSubscribe;
                            break;
                        }
                    }
                }
                adapter.replaceData(devices);
            }
        }
    }

    public void setAppMQTTConfig(View view) {
        if (isWindowLocked()) return;
        startActivityForResult(new Intent(this, SetAppMQTTActivity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
    }

    public void mainAddDevices(View view) {
        if (isWindowLocked()) return;
        if (TextUtils.isEmpty(MQTTAppConfigStr)) {
            startActivityForResult(new Intent(this, SetAppMQTTActivity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
            return;
        }
        if (Utils.isNetworkAvailable(this)) {
            MQTTConfig MQTTAppConfig = new Gson().fromJson(MQTTAppConfigStr, MQTTConfig.class);
            if (TextUtils.isEmpty(MQTTAppConfig.host)) {
                startActivityForResult(new Intent(this, SetAppMQTTActivity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
                return;
            }
            startActivity(new Intent(this, DeviceScannerActivity.class));
        } else {
            String ssid = Utils.getWifiSSID(this);
            ToastUtils.showToast(this, String.format("SSID:%s, the network cannot available,please check", ssid));
            XLog.i(String.format("SSID:%s, the network cannot available,please check", ssid));
        }
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDevice device = (MokoDevice) adapter.getItem(position);
        if (null == device) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!device.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        if (device.isOverload) {
            ToastUtils.showToast(this, "Device is overload, please check it!");
            return;
        }
        if (device.isOverCurrent) {
            ToastUtils.showToast(this, "Device is overcurrent, please check it!");
            return;
        }
        if (device.isOverVoltage) {
            ToastUtils.showToast(this, "Device is overvoltage, please check it!");
            return;
        }
        if (device.isUnderVoltage) {
            ToastUtils.showToast(this, "Device is undervoltage, please check it!");
            return;
        }
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        changeSwitch(device);
    }

    private void changeSwitch(MokoDevice device) {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = device.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        device.on_off = !device.on_off;
        SwitchState switchState = new SwitchState();
        // 设置与当前开关相反的状态
        switchState.switch_state = device.on_off ? 1 : 0;
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = device.mac;
        String message = MQTTMessageAssembler.assembleWriteSwitchInfo(deviceParams, switchState);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_SWITCH_STATE, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDevice mokoDevice = (MokoDevice) adapter.getItem(position);
        if (mokoDevice == null) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, PlugActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDevice);
        startActivity(i);
    }

    @Override
    public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDevice mokoDevice = (MokoDevice) adapter.getItem(position);
        if (mokoDevice == null) return true;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Remove Device");
        dialog.setMessage("Please confirm again whether to \n remove the device,the device \n will be deleted from the device list.");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(JSONMainActivity.this, R.string.network_error);
                return;
            }
//            showLoadingProgressDialog();
            // 取消订阅
            try {
                MQTTSupport.getInstance().unSubscribe(mokoDevice.topicPublish);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            XLog.i(String.format("删除设备:%s", mokoDevice.name));
            DBTools.getInstance(JSONMainActivity.this).deleteDevice(mokoDevice);
            EventBus.getDefault().post(new DeviceDeletedEvent(mokoDevice.id));
        });
        dialog.show(getSupportFragmentManager());
        return true;
    }

    private void subscribeAllDevices() {
        if (!TextUtils.isEmpty(appMqttConfig.topicSubscribe)) {
            try {
                MQTTSupport.getInstance().subscribe(appMqttConfig.topicSubscribe, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            if (devices.isEmpty()) {
                return;
            }
            for (MokoDevice device : devices) {
                try {
                    MQTTSupport.getInstance().subscribe(device.topicPublish, appMqttConfig.qos);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateDeviceNetworkStatus(MQTTMessageArrivedEvent event) {
        if (devices.isEmpty()) return;
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
        for (final MokoDevice device : devices) {
            if (device.mac.equalsIgnoreCase(msgCommon.device_info.mac)) {
                XLog.i("333333deviceMac=" + device.mac + "msgCommon=" + msgCommon.device_info.mac+"id="+msgCommon.msg_id);
                device.isOnline = true;
                if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_SWITCH_STATE
                        || msgCommon.msg_id == MQTTConstants.READ_MSG_ID_SWITCH_INFO) {
                    Type infoType = new TypeToken<SwitchInfo>() {
                    }.getType();
                    SwitchInfo switchInfo = new Gson().fromJson(msgCommon.data, infoType);
                    int switch_state = switchInfo.switch_state;
                    // 启动设备定时离线，90s收不到应答则认为离线
                    device.on_off = switch_state == 1;
                    device.isOverload = switchInfo.overload_state == 1;
                    device.isOverCurrent = switchInfo.overcurrent_state == 1;
                    device.isOverVoltage = switchInfo.overvoltage_state == 1;
                    device.isUnderVoltage = switchInfo.undervoltage_state == 1;
                    device.csq = Integer.parseInt(switchInfo.CSQ);
                    break;
                }
                if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVERLOAD_OCCUR) {
                    Type infoType = new TypeToken<OverloadOccur>() {
                    }.getType();
                    OverloadOccur overloadOccur = new Gson().fromJson(msgCommon.data, infoType);
                    device.isOverload = overloadOccur.state == 1;
                    break;
                }
                if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVER_VOLTAGE_OCCUR) {
                    Type infoType = new TypeToken<OverloadOccur>() {
                    }.getType();
                    OverloadOccur overloadOccur = new Gson().fromJson(msgCommon.data, infoType);
                    device.isOverVoltage = overloadOccur.state == 1;
                    break;
                }
                if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVER_CURRENT_OCCUR) {
                    Type infoType = new TypeToken<OverloadOccur>() {
                    }.getType();
                    OverloadOccur overloadOccur = new Gson().fromJson(msgCommon.data, infoType);
                    device.isOverCurrent = overloadOccur.state == 1;
                    break;
                }
                if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_UNDER_VOLTAGE_OCCUR) {
                    Type infoType = new TypeToken<OverloadOccur>() {
                    }.getType();
                    OverloadOccur overloadOccur = new Gson().fromJson(msgCommon.data, infoType);
                    device.isUnderVoltage = overloadOccur.state == 1;
                    break;
                }
                if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_SWITCH_STATE) {
                    if (mHandler.hasMessages(0)) {
                        dismissLoadingProgressDialog();
                        mHandler.removeMessages(0);
                    }
                    if (msgCommon.result_code != 0) {
                        ToastUtils.showToast(this, "Set up failed");
                    }
                    break;
                }
                break;
            }
        }
        adapter.replaceData(devices);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_MQTT_CONFIG_APP && resultCode == RESULT_OK && null != data) {
            MQTTAppConfigStr = data.getStringExtra(AppConstants.EXTRA_KEY_MQTT_CONFIG_APP);
            appMqttConfig = new Gson().fromJson(MQTTAppConfigStr, MQTTConfig.class);
            mBind.tvTitle.setText(getString(R.string.app_name));
            // 订阅所有设备的Topic
            subscribeAllDevices();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MQTTSupport.getInstance().disconnectMqtt();
    }

    public void onBack(View view) {
        back();
    }

    private void back() {
        if (BuildConfig.IS_LIBRARY) {
            finish();
        } else {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setMessage(R.string.main_exit_tips);
            dialog.setOnAlertConfirmListener(JSONMainActivity.this::finish);
            dialog.show(getSupportFragmentManager());
        }
    }

    @Override
    public void onBackPressed() {
        back();
    }
}
