package com.moko.mknbplugjson.fragment;

import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.moko.mknbplugjson.databinding.FragmentUserDeviceBinding;

public class UserDeviceFragment extends Fragment {
    private final String FILTER_ASCII = "[ -~]*";
    private static final String TAG = UserDeviceFragment.class.getSimpleName();
    private String username;
    private String password;
    private FragmentUserDeviceBinding mBind;

    public UserDeviceFragment() {
    }

    public static UserDeviceFragment newInstance() {
        return new UserDeviceFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = FragmentUserDeviceBinding.inflate(inflater, container, false);
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etMqttUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        mBind.etMqttPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        mBind.etMqttUsername.setText(username);
        mBind.etMqttPassword.setText(password);
        return mBind.getRoot();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause: ");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
    }

    public void setUserName(String username) {
        this.username = username;
        if (null == mBind) return;
        mBind.etMqttUsername.setText(username);
        mBind.etMqttUsername.setSelection(mBind.etMqttUsername.getText().length());
    }

    public void setPassword(String password) {
        this.password = password;
        if (null == mBind) return;
        mBind.etMqttPassword.setText(password);
        mBind.etMqttPassword.setSelection(mBind.etMqttPassword.getText().length());
    }

    public String getUsername() {
        return mBind.etMqttUsername.getText().toString();
    }

    public String getPassword() {
        return mBind.etMqttPassword.getText().toString();
    }
}
