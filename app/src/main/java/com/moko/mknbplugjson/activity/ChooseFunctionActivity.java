package com.moko.mknbplugjson.activity;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.dialog.AlertMessageDialog;
import com.moko.mknbplugjson.service.DfuService;
import com.moko.mknbplugjson.utils.FileUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MokoSupport;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import butterknife.ButterKnife;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;


public class ChooseFunctionActivity extends BaseActivity {
    public static final int REQUEST_CODE_SELECT_FIRMWARE = 0x10;

    private int mSelectedDeviceType;
    private String mSelectedDeviceName;
    private String mSelectedDeviceMac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_function);
        ButterKnife.bind(this);
        mSelectedDeviceName = getIntent().getStringExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_NAME);
        mSelectedDeviceMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_MAC);
        mSelectedDeviceType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_TYPE, 0);
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        MokoSupport.getInstance().disConnectBle();
    }


    public void onBack(View view) {
        if (isWindowLocked())
            return;
        back();
    }


    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        EventBus.getDefault().cancelEventDelivery(event);
        if (isUpdate) {
            return;
        }
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(ChooseFunctionActivity.this,"Disconnected!");
                setResult(RESULT_OK);
                finish();
            });
        }
    }


    public void onMQTTSettingForDevice(View view) {
        if (isWindowLocked())
            return;
        // 跳转配置页面
        Intent intent = new Intent(this, SetDeviceMQTTActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_MAC, mSelectedDeviceMac);
        intent.putExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_NAME, mSelectedDeviceName);
        intent.putExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_TYPE, mSelectedDeviceType);
        startActivityForResult(intent, AppConstants.REQUEST_CODE_DEVICE_MQTT_SETTINGS);
    }

    public void onDFU(View view) {
        if (isWindowLocked())
            return;
        chooseFirmwareFile();
    }

    private boolean isUpdate;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_FIRMWARE) {
            if (resultCode == RESULT_OK) {
                //得到uri，后面就是将uri转化成file的过程。
                Uri uri = data.getData();
                String firmwareFilePath = FileUtils.getPath(this, uri);
                if (TextUtils.isEmpty(firmwareFilePath))
                    return;
                final File firmwareFile = new File(firmwareFilePath);
                if (firmwareFile.exists()) {
                    final DfuServiceInitiator starter = new DfuServiceInitiator(mSelectedDeviceMac)
                            .setDeviceName(mSelectedDeviceName)
                            .setKeepBond(false)
                            .setDisableNotification(true);
                    starter.setZip(null, firmwareFilePath);
                    starter.start(this, DfuService.class);
                    showDFUProgressDialog("Waiting...");
                    isUpdate = true;
                } else {
                    ToastUtils.showToast(this, "file is not exists!");
                }
            }
        }
        if (requestCode == AppConstants.REQUEST_CODE_DEVICE_MQTT_SETTINGS) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK);
                finish();
            }
        }
    }

    public void chooseFirmwareFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), REQUEST_CODE_SELECT_FIRMWARE);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(this, "install file manager app");
        }
    }

    private ProgressDialog mDFUDialog;

    private void showDFUProgressDialog(String tips) {
        mDFUDialog = new ProgressDialog(this);
        mDFUDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDFUDialog.setCanceledOnTouchOutside(false);
        mDFUDialog.setCancelable(false);
        mDFUDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDFUDialog.setMessage(tips);
        if (!isFinishing() && mDFUDialog != null && !mDFUDialog.isShowing()) {
            mDFUDialog.show();
        }
    }

    private String mAlertMessage;

    private void dismissDFUProgressDialog() {
        mDeviceConnectCount = 0;
        if (!isFinishing() && mDFUDialog != null && mDFUDialog.isShowing()) {
            mDFUDialog.dismiss();
        }
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Update Firmware");
        dialog.setCancelGone();
        dialog.setConfirm("OK");
        dialog.setMessage(mAlertMessage);
        dialog.setOnAlertConfirmListener(() -> {
            setResult(RESULT_OK);
            finish();
        });
        dialog.show(getSupportFragmentManager());
    }

    private int mDeviceConnectCount;

    private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            XLog.w("onDeviceConnecting...");
            mDeviceConnectCount++;
            if (mDeviceConnectCount > 3) {
                mAlertMessage = "Opps, update firmware failed!\nPlease reconnect and try it again!";
                dismissDFUProgressDialog();
                final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(ChooseFunctionActivity.this);
                final Intent abortAction = new Intent(DfuService.BROADCAST_ACTION);
                abortAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_ABORT);
                manager.sendBroadcast(abortAction);
            }
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            XLog.w("onDeviceDisconnecting...");
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            mDFUDialog.setMessage("DfuProcessStarting...");
        }


        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            mDFUDialog.setMessage("EnablingDfuMode...");
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            mDFUDialog.setMessage("FirmwareValidating...");
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            isUpdate = !isUpdate;
            mAlertMessage = "Update Firmware successfully!\nPlease reconnect the device.";
            dismissDFUProgressDialog();
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            mDFUDialog.setMessage("DfuAborted...");
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            mDFUDialog.setMessage("Progress:" + percent + "%");
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            XLog.i("Error:" + message);
            isUpdate = !isUpdate;
            mAlertMessage = "Opps, update firmware failed!\nPlease reconnect and try it again!";
            dismissDFUProgressDialog();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
    }
}
