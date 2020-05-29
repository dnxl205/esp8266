package com.example.rgb_test;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.skydoves.colorpickerview.ActionMode;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private Switch mSwitch;
    ArcProgress arcProgress = null;
    private int pwm_r = 0;
    private int pwm_g = 0;
    private int pwm_b = 0;
    private String ColorData = "FFFFFF";

    private String userName = "my_android&a1YSkpQ02ky";
    private String passWord = "2FA7EF6130B23F6E8EB234A87E2E7DBE2E84A26F";
    private String clientID = "3810|securemode=3,signmethod=hmacsha1|";
    private String mqtt_sub_topic = "/sys/a1YSkpQ02ky/my_android/thing/service/property/set";
    private String mqtt_pub_topic = "/sys/a1YSkpQ02ky/my_android/thing/event/property/post";
    private String host = "tcp://a1YSkpQ02ky.iot-as-mqtt.cn-shanghai.aliyuncs.com:1883";

    private MqttClient mqttClient;
    private MqttConnectOptions options;
    private ScheduledExecutorService scheduler;

    private static final String TAG="MainActivity";

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @SuppressLint("SetTextI18n")
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1: //开机校验更新回传
                    break;
                case 2:  // 反馈回传
                    break;
                case 3:  //MQTT 收到消息回传   UTF8Buffer msg=new UTF8Buffer(object.toString());
                    //Toast.makeText(MainActivity.this,msg.obj.toString() ,Toast.LENGTH_SHORT).show();

                    String json_data = msg.obj.toString();
                    json_data = json_data.substring(json_data.indexOf('{'), json_data.lastIndexOf('}')+1);
                    try {
                        JSONObject jsonObject= new JSONObject(json_data);
                        jsonObject = jsonObject.getJSONObject("items");
                        JSONObject jsonObject1 = jsonObject.getJSONObject("Temperature");
                        JSONObject jsonObject2 = jsonObject.getJSONObject("Humidity");
                        int temperature = jsonObject1.optInt("value");
                        int humidity = jsonObject2.optInt("value");

                        //若开关状态为关闭，则不显示仪表盘
                        boolean humiture_switch_statue = ((Switch)findViewById(R.id.humiture_switch)).isChecked();
                        if (humiture_switch_statue == true){
                            arcProgress = findViewById(R.id.arc_progress_1);
                            arcProgress.setProgress(temperature);
                            arcProgress = findViewById(R.id.arc_progress_2);
                            arcProgress.setProgress(humidity);
                            ((TextView)findViewById(R.id.TH_text)).setText("温度:"+String.valueOf(temperature)+"°   湿度:"+String.valueOf(humidity)+"%");
                        }
                        //Toast.makeText(MainActivity.this, "get温湿度！", Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "解析错误", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 30:  //连接失败
                    Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    break;
                case 31:   //连接成功
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    Mqtt_subscribePlus();//订阅topic
                    break;
                case 40:   //获取设备状态处理
                    if (msg.obj.toString()=="" || msg.obj.toString()=="OFFLINE"){
                        ((Switch)findViewById(R.id.humiture_switch)).setChecked(false);
                        Toast.makeText(MainActivity.this, "设备不在线", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(MainActivity.this, "开启成功", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏标题栏
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        //点击颜色缩略图
        findViewById(R.id.color_thumbnail).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                diolog();
            }
        });
        findViewById(R.id.to_light_page).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                diolog();
            }
        });
        findViewById(R.id.to_humiture_page).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,SecondActivity.class);
                startActivity(intent);
            }
        });
        mSwitch = (Switch) findViewById(R.id.light_switch);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    //关--to---开状态
                    findViewById(R.id.img1_1).setBackgroundResource(R.drawable.light_on);
                    Mqtt_publishPlus("{\"id\":\"esp8266\",\"version\":\"1.0\",\"method\":\"thing.event.property.post\",\"params\":{\"led_R\":"+String.valueOf(pwm_r)+",\"led_G\":"+String.valueOf(pwm_g)+",\"led_B\":"+String.valueOf(pwm_b)+"}}");
                }
                else {
                    //开--to---关状态
                    findViewById(R.id.img1_1).setBackgroundResource(R.drawable.light_off);
                    Mqtt_publishPlus("{\"id\":\"esp8266\",\"version\":\"1.0\",\"method\":\"thing.event.property.post\",\"params\":{\"led_R\":"+"0"+",\"led_G\":"+"0"+",\"led_B\":"+"0"+"}}");
                }
            }
        });

        mSwitch = (Switch) findViewById(R.id.humiture_switch);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    //关--to---开状态
                    findViewById(R.id.img2_1).setBackgroundResource(R.drawable.humiture_on);
                    // Android 4.0 之后不能在主线程中请求HTTP请求
                    new Thread(new Runnable(){
                        @Override
                        public void run() {
                            Message msg = Message.obtain();
                            msg.obj = getDeviceStatus();
                            msg.what = 40;
                            handler.sendMessage(msg);
                        }
                    }).start();
                }
                else {
                    //开--to---关状态
                    findViewById(R.id.img2_1).setBackgroundResource(R.drawable.humiture_off);
                    arcProgress = findViewById(R.id.arc_progress_1);
                    arcProgress.setProgress(0);
                    arcProgress = findViewById(R.id.arc_progress_2);
                    arcProgress.setProgress(0);
                    ((TextView)findViewById(R.id.TH_text)).setText("温度:   湿度:");
                }
            }
        });

        //设置颜色缩略图填充颜色、边框宽度和颜色
        GradientDrawable drawable = (GradientDrawable) findViewById(R.id.color_thumbnail).getBackground();
        drawable.setStroke(2,Color.parseColor("#000000"));
        ColorData = loadColorData();
        if (ColorData != ""){
            drawable.setColor(Color.parseColor("#"+ColorData));
            pwm_r = Integer.parseInt(ColorData.substring(0,2),16);
            pwm_g = Integer.parseInt(ColorData.substring(2,4),16);
            pwm_b = Integer.parseInt(ColorData.substring(4,6),16);
        }


        //mqtt相关代码
        mqtt_Init();
        mqtt_startReconnect();
    }

    private void mqtt_Init() {
        try {
            //host为主机名，clientID:MQTT的客户端ID(唯一标识符)，MemoryPersistence设置clientID的保存形式，默认为以内存保存
            mqttClient = new MqttClient(host, clientID,
                    new MemoryPersistence());
            //MQTT的连接设置
            options = new MqttConnectOptions();
            //设置是否清空session会话,这里如果设置为false表示服务器会保留客户端的连接记录
            options.setCleanSession(false);
            options.setUserName(userName);//设置连接的用户名
            options.setPassword(passWord.toCharArray());//设置连接的密码
            options.setConnectionTimeout(3);// 设置超时时间 单位为秒
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(60);
            //设置回调
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    //连接丢失后，一般在这里面进行重连
                    System.out.println("connectionLost----------");
                    //startReconnect();
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //publish后会执行到这里
                    System.out.println("deliveryComplete---------"
                            + token.isComplete());
                }
                @Override
                public void messageArrived(String topicName, MqttMessage message)
                        throws Exception {
                    //subscribe后得到的消息会执行到这里面
                    System.out.println("messageArrived----------");
                    Message msg = new Message();
                    msg.what = 3;   //收到消息标志位
                    msg.obj = topicName + "---" + message.toString();
                    handler.sendMessage(msg);    // hander 回传
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void Connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!(mqttClient.isConnected()) )  //如果还未连接
                    {
                        mqttClient.connect(options);
                        Message msg = new Message();
                        msg.what = 31;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.what = 30;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }

    private void mqtt_startReconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!mqttClient.isConnected()) {
                    Connect();
                }
            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    private void Mqtt_publishPlus(String message2) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(message2.getBytes());
        try {
            mqttClient.publish(mqtt_pub_topic,message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void Mqtt_subscribePlus() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            return;
        }
        try {
            mqttClient.subscribe(mqtt_sub_topic,1);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private String getDeviceStatus() {
        DefaultProfile profile = DefaultProfile.getProfile("cn-shanghai", "<key账户>", "<密钥>");
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain("iot.cn-shanghai.aliyuncs.com");
        request.setVersion("2018-01-20");
        request.setAction("getDeviceStatus");
        request.putQueryParameter("RegionId", "cn-shanghai");
        request.putQueryParameter("ProductKey", "a1YSkpQ02ky");
        request.putQueryParameter("DeviceName", "led_text");
        try {
            CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());
            Log.w("MainActivity",response.getData());

            String status = response.getData();
            try {
                JSONObject jsonObject= new JSONObject(status);
                jsonObject = jsonObject.getJSONObject("Data");
                status = jsonObject.optString("Status");
                Log.w("MainActivity",status);
                return status;
            } catch (JSONException e) {
                e.printStackTrace();
                return "";
            }
        } catch (ServerException e) {
            e.printStackTrace();
            return "";
        } catch (ClientException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void diolog(){
        ColorPickerDialog.Builder builder = new ColorPickerDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle("ColorPicker Dialog");
        builder.setPreferenceName("MyColorPickerDialog");
        builder.setPositiveButton(getString(R.string.confirm),
                new ColorEnvelopeListener() {
                    @Override
                    public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                        GradientDrawable drawable = (GradientDrawable) findViewById(R.id.color_thumbnail).getBackground();
                        drawable.setColor(envelope.getColor());
                        pwm_r = envelope.getArgb()[1];
                        pwm_g = envelope.getArgb()[2];
                        pwm_b = envelope.getArgb()[3];
                        ((Switch)findViewById(R.id.light_switch)).setChecked(true);
                        saveColorData(envelope.getHexCode().substring(2));//"ffaabb"
                        //Toast.makeText(MainActivity.this,envelope.getHexCode().substring(2),Toast.LENGTH_SHORT).show();
                        Mqtt_publishPlus("{\"id\":\"esp8266\",\"version\":\"1.0\",\"method\":\"thing.event.property.post\",\"params\":{\"led_R\":"+String.valueOf(pwm_r)+",\"led_G\":"+String.valueOf(pwm_g)+",\"led_B\":"+String.valueOf(pwm_b)+"}}");
                    }
                });
        builder.attachAlphaSlideBar(false);
        builder.attachBrightnessSlideBar(true);
        builder.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        ColorPickerView colorPickerView = builder.getColorPickerView();
        colorPickerView.setPaletteDrawable(getDrawable(R.drawable.palette));
        colorPickerView.setFlagView(new CustomFlag(this, R.layout.flag_view));
        colorPickerView.setActionMode(ActionMode.LAST);
        colorPickerView.setColorListener(new ColorEnvelopeListener() {
            @Override
            public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                pwm_r = envelope.getArgb()[1];
                pwm_g = envelope.getArgb()[2];
                pwm_b = envelope.getArgb()[3];
                Mqtt_publishPlus("{\"id\":\"esp8266\",\"version\":\"1.0\",\"method\":\"thing.event.property.post\",\"params\":{\"led_R\":"+String.valueOf(pwm_r)+",\"led_G\":"+String.valueOf(pwm_g)+",\"led_B\":"+String.valueOf(pwm_b)+"}}");
            }
        });
        builder.show();


    }

    private void saveColorData(String ColorData){
        FileOutputStream out = null;
        BufferedWriter writer = null;

        try {
            out = openFileOutput("ColorData", Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(ColorData);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private String loadColorData(){
        FileInputStream in = null;
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        try {
            in = openFileInput("ColorData");
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return content.toString();
    }

}
