/*本程序功能：
    连接阿里云实现温湿度上传、手机app对RGB三色灯的控制
*/
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <dht11.h>

#define DHT11PIN        D1
#define LED_IO          D4
#define PWM_IO_R        D2
#define PWM_IO_G        D5
#define PWM_IO_B        D6

/* 连接您的WIFI SSID和密码 */
#define WIFI_SSID         "test"
#define WIFI_PASSWD       "12345678."

/* 阿里云设备证书信息*/
#define PRODUCT_KEY       "a1YSkpQ02ky"
#define DEVICE_NAME       "led_text"
#define DEVICE_SECRET     "hN2hAcbs6z2eSOG95Fgp20elIplFCeIg"
#define REGION_ID         "cn-shanghai"

/* 阿里云MQTT连接配置信息，MQTT连接报文参数,请参见MQTT-TCP连接通信文档，文档地址：https://help.aliyun.com/document_detail/73742.html */
#define MQTT_SERVER       PRODUCT_KEY ".iot-as-mqtt." REGION_ID ".aliyuncs.com"
#define MQTT_PORT         1883
#define MQTT_USRNAME      DEVICE_NAME "&" PRODUCT_KEY
#define CLIENT_ID         "esp8266|securemode=3,signmethod=hmacsha1|"
#define MQTT_PASSWD       "AB856847D39E3301C8208152FD72F5198554BA4F"//此处要使用阿里云加密工具，输入以上证书信息计算得出
//阿里云数据上传格式与目标TOPIC
#define ALINK_BODY_FORMAT         "{\"id\":\"esp8266\",\"version\":\"1.0\",\"method\":\"thing.event.property.post\",\"params\":%s}"
#define ALINK_TOPIC_PROP_POST     "/sys/" PRODUCT_KEY "/" DEVICE_NAME "/thing/event/property/post"


WiFiClient espClient;
dht11 DHT11;
PubSubClient  client(espClient);

int updateTime = 60;//上传数据周期，单位为秒
int t = 0;//临时变量
unsigned long lastMs = 0;
unsigned int pwm_r = 0, pwm_g = 0, pwm_b = 0;
int tem = 0;
int hum = 0;


//对ESP8266进行WIFI初始化并连接
void wifiInit()
{
  //配置wifi并启动连接
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWD);
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(1000);
    Serial.println("WiFi not Connect");
  }
  Serial.println("Connected to AP");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());

  /* 连接WiFi之后，连接MQTT服务器 */
  client.setServer(MQTT_SERVER, MQTT_PORT);
  client.setCallback(callback);
}

//检查MQTT连接状态，发现断线则自动重连
void mqttCheckConnect()
{
  while (!client.connected())
  {
    Serial.println("Connecting to MQTT Server ...");
    if (client.connect(CLIENT_ID, MQTT_USRNAME, MQTT_PASSWD))
    {
      Serial.println("MQTT Connected!");
    } else {
      Serial.print("MQTT Connect err:");
      Serial.println(client.state());
      delay(5000);
    }
  }
}

//MQTT向目标TOPIC上传数据
void mqttIntervalPost()
{
  char param[256];
  char jsonBuf[256];

  //上传的数据在这里编辑，格式如下{"id":"esp8266","version":"1.0","method":"thing.event.property.post","params":"{"led_G":100}"}
  sprintf(param, "{\"Temperature\":%d,\"Humidity\":%d}",tem,hum);
  sprintf(jsonBuf, ALINK_BODY_FORMAT, param);
  Serial.println("上传的json：");
  Serial.println(jsonBuf);
  boolean d = client.publish(ALINK_TOPIC_PROP_POST, jsonBuf);
  Serial.print("publish:0为失败;1为成功===本次为");
  Serial.println(d);
}

//收到云端下发的MQTT消息时的回调函数
void callback(char *topic, byte *payload, unsigned int length)
{
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  payload[length] = '\0';
  Serial.println((char *)payload);

  const char* json = (char *)payload;
  Serial.println("收到的json：");
  Serial.println(json);

  //以下是对下发的json数据进行解析、处理（依赖新版的ArduinoJson库）
  DynamicJsonDocument doc(1024);
  deserializeJson(doc, json);
  JsonObject root = doc.as<JsonObject>();

  if (root["items"].containsKey("led_G")) //containsKey方法为判断json对象是否包含指定字段
  {
    pwm_g = root["items"]["led_G"]["value"];
    analogWrite(PWM_IO_G, pwm_g);
  }
  if (root["items"].containsKey("led_R"))
  {
    pwm_r = root["items"]["led_R"]["value"];
    analogWrite(PWM_IO_R, pwm_r);
  }
  if (root["items"].containsKey("led_B"))
  {
    pwm_b = root["items"]["led_B"]["value"];
    analogWrite(PWM_IO_B, pwm_b);
  }
}


void setup()
{
  //IO口的配置与初始化
  pinMode(LED_IO,  OUTPUT);
  pinMode(PWM_IO_R,  OUTPUT);
  pinMode(PWM_IO_G,  OUTPUT);
  pinMode(PWM_IO_B,  OUTPUT);

  digitalWrite(LED_IO, HIGH);
  analogWriteRange(255);//设置pwm写入范围为0-255，默认是0-1023
  analogWrite(PWM_IO_G, 0);
  analogWrite(PWM_IO_B, 0);
  analogWrite(PWM_IO_R, 0);

  Serial.begin(115200);
  Serial.println("Demo Start");

  wifiInit();
}

void loop()
{
  //millis()是系统启动到目前的总时间
  if (millis() - lastMs >= 1000)
  {
    lastMs = millis();
    t++;
    if (t >= updateTime) {
      t = 0;

      mqttCheckConnect();

      int chk = DHT11.read(DHT11PIN);
      if (chk == DHTLIB_OK || chk == DHTLIB_ERROR_CHECKSUM) {
        Serial.println("DHT11 OK");
        tem = DHT11.temperature;
        hum = DHT11.humidity;
        mqttIntervalPost();
      } else {
        Serial.println("DHT11 error");
      }

    }
  }

  client.loop();
}
