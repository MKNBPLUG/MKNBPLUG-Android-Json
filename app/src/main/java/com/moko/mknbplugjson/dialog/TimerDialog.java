package com.moko.mknbplugjson.dialog;

import android.view.View;
import android.widget.TextView;

import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.view.WheelView;

import java.util.ArrayList;

public class TimerDialog extends MokoBaseDialog {
    public static final String TAG = TimerDialog.class.getSimpleName();
    private WheelView wvHour;
    private WheelView wvMinute;
    private boolean on_off;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_timer;
    }

    @Override
    public void bindView(View v) {
        TextView tvSwitchState = v.findViewById(R.id.tv_switch_state);
        wvHour = v.findViewById(R.id.wv_hour);
        wvMinute = v.findViewById(R.id.wv_minute);
        tvSwitchState.setText(on_off ? R.string.countdown_timer_off : R.string.countdown_timer_on);
        initWheelView();
        v.findViewById(R.id.tv_back).setOnClickListener(V1 -> dismiss());
        v.findViewById(R.id.tv_confirm).setOnClickListener(v1 -> {
            if (null != listener) listener.onConfirmClick(this);
        });
    }

    private void initWheelView() {
        ArrayList<String> hour = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            if (i > 1) {
                hour.add(i + " hours");
            } else {
                hour.add(i + " hour");
            }
        }
        wvHour.setData(hour);
        wvHour.setDefault(0);
        ArrayList<String> minute = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            if (i > 1) {
                minute.add(i + " mins");
            } else {
                minute.add(i + " min");
            }
        }
        wvMinute.setData(minute);
        wvMinute.setDefault(0);
    }

    public int getWvHour() {
        return wvHour.getSelected();
    }

    public int getWvMinute() {
        return wvMinute.getSelected();
    }

    private TimerListener listener;

    public void setListener(TimerListener listener) {
        this.listener = listener;
    }

    public interface TimerListener {
        void onConfirmClick(TimerDialog dialog);
    }

    public void setOnoff(boolean on_off) {
        this.on_off = on_off;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public float getDimAmount() {
        return 0.7f;
    }

}
