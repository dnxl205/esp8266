package com.example.smartsocket;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int webSocket_Server = 1;

    public static final int CAMERA_REQ_CODE = 111;
    public static final int DECODE = 1;
    private static final int REQUEST_CODE_SCAN_ONE = 0X01;

    URI uri;
    JWebSocketClient client;

    private List<Device> deviceList = new ArrayList<>();
    RecyclerView recyclerView;
    DeviceAdapter deviceAdapter;

    private DeviceSQL deviceSQL;
    private SQLiteDatabase deviceDatabase;
    String g_imei;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        Btn_addDevice.setOnClickListener(this);
        Button Btn_queryDevice=(Button)findViewById(R.id.Btn_queryDevice);
        Btn_queryDevice.setOnClickListener(this);

        //websocket连接设置
        uri = URI.create("ws://192.168.0.105:8001");
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
    }

    //活动销毁事件
    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConnect();
    }

    //收到服务器消息
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        public void  handleMessage(Message msg){
            switch (msg.what){
                case webSocket_Server:
                    Toast.makeText(MainActivity.this,"收到消息:"+(String) msg.obj,Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };


    //添加设备按钮点击服务函数
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.Btn_addDevice://点击添加设备按钮
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
                break;
        }
    }

    //修改设备名称
    public void alert_edit(){
        final EditText et = new EditText(this);
        new AlertDialog.Builder(this).setTitle("请输入设备名称")
                .setIcon(android.R.drawable.sym_def_app_icon)
                .setView(et)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = et.getText().toString();
                        //添加到滚动列表
                        add_RecyclerView(g_imei,name,"离线",false);

                        //添加数据到数据库
                        ContentValues values = new ContentValues();
                        values.put("imei",g_imei);
                        values.put("name",name);
                        values.put("online",0);
                        values.put("switch",0);
                        values.put("delayFlag",0);
                        values.put("delayTime",0);
                        deviceDatabase.insert("Device",null,values);
                        values.clear();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
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


    //二维码扫描结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_SCAN_ONE) {
            HmsScan obj = data.getParcelableExtra(ScanUtil.RESULT);
            if (obj != null) {
                g_imei = obj.originalValue;

                //弹出填写设备名称界面
                alert_edit();
            }
        }
    }

    //在前端列表中添加设备
    public void add_RecyclerView(String m_imei,String name,String online,boolean m_switch){
        Device device = new Device(m_imei,name,online,m_switch);
        deviceAdapter.addItem(0,device);
        recyclerView.scrollToPosition(0);
    }

    /**
     * 断开websocket连接
     */
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
