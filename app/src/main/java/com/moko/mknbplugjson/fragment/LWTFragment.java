package com.moko.mknbplugjson.fragment;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.R2;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.utils.ToastUtils;

import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;

public class LWTFragment extends Fragment {
    private static final String TAG = LWTFragment.class.getSimpleName();
    private final String FILTER_ASCII = "[ -~]*";
    @BindView(R2.id.rb_qos_1)
    RadioButton rbQos1;
    @BindView(R2.id.rb_qos_2)
    RadioButton rbQos2;
    @BindView(R2.id.rb_qos_3)
    RadioButton rbQos3;
    @BindView(R2.id.rg_qos)
    RadioGroup rgQos;
    @BindView(R2.id.cb_lwt)
    CheckBox cbLwt;
    @BindView(R2.id.cb_lwt_retain)
    CheckBox cbLwtRetain;
    @BindView(R2.id.et_lwt_topic)
    EditText etLwtTopic;
    @BindView(R2.id.et_lwt_payload)
    EditText etLwtPayload;

    private BaseActivity activity;

    private boolean lwtEnable;
    private boolean lwtRetain;
    private int qos;
    private String topic;
    private String payload;

    public LWTFragment() {
    }

    public static LWTFragment newInstance() {
        LWTFragment fragment = new LWTFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_lwt, container, false);
        ButterKnife.bind(this, view);
        activity = (BaseActivity) getActivity();
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        etLwtTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        etLwtPayload.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        etLwtTopic.setText(topic);
        etLwtPayload.setText(payload);
        cbLwt.setChecked(lwtEnable);
        cbLwtRetain.setChecked(lwtRetain);
        if (qos == 0) {
            rbQos1.setChecked(true);
        } else if (qos == 1) {
            rbQos2.setChecked(true);
        } else if (qos == 2) {
            rbQos3.setChecked(true);
        }
        return view;
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

    public boolean isValid() {
        final String topicStr = etLwtTopic.getText().toString();
        if (TextUtils.isEmpty(topicStr)) {
            ToastUtils.showToast(getActivity(), "LWT Topic Error");
            return false;
        }
        topic = topicStr;
        final String payloadStr = etLwtPayload.getText().toString();
        if (TextUtils.isEmpty(payloadStr)) {
            ToastUtils.showToast(getActivity(), "LWT Payload Error");
            return false;
        }
        payload = payloadStr;
        return true;
    }

    public void setQos(int qos) {
        this.qos = qos;
        if (rgQos == null)
            return;
        if (qos == 0) {
            rbQos1.setChecked(true);
        } else if (qos == 1) {
            rbQos2.setChecked(true);
        } else if (qos == 2) {
            rbQos3.setChecked(true);
        }
    }

    public int getQos() {
        int qos = 0;
        if (rbQos2.isChecked()) {
            qos = 1;
        } else if (rbQos3.isChecked()) {
            qos = 2;
        }
        return qos;
    }

    public boolean getLwtEnable() {
        return cbLwt.isChecked();
    }

    public void setLwtEnable(boolean lwtEnable) {
        this.lwtEnable = lwtEnable;
        if (cbLwt == null)
            return;
        cbLwt.setChecked(lwtEnable);
    }

    public boolean getLwtRetain() {
        return cbLwtRetain.isChecked();
    }

    public void setLwtRetain(boolean lwtRetain) {
        this.lwtRetain = lwtRetain;
        if (cbLwtRetain == null)
            return;
        cbLwtRetain.setChecked(lwtRetain);
    }

    public void setTopic(String topic) {
        this.topic = topic;
        if (etLwtTopic == null)
            return;
        etLwtTopic.setText(topic);
    }

    public String getTopic() {
        return etLwtTopic.getText().toString();
    }

    public void setPayload(String payload) {
        this.payload = payload;
        if (etLwtPayload == null)
            return;
        etLwtPayload.setText(payload);
    }

    public String getPayload() {
        return etLwtPayload.getText().toString();
    }
}
