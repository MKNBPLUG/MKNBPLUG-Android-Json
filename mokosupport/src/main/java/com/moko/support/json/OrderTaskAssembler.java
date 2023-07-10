package com.moko.support.json;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.json.entity.ParamsKeyEnum;
import com.moko.support.json.task.DebugTask;
import com.moko.support.json.task.ParamsTask;
import com.moko.support.json.task.SetPasswordTask;

import java.io.File;

import androidx.annotation.IntRange;

public class OrderTaskAssembler {
    ///////////////////////////////////////////////////////////////////////////
    // READ
    ///////////////////////////////////////////////////////////////////////////

    public static OrderTask getDeviceMac() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_DEVICE_MAC);
        return task;
    }

    public static OrderTask getDeviceName() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_DEVICE_NAME);
        return task;
    }

    public static OrderTask getMqttServer(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_HOST);
        return task;
    }

    public static OrderTask getMqttPort(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_PORT);
        return task;
    }

    public static OrderTask getMqttClientId(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_CLIENT_ID);
        return task;
    }

    public static OrderTask getMqttSubscribe(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_SUBSCRIBE_TOPIC);
        return task;
    }

    public static OrderTask getMqttPublish(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_PUBLISH_TOPIC);
        return task;
    }

    public static OrderTask getMqttCleanSession(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_CLEAN_SESSION);
        return task;
    }

    public static OrderTask getMqttQos(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_QOS);
        return task;
    }

    public static OrderTask getMqttKeepAlive(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_KEEP_ALIVE);
        return task;
    }

    public static OrderTask getMqttUserName(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_USERNAME);
        return task;
    }

    public static OrderTask getMqttPassword(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_PASSWORD);
        return task;
    }

    public static OrderTask getMqttSSlMode(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_CONNECT_MODE);
        return task;
    }

    public static OrderTask getMqttLwtEnable(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_LWT_ENABLE);
        return task;
    }

    public static OrderTask getMqttLwtRetainEnable(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_LWT_RETAIN);
        return task;
    }

    public static OrderTask getMqttLwtQos(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_LWT_QOS);
        return task;
    }

    public static OrderTask getMqttLwtTopic(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_LWT_TOPIC);
        return task;
    }

    public static OrderTask getMqttLwtMsg(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_LWT_PAYLOAD);
        return task;
    }

    public static OrderTask getMqttApn(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_APN);
        return task;
    }

    public static OrderTask getMqttApnUsername(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_APN_USERNAME);
        return task;
    }

    public static OrderTask getMqttApnPassword(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_APN_PASSWORD);
        return task;
    }

    public static OrderTask getMqttNetworkPriority(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_NETWORK_PRIORITY);
        return task;
    }

    public static OrderTask getMqttNtpHost(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_NTP_URL);
        return task;
    }

    public static OrderTask getMqttTimezone(){
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_NTP_TIME_ZONE);
        return task;
    }

//    public static OrderTask getMqttDebugMode(){
//        ParamsTask task = new ParamsTask();
//        task.setData(ParamsKeyEnum.KEY_CHANGE_MODE);
//        return task;
//    }

//    public static OrderTask getProductModel() {
//        ParamsTask task = new ParamsTask();
//        task.setData(ParamsKeyEnum.KEY_PRODUCT_MODEL);
//        return task;
//    }
//
//    public static OrderTask getManufacturer() {
//        ParamsTask task = new ParamsTask();
//        task.setData(ParamsKeyEnum.KEY_MANUFACTURER);
//        return task;
//    }
//
//    public static OrderTask getHardwareVersion() {
//        ParamsTask task = new ParamsTask();
//        task.setData(ParamsKeyEnum.KEY_HARDWARE_VERSION);
//        return task;
//    }
//
//    public static OrderTask getSoftwareVersion() {
//        ParamsTask task = new ParamsTask();
//        task.setData(ParamsKeyEnum.KEY_SOFTWARE_VERSION);
//        return task;
//    }
//
//    public static OrderTask getDeviceType() {
//        ParamsTask task = new ParamsTask();
//        task.setData(ParamsKeyEnum.KEY_DEVICE_TYPE);
//        return task;
//    }
//
//    public static OrderTask getChannelDomain() {
//        ParamsTask task = new ParamsTask();
//        task.setData(ParamsKeyEnum.KEY_CHANNEL_DOMAIN);
//        return task;
//    }


    ///////////////////////////////////////////////////////////////////////////
    // WIRTE
    ///////////////////////////////////////////////////////////////////////////

    public static OrderTask setPassword(String password) {
        SetPasswordTask task = new SetPasswordTask();
        task.setData(password);
        return task;
    }

    public static OrderTask setMqttHost(String host) {
        ParamsTask task = new ParamsTask();
        task.setMqttHost(host);
        return task;
    }

    public static OrderTask setMqttPort(@IntRange(from = 1, to = 65535) int port) {
        ParamsTask task = new ParamsTask();
        task.setMqttPort(port);
        return task;
    }

    public static OrderTask setMqttUserName(String username) {
        ParamsTask task = new ParamsTask();
        task.setMqttUsername(username);
        return task;
    }

    public static OrderTask setMqttPassword(String password) {
        ParamsTask task = new ParamsTask();
        task.setMqttPassword(password);
        return task;
    }

    public static OrderTask setMqttClientId(String clientId) {
        ParamsTask task = new ParamsTask();
        task.setMqttClientId(clientId);
        return task;
    }

    public static OrderTask setMqttCleanSession(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setMqttCleanSession(enable);
        return task;
    }

    public static OrderTask setMqttKeepAlive(@IntRange(from = 10, to = 120) int keepAlive) {
        ParamsTask task = new ParamsTask();
        task.setMqttKeepAlive(keepAlive);
        return task;
    }

    public static OrderTask setMqttQos(@IntRange(from = 0, to = 2) int qos) {
        ParamsTask task = new ParamsTask();
        task.setMqttQos(qos);
        return task;
    }

    public static OrderTask setMqttSubscribeTopic(String topic) {
        ParamsTask task = new ParamsTask();
        task.setMqttSubscribeTopic(topic);
        return task;
    }

    public static OrderTask setMqttPublishTopic(String topic) {
        ParamsTask task = new ParamsTask();
        task.setMqttPublishTopic(topic);
        return task;
    }

    public static OrderTask setLwtEnable(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setLwtEnable(enable);
        return task;
    }

    public static OrderTask setLwtQos(@IntRange(from = 0, to = 2) int qos) {
        ParamsTask task = new ParamsTask();
        task.setLwtQos(qos);
        return task;
    }

    public static OrderTask setLwtRetain(@IntRange(from = 0, to = 1) int retain) {
        ParamsTask task = new ParamsTask();
        task.setLwtRetain(retain);
        return task;
    }

    public static OrderTask setLwtTopic(String topic) {
        ParamsTask task = new ParamsTask();
        task.setLwtTopic(topic);
        return task;
    }

    public static OrderTask setLwtPayload(String payload) {
        ParamsTask task = new ParamsTask();
        task.setLwtPayload(payload);
        return task;
    }

    public static OrderTask setMqttDeivceId(String deviceId) {
        ParamsTask task = new ParamsTask();
        task.setMqttDeviceId(deviceId);
        return task;
    }

    public static OrderTask setMqttConnectMode(@IntRange(from = 0, to = 2) int mode) {
        ParamsTask task = new ParamsTask();
        task.setMqttConnectMode(mode);
        return task;
    }

    public static OrderTask setNTPUrl(String url) {
        ParamsTask task = new ParamsTask();
        task.setNTPUrl(url);
        return task;
    }

    public static OrderTask setNTPTimezone(@IntRange(from = -24, to = 28) int timeZone) {
        ParamsTask task = new ParamsTask();
        task.setNTPTimeZone(timeZone);
        return task;
    }

    public static OrderTask setApn(String apn) {
        ParamsTask task = new ParamsTask();
        task.setApn(apn);
        return task;
    }

    public static OrderTask setApnUsername(String username) {
        ParamsTask task = new ParamsTask();
        task.setApnUsername(username);
        return task;
    }

    public static OrderTask setApnPassword(String password) {
        ParamsTask task = new ParamsTask();
        task.setApnPassword(password);
        return task;
    }

    public static OrderTask setNetworkPriority(@IntRange(from = 0, to = 10) int priority) {
        ParamsTask task = new ParamsTask();
        task.setNetworkPriority(priority);
        return task;
    }

    public static OrderTask setDataFormat(@IntRange(from = 0, to = 1) int dataFormat) {
        ParamsTask task = new ParamsTask();
        task.setDataFormat(dataFormat);
        return task;
    }

    public static OrderTask testMode() {
        ParamsTask task = new ParamsTask();
        task.testMode();
        return task;
    }

    public static OrderTask setMode(@IntRange(from = 0, to = 1) int mode) {
        ParamsTask task = new ParamsTask();
        task.setMode(mode);
        return task;
    }

    public static OrderTask setCA(File file) throws Exception {
        ParamsTask task = new ParamsTask();
        task.setFile(ParamsKeyEnum.KEY_MQTT_CA, file);
        return task;
    }

    public static OrderTask setClientCert(File file) throws Exception {
        ParamsTask task = new ParamsTask();
        task.setFile(ParamsKeyEnum.KEY_MQTT_CLIENT_CERT, file);
        return task;
    }

    public static OrderTask setClientKey(File file) throws Exception {
        ParamsTask task = new ParamsTask();
        task.setFile(ParamsKeyEnum.KEY_MQTT_CLIENT_KEY, file);
        return task;
    }

    public static OrderTask exitDebugMode() {
        DebugTask task = new DebugTask();
        task.exitDebugMode();
        return task;
    }


}
