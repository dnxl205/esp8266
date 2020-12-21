package com.example.smartsocket;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class DeviceSQL extends SQLiteOpenHelper {
    public static final String CREATE_DEVICE = "create table Device ("
            +"id integer primary key autoincrement,"
            +"imei text,"
            +"name text,"
            +"online integer,"
            +"switch integer,"
<<<<<<< HEAD
            +"delayFlag integer)";
=======
            +"delayFlag integer,"
            +"delayTime integer)";
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518

    private Context mContext;

    public DeviceSQL(Context context, String name, SQLiteDatabase.CursorFactory factory,int version){
        super(context,name,factory,version);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_DEVICE);
<<<<<<< HEAD
        Toast.makeText(mContext,"创建设备数据库完成",Toast.LENGTH_SHORT).show();
=======
        Toast.makeText(mContext,"Create succeeded",Toast.LENGTH_SHORT).show();
>>>>>>> fe7d45f0cff4d7e1bdcbaeb013c0d35ba9fda518
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("drop table if exists Device");
        onCreate(sqLiteDatabase);
    }


}
