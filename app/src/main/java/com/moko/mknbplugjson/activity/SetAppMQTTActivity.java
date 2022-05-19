package com.moko.mknbplugjson.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.R2;
import com.moko.mknbplugjson.adapter.MQTTFragmentAdapter;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.dialog.AlertMessageDialog;
import com.moko.mknbplugjson.entity.MQTTConfig;
import com.moko.mknbplugjson.fragment.GeneralFragment;
import com.moko.mknbplugjson.fragment.SSLFragment;
import com.moko.mknbplugjson.fragment.UserFragment;
import com.moko.mknbplugjson.utils.FileUtils;
import com.moko.mknbplugjson.utils.SPUtiles;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.mknbplugjson.utils.Utils;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.event.MQTTConnectionCompleteEvent;
import com.moko.support.json.event.MQTTConnectionFailureEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import butterknife.BindView;
import butterknife.ButterKnife;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class SetAppMQTTActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {
    private final String FILTER_ASCII = "[ -~]*";
    @BindView(R2.id.et_mqtt_host)
    EditText etMqttHost;
    @BindView(R2.id.et_mqtt_port)
    EditText etMqttPort;
    @BindView(R2.id.et_mqtt_client_id)
    EditText etMqttClientId;
    @BindView(R2.id.et_mqtt_subscribe_topic)
    EditText etMqttSubscribeTopic;
    @BindView(R2.id.et_mqtt_publish_topic)
    EditText etMqttPublishTopic;
    @BindView(R2.id.rb_general)
    RadioButton rbGeneral;
    @BindView(R2.id.rb_user)
    RadioButton rbUser;
    @BindView(R2.id.rb_ssl)
    RadioButton rbSsl;
    @BindView(R2.id.vp_mqtt)
    ViewPager2 vpMqtt;
    @BindView(R2.id.rg_mqtt)
    RadioGroup rgMqtt;
    private GeneralFragment generalFragment;
    private UserFragment userFragment;
    private SSLFragment sslFragment;
    private MQTTFragmentAdapter adapter;
    private ArrayList<Fragment> fragments;

    private MQTTConfig mqttConfig;

    private String importFilePath;
    private String expertFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt_app);
        ButterKnife.bind(this);
        String MQTTConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        if (TextUtils.isEmpty(MQTTConfigStr)) {
            mqttConfig = new MQTTConfig();
        } else {
            Gson gson = new Gson();
            mqttConfig = gson.fromJson(MQTTConfigStr, MQTTConfig.class);
        }
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        etMqttHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        etMqttClientId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        etMqttSubscribeTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        etMqttPublishTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        createFragment();
        initData();
        adapter = new MQTTFragmentAdapter(this);
        adapter.setFragmentList(fragments);
        vpMqtt.setAdapter(adapter);
        vpMqtt.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    rbGeneral.setChecked(true);
                } else if (position == 1) {
                    rbUser.setChecked(true);
                } else if (position == 2) {
                    rbSsl.setChecked(true);
                }
            }
        });
        vpMqtt.setOffscreenPageLimit(3);
        rgMqtt.setOnCheckedChangeListener(this);
    }


    private void createFragment() {
        fragments = new ArrayList<>();
        generalFragment = GeneralFragment.newInstance();
        userFragment = UserFragment.newInstance();
        sslFragment = SSLFragment.newInstance();
        fragments.add(generalFragment);
        fragments.add(userFragment);
        fragments.add(sslFragment);
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 10)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        String mqttConfigStr = new Gson().toJson(mqttConfig, MQTTConfig.class);
        SPUtiles.setStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, mqttConfigStr);
        ToastUtils.showToast(SetAppMQTTActivity.this, getString(R.string.success));
        dismissLoadingProgressDialog();
        Intent intent = new Intent();
        intent.putExtra(AppConstants.EXTRA_KEY_MQTT_CONFIG_APP, mqttConfigStr);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionFailureEvent(MQTTConnectionFailureEvent event) {
        ToastUtils.showToast(SetAppMQTTActivity.this, getString(R.string.mqtt_connect_failed));
        dismissLoadingProgressDialog();
        finish();
    }

    private void initData() {
        etMqttHost.setText(mqttConfig.host);
        etMqttPort.setText(mqttConfig.port);
        etMqttClientId.setText(mqttConfig.clientId);
        etMqttSubscribeTopic.setText(mqttConfig.topicSubscribe);
        etMqttPublishTopic.setText(mqttConfig.topicPublish);
        generalFragment.setCleanSession(mqttConfig.cleanSession);
        generalFragment.setQos(mqttConfig.qos);
        generalFragment.setKeepAlive(mqttConfig.keepAlive);
        userFragment.setUserName(mqttConfig.username);
        userFragment.setPassword(mqttConfig.password);
        sslFragment.setConnectMode(mqttConfig.connectMode);
        sslFragment.setCAPath(mqttConfig.caPath);
        sslFragment.setClientKeyPath(mqttConfig.clientKeyPath);
        sslFragment.setClientCertPath(mqttConfig.clientCertPath);
    }

    public void back(View view) {
        back();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setMessage("Please confirm whether to save the modified parameters?");
        dialog.setConfirm("YES");
        dialog.setCancel("NO");
        dialog.setOnAlertConfirmListener(() -> {
            onSave(null);
        });
        dialog.setOnAlertCancelListener(() -> {
            finish();
        });
        dialog.show(getSupportFragmentManager());
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        if (checkedId == R.id.rb_general)
            vpMqtt.setCurrentItem(0);
        else if (checkedId == R.id.rb_user)
            vpMqtt.setCurrentItem(1);
        else if (checkedId == R.id.rb_ssl)
            vpMqtt.setCurrentItem(2);
    }

    public void onSave(View view) {
        if (isWindowLocked())
            return;
        if (isVerify()) return;
        String mqttConfigStr = new Gson().toJson(mqttConfig, MQTTConfig.class);
        MQTTSupport.getInstance().disconnectMqtt();
        showLoadingProgressDialog();
        etMqttHost.postDelayed(() -> {
            try {
                MQTTSupport.getInstance().connectMqtt(mqttConfigStr);
            } catch (FileNotFoundException e) {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "The SSL certificates path is invalid, please select a valid file path and save it.");
                // 读取stacktrace信息
                final Writer result = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(result);
                e.printStackTrace(printWriter);
                StringBuffer errorReport = new StringBuffer();
                errorReport.append(result.toString());
                XLog.e(errorReport.toString());
            }
        }, 2000);
    }

    private boolean isVerify() {
        String host = etMqttHost.getText().toString().replaceAll(" ", "");
        String port = etMqttPort.getText().toString();
        String clientId = etMqttClientId.getText().toString().replaceAll(" ", "");
        String subscribeTopic = etMqttSubscribeTopic.getText().toString().replaceAll(" ", "");
        String publishTopic = etMqttPublishTopic.getText().toString().replaceAll(" ", "");

        if (TextUtils.isEmpty(host)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_host));
            return true;
        }
        if (TextUtils.isEmpty(port)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port_empty));
            return true;
        }
        if (Integer.parseInt(port) < 1 || Integer.parseInt(port) > 65535) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port));
            return true;
        }
        if (TextUtils.isEmpty(clientId)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_client_id_empty));
            return true;
        }
        if (!generalFragment.isValid() || !sslFragment.isValid())
            return true;
        mqttConfig.host = host;
        mqttConfig.port = port;
        mqttConfig.clientId = clientId;
        mqttConfig.cleanSession = generalFragment.isCleanSession();
        mqttConfig.qos = generalFragment.getQos();
        mqttConfig.keepAlive = generalFragment.getKeepAlive();
        mqttConfig.keepAlive = generalFragment.getKeepAlive();
        mqttConfig.topicSubscribe = subscribeTopic;
        mqttConfig.topicPublish = publishTopic;
        mqttConfig.username = userFragment.getUsername();
        mqttConfig.password = userFragment.getPassword();
        mqttConfig.connectMode = sslFragment.getConnectMode();
        mqttConfig.caPath = sslFragment.getCaPath();
        mqttConfig.clientKeyPath = sslFragment.getClientKeyPath();
        mqttConfig.clientCertPath = sslFragment.getClientCertPath();

        if (!mqttConfig.topicPublish.isEmpty() && !mqttConfig.topicSubscribe.isEmpty()
                && mqttConfig.topicPublish.equals(mqttConfig.topicSubscribe)) {
            ToastUtils.showToast(this, "Subscribed and published topic can't be same !");
            return true;
        }
        return false;
    }

    public void selectCertificate(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCertificate();
    }

    public void selectCAFile(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCAFile();
    }

    public void selectKeyFile(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectKeyFile();
    }

    public void selectCertFile(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCertFile();
    }

    public void onExportSettings(View view) {
        if (isWindowLocked())
            return;
        if (isVerify()) return;
        final File expertFile = new File(expertFilePath);
        final File importFile = new File(importFilePath);
        try {
            Workbook workbook = Workbook.getWorkbook(importFile);
            WritableWorkbook wwb = Workbook.createWorkbook(expertFile, workbook);

            WritableSheet ws = wwb.getSheet(0);
            ws.addCell(new Label(1, 1, String.format("value:%s", mqttConfig.host)));
            ws.addCell(new Label(1, 2, String.format("value:%s", mqttConfig.port)));
            ws.addCell(new Label(1, 3, String.format("value:%s", mqttConfig.clientId)));
            if (!TextUtils.isEmpty(mqttConfig.topicSubscribe))
                ws.addCell(new Label(1, 4, String.format("value:%s", mqttConfig.topicSubscribe)));
            if (!TextUtils.isEmpty(mqttConfig.topicPublish))
                ws.addCell(new Label(1, 5, String.format("value:%s", mqttConfig.topicPublish)));
            ws.addCell(new Label(1, 6, String.format("value:%s", mqttConfig.cleanSession ? "1" : "0")));
            ws.addCell(new Label(1, 7, String.format("value:%d", mqttConfig.qos)));
            ws.addCell(new Label(1, 8, String.format("value:%d", mqttConfig.keepAlive)));
            if (!TextUtils.isEmpty(mqttConfig.username))
                ws.addCell(new Label(1, 9, String.format("value:%s", mqttConfig.username)));
            if (!TextUtils.isEmpty(mqttConfig.password))
                ws.addCell(new Label(1, 10, String.format("value:%s", mqttConfig.password)));
            if (mqttConfig.connectMode > 0) {
                ws.addCell(new Label(1, 11, String.format("value:%d", 1)));
                ws.addCell(new Label(1, 12, String.format("value:%d", mqttConfig.connectMode)));
            } else {
                ws.addCell(new Label(1, 11, String.format("value:%d", mqttConfig.connectMode)));
                ws.addCell(new Label(1, 12, String.format("value:%d", 1)));
            }

            // 从内存中写入文件中
            workbook.close();
            wwb.write();
            wwb.close();
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                Uri uri = IOUtils.insertDownloadFile(this, expertFile);
//                if ("content".equalsIgnoreCase(uri.getScheme())) {
//                    String filePath = FileUtils.getDataColumn(this, uri, null, null);
//                    if (!TextUtils.isEmpty(filePath)) {
//                        ToastUtils.showToast(this, "导出成功！");
//                        return;
//                    }
//                }
//            }
//            ToastUtils.showToast(this, "导出成功！");
            Utils.sendEmail(this, "", "", "Settings for APP", "Choose Email Client", expertFile);
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showToast(this, "export error！");
        }
    }

    public void onImportSettings(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), AppConstants.REQUEST_CODE_OPEN_APP_SETTINGS_FILE);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(this, "install file manager app");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_OPEN_APP_SETTINGS_FILE) {
            if (resultCode == RESULT_OK) {
                //得到uri，后面就是将uri转化成file的过程。
                Uri uri = data.getData();
                String paramFilePath = FileUtils.getPath(this, uri);
                if (TextUtils.isEmpty(paramFilePath)) {
                    return;
                }
                if (!paramFilePath.endsWith(".xls") && !paramFilePath.endsWith(".xlsx")) {
                    ToastUtils.showToast(this, "Please select the correct file!");
                    return;
                }
                final File paramFile = new File(paramFilePath);
                if (paramFile.exists()) {
                    importFilePath = paramFilePath;
//                    String name = paramFilePath.substring(0, paramFilePath.lastIndexOf("."));
//                    String suffix = paramFilePath.substring(paramFilePath.lastIndexOf("."));
                    expertFilePath = paramFile.getParent() + File.separator + "export" + File.separator + "settings_for_app.xlsx";
                    try {
                        Workbook workbook = Workbook.getWorkbook(paramFile);
                        Sheet sheet = workbook.getSheet(0);
                        int rows = sheet.getRows();
                        int columns = sheet.getColumns();
                        // 从第二行开始
                        if (rows != 13 && columns != 3) {
                            ToastUtils.showToast(this, "Please select the correct file!");
                            return;
                        }
                        mqttConfig.host = sheet.getCell(1, 1).getContents().replaceAll("value:", "");
                        mqttConfig.port = sheet.getCell(2, 1).getContents().replaceAll("value:", "");
                        mqttConfig.clientId = sheet.getCell(3, 1).getContents().replaceAll("value:", "");
                        String topicSubscribe = sheet.getCell(4, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(topicSubscribe)) {
                            mqttConfig.topicSubscribe = topicSubscribe;
                        }
                        String topicPublish = sheet.getCell(5, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(topicPublish)) {
                            mqttConfig.topicPublish =topicPublish;
                        }
                        mqttConfig.cleanSession = "1".equals(sheet.getCell(6, 1).getContents().replaceAll("value:", ""));
                        mqttConfig.qos = Integer.parseInt(sheet.getCell(7, 1).getContents().replaceAll("value:", ""));
                        mqttConfig.keepAlive = Integer.parseInt(sheet.getCell(8, 1).getContents().replaceAll("value:", ""));
                        String username = sheet.getCell(9, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(username)) {
                            mqttConfig.username = username;
                        }
                        String password = sheet.getCell(10, 1).getContents().replaceAll("value:", "");
                        if (!TextUtils.isEmpty(password)) {
                            mqttConfig.password = password;
                        }
                        // 0/1
                        mqttConfig.connectMode = Integer.parseInt(sheet.getCell(11, 1).getContents().replaceAll("value:", ""));
                        if (mqttConfig.connectMode > 0) {
                            // 1/2
                            mqttConfig.connectMode = Integer.parseInt(sheet.getCell(12, 1).getContents().replaceAll("value:", ""));
                        }
                        initData();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ToastUtils.showToast(this, "Import success!");
                } else {
                    Toast.makeText(this, "file is not exists!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
