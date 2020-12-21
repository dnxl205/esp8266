package com.example.smartsocket;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
<<<<<<< HEAD
=======
import android.content.Context;
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
<<<<<<< HEAD
=======
import android.os.Build;
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
<<<<<<< HEAD
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
=======
import android.widget.EditText;
import android.widget.TextView;
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import org.json.JSONException;
import org.json.JSONObject;

<<<<<<< HEAD
import java.io.IOException;
=======
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    //通讯编号号
    public static final int webSocket_Server = 1;
    public static final int CAMERA_REQ_CODE = 111;
    public static final int DECODE = 1;
    private static final int REQUEST_CODE_SCAN_ONE = 0X11;

    //webSocket变量
    URI uri;
    JWebSocketClient client;

    //设备列表及适配器
    private List<Device> deviceList = new ArrayList<>();
<<<<<<< HEAD
    DeviceAdapter deviceAdapter;

    //本地数据库变量
    private DeviceSQL deviceSQL;
    private SQLiteDatabase deviceDatabase;
    private SQLiteDatabase delayDatabase;
    private DelaySQL delaySQL;
    String g_imei;

    //UI控件
    RecyclerView recyclerView;
    RelativeLayout relativeLayout_noDevice;
    Button Btn_addDevice;
=======
    RecyclerView recyclerView;
    DeviceAdapter deviceAdapter;

    private DeviceSQL deviceSQL;
    private SQLiteDatabase deviceDatabase;
    String g_imei;
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //使状态栏透明
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_main);

<<<<<<< HEAD
        //获取UI控件
        relativeLayout_noDevice = findViewById(R.id.no_device);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        Btn_addDevice=(Button)findViewById(R.id.Btn_addDevice);

        //创建设备数据库（若已存在，获取对象）
        deviceSQL = new DeviceSQL(this,"Device.db",null,1);
        deviceDatabase = deviceSQL.getWritableDatabase();
        delaySQL = new DelaySQL(this,"Delay.db",null,1);
        delayDatabase = delaySQL.getWritableDatabase();

        //查询设备数据库,加载滚动列表
        Cursor cursor = deviceDatabase.query("Device", null, null, null, null, null, null);
        if (cursor.getCount()>0){
            //取消无设备的提示，显示设备列表
            relativeLayout_noDevice.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            //开始恢复数据
            if (cursor.moveToFirst()) {
                do {
                    // 遍历Cursor对象， 取出数据并打印
                    String m_name = cursor.getString(cursor.getColumnIndex("name"));
                    String m_imei = cursor.getString(cursor.getColumnIndex("imei"));
                    int m_switch = cursor.getInt(cursor.getColumnIndex("switch"));

                    Device device = new Device(m_imei,m_name,"离线",m_switch==1);
                    deviceList.add(device);

                    //add_RecyclerView(m_imei,m_name,"离线",m_switch==1);
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        //配置滚动列表适配器
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        deviceAdapter = new DeviceAdapter(MainActivity.this,deviceList);
        recyclerView.setAdapter(deviceAdapter);

        //注册deviceView长按事件
        deviceAdapter.setOnItemLongClickListener(new DeviceAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, final int position) {
                //弹出删除警告框
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("删除");
                builder.setMessage("确定要删除此设备吗？");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //删除在数据库中的数据
                        String t_imei = deviceList.get(position).getImei();
                        deviceDatabase.delete("Device","imei=?", new String []{t_imei});
                        delayDatabase.delete("Delay","imei=?", new String []{t_imei});
                        //删除在列表中的数据
                        deviceAdapter.removeData(position);

                        //查看剩余设备个数
                        if(deviceAdapter.getItemCount()==0){
                            //显示无设备的提示，显示设备列表
                            relativeLayout_noDevice.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }

                        Toast.makeText(MainActivity.this,"删除成功",Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {}
                });
                builder.show();
            }
        });


        //注册设备switch开关监听器
        deviceAdapter.setOnItemClickListener(new DeviceAdapter.OnItemClickListener(){
            @Override
            public void onClick(CompoundButton compoundButton, String deviceName, boolean deviceStatus, int position){
                //排除非人为按下的事件
                if(!compoundButton.isPressed())
                    return;

                //以下为手动按下switch控件事件：更新数据到数据库，并向webSocket发送数据
                deviceList.get(position).setDeviceStatus_switch(deviceStatus);
                ContentValues values = new ContentValues();
                values.put("switch", deviceStatus?1:0);
                deviceDatabase.update("Device",values,"name=?",new String []{deviceName});
                values.clear();

                //向websocket发送消息
                String imei = deviceList.get(position).getImei();
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("imei",imei);
                    jsonObject.put("switch",deviceStatus);
                }catch (JSONException e){
                    e.printStackTrace();
                }
                Log.d("MainActivity","is Pressed!!!!");
                //send_to_webSocket(jsonObject.toString());
            }
        });

        //注册“添加设备”按钮的单击监听器
=======
        //创建数据库（若已存在，获取对象）
        deviceSQL = new DeviceSQL(this,"Device.db",null,1);
        deviceDatabase = deviceSQL.getWritableDatabase();

        //配置滚动列表适配器
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        deviceAdapter = new DeviceAdapter(deviceList);
        recyclerView.setAdapter(deviceAdapter);

        //设备switch开关事件
        deviceAdapter.setOnItemClickListener(new DeviceAdapter.OnItemClickListener(){
            @Override
            public void onClick(String deviceName, boolean deviceStatus, int position){
                deviceList.get(position).setDeviceStatus_switch(deviceStatus);

                //向websocket发送消息
                String imei = deviceList.get(position).getImei();
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("imei",imei);
                    jsonObject.put("switch",deviceStatus);
                }catch (JSONException e){
                    e.printStackTrace();
                }

                if (client != null && client.isOpen()) {
                    client.send(jsonObject.toString());
                }
            }
        });

        //查询数据库,更新滚动列表
        Cursor cursor = deviceDatabase.query("Device", null, null, null, null, null, null);
        if (cursor.getCount()>0)
            if (cursor.moveToFirst()) {
                do {
                    // 遍历Cursor对象， 取出数据并打印
                    String name = cursor.getString(cursor.getColumnIndex("name"));
                    String m_imei = cursor.getString(cursor.getColumnIndex("imei"));
                    add_RecyclerView(m_imei,name,"离线",false);
                } while (cursor.moveToNext());
            }
        cursor.close();

        //注册按键的点击事件
        Button Btn_addDevice=(Button)findViewById(R.id.Btn_addDevice);
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
        Btn_addDevice.setOnClickListener(this);
        Button Btn_queryDevice=(Button)findViewById(R.id.Btn_queryDevice);
        Btn_queryDevice.setOnClickListener(this);

        //若存在设备，更新设备在线情况和插座开关状态
        if(deviceList.size()>0){
            //更新设备在线情况（直接调用云端API）
            for(int i=0;i<deviceList.size();i++){
                String t_url = "http://42.192.99.208:8081/getStatus?IMEI="+deviceList.get(i).getImei();
                //异步GET,并设置回调函数
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().get().url(t_url).build();
                Call call = client.newCall(request);
                final int finalI = i;
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d("MainActivity","onFailure!!!!");
                    }
                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        Log.d("MainActivity","onResponse!!!!");
                        final String res = response.body().string();
                        Log.d("MainActivity",res);

                        try {
                            JSONObject object =new JSONObject(res);
                            final String t_imei = object.getString("IMEI");
                            final int t_status = object.getInt("status");
                            Log.d("MainActivity","IMEI:"+ t_imei +"status:"+t_status);

                            //更新主界面UI
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    deviceList.get(finalI).setDeviceStatus_text(t_status==0?"离线":"在线");
                                    deviceAdapter.notifyItemChanged(finalI);
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            //更新插座开关状态(获取设备影子)
//            for(int j=0;j<deviceList.size();j++){
//
//            }


        }

        //webSocket连接
       //webSocket_connet();
    }

    //连接webSocket函数
    public void webSocket_connet()
    {
        //websocket连接设置
        uri = URI.create("ws://192.168.0.109:8001");
        client = new JWebSocketClient(uri) {
            @Override
            public void onMessage(String res) {
                //message就是接收到的消息
                Log.e("JWebSocketClient", res);

                //发送消息到主线程
                Message message = new Message();
                message.what = webSocket_Server;
                message.obj = res;
                handler.sendMessage(message);
            }
        };
        //启动websocket连接（当服务器不在线时，app启动时间会大大加长）
        try {
            client.connectBlocking();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
<<<<<<< HEAD
    }

    //OKHTTP的异步GET请求
    public void okhttp_get(String url){
        url = "http://42.192.99.208:8081/getShadow?IMEI=led_text";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
        Call call = client.newCall(request);
        //异步调用,并设置回调函数
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("MainActivity","onFailure!!!!");
            }
            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                Log.d("MainActivity","onResponse!!!!");
                final String res = response.body().string();
                Log.d("MainActivity",res);
            }
        });

    }

    //OKHTTP的POST请求
    public void okhttp_post(String url){
        url = "http://42.192.99.208:8081/updateShadow?IMEI=led_text";
        OkHttpClient client = new OkHttpClient();
        FormBody formBody = new FormBody.Builder().build();//可以.add(键值对)添加参数
        final Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("MainActivity","onFailure!!!!");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String res = response.body().string();
                Log.d("MainActivity",res);
            }
        });
    }

    //向websocket发送消息
    public void send_to_webSocket(String msg){
        if (client != null && client.isOpen()) {
            client.send(msg);
        }
    }

=======
    }

    //活动销毁事件
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConnect();
    }

<<<<<<< HEAD
    //handle处理函数
=======
    //收到服务器消息
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        public void  handleMessage(Message msg){
            switch (msg.what){
                case webSocket_Server://收到web服务器下发命令
                    //webSocket_Server下发数据格式：deviceName:switch:0/1,例如：893284284243:switch:0
                    String data = (String) msg.obj;
                    String m_imei=data.substring(0, data.indexOf(":"));
                    String m_switch = data.substring(data.length()-1);
                    deviceList.get(0).setDeviceStatus_switch(m_switch.equals("1"));
                    deviceAdapter.notifyItemChanged(0);
                    break;
                default:
                    break;
            }
        }
    };

<<<<<<< HEAD
    //点击事件回调
=======

    //添加设备按钮点击服务函数
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.Btn_addDevice://点击添加设备按钮
<<<<<<< HEAD
                loadScanKitBtnClick();//开始二维码扫描
=======
                //开始二维码扫描
                loadScanKitBtnClick();
                break;

            case R.id.Btn_queryDevice://点击查看数据库按钮
//                if(deviceList.get(0).getDeviceStatus_switch())
//                    deviceList.get(0).setDeviceStatus_text("在线");
//                else
//                    deviceList.get(0).setDeviceStatus_text("离线");
//                deviceAdapter.notifyItemChanged(0);

                //查询数据库
//                Cursor cursor = deviceDatabase.query("Device", null, null, null, null, null, null);
//                Log.d("MainActivity", "count is " + cursor.getCount());
//                if (cursor.moveToFirst()) {
//                    do {
//                        // 遍历Cursor对象， 取出数据并打印
//                        String imei = cursor.getString(cursor.getColumnIndex("imei"));
//                        String name = cursor.getString(cursor.getColumnIndex("name"));
//                        Log.d("MainActivity", "imei is " + imei);
//                        Log.d("MainActivity", "name is " + name);
//                    } while (cursor.moveToNext());
//                }
//                cursor.close();
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
                break;
        }
    }

<<<<<<< HEAD
    //修改设备名称弹窗
=======
    //修改设备名称
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
    public void alert_edit(){
        final EditText et = new EditText(this);
        new AlertDialog.Builder(this).setTitle("请输入设备名称")
                .setIcon(android.R.drawable.sym_def_app_icon)
                .setView(et)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = et.getText().toString();
<<<<<<< HEAD

                        if(TextUtils.isEmpty(name)){
                            Toast.makeText(MainActivity.this,"添加失败：设备名称不能为空",Toast.LENGTH_LONG).show();
                            return;
                        }

=======
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
                        //添加到滚动列表
                        add_RecyclerView(g_imei,name,"离线",false);

                        //添加数据到数据库
                        ContentValues values = new ContentValues();
                        values.put("imei",g_imei);
                        values.put("name",name);
                        values.put("online",0);
                        values.put("switch",0);
                        values.put("delayFlag",0);
<<<<<<< HEAD
                        deviceDatabase.insert("Device",null,values);
                        values.clear();

                        //取消无设备的提示
                        if(deviceList.size()>0){
                            relativeLayout_noDevice.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
=======
                        values.put("delayTime",0);
                        deviceDatabase.insert("Device",null,values);
                        values.clear();
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
<<<<<<< HEAD
                    public void onClick(DialogInterface dialogInterface, int i) {}
                }).show();
    }

=======
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //添加到滚动列表
                        add_RecyclerView(g_imei,"智能插座","离线",false);

                        //添加数据到数据库
                        ContentValues values = new ContentValues();
                        values.put("imei",g_imei);
                        values.put("name","智能插座");
                        values.put("online",0);
                        values.put("switch",0);
                        values.put("delayFlag",0);
                        values.put("delayTime",0);
                        deviceDatabase.insert("Device",null,values);
                        values.clear();
                    }
                }).show();
    }


>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
    //动态权限申请
    public void loadScanKitBtnClick() {
        requestPermission(CAMERA_REQ_CODE, DECODE);
    }

    //编辑请求权限
    private void requestPermission(int requestCode, int mode) {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                requestCode);
    }

    //权限申请返回
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (permissions == null || grantResults == null) {
            return;
        }

        if (grantResults.length < 2 || grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (requestCode == CAMERA_REQ_CODE) {
            //启动扫描Acticity
            ScanUtil.startScan(this, REQUEST_CODE_SCAN_ONE, new HmsScanAnalyzerOptions.Creator().create());
        }
    }

<<<<<<< HEAD
    //Activity回调,包括扫描结果回调和DeviceActivity回调
=======

    //二维码扫描结果
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_SCAN_ONE) {
            HmsScan obj = data.getParcelableExtra(ScanUtil.RESULT);
            if (obj != null) {
<<<<<<< HEAD
                //Toast.makeText(this,"扫描结果："+obj.originalValue,Toast.LENGTH_SHORT).show();
                g_imei = obj.originalValue;

                //重复扫描检测
                Cursor cursor = deviceDatabase.query("Device", null, "imei=?",new String []{g_imei}, null, null, null);
                if (cursor.getCount()>0){
                    Toast.makeText(MainActivity.this,"二维码不能重复扫描",Toast.LENGTH_LONG).show();
                    return;
                }

=======
                g_imei = obj.originalValue;

>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
                //弹出填写设备名称界面
                alert_edit();
            }
        }
        if(requestCode == 1){
            Boolean returnedData = data.getBooleanExtra("m_switch",false);
            deviceList.get(0).setDeviceStatus_switch(returnedData);
            deviceAdapter.notifyItemChanged(0);
        }
    }

    //在前端列表中添加设备
    public void add_RecyclerView(String m_imei,String name,String online,boolean m_switch){
        Device device = new Device(m_imei,name,online,m_switch);
        deviceAdapter.addItem(0,device);
        recyclerView.scrollToPosition(0);
    }

<<<<<<< HEAD
     //断开websocket连接
=======
    //在前端列表中添加设备
    public void add_RecyclerView(String m_imei,String name,String online,boolean m_switch){
        Device device = new Device(m_imei,name,online,m_switch);
        deviceAdapter.addItem(0,device);
        recyclerView.scrollToPosition(0);
    }

    /**
     * 断开websocket连接
     */
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
    private void closeConnect() {
        try {
            if (null != client) {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client = null;
        }
    }

}
