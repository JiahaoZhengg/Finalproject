/**
 * Function Description: 
   Reports the detected temperature and humidity 
   information and receives the APP data control lamp
 */

// Import library file
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <Ticker.h>
#include <DHT.h>
 
// Configure the wifi information and MQTT proxy server
const char* ssid = "myRobot";
const char* password = "jssz1998!";
const char* mqtt_host = "bemfa.com";
const int mqtt_port = 9501;

// Instantiating an object
Ticker ticker;
WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

// 定义DH11引脚
#define DHTTYPE DHT11
#define DHT11_PIN 2  //D4
DHT dht(DHT11_PIN,DHTTYPE);

int LED = 14; // D5

// Ticker计数用变量
int count;    

// 全局变量温湿度
float tem;
float hum;

// ----------------初始化----------------
void setup() {
  // 设置串口波特率
  Serial.begin(9600);
  
  //设置ESP8266工作模式为无线终端模式
  WiFi.mode(WIFI_STA);
  
  // 连接WiFi
  connectWifi();
  
  // 设置MQTT服务器和端口号
  mqttClient.setServer(mqtt_host, mqtt_port);

  // 设置MQTT订阅回调函数
  mqttClient.setCallback(receiveCallback);
 
  // 连接MQTT服务器
  connectMQTTServer();

  // Ticker定时对象
  ticker.attach(1, tickerCount);  

  // 设置dht11为数据输入模式
  pinMode(DHT11_PIN, INPUT);

  pinMode(LED, OUTPUT);
  digitalWrite(LED,HIGH);
}

// ----------------主循环----------------
void loop() { 
  if (mqttClient.connected()) { // 如果开发板成功连接服务器
    // 每隔3秒钟发布一次信息
    if (count >= 3){
      pubData();
      count = 0;
    }    
    // 保持心跳
    mqttClient.loop();
  } else {                  // 如果开发板未能成功连接服务器
    connectMQTTServer();    // 则尝试连接服务器
  }

}
 
void tickerCount(){
  count++;
}

// 连接MQTT服务器
void connectMQTTServer(){
  // 客户端ID(巴法云的密钥)
  String clientId = "f6cb7dc865c95124ec3d6b30054b8753";
 
  // 连接MQTT服务器
  if (mqttClient.connect(clientId.c_str())) { 
    Serial.println("服务器已连接！");
    Serial.println("客户端ID:");
    Serial.println(clientId);
    // 订阅指定主题
    subscribeTopic(); 
  }
}
 
// 发布数据
void pubData(){
  // 客户端发布信息用数字
  static int value; 
 
  // 构建一个发布topic名称
  String topicString = "smarthomeSub";
  
  char publishTopic[topicString.length() + 1];  
  strcpy(publishTopic, topicString.c_str());

  getTemAndHum();
 
  // 构建发布数据
  String messageString = (String)tem + "#" + (String)hum; 
  char publishData[messageString.length() + 1];   
  strcpy(publishData, messageString.c_str());
  
  // 实现ESP8266向主题发布信息
  if(mqttClient.publish(publishTopic, publishData)){
    Serial.println("Topic:");
    Serial.println(publishTopic);
    Serial.println("send data");
    Serial.println(publishData);    
  } else {
    Serial.println("Failed to send data！"); 
  }
}

// 收到信息后的回调函数
void receiveCallback(char* topic, byte* payload, unsigned int length) {
  Serial.print("接收数据:");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println("");

  if ((char)payload[0] == '0') {
    Serial.println("开灯！");
    digitalWrite(LED,LOW);
  } else {                           
    Serial.println("关灯！");
    digitalWrite(LED,HIGH);
  }
}
 
// 订阅指定主题
void subscribeTopic(){
  
  // 构建订阅主题名称
  String topicString = "smarthomePub";
  char subTopic[topicString.length() + 1];  
  strcpy(subTopic, topicString.c_str());
  
  // 通过串口监视器输出是否成功订阅主题以及订阅的主题名称
  if(mqttClient.subscribe(subTopic)){
    Serial.println("订阅主题:");
    Serial.println(subTopic);
  } else {
    Serial.print("订阅主题失败！");
  }  
}

// 连接wifi
void connectWifi(){
  WiFi.begin(ssid, password);
 
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print("wifi连接中...");
  }
  Serial.println("wifi连接成功！");  
}

// 获取温湿度数据
void getTemAndHum(){ 
  dht.begin();
  tem = dht.readTemperature();
  hum = dht.readHumidity();
}
