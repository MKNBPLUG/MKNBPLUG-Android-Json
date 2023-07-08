package com.moko.mknbplugjson.dialog;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.utils.ToastUtils;

public class PasswordDialog extends MokoBaseDialog {
    public static final String TAG = PasswordDialog.class.getSimpleName();
    private EditText etPassword;
    private TextView tvPasswordEnsure;
    private final String FILTER_ASCII = "[ -~]*";
    private String password;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_password;
    }

    @Override
    public void bindView(View v) {
        etPassword = v.findViewById(R.id.et_password);
        tvPasswordEnsure = v.findViewById(R.id.tv_password_ensure);
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8), filter});
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tvPasswordEnsure.setEnabled(s.toString().length() == 8);
            }
        });
        if (!TextUtils.isEmpty(password)) {
            etPassword.setText(password);
            etPassword.setSelection(password.length());
        }
        etPassword.postDelayed(() -> {
            //设置可获得焦点
            etPassword.setFocusable(true);
            etPassword.setFocusableInTouchMode(true);
            //请求获得焦点
            etPassword.requestFocus();
            //调用系统输入法
            InputMethodManager inputManager = (InputMethodManager) etPassword
                    .getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(etPassword, 0);
        }, 200);
        v.findViewById(R.id.tv_password_cancel).setOnClickListener(v1 -> {
            dismiss();
            if (passwordClickListener != null) {
                passwordClickListener.onDismiss();
            }
        });
        v.findViewById(R.id.tv_password_ensure).setOnClickListener(v1 -> {
            dismiss();
            if (TextUtils.isEmpty(etPassword.getText().toString())) {
                ToastUtils.showToast(getContext(), getContext().getString(R.string.password_null));
                return;
            }
            if (passwordClickListener != null)
                passwordClickListener.onEnsureClicked(etPassword.getText().toString());
        });
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

    private PasswordClickListener passwordClickListener;

    public void setOnPasswordClicked(PasswordClickListener passwordClickListener) {
        this.passwordClickListener = passwordClickListener;
    }

    public interface PasswordClickListener {

        void onEnsureClicked(String password);

        void onDismiss();
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
