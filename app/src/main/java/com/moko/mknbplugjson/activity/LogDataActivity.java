package com.moko.mknbplugjson.activity;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.R2;
import com.moko.mknbplugjson.adapter.LogDataListAdapter;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.db.DBTools;
import com.moko.mknbplugjson.dialog.AlertMessageDialog;
import com.moko.mknbplugjson.entity.LogData;
import com.moko.mknbplugjson.entity.MQTTConfig;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtiles;
import com.moko.mknbplugjson.utils.Utils;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.MokoSupport;
import com.moko.support.json.OrderTaskAssembler;
import com.moko.support.json.entity.OrderCHAR;
import com.moko.support.json.event.DeviceDeletedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class LogDataActivity extends BaseActivity implements BaseQuickAdapter.OnItemClickListener {

    public static String TAG = LogDataActivity.class.getSimpleName();
    @BindView(R2.id.tv_sync_switch)
    TextView tvSyncSwitch;
    @BindView(R2.id.iv_sync)
    ImageView ivSync;
    @BindView(R2.id.tv_export)
    TextView tvExport;
    @BindView(R2.id.tv_empty)
    TextView tvEmpty;
    @BindView(R2.id.rv_export_data)
    RecyclerView rvLogData;
    @BindView(R2.id.tv_log_info)
    TextView tvLogInfo;
    private StringBuilder storeString;
    private ArrayList<LogData> LogDatas;
    private boolean isSync;
    private LogDataListAdapter adapter;
    private String logDirPath;
    private String mDeviceMac;
    private int selectedCount;
    private String syncTime;
    private Animation animation = null;
    private boolean isDisconnected;
    private boolean isBack;
    private boolean isExitFinish;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_data);
        ButterKnife.bind(this);
        mDeviceMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_DEVICE_MAC).replaceAll(":", "");
        logDirPath = JSONMainActivity.PATH_LOGCAT + File.separator + mDeviceMac;
        LogDatas = new ArrayList<>();
        adapter = new LogDataListAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(LogDatas);
        adapter.setOnItemClickListener(this);
        rvLogData.setLayoutManager(new LinearLayoutManager(this));
        rvLogData.setAdapter(adapter);
        File file = new File(logDirPath);
        if (file.exists()) {
            File[] logFiles = file.listFiles();
            Arrays.sort(logFiles, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    long diff = f1.lastModified() - f2.lastModified();
                    if (diff > 0)
                        return 1;
                    else if (diff == 0)
                        return 0;
                    else
                        return -1;
                }

                public boolean equals(Object obj) {
                    return true;
                }

            });
            for (int i = 0, l = logFiles.length; i < l; i++) {
                File logFile = logFiles[i];
                LogData data = new LogData();
                data.filePath = logFile.getAbsolutePath();
                data.name = logFile.getName().replaceAll(".txt", "");
                LogDatas.add(data);
            }
            adapter.replaceData(LogDatas);
        }
        // 点击无效间隔改为1秒
        voidDuration = 1000;
        storeString = new StringBuilder();
        mMokoDevice = DBTools.getInstance(this).selectDeviceByMac(mDeviceMac);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        EventBus.getDefault().cancelEventDelivery(event);
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                dismissLoadingProgressDialog();
                isDisconnected = true;
                // 中途断开，要先保存数据
                tvSyncSwitch.setEnabled(false);
                if (isSync)
                    stopSync();
                else {
                    if (isExitFinish)
                        setResult(RESULT_OK);
                    finish();
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    case CHAR_DEBUG_EXIT:
                        isExitFinish = true;
                        if (mMokoDevice == null)
                            return;
                        dismissLoadingProgressDialog();
                        if (TextUtils.isEmpty(appMqttConfig.topicSubscribe)) {
                            // 取消订阅
                            try {
                                MQTTSupport.getInstance().unSubscribe(mMokoDevice.topicPublish);
                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                        }
                        XLog.i(String.format("删除设备:%s", mMokoDevice.name));
                        DBTools.getInstance(this).deleteDevice(mMokoDevice);
                        EventBus.getDefault().post(new DeviceDeletedEvent(mMokoDevice.id));
                        tvSyncSwitch.postDelayed(() -> {
                            dismissLoadingProgressDialog();
                            // 跳转首页，刷新数据
                            Intent intent = new Intent(this, JSONMainActivity.class);
                            intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
                            intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_ID, mMokoDevice.deviceId);
                            startActivity(intent);
                        }, 500);
                        break;
                }
            }
            if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    case CHAR_DISCONNECTED_NOTIFY:
                        break;
                    case CHAR_DEBUG_LOG:
                        String log = new String(value);
                        storeString.append(log);
                        tvLogInfo.append(log);
                        break;
                }
            }
        });
    }


    public void onSyncSwitch(View view) {
        if (isWindowLocked())
            return;
        int size = LogDatas.size();
        if (size >= 10) {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setTitle("Tips");
            dialog.setMessage("Up to 10 log files can be stored, please delete the useless logs first！");
            dialog.setConfirm("OK");
            dialog.setCancelGone();
            dialog.show(getSupportFragmentManager());
            return;
        }
        if (animation == null) {
            storeString = new StringBuilder();
            tvLogInfo.setText("");
            tvSyncSwitch.setText("Stop");
            isSync = true;
            animation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
            ivSync.startAnimation(animation);
            MokoSupport.getInstance().enableDebugLogNotify();
            Calendar calendar = Calendar.getInstance();
            syncTime = MokoUtils.calendar2strDate(calendar, "yyyy-MM-dd HH-mm-ss");
        } else {
            MokoSupport.getInstance().disableDebugLogNotify();
            stopSync();
        }
    }

    public void writeLogFile2SDCard(String filePath) {
        String log = storeString.toString();
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(log);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onEmpty(View view) {
        if (isWindowLocked())
            return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning!");
        dialog.setMessage("Are you sure to empty the saved debugger log?");
        dialog.setOnAlertConfirmListener(() -> {
            Iterator<LogData> iterator = LogDatas.iterator();
            while (iterator.hasNext()) {
                LogData LogData = iterator.next();
                if (!LogData.isSelected)
                    continue;
                File file = new File(LogData.filePath);
                if (file.exists())
                    file.delete();
                iterator.remove();
                selectedCount--;
            }
            if (selectedCount > 0) {
                tvEmpty.setEnabled(true);
                tvExport.setEnabled(true);
            } else {
                tvEmpty.setEnabled(false);
                tvExport.setEnabled(false);
            }
            adapter.replaceData(LogDatas);
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onExport(View view) {
        if (isWindowLocked())
            return;
        ArrayList<File> selectedFiles = new ArrayList<>();
        for (LogData LogData : LogDatas) {
            if (LogData.isSelected) {
                selectedFiles.add(new File(LogData.filePath));
            }
        }
        if (!selectedFiles.isEmpty()) {
            File[] files = selectedFiles.toArray(new File[]{});
            // 发送邮件
            String address = "Development@mokotechnology.com";
            String title = "Debugger Log";
            String content = title;
            Utils.sendEmail(LogDataActivity.this, address, content, title, "Choose Email Client", files);
        }
    }
    private void backHome() {
        if (isSync) {
            MokoSupport.getInstance().disableDebugLogNotify();
            stopSync();
        } else {
            if (isDisconnected) {
                finish();
                return;
            }
            MokoSupport.getInstance().disConnectBle();
        }
    }

    private void stopSync() {
        tvSyncSwitch.setText("Start");
        isSync = false;
        // 关闭通知
        ivSync.clearAnimation();
        animation = null;
        if (storeString.length() == 0) {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setTitle("Tips");
            dialog.setMessage("No debug logs are sent during this process！");
            dialog.setConfirm("OK");
            dialog.setCancelGone();
            dialog.setOnAlertConfirmListener(() -> {
                if (isDisconnected || isBack)
                    finish();
            });
            dialog.show(getSupportFragmentManager());
            return;
        }
        File logDir = new File(logDirPath);
        if (!logDir.exists())
            logDir.mkdirs();
        String logFilePath = logDirPath + File.separator + String.format("%s.txt", syncTime);
        writeLogFile2SDCard(logFilePath);
        LogData LogData = new LogData();
        LogData.name = syncTime;
        LogData.filePath = logFilePath;
        LogDatas.add(LogData);
        adapter.replaceData(LogDatas);
        if (isBack)
            finish();
    }

    @Override
    public void onBackPressed() {
        isBack = true;
        backHome();
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        LogData LogData = (LogData) adapter.getItem(position);
        if (LogData != null) {
            LogData.isSelected = !LogData.isSelected;
            if (LogData.isSelected) {
                selectedCount++;
            } else {
                selectedCount--;
            }
            if (selectedCount > 0) {
                tvEmpty.setEnabled(true);
                tvExport.setEnabled(true);
            } else {
                tvEmpty.setEnabled(false);
                tvExport.setEnabled(false);
            }
            adapter.notifyItemChanged(position);
        }
    }

    public void onBack(View view) {
        if (isWindowLocked())
            return;
        isBack = true;
        backHome();
    }

    public void onExitDebugMode(View view) {
        if (isWindowLocked())
            return;
        showLoadingProgressDialog();
        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.exitDebugMode());
    }
}
