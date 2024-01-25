package com.moko.mknbplugjson.fragment;

import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.moko.mknbplugjson.databinding.FragmentUserAppBinding;

public class UserFragment extends Fragment {
    private final String FILTER_ASCII = "[ -~]*";
    private static final String TAG = UserFragment.class.getSimpleName();
    private String username;
    private String password;
    private FragmentUserAppBinding mBind;

    public UserFragment() {
    }

    public static UserFragment newInstance() {
        return new UserFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = FragmentUserAppBinding.inflate(inflater, container, false);
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
        if (null != mBind) mBind.etMqttUsername.setText(username);
    }

    public void setPassword(String password) {
        this.password = password;
        if (null != mBind) mBind.etMqttPassword.setText(password);
    }

    public String getUsername() {
        return mBind.etMqttUsername.getText().toString();
    }

    public String getPassword() {
        return mBind.etMqttPassword.getText().toString();
    }
}
