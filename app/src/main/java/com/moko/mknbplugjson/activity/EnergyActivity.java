package com.moko.mknbplugjson.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.R2;
import com.moko.mknbplugjson.adapter.EnergyListAdapter;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.dialog.AlertMessageDialog;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.EnergyHistory;
import com.moko.support.json.entity.EnergyInfo;
import com.moko.support.json.entity.EnergyTotal;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.event.DeviceOnlineEvent;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;


public class EnergyActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {

    @BindView(R2.id.rg_energy)
    RadioGroup rgEnergy;
    @BindView(R2.id.tv_energy_total)
    TextView tvEnergyTotal;
    @BindView(R2.id.tv_duration)
    TextView tvDuration;
    @BindView(R2.id.tv_unit)
    TextView tvUnit;
    @BindView(R2.id.rv_energy)
    RecyclerView rvEnergy;
    @BindView(R2.id.rb_hourly)
    RadioButton rbHourly;
    @BindView(R2.id.rb_daily)
    RadioButton rbDaily;
    @BindView(R2.id.rb_totally)
    RadioButton rbTotally;
    @BindView(R2.id.cl_energy)
    ConstraintLayout clEnergy;
    @BindView(R2.id.tv_energy_desc)
    TextView tvEnergyDesc;
    @BindView(R2.id.tv_title)
    TextView tvTitle;
    private EnergyListAdapter adapter;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private List<EnergyInfo> energyInfoList;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy);
        ButterKnife.bind(this);
        if (getIntent().getExtras() != null) {
            mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
            tvTitle.setText(mMokoDevice.name);
        }
        String mqttConfigAppStr = SPUtils.getStringValue(EnergyActivity.this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mHandler = new Handler(Looper.getMainLooper());
        energyInfoList = new ArrayList<>();
        adapter = new EnergyListAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(energyInfoList);
        rvEnergy.setLayoutManager(new LinearLayoutManager(this));
        rvEnergy.setAdapter(adapter);
        rgEnergy.setOnCheckedChangeListener(this);
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getEnergyHourly();
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
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_ENERGY_HOURLY
                || msgCommon.msg_id == MQTTConstants.READ_MSG_ID_ENERGY_HOURLY) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (!rbHourly.isChecked()) return;
            Type infoType = new TypeToken<EnergyHistory>() {
            }.getType();
            EnergyHistory energyHistoryHourly = new Gson().fromJson(msgCommon.data, infoType);
            if (TextUtils.isEmpty(energyHistoryHourly.timestamp)) return;
            String date = energyHistoryHourly.timestamp.substring(5, 10);
            String hour = energyHistoryHourly.timestamp.substring(11, 13);
            tvDuration.setText(String.format("00:00 to %s:00,%s", hour, date));
            energyInfoList.clear();
            int energyDataSum = 0;
            for (int i = 0; i < energyHistoryHourly.num; i++) {
                int energyInt = energyHistoryHourly.energy[i];
                energyDataSum += energyInt;
                EnergyInfo energyInfo = new EnergyInfo();
                energyInfo.time = String.format("%02d:00", i);
                energyInfo.value = MokoUtils.getDecimalFormat("0.##").format(energyInt * 0.01f);
                energyInfoList.add(0, energyInfo);
            }
            adapter.replaceData(energyInfoList);
            tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(energyDataSum * 0.01f));
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_ENERGY_DAILY
                || msgCommon.msg_id == MQTTConstants.READ_MSG_ID_ENERGY_DAILY) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (!rbDaily.isChecked()) return;
            Type infoType = new TypeToken<EnergyHistory>() {
            }.getType();
            EnergyHistory energyHistoryDaily = new Gson().fromJson(msgCommon.data, infoType);
            if (TextUtils.isEmpty(energyHistoryDaily.timestamp)) return;
            int year = Integer.parseInt(energyHistoryDaily.timestamp.substring(0, 4));
            int month = Integer.parseInt(energyHistoryDaily.timestamp.substring(5, 7));
            int day = Integer.parseInt(energyHistoryDaily.timestamp.substring(8, 10));
            int hour = Integer.parseInt(energyHistoryDaily.timestamp.substring(11, 13));
            int count = energyHistoryDaily.num;
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            String end = MokoUtils.calendar2strDate(calendar, "MM-dd");
            Calendar startCalendar = (Calendar) calendar.clone();
            startCalendar.add(Calendar.DAY_OF_MONTH, -(count - 1));
            String start = MokoUtils.calendar2strDate(startCalendar, "MM-dd");
            tvDuration.setText(String.format("%s to %s", start, end));
            energyInfoList.clear();
            int energyDataSum = 0;
            for (int i = 0; i < count; i++) {
                int energyInt = energyHistoryDaily.energy[i];
                energyDataSum += energyInt;
                EnergyInfo energyInfo = new EnergyInfo();
                String date = MokoUtils.calendar2strDate(calendar, "MM-dd");
                energyInfo.time = date;
                energyInfo.value = MokoUtils.getDecimalFormat("0.##").format(energyInt * 0.01f);
                energyInfoList.add(energyInfo);
                calendar.add(Calendar.DAY_OF_MONTH, -1);
            }
            adapter.replaceData(energyInfoList);
            tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(energyDataSum * 0.01f));
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_ENERGY_TOTAL
                || msgCommon.msg_id == MQTTConstants.READ_MSG_ID_ENERGY_TOTAL) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (!rbTotally.isChecked()) return;
            Type infoType = new TypeToken<EnergyTotal>() {
            }.getType();
            EnergyTotal energyTotal = new Gson().fromJson(msgCommon.data, infoType);
            tvEnergyTotal.setText(MokoUtils.getDecimalFormat("0.##").format(energyTotal.energy * 0.01f));
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_ENERGY_CLEAR) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            energyInfoList.clear();
            adapter.replaceData(energyInfoList);
            tvEnergyTotal.setText("0");
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

    private void getEnergyHourly() {
        XLog.i("查询当天每小时电能");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadEnergyHourly(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_ENERGY_HOURLY, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getEnergyDaily() {
        XLog.i("查询最近30天电能");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadEnergyDaily(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_ENERGY_DAILY, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getEnergyTotal() {
        XLog.i("查询总累计电能");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadEnergyTotal(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_ENERGY_TOTAL, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.rb_hourly) {
            // 切换日
            clEnergy.setVisibility(View.VISIBLE);
            tvUnit.setText("Hour");
            tvEnergyDesc.setText("Today energy:");
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                finish();
            }, 30 * 1000);
            getEnergyHourly();
        } else if (checkedId == R.id.rb_daily) {
            // 切换月
            clEnergy.setVisibility(View.VISIBLE);
            tvUnit.setText("Date");
            tvEnergyDesc.setText("Last 30 days energy:");
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                finish();
            }, 30 * 1000);
            getEnergyDaily();
        } else if (checkedId == R.id.rb_totally) {
            // 切换总电能
            tvEnergyDesc.setText("Historical total energy:");
            clEnergy.setVisibility(View.GONE);
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                finish();
            }, 30 * 1000);
            getEnergyTotal();
        }
    }

    public void onEmpty(View view) {
        if (isWindowLocked()) {
            return;
        }
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Reset Energy Data");
        dialog.setMessage("After reset, all energy data will be deleted, please confirm again whether to reset it？");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            if (!mMokoDevice.isOnline) {
                ToastUtils.showToast(this, R.string.device_offline);
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            clearEnergy();
        });
        dialog.show(getSupportFragmentManager());
    }

    private void clearEnergy() {
        XLog.i("清除电能数据");
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.device_id = mMokoDevice.deviceId;
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleConfigEnergyClear(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_ENERGY_CLEAR, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
