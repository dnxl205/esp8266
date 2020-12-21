package com.example.smartsocket;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;


//倒计时和定时任务的数据库都用这个表示
public class DelaySQL extends SQLiteOpenHelper {
    public static final String CREATE_DEVICE = "create table Delay ("
            +"id integer primary key autoincrement,"
            +"imei text,"
            +"isEffective integer,"
            +"delaySwitch integer,"
            +"delayType integer,"//0:保留值；1：延时；2：定时
            +"delayTime integer)";

    private Context mContext;

    public DelaySQL(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context,name,factory,version);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_DEVICE);
        Toast.makeText(mContext,"创建延时数据库完成",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("drop table if exists Device");
        onCreate(sqLiteDatabase);
    }


}
