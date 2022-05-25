package com.moko.support.json;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.support.json.entity.APNSettings;
import com.moko.support.json.entity.ButtonControlEnable;
import com.moko.support.json.entity.ConnectionTimeout;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.EnergyStorageReport;
import com.moko.support.json.entity.FirmwareOTA;
import com.moko.support.json.entity.IndicatorStatus;
import com.moko.support.json.entity.LWTSettings;
import com.moko.support.json.entity.LoadStatusNotify;
import com.moko.support.json.entity.MQTTSettings;
import com.moko.support.json.entity.MsgCommon;
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
import com.moko.support.json.entity.SwitchInfo;
import com.moko.support.json.entity.SystemTime;
import com.moko.support.json.entity.TimeZone;

public class MQTTMessageAssembler {


    public static String assembleConfigClearOverloadStatus(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_CLEAR_OVERLOAD_PROTECTION;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleConfigClearOverVoltageStatus(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_CLEAR_OVER_VOLTAGE_PROTECTION;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleConfigClearOverCurrentStatus(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_CLEAR_OVER_CURRENT_PROTECTION;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleConfigClearUnderVoltageStatus(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_CLEAR_UNDER_VOLTAGE_PROTECTION;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteSwitchInfo(DeviceParams deviceParams, SwitchInfo data) {
        MsgCommon<SwitchInfo> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_SWITCH_STATE;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteTimer(DeviceParams deviceParams, SetCountdown data) {
        MsgCommon<SetCountdown> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_COUNTDOWN;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadDeviceInfo(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_DEVICE_INFO;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadSwitchInfo(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_SWITCH_INFO;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadPowerInfo(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_POWER_INFO;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadEnergyHourly(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_ENERGY_HOURLY;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadEnergyDaily(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_ENERGY_DAILY;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadEnergyTotal(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_ENERGY_TOTAL;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleConfigEnergyClear(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_ENERGY_CLEAR;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadButtonControlEnable(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_BUTTON_CONTROL_ENABLE;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteButtonControlEnable(DeviceParams deviceParams, ButtonControlEnable data) {
        MsgCommon<ButtonControlEnable> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.data = data;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_BUTTON_CONTROL_ENABLE;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }


    public static String assembleWriteReset(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_RESET;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadPowerOnDefault(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_POWER_ON_DEFAULT;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWritePowerOnDefault(DeviceParams deviceParams, PowerOnDefault data) {
        MsgCommon<PowerOnDefault> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_POWER_ON_DEFAULT;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadReportInterval(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_REPORT_INTERVAL;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteReportInterval(DeviceParams deviceParams, ReportInterval data) {
        MsgCommon<ReportInterval> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_REPORT_INTERVAL;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadPowerReportSetting(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_POWER_REPORT_SETTING;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWritePowerReportSetting(DeviceParams deviceParams, PowerReportSetting data) {
        MsgCommon<PowerReportSetting> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_POWER_REPORT_SETTING;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadEnergyReportParams(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_ENERGY_REPORT_PARAMS;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteEnergyReportParams(DeviceParams deviceParams, EnergyStorageReport data) {
        MsgCommon<EnergyStorageReport> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_ENERGY_REPORT_PARAMS;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadConnectionTimeout(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_CONNECTION_TIMEOUT;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteConnectionTimeout(DeviceParams deviceParams, ConnectionTimeout data) {
        MsgCommon<ConnectionTimeout> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_CONNECTION_TIMEOUT;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadNTPParams(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_NTP_PARAMS;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteNTPParams(DeviceParams deviceParams, NTPParams data) {
        MsgCommon<NTPParams> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_NTP_PARAMS;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadTimeZone(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_TIMEZONE;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteTimeZone(DeviceParams deviceParams, TimeZone data) {
        MsgCommon<TimeZone> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_TIMEZONE;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadSystemTime(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_SYSTEM_TIME;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteSystemTime(DeviceParams deviceParams, SystemTime data) {
        MsgCommon<SystemTime> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_SYSTEM_TIME;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadDeviceType(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_DEVICE_TYPE;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadOverloadProtection(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_OVER_LOAD_PROTECTION;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteOverloadProtection(DeviceParams deviceParams, OverloadProtection data) {
        MsgCommon<OverloadProtection> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_OVER_LOAD_PROTECTION;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadOverVoltageProtection(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_OVER_VOLTAGE_PROTECTION;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteOverVoltageProtection(DeviceParams deviceParams, OverloadProtection data) {
        MsgCommon<OverloadProtection> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_OVER_VOLTAGE_PROTECTION;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadUnderVoltageProtection(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_UNDER_VOLTAGE_PROTECTION;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteUnderVoltageProtection(DeviceParams deviceParams, OverloadProtection data) {
        MsgCommon<OverloadProtection> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_UNDER_VOLTAGE_PROTECTION;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadOverCurrentProtection(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_OVER_CURRENT_PROTECTION;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteOverCurrentProtection(DeviceParams deviceParams, OverloadProtection data) {
        MsgCommon<OverloadProtection> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_OVER_CURRENT_PROTECTION;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadLoadStatusNotify(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_LOAD_NOTIFY_ENABLE;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteLoadStatusNotify(DeviceParams deviceParams, LoadStatusNotify data) {
        MsgCommon<LoadStatusNotify> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_LOAD_NOTIFY_ENABLE;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadNetConnectingStatus(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_NET_CONNECTING_STATUS;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteNetConnectingStatus(DeviceParams deviceParams, NetConnectingStatus data) {
        MsgCommon<NetConnectingStatus> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_NET_CONNECTING_STATUS;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadNetConnectedStatus(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_NET_CONNECTED_STATUS;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteNetConnectedStatus(DeviceParams deviceParams, NetConnectedStatus data) {
        MsgCommon<NetConnectedStatus> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_NET_CONNECTED_STATUS;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadPowerStatus(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_POWER_SWITCH_STATUS;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWritePowerStatus(DeviceParams deviceParams, PowerStatus data) {
        MsgCommon<PowerStatus> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_POWER_SWITCH_STATUS;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadPowerProtectStatus(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_POWER_PROTECT;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWritePowerProtectStatus(DeviceParams deviceParams, PowerProtectStatus data) {
        MsgCommon<PowerProtectStatus> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_POWER_PROTECT;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadIndicatorColor(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_INDICATOR_STATUS_COLOR;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteIndicatorColor(DeviceParams deviceParams, IndicatorStatus data) {
        MsgCommon<IndicatorStatus> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_INDICATOR_STATUS_COLOR;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadDeviceStatus(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_DEVICE_STATUS;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteMQTTSettings(DeviceParams deviceParams, MQTTSettings data) {
        MsgCommon<MQTTSettings> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_MQTT_SETTINGS;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteLWTSettings(DeviceParams deviceParams, LWTSettings data) {
        MsgCommon<LWTSettings> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_LWT_SETTINGS;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteAPNSettings(DeviceParams deviceParams, APNSettings data) {
        MsgCommon<APNSettings> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_APN_SETTINGS;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteNetworkSettings(DeviceParams deviceParams, NetworkSettings data) {
        MsgCommon<NetworkSettings> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_NETWORK_PRIORITY;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteMQTTConfigFinish(DeviceParams deviceParams) {
        MsgCommon<NetworkSettings> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_MQTT_CONFIG_FINISH;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteDeviceReconnect(DeviceParams deviceParams) {
        MsgCommon<Object> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_MQTT_RECONNECT;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteOTA(DeviceParams deviceParams, FirmwareOTA data) {
        MsgCommon<FirmwareOTA> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_OTA;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }


    public static String assembleWriteOTAOneWay(DeviceParams deviceParams, OTAOneWayParams data) {
        MsgCommon<OTAOneWayParams> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_OTA_ONE_WAY;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleWriteOTABothWay(DeviceParams deviceParams, OTABothWayParams data) {
        MsgCommon<OTABothWayParams> msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.CONFIG_MSG_ID_OTA_BOTH_WAY;
        msgCommon.data = data;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }


    public static String assembleReadSettingsForDevice(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_DEVICE_SETTINGS;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public static String assembleReadSettingsForLWT(DeviceParams deviceParams) {
        MsgCommon msgCommon = new MsgCommon();
        msgCommon.device_info = deviceParams;
        msgCommon.msg_id = MQTTConstants.READ_MSG_ID_LWT_SETTINGS;
        String message = new Gson().toJson(msgCommon);
        XLog.e("app_to_device--->" + message);
        return message;
    }
}
