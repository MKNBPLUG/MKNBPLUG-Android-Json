package com.moko.support.json;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.support.json.entity.APNSettings;
import com.moko.support.json.entity.ButtonControlEnable;
import com.moko.support.json.entity.ConnectionTimeout;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.DeviceTimeZone;
import com.moko.support.json.entity.EnergyStorageReport;
import com.moko.support.json.entity.FirmwareOTA;
import com.moko.support.json.entity.IndicatorStatus;
import com.moko.support.json.entity.LWTSettings;
import com.moko.support.json.entity.LoadStatusNotify;
import com.moko.support.json.entity.MQTTSettings;
import com.moko.support.json.entity.MsgReq;
import com.moko.support.json.entity.NTPParams;
import com.moko.support.json.entity.NetConnectedStatus;
import com.moko.support.json.entity.NetConnectingStatus;
import com.moko.support.json.entity.NetworkSettings;
import com.moko.support.json.entity.OTABothWayParams;
import com.moko.support.json.entity.OTAOneWayParams;
import com.moko.support.json.entity.OverloadProtection;
import com.moko.support.json.entity.PowerOnDefault;
import com.moko.support.json.entity.PowerProtectStatus;
import com.moko.support.json.entity.PowerReportSetting;
import com.moko.support.json.entity.PowerStatus;
import com.moko.support.json.entity.ReportInterval;
import com.moko.support.json.entity.SetCountdown;
import com.moko.support.json.entity.SwitchState;
import com.moko.support.json.entity.SystemTime;

public class MQTTMessageAssembler {


    public static String assembleConfigClearOverloadStatus(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_CLEAR_OVERLOAD_PROTECTION;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleConfigClearOverVoltageStatus(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_CLEAR_OVER_VOLTAGE_PROTECTION;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleConfigClearOverCurrentStatus(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_CLEAR_OVER_CURRENT_PROTECTION;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleConfigClearUnderVoltageStatus(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_CLEAR_UNDER_VOLTAGE_PROTECTION;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteSwitchInfo(DeviceParams deviceParams, SwitchState data) {
        MsgReq<SwitchState> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_SWITCH_STATE;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteTimer(DeviceParams deviceParams, SetCountdown data) {
        MsgReq<SetCountdown> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_COUNTDOWN;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadDeviceInfo(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_DEVICE_INFO;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadSwitchInfo(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_SWITCH_INFO;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadPowerInfo(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_POWER_INFO;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadEnergyHourly(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_ENERGY_HOURLY;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadEnergyDaily(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_ENERGY_DAILY;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadEnergyTotal(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_ENERGY_TOTAL;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleConfigEnergyClear(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_ENERGY_CLEAR;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadButtonControlEnable(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_BUTTON_CONTROL_ENABLE;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteButtonControlEnable(DeviceParams deviceParams, ButtonControlEnable data) {
        MsgReq<ButtonControlEnable> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.data = data;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_BUTTON_CONTROL_ENABLE;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadDeviceType(DeviceParams deviceParams){
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_DEVICE_TYPE;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteReset(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_RESET;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadPowerOnDefault(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_POWER_ON_DEFAULT;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWritePowerOnDefault(DeviceParams deviceParams, PowerOnDefault data) {
        MsgReq<PowerOnDefault> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_POWER_ON_DEFAULT;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadReportInterval(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_REPORT_INTERVAL;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteReportInterval(DeviceParams deviceParams, ReportInterval data) {
        MsgReq<ReportInterval> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_REPORT_INTERVAL;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadPowerReportSetting(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_POWER_REPORT_SETTING;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWritePowerReportSetting(DeviceParams deviceParams, PowerReportSetting data) {
        MsgReq<PowerReportSetting> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_POWER_REPORT_SETTING;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadEnergyReportParams(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_ENERGY_REPORT_PARAMS;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteEnergyReportParams(DeviceParams deviceParams, EnergyStorageReport data) {
        MsgReq<EnergyStorageReport> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_ENERGY_REPORT_PARAMS;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadConnectionTimeout(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_CONNECTION_TIMEOUT;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteConnectionTimeout(DeviceParams deviceParams, ConnectionTimeout data) {
        MsgReq<ConnectionTimeout> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_CONNECTION_TIMEOUT;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadNTPParams(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_NTP_PARAMS;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteNTPParams(DeviceParams deviceParams, NTPParams data) {
        MsgReq<NTPParams> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_NTP_PARAMS;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadTimeZone(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_TIMEZONE;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteTimeZone(DeviceParams deviceParams, DeviceTimeZone data) {
        MsgReq<DeviceTimeZone> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_TIMEZONE;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadSystemTime(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_SYSTEM_TIME;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteSystemTime(DeviceParams deviceParams, SystemTime data) {
        MsgReq<SystemTime> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_SYSTEM_TIME;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadDeviceStandard(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_DEVICE_STANDARD;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadOverloadProtection(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_OVER_LOAD_PROTECTION;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteOverloadProtection(DeviceParams deviceParams, OverloadProtection data) {
        MsgReq<OverloadProtection> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_OVER_LOAD_PROTECTION;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadOverVoltageProtection(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_OVER_VOLTAGE_PROTECTION;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteOverVoltageProtection(DeviceParams deviceParams, OverloadProtection data) {
        MsgReq<OverloadProtection> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_OVER_VOLTAGE_PROTECTION;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadUnderVoltageProtection(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_UNDER_VOLTAGE_PROTECTION;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteUnderVoltageProtection(DeviceParams deviceParams, OverloadProtection data) {
        MsgReq<OverloadProtection> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_UNDER_VOLTAGE_PROTECTION;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadOverCurrentProtection(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_OVER_CURRENT_PROTECTION;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteOverCurrentProtection(DeviceParams deviceParams, OverloadProtection data) {
        MsgReq<OverloadProtection> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_OVER_CURRENT_PROTECTION;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadLoadStatusNotify(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_LOAD_NOTIFY_ENABLE;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteLoadStatusNotify(DeviceParams deviceParams, LoadStatusNotify data) {
        MsgReq<LoadStatusNotify> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_LOAD_NOTIFY_ENABLE;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadNetConnectingStatus(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_NET_CONNECTING_STATUS;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteNetConnectingStatus(DeviceParams deviceParams, NetConnectingStatus data) {
        MsgReq<NetConnectingStatus> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_NET_CONNECTING_STATUS;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadNetConnectedStatus(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_NET_CONNECTED_STATUS;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteNetConnectedStatus(DeviceParams deviceParams, NetConnectedStatus data) {
        MsgReq<NetConnectedStatus> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_NET_CONNECTED_STATUS;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadPowerStatus(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_POWER_SWITCH_STATUS;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWritePowerStatus(DeviceParams deviceParams, PowerStatus data) {
        MsgReq<PowerStatus> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_POWER_SWITCH_STATUS;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadPowerProtectStatus(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_POWER_PROTECT;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWritePowerProtectStatus(DeviceParams deviceParams, PowerProtectStatus data) {
        MsgReq<PowerProtectStatus> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_POWER_PROTECT;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadIndicatorColor(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_INDICATOR_STATUS_COLOR;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteIndicatorColor(DeviceParams deviceParams, IndicatorStatus data) {
        MsgReq<IndicatorStatus> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_INDICATOR_STATUS_COLOR;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadDeviceStatus(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_DEVICE_STATUS;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteMQTTSettings(DeviceParams deviceParams, MQTTSettings data) {
        MsgReq<MQTTSettings> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_MQTT_SETTINGS;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteLWTSettings(DeviceParams deviceParams, LWTSettings data) {
        MsgReq<LWTSettings> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_LWT_SETTINGS;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteAPNSettings(DeviceParams deviceParams, APNSettings data) {
        MsgReq<APNSettings> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_APN_SETTINGS;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteNetworkSettings(DeviceParams deviceParams, NetworkSettings data) {
        MsgReq<NetworkSettings> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_NETWORK_PRIORITY;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteMQTTConfigFinish(DeviceParams deviceParams) {
        MsgReq<NetworkSettings> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_MQTT_CONFIG_FINISH;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteDeviceReconnect(DeviceParams deviceParams) {
        MsgReq<Object> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_MQTT_RECONNECT;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteOTA(DeviceParams deviceParams, FirmwareOTA data) {
        MsgReq<FirmwareOTA> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_OTA;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }


    public static String assembleWriteOTAOneWay(DeviceParams deviceParams, OTAOneWayParams data) {
        MsgReq<OTAOneWayParams> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_OTA_ONE_WAY;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteOTABothWay(DeviceParams deviceParams, OTABothWayParams data) {
        MsgReq<OTABothWayParams> msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.CONFIG_MSG_ID_OTA_BOTH_WAY;
        msgReq.data = data;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }


    public static String assembleReadSettingsForDevice(DeviceParams deviceParams) {
        MsgReq msgReq = new MsgReq();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_DEVICE_SETTINGS;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadSettingsForLWT(DeviceParams deviceParams) {
        MsgReq<LWTSettings> msgReq = new MsgReq<>();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_LWT_SETTINGS;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadSettingsForApn(DeviceParams deviceParams) {
        MsgReq<APNSettings> msgReq = new MsgReq<>();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_APN_SETTINGS;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadNetworkPriority(DeviceParams deviceParams) {
        MsgReq<NetworkSettings> msgReq = new MsgReq<>();
        msgReq.device_info = deviceParams;
        msgReq.msg_id = MQTTConstants.READ_MSG_ID_NETWORK_PRIORITY;
        String message = new Gson().toJson(msgReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }
}
