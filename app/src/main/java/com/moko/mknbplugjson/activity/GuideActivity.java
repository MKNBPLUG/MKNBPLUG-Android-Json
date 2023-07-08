package com.moko.mknbplugjson.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityGuideBinding;
import com.moko.mknbplugjson.utils.Utils;

public class GuideActivity extends BaseActivity<ActivityGuideBinding> {

    @Override
    protected void onCreate() {
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isWriteStoragePermissionOpen()) {
                showRequestPermissionDialog();
                return;
            }
        }
        delayGotoMain();
    }

    @Override
    protected ActivityGuideBinding getViewBinding() {
        return ActivityGuideBinding.inflate(getLayoutInflater());
    }

    private void delayGotoMain() {
        if (!Utils.isLocServiceEnable(this)) {
            showOpenLocationDialog();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isLocationPermissionOpen()) {
                showRequestPermissionDialog2();
                return;
            } else {
                AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                int checkOp = appOpsManager.checkOp(AppOpsManager.OPSTR_FINE_LOCATION, Process.myUid(), getPackageName());
                if (checkOp != AppOpsManager.MODE_ALLOWED) {
                    showOpenSettingsDialog2();
                    return;
                }
            }
        }
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(() -> {
                    startActivity(new Intent(GuideActivity.this, JSONMainActivity.class));
                    GuideActivity.this.finish();
                });
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isWriteStoragePermissionOpen()) {
                    showOpenSettingsDialog();
                } else {
                    delayGotoMain();
                }
            }
        }
        if (requestCode == AppConstants.REQUEST_CODE_PERMISSION_2) {
            delayGotoMain();
        }
        if (requestCode == AppConstants.REQUEST_CODE_LOCATION_SETTINGS) {
            if (!Utils.isLocServiceEnable(this)) {
                showOpenLocationDialog();
            } else {
                delayGotoMain();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppConstants.PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // 判断用户是否 点击了不再提醒。(检测该权限是否还可以申请)
                    boolean shouldShowRequest = shouldShowRequestPermissionRationale(permissions[0]);
                    if (shouldShowRequest) {
                        if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                            showRequestPermissionDialog2();
                        } else {
                            showRequestPermissionDialog();
                        }
                    } else {
                        if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                            showOpenSettingsDialog2();
                        } else {
                            showOpenSettingsDialog();
                        }
                    }
                } else {
                    delayGotoMain();
                }
            }
        }
    }

    private void showOpenSettingsDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.permission_storage_close_title)
                .setMessage(R.string.permission_storage_close_content)
                .setPositiveButton(getString(R.string.confirm), (dialog12, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    // 根据包名打开对应的设置界面
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, AppConstants.REQUEST_CODE_PERMISSION);
                })
                .setNegativeButton(getString(R.string.cancel), (dialog1, which) -> {
                    finish();
                }).create();
        dialog.show();
    }

    private void showRequestPermissionDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.permission_storage_need_title)
                .setMessage(R.string.permission_storage_need_content)
                .setPositiveButton(getString(R.string.confirm), (dialog1, which) -> ActivityCompat.requestPermissions(GuideActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, AppConstants.PERMISSION_REQUEST_CODE))
                .setNegativeButton(getString(R.string.cancel), (dialog12, which) -> {
                    finish();
                }).create();
        dialog.show();
    }

    private void showOpenLocationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.location_need_title)
                .setMessage(R.string.location_need_content)
                .setPositiveButton(getString(R.string.permission_open), (dialog1, which) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, AppConstants.REQUEST_CODE_LOCATION_SETTINGS);
                })
                .setNegativeButton(getString(R.string.cancel), (dialog12, which) -> {
                    finish();
                }).create();
        dialog.show();
    }

    private void showOpenSettingsDialog2() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.permission_location_close_title)
                .setMessage(R.string.permission_location_close_content)
                .setPositiveButton(getString(R.string.permission_open), (dialog1, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    // 根据包名打开对应的设置界面
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, AppConstants.REQUEST_CODE_PERMISSION_2);
                })
                .setNegativeButton(getString(R.string.cancel), (dialog12, which) -> {
                    finish();
                }).create();
        dialog.show();
    }

    private void showRequestPermissionDialog2() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.permission_location_need_title)
                .setMessage(R.string.permission_location_need_content)
                .setPositiveButton(getString(R.string.ensure), (dialog1, which) -> ActivityCompat.requestPermissions(GuideActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, AppConstants.PERMISSION_REQUEST_CODE))
                .setNegativeButton(getString(R.string.cancel), (dialog12, which) -> {
                    finish();
                }).create();
        dialog.show();
    }
}
