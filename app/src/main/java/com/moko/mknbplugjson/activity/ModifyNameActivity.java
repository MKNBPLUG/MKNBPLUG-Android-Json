package com.moko.mknbplugjson.activity;

import android.content.Context;
import android.content.Intent;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityModifyDeviceNameBinding;
import com.moko.mknbplugjson.db.DBTools;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.ToastUtils;

public class ModifyNameActivity extends BaseActivity<ActivityModifyDeviceNameBinding> {
    private final String FILTER_ASCII = "[ -~]*";
    public static String TAG = ModifyNameActivity.class.getSimpleName();
    private MokoDevice device;

    @Override
    protected void onCreate() {
        device = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etNickName.setText(device.name);
        mBind.etNickName.setSelection(mBind.etNickName.getText().toString().length());
        mBind.etNickName.setFilters(new InputFilter[]{filter, new InputFilter.LengthFilter(20)});
        mBind.etNickName.postDelayed(() -> {
            InputMethodManager inputManager = (InputMethodManager) mBind.etNickName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(mBind.etNickName, 0);
        }, 300);
    }

    @Override
    protected ActivityModifyDeviceNameBinding getViewBinding() {
        return ActivityModifyDeviceNameBinding.inflate(getLayoutInflater());
    }

    public void modifyDone(View view) {
        String nickName = mBind.etNickName.getText().toString();
        if (TextUtils.isEmpty(nickName)) {
            ToastUtils.showToast(this, R.string.modify_device_name_empty);
            return;
        }
        device.name = nickName;
        DBTools.getInstance(this).updateDevice(device);
        // 跳转首页，刷新数据
        Intent intent = new Intent(this, JSONMainActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_MAC, device.mac);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
}
