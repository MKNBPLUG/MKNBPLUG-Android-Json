package com.moko.mknbplugjson.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.moko.mknbplugjson.databinding.FragmentGeneralAppBinding;
import com.moko.mknbplugjson.utils.ToastUtils;

public class GeneralFragment extends Fragment {
    private static final String TAG = GeneralFragment.class.getSimpleName();
    private boolean cleanSession;
    private int qos;
    private int keepAlive;
    private FragmentGeneralAppBinding mBind;

    public GeneralFragment() {
    }

    public static GeneralFragment newInstance() {
        return new GeneralFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = FragmentGeneralAppBinding.inflate(inflater, container, false);
        mBind.cbCleanSession.setChecked(cleanSession);
        if (qos == 0) {
            mBind.rbQos1.setChecked(true);
        } else if (qos == 1) {
            mBind.rbQos2.setChecked(true);
        } else if (qos == 2) {
            mBind.rbQos3.setChecked(true);
        }
        mBind.etKeepAlive.setText(String.valueOf(keepAlive));
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

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
        if (null != mBind) mBind.cbCleanSession.setChecked(cleanSession);
    }

    public void setQos(int qos) {
        this.qos = qos;
        if (null == mBind) return;
        if (qos == 0) {
            mBind.rbQos1.setChecked(true);
        } else if (qos == 1) {
            mBind.rbQos2.setChecked(true);
        } else if (qos == 2) {
            mBind.rbQos3.setChecked(true);
        }
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
        if (null != mBind) mBind.etKeepAlive.setText(String.valueOf(keepAlive));
    }

    public boolean isValid() {
        final String keepAliveStr = mBind.etKeepAlive.getText().toString();
        if (TextUtils.isEmpty(keepAliveStr)) {
            ToastUtils.showToast(getActivity(), "Error");
            return false;
        }
        final int keepAlive = Integer.parseInt(keepAliveStr);
        if (keepAlive < 10 || keepAlive > 120) {
            ToastUtils.showToast(getActivity(), "Keep Alive range is 10-120");
            return false;
        }
        return true;
    }

    public boolean isCleanSession() {
        return mBind.cbCleanSession.isChecked();
    }

    public int getQos() {
        int qos = 0;
        if (mBind.rbQos2.isChecked()) {
            qos = 1;
        } else if (mBind.rbQos3.isChecked()) {
            qos = 2;
        }
        return qos;
    }

    public int getKeepAlive() {
        String keepAliveStr = mBind.etKeepAlive.getText().toString();
        return Integer.parseInt(keepAliveStr);
    }
}
