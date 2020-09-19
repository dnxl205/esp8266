package com.example.smartsocket;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDevice();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        DeviceAdapter adapter = new DeviceAdapter(deviceList);
        recyclerView.setAdapter(adapter);

        Button Btn_addDevice=(Button)findViewById(R.id.Btn_addDevice);
        Btn_addDevice.setOnClickListener(this);

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
//        try {
//            client.connectBlocking();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    private void initDevice(){
        Device device1 = new Device("卧室插座","在线",true);
        deviceList.add(device1);
        Device device2 = new Device("厨房插座","离线",false);
        deviceList.add(device2);
        Device device3 = new Device("客厅插座","在线",true);
        deviceList.add(device3);
        Device device4 = new Device("卫生间插座","离线",false);
        deviceList.add(device4);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConnect();
    }

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


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.Btn_addDevice://点击添加设备按钮
                //Toast.makeText(MainActivity.this,"添加设备",Toast.LENGTH_SHORT).show();
                //loadScanKitBtnClick();//开始二维码扫描
                //向websocket发送消息
                if (client != null && client.isOpen()) {
                    client.send("你好");
                }
                break;
        }
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


    //Activity回调,返回扫描结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_SCAN_ONE) {
            HmsScan obj = data.getParcelableExtra(ScanUtil.RESULT);
            if (obj != null) {
                Toast.makeText(this,"扫描结果："+obj.originalValue,Toast.LENGTH_SHORT).show();
            }
        }
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
