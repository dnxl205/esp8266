package com.example.smartsocket;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static android.view.KeyEvent.KEYCODE_BACK;

public class DelayActivity extends AppCompatActivity {

    private List<Delay> delayList = new ArrayList<>();
    DelayAdapter delayAdapter;
    RecyclerView recyclerView;

    //设备数据
    String m_imei;
    Boolean m_isEffective;

    //数据库变量
    private SQLiteDatabase delayDatabase;
    private DelaySQL delaySQL;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delay);

        //获取设备imei
        Intent intent = getIntent();
        m_imei = intent.getStringExtra("imei");

        //创建延时数据库（若已存在，获取对象）
        delaySQL = new DelaySQL(this,"Delay.db",null,1);
        delayDatabase = delaySQL.getWritableDatabase();

        //查询设备数据库,加载滚动列表
        Cursor cursor = delayDatabase.query("Delay", new String[]{"delaySwitch,delayTime,isEffective"}, "imei=? and delayType=2", new String []{m_imei}, null, null, null);
        if (cursor.getCount()>0){
            if (cursor.moveToFirst()) {
                do {
                    //遍历Cursor对象， 取出数据
                    int t_delaySwitch = cursor.getInt(cursor.getColumnIndex("delaySwitch"));
                    long t_timestamp = cursor.getLong(cursor.getColumnIndex("delayTime"));
                    int t_isEffective = cursor.getInt(cursor.getColumnIndex("isEffective"));

                    //将时间戳转化为文本格式
                    SimpleDateFormat ft = new SimpleDateFormat("yyyy/MM/dd");
                    String t_date = ft.format(t_timestamp);
                    ft = new SimpleDateFormat("HH:mm");
                    String t_time = ft.format(t_timestamp);

                    //添加到列表
                    Delay delay = new Delay(t_delaySwitch==1,t_isEffective==1,t_date,t_time,t_timestamp);
                    delayList.add(delay);

                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        //配置滚动列表适配器
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView = findViewById(R.id.recycler_view_delay);
        recyclerView.setLayoutManager(linearLayoutManager);
        delayAdapter = new DelayAdapter(delayList);
        recyclerView.setAdapter(delayAdapter);

        //注册delay_view长按事件
        delayAdapter.setOnItemLongClickListener(new DelayAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, final int position) {
                //弹出删除警告框
                AlertDialog.Builder builder = new AlertDialog.Builder(DelayActivity.this);
                builder.setTitle("删除");
                builder.setMessage("确定要删除此定时任务吗？");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //删除在数据库中的数据
                        long t_timestamp = delayList.get(position).getTimestamp();
                        delayDatabase.delete("Delay","imei=? and delayTime=? and delayType=2", new String []{m_imei,String.valueOf(t_timestamp)});
                        //删除在列表中的数据
                        delayAdapter.removeData(position);

                        Toast.makeText(DelayActivity.this,"删除成功",Toast.LENGTH_SHORT).show();
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
        delayAdapter.setOnItemClickListener(new DelayAdapter.OnItemClickListener() {
            @Override
            public void onClick(CompoundButton compoundButton, int position,boolean b) throws JSONException {
                //排除非人为按下的事件
                if(!compoundButton.isPressed())
                    return;

                    //更新数据到数据库
                    delayList.get(position).setEffective(b);
                    ContentValues values = new ContentValues();
                    values.put("isEffective", b?1:0);
                    long t_timestamp = delayList.get(position).getTimestamp();
                    delayDatabase.update("Delay",values,"imei=? and delayTime=?",new String []{m_imei, String.valueOf(t_timestamp)});
                    values.clear();
            }
        });

        //注册返回图标点击事件
        RelativeLayout r_back = findViewById(R.id.delay_back);
        r_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //日期选择器起止日期参数
        final Calendar startDate = Calendar.getInstance();
        final Calendar endDate = Calendar.getInstance();
        endDate.set(2021,11,31);

        //添加定时开启任务
        Button btn_addDelay_on = findViewById(R.id.Btn_addDelay_on);
        btn_addDelay_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //时间选择器
                TimePickerView pvTime_on = new TimePickerBuilder(DelayActivity.this, new OnTimeSelectListener() {
                    @Override
                    public void onTimeSelect(Date date, View v) {
                        SimpleDateFormat ft1 = new SimpleDateFormat("yyyy/MM/dd");
                        String t_date = ft1.format(date);

                        SimpleDateFormat ft2 = new SimpleDateFormat("HH:mm");
                        String t_time = ft2.format(date);

                        //添加数据到数据库
                        ContentValues values = new ContentValues();
                        values.put("imei",m_imei);
                        values.put("delaySwitch",1);
                        values.put("isEffective",1);
                        values.put("delayType",2);
                        values.put("delayTime",date.getTime());
                        delayDatabase.insert("Delay",null,values);
                        values.clear();

                        //添加到列表
                        delayAdapter.addItem(0,new Delay(true,true,t_date,t_time,date.getTime()));
                    }
                })
                        .setRangDate(startDate,endDate)//起始终止年月日设定
                        .isCenterLabel(true) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                        .setTitleText("定时开启")//标题文字
                        .setType(new boolean[]{true, true, true, true, true, false})// 默认全部显示
                        .build();
                pvTime_on.show();
            }
        });

        //添加定时关闭任务
        Button btn_addDelay_off = findViewById(R.id.Btn_addDelay_off);
        btn_addDelay_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //时间选择器
                TimePickerView pvTime_off = new TimePickerBuilder(DelayActivity.this, new OnTimeSelectListener() {
                    @Override
                    public void onTimeSelect(Date date, View v) {
                        SimpleDateFormat ft1 = new SimpleDateFormat("yyyy/MM/dd");
                        String t_date = ft1.format(date);

                        SimpleDateFormat ft2 = new SimpleDateFormat("HH:mm");
                        String t_time = ft2.format(date);

                        //添加数据到数据库
                        ContentValues values = new ContentValues();
                        values.put("imei",m_imei);
                        values.put("delaySwitch",0);
                        values.put("isEffective",1);
                        values.put("delayType",2);
                        values.put("delayTime",date.getTime());
                        delayDatabase.insert("Delay",null,values);
                        values.clear();

                        //添加到列表
                        delayAdapter.addItem(0,new Delay(false,true,t_date,t_time,date.getTime()));
                    }
                })
                        .setRangDate(startDate,endDate)//起始终止年月日设定
                        .isCenterLabel(true) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                        .setTitleText("定时关闭")//标题文字
                        .setType(new boolean[]{true, true, true, true, true, false})// 默认全部显示
                        .build();
                pvTime_off.show();
            }
        });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KEYCODE_BACK) {
            finish();
            return true;
        }
        return false;
    }

}
