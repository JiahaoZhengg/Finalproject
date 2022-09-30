package com.example.smart_home;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;


public class MQTTService extends Service{

    public static final String TAG = MQTTService.class.getSimpleName();

    private static MqttAndroidClient client;
    private MqttConnectOptions conOpt;

    // 代理服务器
    private String host = "tcp://bemfa.com:9501";  //tcp://test.ranye-iot.net:1883
    // 要订阅的主题
    private static String subTopic = "smarthomeSub";
    // 要发布的主题
    private static String pubTopic = "smarthomePub";
    private String clientId = "f6cb7dc865c95124ec3d6b30054b8753";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(getClass().getName(), "onCreate");
        init();

    }

    // 发布消息
    public static void publish(String msg) {
        String topic = pubTopic;
        Integer qos = 0;
        Boolean retained = false;
        try {
            if (client != null) {
                client.publish(topic, msg.getBytes(), qos.intValue(), retained.booleanValue());
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // 初始化
    private void init() {
        Log.i(TAG, "连接服务器...");
        // 服务器地址（协议+地址+端口号）
        String uri = host;
        client = new MqttAndroidClient(this, uri, clientId);
        // 设置MQTT监听并且接受消息
        client.setCallback(MainActivity.mqttCallback);
        conOpt = new MqttConnectOptions();
        // 断线重连
        conOpt.setAutomaticReconnect(true);
        // 清除缓存
        conOpt.setCleanSession(true);
        // 设置超时时间，单位：秒
        conOpt.setConnectionTimeout(10);
        // 心跳包发送间隔，单位：秒
        conOpt.setKeepAliveInterval(20);

        boolean doConnect = true;
        String message = "";
        Log.e(getClass().getName(), "message是:" + message);
        String topic = subTopic;
        Integer qos = 0;
        Boolean retained = false;
        if ((!message.equals("")) || (!topic.equals(""))) {
            // 最后的遗嘱
            // MQTT本身就是为信号不稳定的网络设计的，所以难免一些客户端会无故的和Broker断开连接。
            //当客户端连接到Broker时，可以指定LWT，Broker会定期检测客户端是否有异常。
            //当客户端异常掉线时，Broker就往连接时指定的topic里推送当时指定的LWT消息。
            try {
                conOpt.setWill(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
            } catch (Exception e) {
                Log.e(TAG, "Exception：", e);
                doConnect = false;
                iMqttActionListener.onFailure(null, e);
            }
        }
        if (doConnect) {
            Log.e(TAG, "开始连接...");
            doClientConnection();
        }

    }

    @Override
    public void onDestroy() {
        stopSelf();
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    /**
     * 连接MQTT服务器
     */
    private void doClientConnection() {
        if (!client.isConnected() && isConnectIsNormal()) {
            try {
                client.connect(conOpt, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "连接成功 ");
            try {
                client.subscribe(subTopic, 1);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            arg1.printStackTrace();
            // 连接失败，重连
        }
    };



    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNormal() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "MQTT当前网络名称：" + name);
            return true;
        } else {
            Log.i(TAG, "MQTT 没有可用网络");
            return false;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.e(getClass().getName(), "onBind");
        return new CustomBinder();
    }

    public class CustomBinder extends Binder {
        public MQTTService getService() {
            return MQTTService.this;
        }
    }

}