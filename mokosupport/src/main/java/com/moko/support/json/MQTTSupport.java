package com.moko.support.json;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.event.MQTTConnectionCompleteEvent;
import com.moko.support.json.event.MQTTConnectionFailureEvent;
import com.moko.support.json.event.MQTTConnectionLostEvent;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class MQTTSupport {
    private static final String TAG = "MQTTSupport";

    private static volatile MQTTSupport INSTANCE;

    private Context mContext;


    private MqttAndroidClient mqttAndroidClient;
    private IMqttActionListener listener;

    private MQTTSupport() {
        //no instance
    }

    public static MQTTSupport getInstance() {
        if (INSTANCE == null) {
            synchronized (MQTTSupport.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MQTTSupport();
                }
            }
        }
        return INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        listener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                XLog.w(String.format("%s:%s", TAG, "connect success"));
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                XLog.w(String.format("%s:%s", TAG, "connect failure"));
                EventBus.getDefault().post(new MQTTConnectionFailureEvent());
            }
        };
    }

    public void connectMqtt(String mqttAppConfigStr) throws FileNotFoundException {
        if (TextUtils.isEmpty(mqttAppConfigStr))
            return;
        MQTTConfig mqttConfig = new Gson().fromJson(mqttAppConfigStr, MQTTConfig.class);
        if (!mqttConfig.isError()) {
            String uri;
            if (mqttConfig.connectMode > 0) {
                uri = "ssl://" + mqttConfig.host + ":" + mqttConfig.port;
            } else {
                uri = "tcp://" + mqttConfig.host + ":" + mqttConfig.port;
            }
            mqttAndroidClient = new MqttAndroidClient(mContext, uri, mqttConfig.clientId);
            mqttAndroidClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (reconnect) {
                        XLog.w("Reconnected to : " + serverURI);
                    } else {
                        XLog.w("Connected to : " + serverURI);
                    }
                    EventBus.getDefault().post(new MQTTConnectionCompleteEvent());
                }

                @Override
                public void connectionLost(Throwable cause) {
                    EventBus.getDefault().post(new MQTTConnectionLostEvent());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setAutomaticReconnect(true);
            connOpts.setCleanSession(mqttConfig.cleanSession);
            connOpts.setKeepAliveInterval(mqttConfig.keepAlive);
            if (!TextUtils.isEmpty(mqttConfig.username)) {
                connOpts.setUserName(mqttConfig.username);
            }
            if (!TextUtils.isEmpty(mqttConfig.password)) {
                connOpts.setPassword(mqttConfig.password.toCharArray());
            }
            if (mqttConfig.connectMode > 0) {

                switch (mqttConfig.connectMode) {
//                    case 1:
//                        // ???????????????
//                        try {
//                            connOpts.setSocketFactory(getAllTMSocketFactory());
//                            connOpts.setSSLHostnameVerifier(new HostnameVerifier() {
//                                @Override
//                                public boolean verify(String hostname, SSLSession session) {
//                                    return true;
//                                }
//                            });
//                        } catch (Exception e) {
//                            // ??????stacktrace??????
//                            final Writer result = new StringWriter();
//                            final PrintWriter printWriter = new PrintWriter(result);
//                            e.printStackTrace(printWriter);
//                            StringBuffer errorReport = new StringBuffer();
//                            errorReport.append(result.toString());
//                            XLog.e(errorReport.toString());
//                        }
//                        break;
                    case 1:
                        // ????????????
                        try {
                            connOpts.setSocketFactory(getSingleSocketFactory(mqttConfig.caPath));
                        } catch (Exception e) {
                            // ??????stacktrace??????
                            final Writer result = new StringWriter();
                            final PrintWriter printWriter = new PrintWriter(result);
                            e.printStackTrace(printWriter);
                            StringBuffer errorReport = new StringBuffer();
                            errorReport.append(result.toString());
                            XLog.e(errorReport.toString());
                        }
                        break;
                    case 2:
                        // ????????????
                        try {
                            connOpts.setSocketFactory(getSocketFactory(mqttConfig.caPath, mqttConfig.clientKeyPath, mqttConfig.clientCertPath));
                            connOpts.setHttpsHostnameVerificationEnabled(false);
                        } catch (FileNotFoundException fileNotFoundException) {
                            throw fileNotFoundException;
                        } catch (Exception e) {
                            // ??????stacktrace??????
                            final Writer result = new StringWriter();
                            final PrintWriter printWriter = new PrintWriter(result);
                            e.printStackTrace(printWriter);
                            StringBuffer errorReport = new StringBuffer();
                            errorReport.append(result.toString());
                            XLog.e(errorReport.toString());
                        }
                        break;
                }
            }
            try {
                connectMqtt(connOpts);
            } catch (MqttException e) {
                // ??????stacktrace??????
                final Writer result = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(result);
                e.printStackTrace(printWriter);
                StringBuffer errorReport = new StringBuffer();
                errorReport.append(result.toString());
                XLog.e(errorReport.toString());
            }
            return;
        }
    }

    static class AllTM implements TrustManager, X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(X509Certificate[] certs) {
            XLog.d("isServerTrusted");
            for (java.security.cert.X509Certificate certificate : certs) {
                XLog.w("Accepting:" + certificate);
            }
            return true;
        }

        public boolean isClientTrusted(X509Certificate[] certs) {
            XLog.d("isClientTrusted");
            for (java.security.cert.X509Certificate certificate : certs) {
                XLog.w("Accepting:" + certificate);
            }
            return true;
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            XLog.d("Server authtype=" + authType);
            for (java.security.cert.X509Certificate certificate : certs) {
                XLog.w("Accepting:" + certificate);
            }
            return;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            XLog.d("Client authtype=" + authType);
            for (java.security.cert.X509Certificate certificate : certs) {
                XLog.w("Accepting:" + certificate);
            }
            return;
        }
    }


    /**
     * ???????????????
     *
     * @Date 2019/8/5
     * @Author wenzheng.liu
     * @Description
     */
//    private SocketFactory getAllTMSocketFactory() {
//        TrustManager[] trustAllCerts = new TrustManager[1];
//        TrustManager tm = new AllTM();
//        trustAllCerts[0] = tm;
//        SSLContext sc = null;
//        try {
//            sc = SSLContext.getInstance("SSL");
//            sc.init(null, trustAllCerts, null);
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (KeyManagementException e) {
//            e.printStackTrace();
//        }
//        return sc.getSocketFactory();
//    }

    /**
     * ????????????
     *
     * @return
     * @throws Exception
     */

    private SSLSocketFactory getSingleSocketFactory(String caFile) throws Exception {
        // ??????????????????
        Security.addProvider(new BouncyCastleProvider());

        X509Certificate caCert = null;

        FileInputStream fis = new FileInputStream(caFile);

        BufferedInputStream bis = new BufferedInputStream(fis);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        while (bis.available() > 0) {

            caCert = (X509Certificate) cf.generateCertificate(bis);

        }

        KeyStore caKs =
                KeyStore.getInstance(KeyStore.getDefaultType());

        caKs.load(null, null);

        caKs.setCertificateEntry("ca-certificate", caCert);

        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance("X509");

        tmf.init(caKs);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");

        sslContext.init(null, tmf.getTrustManagers(), null);

        return sslContext.getSocketFactory();

    }

    /**
     * ????????????
     *
     * @return
     * @throws Exception
     */
    private SSLSocketFactory getSocketFactory(String caFile, String clientKeyFile, String clientCertFile) throws Exception {

        FileInputStream ca = new FileInputStream(caFile);
        FileInputStream clientCert = new FileInputStream(clientCertFile);
        FileInputStream clientKey = new FileInputStream(clientKeyFile);
        Security.addProvider(new BouncyCastleProvider());
        // load CA certificate
        X509Certificate caCert = null;

        BufferedInputStream bis = new BufferedInputStream(ca);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        while (bis.available() > 0) {
            caCert = (X509Certificate) cf.generateCertificate(bis);
        }

        // load client certificate
        bis = new BufferedInputStream(clientCert);
        X509Certificate cert = null;
        while (bis.available() > 0) {
            cert = (X509Certificate) cf.generateCertificate(bis);
        }

        // load client private key
        PEMParser pemParser = new PEMParser(new InputStreamReader(clientKey));
        Object object = pemParser.readObject();
        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
                .build("".toCharArray());
        JcaPEMKeyConverter converter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            //??????Android P??????????????????????????????
            converter = new JcaPEMKeyConverter();
        } else {
            converter = new JcaPEMKeyConverter().setProvider("BC");
        }

        PrivateKey privateKey;
        if (object instanceof PEMEncryptedKeyPair) {
            XLog.e("Encrypted key - we will use provided password");
            KeyPair keyPair = converter.getKeyPair(((PEMEncryptedKeyPair) object)
                    .decryptKeyPair(decProv));
            privateKey = keyPair.getPrivate();
        } else if (object instanceof PrivateKeyInfo) {
            XLog.e("PrivateKeyInfo");
            privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
        } else {
            XLog.e("Unencrypted key - no password needed");
            KeyPair keyPair = converter.getKeyPair((PEMKeyPair) object);
            privateKey = keyPair.getPrivate();
        }
        pemParser.close();

        // CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(caKs);

        // client key and certificates are sent to server so it can authenticate
        // us
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("certificate", cert);
        ks.setKeyEntry("private-key", privateKey, "".toCharArray(),
                new java.security.cert.Certificate[]{cert});
        KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(KeyManagerFactory
                        .getDefaultAlgorithm());
        kmf.init(ks, "".toCharArray());

        // finally, create SSL socket factory
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }

    private void connectMqtt(MqttConnectOptions options) throws MqttException {
        if (mqttAndroidClient != null && !mqttAndroidClient.isConnected()) {
            mqttAndroidClient.connect(options, null, listener);
        }
    }

    public void disconnectMqtt() {
        if (!isConnected())
            return;
        mqttAndroidClient.close();
        try {
            mqttAndroidClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mqttAndroidClient = null;
    }

    public void subscribe(String topic, int qos) throws MqttException {
        if (!isConnected())
            return;
        mqttAndroidClient.subscribe(topic, qos, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                XLog.w(String.format("%s:%s->%s", TAG, topic, "subscribe success"));
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                XLog.w(String.format("%s:%s->%s", TAG, topic, "subscribe failure"));
            }
        });
        mqttAndroidClient.subscribe(topic, qos, new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String messageInfo = new String(message.getPayload());
                Log.w("MKNBPLUGJSON", String.format("Message:%s:%s", topic, messageInfo));
                EventBus.getDefault().post(new MQTTMessageArrivedEvent(topic, new String(message.getPayload())));
            }
        });

    }

    public void unSubscribe(String topic) throws MqttException {
        if (!isConnected())
            return;
        mqttAndroidClient.unsubscribe(topic, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                XLog.w(String.format("%s:%s->%s", TAG, topic, "unsubscribe success"));
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                XLog.w(String.format("%s:%s->%s", TAG, topic, "unsubscribe failure"));
            }
        });

    }

    public void publish(String topic, String message, int msgId, int qos) throws MqttException {
        if (!isConnected())
            return;
        MqttMessage messageInfo = new MqttMessage();
        messageInfo.setPayload(message.getBytes());
        messageInfo.setQos(qos);
        messageInfo.setRetained(false);
        mqttAndroidClient.publish(topic, messageInfo, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                XLog.w(String.format("%s:%s->%s", TAG, topic, "publish success"));
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                XLog.w(String.format("%s:%s->%s", TAG, topic, "publish failure"));
            }
        });
    }

    public boolean isConnected() {
        if (mqttAndroidClient != null) {
            return mqttAndroidClient.isConnected();
        }
        return false;
    }
}
