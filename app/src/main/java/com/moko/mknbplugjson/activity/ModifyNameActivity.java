package com.moko.mknbplugjson.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.R2;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.db.DBTools;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.event.MQTTConnectionCompleteEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;


public class ModifyNameActivity extends BaseActivity {
    private final String FILTER_ASCII = "[ -~]*";
    public static String TAG = ModifyNameActivity.class.getSimpleName();

    @BindView(R2.id.et_nick_name)
    EditText etNickName;
    private MokoDevice device;
    private InputFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_device_name);
        ButterKnife.bind(this);
        device = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        etNickName.setText(device.name);
        etNickName.setSelection(etNickName.getText().toString().length());
        etNickName.setFilters(new InputFilter[]{filter, new InputFilter.LengthFilter(20)});
        etNickName.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager inputManager = (InputMethodManager) etNickName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(etNickName, 0);
            }
        }, 300);
    }


    public void modifyDone(View view) {
        String nickName = etNickName.getText().toString();
        if (TextUtils.isEmpty(nickName)) {
            ToastUtils.showToast(this, R.string.modify_device_name_empty);
            return;
        }
        device.name = nickName;
        DBTools.getInstance(this).updateDevice(device);
        // ???????????????????????????
        Intent intent = new Intent(this, JSONMainActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE_ID, device.deviceId);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
    }
}
