package com.moko.mknbplugjson.dialog;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.view.ProgressDrawable;

public class LoadingMessageDialog extends MokoBaseDialog {
    private static final int DIALOG_DISMISS_DELAY_TIME = 190000;
    public static final String TAG = LoadingMessageDialog.class.getSimpleName();
    private ImageView ivLoading;
    private String message;
    private int messageId = -1;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_loading_message;
    }

    @Override
    public void bindView(View v) {
        ivLoading = v.findViewById(R.id.iv_loading);
        TextView tvLoadingMessage = v.findViewById(R.id.tv_loading_message);
        ProgressDrawable progressDrawable = new ProgressDrawable();
        progressDrawable.setColor(ContextCompat.getColor(getContext(), R.color.black_333333));
        ivLoading.setImageDrawable(progressDrawable);
        progressDrawable.start();
        if (messageId > 0) {
            message = getString(messageId);
        }
        if (TextUtils.isEmpty(message)) {
            message = getString(R.string.setting_syncing);
        }
        tvLoadingMessage.setText(message);
        tvLoadingMessage.postDelayed(() -> {
            if (isVisible()) {
                dismissAllowingStateLoss();
                if (callback != null) {
                    callback.onOvertimeDismiss();
                }
            }
        }, DIALOG_DISMISS_DELAY_TIME);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (callback != null) {
            callback.onDismiss();
        }
    }

    @Override
    public int getDialogStyle() {
        return R.style.CenterDialog;
    }

    @Override
    public int getGravity() {
        return Gravity.CENTER;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public float getDimAmount() {
        return 0.7f;
    }

    @Override
    public boolean getCancelOutside() {
        return false;
    }

    @Override
    public boolean getCancellable() {
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((ProgressDrawable) ivLoading.getDrawable()).stop();
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setMessage(@StringRes int messageId) {
        this.messageId = messageId;
    }

    private DialogDismissCallback callback;

    public void setDialogDismissCallback(final DialogDismissCallback callback) {
        this.callback = callback;
    }

    public interface DialogDismissCallback {
        void onOvertimeDismiss();

        void onDismiss();
    }
}
