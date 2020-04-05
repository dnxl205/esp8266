/*本程序功能：连接阿里云实现温湿度上传、RGB三色灯的控制*/

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


/* 设备证书信息*/
#define PRODUCT_KEY       "a1YSkpQ02ky"
#define DEVICE_NAME       "led_text"
#define DEVICE_SECRET     "hN2hAcbs6z2eSOG95Fgp20elIplFCeIg"
#define REGION_ID         "cn-shanghai"

/* 线上环境域名和端口号，不需要改 */
#define MQTT_SERVER       PRODUCT_KEY ".iot-as-mqtt." REGION_ID ".aliyuncs.com"
#define MQTT_PORT         1883
#define MQTT_USRNAME      DEVICE_NAME "&" PRODUCT_KEY

#define CLIENT_ID         "esp8266|securemode=3,signmethod=hmacsha1|"
// MQTT连接报文参数,请参见MQTT-TCP连接通信文档，文档地址：https://help.aliyun.com/document_detail/73742.html
// 加密明文是参数和对应的值（clientIdesp8266deviceName${deviceName}productKey${productKey}timestamp1234567890）按字典顺序拼接
// 密钥是设备的DeviceSecret
//要使用加密工具，输入以上证书信息加密
#define MQTT_PASSWD       "AB856847D39E3301C8208152FD72F5198554BA4F"

#define ALINK_BODY_FORMAT         "{\"id\":\"esp8266\",\"version\":\"1.0\",\"method\":\"thing.event.property.post\",\"params\":%s}"
#define ALINK_TOPIC_PROP_POST     "/sys/" PRODUCT_KEY "/" DEVICE_NAME "/thing/event/property/post"


unsigned long lastMs = 0;
WiFiClient espClient;
dht11 DHT11;
PubSubClient  client(espClient);

unsigned int pwm_r=0,pwm_g=0,pwm_b=0;
int tem=0;
int hum=0;
int status=0;
int t=0;

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
    DynamicJsonDocument doc(1024);
    deserializeJson(doc, json);
    JsonObject root = doc.as<JsonObject>();

    //云端下发时自动触发回调函数
    //云端下发的数据只有一个数据点，因此要判断是哪一个数据点下发了数据
    if( root["items"].containsKey("LightSwitch") )  //containsKey方法为判断json对象是否包含指定字段
    {  
       status = root["items"]["LightSwitch"]["value"];
       if (status == 1) 
          digitalWrite(LED_IO, HIGH);
       else if (status == 0) 
          digitalWrite(LED_IO, LOW);
    }
    if(root["items"].containsKey("led_G"))
    {
       pwm_g = root["items"]["led_G"]["value"];
       analogWrite(PWM_IO_G, pwm_g);
       Serial.print("get_pwm_g:");
       Serial.println(pwm_g);
    }
    if(root["items"].containsKey("led_R"))
    {
       pwm_r = root["items"]["led_R"]["value"];
       analogWrite(PWM_IO_R, pwm_r);
       Serial.print("get_pwm_r:");
       Serial.println(pwm_r);
    }
    if(root["items"].containsKey("led_B"))
    {
       pwm_b = root["items"]["led_B"]["value"];
       analogWrite(PWM_IO_B, pwm_b);
       Serial.print("get_pwm_b:");
       Serial.println(pwm_b);
    }     
}

void wifiInit()
{
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
    
    client.setServer(MQTT_SERVER, MQTT_PORT);   /* 连接WiFi之后，连接MQTT服务器 */
    client.setCallback(callback);
}

void mqttCheckConnect()
{
    while (!client.connected())
    {
        Serial.println("Connecting to MQTT Server ...");
        if (client.connect(CLIENT_ID, MQTT_USRNAME, MQTT_PASSWD))
        {
            Serial.println("MQTT Connected!");
        }
        else
        {
            Serial.print("MQTT Connect err:");
            Serial.println(client.state());
            delay(5000);
        }
    }
}

void mqttIntervalPost()
{
    char param[256];
    char jsonBuf[256];

    //上传的数据在这里编辑，格式如下{"id":"esp8266","version":"1.0","method":"thing.event.property.post","params":"{"led_G":100}"}      
    sprintf(param, "{\"LightSwitch\":%d,\"Temperature\":%d,\"Humidity\":%d}",digitalRead(LED_IO),tem,hum);
    sprintf(jsonBuf, ALINK_BODY_FORMAT, param);
    Serial.println("上传的json：");
    Serial.println(jsonBuf);
    boolean d = client.publish(ALINK_TOPIC_PROP_POST, jsonBuf);
    Serial.print("publish:0为失败;1为成功===本次为");
    Serial.println(d);
}

void setup() 
{
    pinMode(LED_IO,  OUTPUT);
    digitalWrite(LED_IO, HIGH);
    
    pinMode(PWM_IO_R,  OUTPUT);
    pinMode(PWM_IO_G,  OUTPUT);
    pinMode(PWM_IO_B,  OUTPUT);
    analogWriteRange(255);//设置pwm写入范围为0-255，默认是0-1023
    analogWrite(PWM_IO_G, 0);
    analogWrite(PWM_IO_B, 0);
    analogWrite(PWM_IO_R, 0);
    /* initialize serial for debugging */
    Serial.begin(115200);
    Serial.println("Demo Start");

    wifiInit();
}

void loop()
{
    //millis()是系统启动到目前的总时间，以下为5s上传一次数据
    if (millis() - lastMs >= 5000)
    {
        lastMs = millis();
        mqttCheckConnect(); 
        
        int chk = DHT11.read(DHT11PIN);
        tem=DHT11.temperature;
        hum=DHT11.humidity;

        if(chk==DHTLIB_OK||chk==DHTLIB_ERROR_CHECKSUM){
          Serial.print("Temperature (oC): ");
          Serial.println(tem);
          Serial.print("Humidity (%): ");
          Serial.println(hum);
          /* 上报消息心跳周期 */
          mqttIntervalPost();
        }else{
          Serial.println("DHT11 error"); 
        }       
    }
    client.loop();
}

void pwm_test(){
  Serial.print("pwm:");
  Serial.println(t);
  analogWrite(PWM_IO_R, t);
  t++;
  if(t>255) t=0;
  delay(50);
}
