package com.example.smart_home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private Button button;
    private ImageView imageView;
    static TextView temData;
    static TextView humData;
    static TextView signal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 开启服务
        Intent intent = new Intent(getApplicationContext(), MQTTService.class);
        startService(intent);

        // 获取页面组件
        button = findViewById(R.id.button);
//        signal = findViewById(R.id.signal);
        imageView = findViewById(R.id.deng);
        temData = findViewById(R.id.tem_data);
        humData = findViewById(R.id.hum_data);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "发布信息 ");
                if ("CLOSE".equals(button.getText())) {
                    MQTTService.publish("1");
                    button.setText("OPEN");
                    imageView.setImageResource(R.drawable.deng_close);
                    Log.i(TAG, "指令--开灯");
                } else if ("OPEN".equals(button.getText())) {
                    MQTTService.publish("0");
                    button.setText("CLOSE");
                    imageView.setImageResource(R.drawable.deng_open);
                    Log.i(TAG, "指令--关灯");
                }
            }
        });

//        timeTasks();
    }

    // MQTT监听并且接受消息
    static MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String str = new String(message.getPayload());
            Log.i(TAG, topic + "接收数据:" + str);

            String[] split = str.split("#");
            String tem = split[0] + "℃";
            String hum = split[1] + "%";

            MainActivity.temData.setText(tem);
            MainActivity.humData.setText(hum);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {

        }

        @Override
        public void connectionLost(Throwable arg0) {
            // 失去连接，重连
        }
    };

    /**
     * 定时请求任务
     */
    private Timer mTimer;

    public void timeTasks() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {

            @Override
            // 子线程访问网络(谷歌不允许主线程访问网络，防止造成阻塞)
            public void run() {

                String respJson = "";
                // 执行GET请求
                try {
                    respJson = get("https://apis.bemfa.com/va/online?" +
                            "uid=f6cb7dc865c95124ec3d6b30054b8753&topic=smarthomeSub&type=1");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i(TAG,  "获取请求设备是否在线:" + respJson);
                // json转为JsonObject对象
                boolean data = false;
                if (respJson != null && respJson.length() > 0) {
                    JSONObject jsonObject = JSONObject.parseObject(respJson);
                    Log.i(TAG,  "获取请求设备是否在线code:" + jsonObject.get("code"));
                    if ((int) jsonObject.get("code") == 0) {
                        data = (boolean) jsonObject.get("data");
                    }
                }

                // 开启UI主线程，（主线程才能修改UI）
                boolean finalData = data;
                runOnUiThread(new Runnable() {
                    public void run() {
                        // 更新TextView组件内容
                        if (finalData){
                            signal.setText("online");
                        }else {
                            signal.setText("offline");
                        }
                    }
                });

            }
            // 立即执行一次，然后每隔2秒执行一次
        }, 0, 2000);

    }

    /**
     * 发送get请求
     *
     * @param path
     * @return
     * @throws Exception
     */
    public static String get(String path) throws Exception {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // 设置连接超时为10秒
        conn.setConnectTimeout(10000);
        // 设置请求类型为Get类型
        conn.setRequestMethod("GET");
        // 判断请求Url是否成功
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("请求url失败");
        }
        InputStream inStream = conn.getInputStream();
        String result = readFromStream(inStream);
        inStream.close();
        return result;
    }

    /**
     * 简单的流转换为字符串
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static String readFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        is.close();
        String result = baos.toString();
        baos.close();
        return result;
    }


}