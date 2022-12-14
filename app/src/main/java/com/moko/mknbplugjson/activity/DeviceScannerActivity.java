package com.moko.mknbplugjson.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.elvishew.xlog.XLog;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.R2;
import com.moko.mknbplugjson.adapter.DeviceInfoAdapter;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.dialog.PasswordDialog;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MokoBleScanner;
import com.moko.support.json.MokoSupport;
import com.moko.support.json.OrderTaskAssembler;
import com.moko.support.json.callback.MokoScanDeviceCallback;
import com.moko.support.json.entity.DeviceInfo;
import com.moko.support.json.entity.OrderCHAR;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.support.v18.scanner.ScanRecord;
import no.nordicsemi.android.support.v18.scanner.ScanResult;


public class DeviceScannerActivity extends BaseActivity implements MokoScanDeviceCallback, BaseQuickAdapter.OnItemClickListener {


    @BindView(R2.id.iv_refresh)
    ImageView ivRefresh;
    @BindView(R2.id.rv_devices)
    RecyclerView rvDevices;
    private Animation animation = null;
    private DeviceInfoAdapter mAdapter;
    private ConcurrentHashMap<String, DeviceInfo> mDeviceMap;
    private ArrayList<DeviceInfo> mDevices;
    private Handler mHandler;
    private MokoBleScanner mokoBleScanner;
    private boolean isPasswordError;
    private String mPassword;
    private String mSavedPassword;
    private String mSelectedName;
    private String mSelectedMac;
    private int mSelectedDeviceType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        ButterKnife.bind(this);
        mDeviceMap = new ConcurrentHashMap<>();
        mDevices = new ArrayList<>();
        mAdapter = new DeviceInfoAdapter();
        mAdapter.openLoadAnimation();
        mAdapter.replaceData(mDevices);
        mAdapter.setOnItemClickListener(this);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(mAdapter);
        mokoBleScanner = new MokoBleScanner(this);
        mHandler = new Handler(Looper.getMainLooper());
        mSavedPassword = SPUtils.getStringValue(this, AppConstants.SP_KEY_PASSWORD, "");
        if (animation == null) {
            startScan();
        }
    }

    @Override
    public void onStartScan() {
        mDeviceMap.clear();
        new Thread(() -> {
            while (animation != null) {
                runOnUiThread(() -> {
                    mAdapter.replaceData(mDevices);
                });
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updateDevices();
            }
        }).start();
    }

    @Override
    public void onScanDevice(DeviceInfo deviceInfo) {
        ScanResult scanResult = deviceInfo.scanResult;
        ScanRecord scanRecord = scanResult.getScanRecord();
        SparseArray<byte[]> manufacturer = scanRecord.getManufacturerSpecificData();
        if (manufacturer == null || manufacturer.size() == 0)
            return;
        byte[] manufacturerSpecificDataByte = scanRecord.getManufacturerSpecificData(manufacturer.keyAt(0));
        assert manufacturerSpecificDataByte != null;
        if (manufacturerSpecificDataByte.length != 13) return;
        Map<ParcelUuid, byte[]> map = scanRecord.getServiceData();
        if (map == null || map.isEmpty())
            return;
        int deviceType = -1;
        Iterator iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            ParcelUuid parcelUuid = (ParcelUuid) iterator.next();
            if (parcelUuid.toString().startsWith("0000aa08")) {
                byte[] bytes = map.get(parcelUuid);
                if (bytes != null) {
                    deviceType = bytes[0] & 0xFF;
                    break;
                }
            }
        }
        if (deviceType == -1)
            return;
        deviceInfo.deviceMode = manufacturerSpecificDataByte[12] & 0xFF;
        deviceInfo.deviceType = deviceType;
        mDeviceMap.put(deviceInfo.mac, deviceInfo);
    }

    @Override
    public void onStopScan() {
        ivRefresh.clearAnimation();
        animation = null;
    }

    private void updateDevices() {
        mDevices.clear();
        mDevices.addAll(mDeviceMap.values());
        // ??????
        if (!mDevices.isEmpty()) {
            System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
            Collections.sort(mDevices, new Comparator<DeviceInfo>() {
                @Override
                public int compare(DeviceInfo lhs, DeviceInfo rhs) {
                    if (lhs.rssi > rhs.rssi) {
                        return -1;
                    } else if (lhs.rssi < rhs.rssi) {
                        return 1;
                    }
                    return 0;
                }
            });
        }
    }

    private void startScan() {
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // ??????????????????????????????
            MokoSupport.getInstance().enableBluetooth();
            return;
        }
        animation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
        ivRefresh.startAnimation(animation);
        mokoBleScanner.startScanDevice(this);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mokoBleScanner.stopScanDevice();
            }
        }, 1000 * 60);
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        if (animation != null) {
            mHandler.removeMessages(0);
            mokoBleScanner.stopScanDevice();
        }
        finish();
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        DeviceInfo deviceInfo = (DeviceInfo) adapter.getItem(position);
        if (deviceInfo != null) {
            if (animation != null) {
                mHandler.removeMessages(0);
                mokoBleScanner.stopScanDevice();
            }
            mPassword = "";
            mSelectedName = deviceInfo.name;
            mSelectedMac = deviceInfo.mac;
            mSelectedDeviceType = deviceInfo.deviceType;
            if (deviceInfo.deviceMode == 1) {
                // show password
                final PasswordDialog dialog = new PasswordDialog();
                dialog.setPassword(mSavedPassword);
                dialog.setOnPasswordClicked(new PasswordDialog.PasswordClickListener() {
                    @Override
                    public void onEnsureClicked(String password) {
                        if (!MokoSupport.getInstance().isBluetoothOpen()) {
                            MokoSupport.getInstance().enableBluetooth();
                            return;
                        }
                        XLog.i(password);
                        mPassword = password;
                        if (animation != null) {
                            mHandler.removeMessages(0);
                            mokoBleScanner.stopScanDevice();
                        }
                        showLoadingProgressDialog();
                        ivRefresh.postDelayed(() -> MokoSupport.getInstance().connDevice(deviceInfo.mac), 500);
                    }

                    @Override
                    public void onDismiss() {

                    }
                });
                dialog.show(getSupportFragmentManager());
            } else if (deviceInfo.deviceMode == 2) {
                showLoadingProgressDialog();
                ivRefresh.postDelayed(() -> MokoSupport.getInstance().connDevice(deviceInfo.mac), 500);
            }
        }
    }

    public void onBack(View view) {
        back();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            mPassword = "";
            dismissLoadingProgressDialog();
            dismissLoadingMessageDialog();
            if (isPasswordError) {
                isPasswordError = false;
            } else {
                ToastUtils.showToast(this, "Connection Failed, please try again");
            }
            if (animation == null) {
                startScan();
            }
        }
        if (MokoConstants.ACTION_DISCOVER_SUCCESS.equals(action)) {
            dismissLoadingProgressDialog();
            if (TextUtils.isEmpty(mPassword)) {
                // ??????Debug??????
                Intent intent = new Intent(this, LogDataActivity.class);
                intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_MAC, mSelectedMac);
                startActivityForResult(intent, AppConstants.REQUEST_CODE_LOG);
                return;
            }
            showLoadingMessageDialog("Verifying..");
            mHandler.postDelayed(() -> {
                // open password notify and set passwrord
                List<OrderTask> orderTasks = new ArrayList<>();
                orderTasks.add(OrderTaskAssembler.setPassword(mPassword));
                MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
            }, 500);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            int responseType = response.responseType;
            byte[] value = response.responseValue;
            switch (orderCHAR) {
                case CHAR_PASSWORD:
                    MokoSupport.getInstance().disConnectBle();
                    break;
            }
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            int responseType = response.responseType;
            byte[] value = response.responseValue;
            switch (orderCHAR) {
                case CHAR_PASSWORD:
                    dismissLoadingMessageDialog();
                    if (value.length == 5) {
                        int header = value[0] & 0xFF;// 0xED
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header != 0xED)
                            return;
                        int length = value[3] & 0xFF;
                        if (flag == 0x01 && cmd == 0x01 && length == 0x01) {
                            int result = value[4] & 0xFF;
                            if (1 == result) {
                                mSavedPassword = mPassword;
                                SPUtils.setStringValue(this, AppConstants.SP_KEY_PASSWORD, mSavedPassword);
                                XLog.i("Success");

                                // ??????????????????
                                Intent intent = new Intent(this, ChooseFunctionActivity.class);
                                intent.putExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_MAC, mSelectedMac);
                                intent.putExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_NAME, mSelectedName);
                                intent.putExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_TYPE, mSelectedDeviceType);
                                startActivityForResult(intent, AppConstants.REQUEST_CODE_DEVICE_MQTT_SETTINGS);
                            }
                            if (0 == result) {
                                isPasswordError = true;
                                ToastUtils.showToast(this, "Password Error");
                                MokoSupport.getInstance().disConnectBle();
                            }
                        }
                    }
            }
        }
    }

    public void onRefresh(View view) {
        if (isWindowLocked())
            return;
        if (animation == null) {
            startScan();
        } else {
            mHandler.removeMessages(0);
            mokoBleScanner.stopScanDevice();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_LOG) {
            if (animation == null) {
                startScan();
            }
        }
        if (requestCode == AppConstants.REQUEST_CODE_DEVICE_MQTT_SETTINGS) {
            if (resultCode != RESULT_OK)
                return;
            if (animation == null) {
                startScan();
            }
        }
    }
}
