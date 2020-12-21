package com.example.smartsocket;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static android.view.KeyEvent.KEYCODE_BACK;

public class DeviceActivity extends AppCompatActivity {
    RelativeLayout relativeLayout_back;
    LinearLayout layout1;
    LinearLayout layout2;
    LinearLayout layout3;
    ImageView imageV_power;
    ImageView imageV_time;
    ImageView imageV_delay;
    TextView textV_deviceName;
    TextView textV_switch;
    TextView textV_delayAlert;
    TextView textV_timerAlert;

    //数据库变量
    private DeviceSQL deviceSQL;
    private SQLiteDatabase deviceDatabase;
    private SQLiteDatabase delayDatabase;
    private DelaySQL delaySQL;

    //设备数据
    String m_imei;
    String m_deviceName;
    Boolean m_switch;
    int i_delaySwitch;
    Long l_delayTime;

    //3个图标控制变量
    Boolean isDelayOn;
    Boolean isPowerOn;
    Boolean isTimerOn;

    //倒计时数据是否保存过
    Boolean isSaved;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        //创建延时数据库（若已存在，获取对象）
        deviceSQL = new DeviceSQL(this,"Device.db",null,1);
        deviceDatabase = deviceSQL.getWritableDatabase();
        delaySQL = new DelaySQL(this,"Delay.db",null,1);
        delayDatabase = delaySQL.getWritableDatabase();

        //获取资源
        layout1 = findViewById(R.id.back_color_1);
        layout2 = findViewById(R.id.back_color_2);
        layout3 = findViewById(R.id.back_color_3);
        textV_deviceName = findViewById(R.id.deviceName);
        textV_switch = findViewById(R.id.power_text);
        textV_delayAlert = findViewById(R.id.delay_alert);
        textV_timerAlert = findViewById(R.id.timer_alert);
        imageV_power = findViewById(R.id.power);
        imageV_delay = findViewById(R.id.delay);
        imageV_time = findViewById(R.id.time);
        relativeLayout_back = (RelativeLayout) findViewById(R.id.back);

        //获取上一个活动携带的Intent数据:设备唯一标识、设备名称、插座状态
        Intent intent = getIntent();
        m_imei = intent.getStringExtra("imei");
        m_deviceName = intent.getStringExtra("name");
        m_switch = intent.getBooleanExtra("switch",false);

        //初始化图标控制变量
        isPowerOn = m_switch;
        isDelayOn = false;
        isTimerOn = false;

        //默认无倒计时任务和定时器任务
        isSaved = false;
        textV_delayAlert.setVisibility(View.INVISIBLE);
        textV_timerAlert.setVisibility(View.INVISIBLE);

        //查询是否有倒计时任务
        Cursor cursor = delayDatabase.query("Delay", new String[]{"delaySwitch,delayTime"}, "imei=? and delayType=1 and isEffective=1", new String []{m_imei}, null, null, null);
        if (cursor.getCount()>0){
            //若有倒计时任务
            isSaved = true;
            isDelayOn = true;
            imageV_delay.setBackground(getDrawable(R.drawable.delay_on));

            //导出数据
            cursor.moveToFirst();
            l_delayTime = cursor.getLong(cursor.getColumnIndex("delayTime"));
            i_delaySwitch = cursor.getInt(cursor.getColumnIndex("delaySwitch"));

            //转换为文本格式
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String t_date = ft.format(l_delayTime);
            textV_delayAlert.setVisibility(View.VISIBLE);
            textV_delayAlert.setText(i_delaySwitch==0?"倒计时：将于"+t_date+"关闭":"倒计时：将于"+t_date+"开启");
        }
        cursor.close();

        //更新标题文字和主题色等
        textV_deviceName.setText(m_deviceName);
        reflashTheme();

        //注册返回图标的单击事件
        relativeLayout_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //处理返回数据和增删响应数据
                handleData();
                finish();
            }
        });

        //注册定时按钮的单击事件
        imageV_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //跳转到定时页面
                Intent intent=new Intent(DeviceActivity.this,DelayActivity.class);
                intent.putExtra("imei",m_imei);
                startActivity(intent);
            }
        });

        //注册电源按钮的单击事件
        imageV_power.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //更新数据
                isPowerOn = !isPowerOn;
                //更新UI
                reflashTheme();
            }
        });

        //注册延时按钮的单击事件
        imageV_delay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isDelayOn) {
                    //若延时已开启，则此次点击为删除倒计时任务
                    isDelayOn = false;
                    textV_delayAlert.setVisibility(View.INVISIBLE);//设置延时开关文字为隐藏
                    imageV_delay.setBackground(getDrawable(R.drawable.delay_off));//设置延时图标为关闭

                    //删除数据
                    delayDatabase.delete("Delay","imei=? and delayType=1", new String []{m_imei});
                }else {
                    //设置时间选择器默认值
                    Date date = new Date(2020,6,5,0,0);
                    Calendar cal=Calendar.getInstance();
                    cal.setTime(date);
                    //时间选择器
                    TimePickerView pvTime = new TimePickerBuilder(DeviceActivity.this, new OnTimeSelectListener() {
                        @Override
                        public void onTimeSelect(Date date, View v) {
                            //保存开或是关状态
                            i_delaySwitch = isPowerOn?0:1;

                            //解析用户选择的时间
                            SimpleDateFormat ft1 = new SimpleDateFormat("HH");
                            int hours = Integer.parseInt(ft1.format(date));
                            SimpleDateFormat ft2 = new SimpleDateFormat("mm");
                            int minutes = Integer.parseInt(ft2.format(date));

                            //折算为时间戳
                            long time = hours*3600000;
                            time += minutes*60000;
                            l_delayTime = System.currentTimeMillis()+time;

                            //提示插座的延时开关信息
                            SimpleDateFormat ft3 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                            String t_date = ft3.format(l_delayTime);
                            textV_delayAlert.setVisibility(View.VISIBLE);
                            textV_delayAlert.setText(isPowerOn?"倒计时：将于"+t_date+"关闭":"倒计时：将于"+t_date+"开启");

                            //更新延时图标
                            isDelayOn = true;
                            imageV_delay.setBackground(getDrawable(R.drawable.delay_on));

                            //保存倒计时数据到数据库
                            ContentValues values = new ContentValues();
                            values.put("imei",m_imei);
                            values.put("delaySwitch",i_delaySwitch);
                            values.put("delayType",1);
                            values.put("isEffective",1);
                            values.put("delayTime",l_delayTime);
                            delayDatabase.insert("Delay",null,values);
                            values.clear();
                        }
                    })
                            .setTitleText(m_switch?"倒计时关闭插座":"倒计时开启插座")//标题文字
                            .isCenterLabel(true) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                            .setLabel("年","月","日","小时","分钟","秒")
                            .setType(new boolean[]{false, false, false, true, true, false})// 默认全部显示年月日时分秒
                            .setDate(cal)//设置默认时间
                            .setTextColorCenter(Color.parseColor("#000000"))//选中项颜色
                            .setTextColorOut(Color.parseColor("#d0d0d0"))//非选中项颜色
                            .setItemVisibleCount(5)//滚轮最大可见数目的设置
                            .setLineSpacingMultiplier((float)2)//滚轮相邻文字间距
                            .isAlphaGradient(true)
                            .build();
                    pvTime.show();
                }
            }
        });
    }

    //该活动为前台活动时会被调用
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume() {
        super.onResume();

        //查询定时器是否有任务
        queryTimerTask();

        displayDelaySQL();

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void queryTimerTask()
    {
        //查询是否有定时器任务(timestamp升序排序)
        Cursor cursor = delayDatabase.query("Delay", new String[]{"delaySwitch,delayTime"}, "imei=? and delayType=2 and isEffective=1", new String []{m_imei}, null, null, "delayTime"+" ASC");
        if (cursor.getCount()>0){
            //若有倒计时任务
            isDelayOn = true;
            imageV_time.setBackground(getDrawable(R.drawable.time_on));

            //导出数据
            cursor.moveToFirst();
            long t_timestamp = cursor.getLong(cursor.getColumnIndex("delayTime"));
            int t_delaySwitch = cursor.getInt(cursor.getColumnIndex("delaySwitch"));

            //转换为文本格式
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String t_date = ft.format(t_timestamp);

            //显示提示文字
            textV_timerAlert.setVisibility(View.VISIBLE);
            textV_timerAlert.setText(t_delaySwitch==0?"定时器：将于"+t_date+"关闭":"定时器：将于"+t_date+"开启");
        }else{
            imageV_time.setBackground(getDrawable(R.drawable.time_off));
            textV_timerAlert.setVisibility(View.INVISIBLE);
        }
        cursor.close();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KEYCODE_BACK) {
            //处理返回数据和增删响应数据
            handleData();

            finish();
            return true;
        }
        return false;
    }

    //点击返回键或返回图标的数据处理
    private void handleData()
    {
        //传回数据
        Intent intent = new Intent();
        intent.putExtra("m_switch",isPowerOn);
        setResult(RESULT_OK,intent);

        //保存电源开关数据
        savePowerData();
    }

    private void displayDelaySQL(){
        Cursor cursor = delayDatabase.query("Delay", null, null, null, null, null, null);
        if (cursor.getCount()>0){
            if (cursor.moveToFirst()) {
                do {
                    String t_imei = cursor.getString(cursor.getColumnIndex("imei"));
                    int t_isEffective = cursor.getInt(cursor.getColumnIndex("isEffective"));
                    int t_delaySwitch = cursor.getInt(cursor.getColumnIndex("delaySwitch"));
                    int t_delayType = cursor.getInt(cursor.getColumnIndex("delayType"));
                    long t_delayTime = cursor.getLong(cursor.getColumnIndex("delayTime"));

                    Log.d("delaySQL","imei:"+t_imei+"  isEffective:"+t_isEffective+"  delaySwitch:"+t_delaySwitch+"  delayType:"+t_delayType+"  delayTime:"+t_delayTime);
                } while (cursor.moveToNext());
            }
        }
        cursor.close();
    }

    //更新电源开关数据
    private void savePowerData(){
        ContentValues values = new ContentValues();
        values.put("switch", isPowerOn?1:0);
        deviceDatabase.update("Device",values,"imei=?",new String []{m_imei});
        values.clear();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void reflashTheme(){
        //更新UI
        imageV_power.setBackground(getDrawable(isPowerOn?R.drawable.power_on:R.drawable.power_off));//更新开关图标
        textV_switch.setText(isPowerOn?"插座已开启":"插座已关闭");//更新开关文字状态
        //更新背景色
        layout1.setBackgroundColor(Color.parseColor(isPowerOn?"#1EC46D":"#454E69"));
        layout2.setBackgroundColor(Color.parseColor(isPowerOn?"#1EC46D":"#454E69"));
        layout3.setBackgroundColor(Color.parseColor(isPowerOn?"#1EC46D":"#454E69"));
        //更新状态栏颜色
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.parseColor(isPowerOn?"#1EC46D":"#454E69"));
    }



}
