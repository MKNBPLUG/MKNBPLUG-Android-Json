package com.moko.mknbplugjson.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.databinding.FragmentSslDevicePathBinding;
import com.moko.mknbplugjson.dialog.BottomDialog;
import com.moko.mknbplugjson.utils.ToastUtils;

import java.util.ArrayList;

public class SSLDevicePathFragment extends Fragment {
    private static final String TAG = SSLDevicePathFragment.class.getSimpleName();
    private int connectMode = 0;
    private ArrayList<String> values;
    private int selected;
    private FragmentSslDevicePathBinding mBind;

    public SSLDevicePathFragment() {
    }

    public static SSLDevicePathFragment newInstance() {
        return new SSLDevicePathFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = FragmentSslDevicePathBinding.inflate(inflater, container, false);
        mBind.clCertificate.setVisibility(connectMode > 0 ? View.VISIBLE : View.GONE);
        mBind.cbSsl.setChecked(connectMode > 0);
        mBind.cbSsl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                connectMode = 0;
            } else {
                connectMode = selected + 1;
            }
            mBind.clCertificate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        values = new ArrayList<>();
        values.add("CA certificate file");
        values.add("Self signed certificates");
        if (connectMode > 0) {
            selected = connectMode - 1;
            mBind.tvCertification.setText(values.get(selected));
        }
        if (selected == 0) {
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
        } else if (selected == 1) {
            mBind.llClientKey.setVisibility(View.VISIBLE);
            mBind.llClientCert.setVisibility(View.VISIBLE);
        }
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

    public void selectCertificate() {
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(values, selected);
        dialog.setListener(value -> {
            selected = value;
            mBind.tvCertification.setText(values.get(selected));
            if (selected == 0) {
                mBind.llClientKey.setVisibility(View.GONE);
                mBind.llClientCert.setVisibility(View.GONE);
            } else if (selected == 1) {
                mBind.llClientKey.setVisibility(View.VISIBLE);
                mBind.llClientCert.setVisibility(View.VISIBLE);
            }
            connectMode = selected + 1;
        });
        if (null != getActivity()) dialog.show(getActivity().getSupportFragmentManager());
    }

    public boolean isValid() {
        final String host = mBind.etMqttHost.getText().toString();
        final String port = mBind.etMqttPort.getText().toString();
        final String caFile = mBind.etCaPath.getText().toString();
        final String clientKeyFile = mBind.etClientKeyPath.getText().toString();
        final String clientCertFile = mBind.etClientCertPath.getText().toString();
        if (connectMode > 0) {
            if (TextUtils.isEmpty(host)) {
                ToastUtils.showToast(requireContext(), "Host error");
                return false;
            }
            if (TextUtils.isEmpty(port)) {
                ToastUtils.showToast(requireContext(), "Port error");
                return false;
            }
            int portInt = Integer.parseInt(port);
            if (portInt < 1 || portInt > 65535) {
                ToastUtils.showToast(requireContext(), "Port error");
                return false;
            }
        }
        if (connectMode == 1) {
            if (TextUtils.isEmpty(caFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_ca));
                return false;
            }
        } else if (connectMode == 2) {
            if (TextUtils.isEmpty(caFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_ca));
                return false;
            }
            if (TextUtils.isEmpty(clientKeyFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_client_key));
                return false;
            }
            if (TextUtils.isEmpty(clientCertFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_client_cert));
                return false;
            }
        }
        return true;
    }

    public int getConnectMode() {
        return connectMode;
    }

    public String getSSLHost() {
        return mBind.etMqttHost.getText().toString();
    }

    public int getSSLPort() {
        final String port = mBind.etMqttPort.getText().toString();
        return Integer.parseInt(port);
    }

    public String getCAPath() {
        return mBind.etCaPath.getText().toString();
    }

    public String getClientCerPath() {
        return mBind.etClientCertPath.getText().toString();
    }

    public String getClientKeyPath() {
        return mBind.etClientKeyPath.getText().toString();
    }
}
