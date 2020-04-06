package com.example.rgb_test;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

public class SecondActivity extends AppCompatActivity {
    private static final String TAG="SecondActivity";


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1){
                //Toast.makeText(SecondActivity.this,msg.obj.toString(),Toast.LENGTH_SHORT).show();
                try {
                    JSONArray jsonArray1 = getJsonArray(msg.obj.toString());
                    drawTemperatureChart(jsonArray1);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (msg.what == 2){
                JSONArray jsonArray2 = getJsonArray(msg.obj.toString());
                try {
                    drawHumidityChart(jsonArray2);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏标题栏
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
        // Android 4.0 之后不能在主线程中请求HTTP请求
        new Thread(new Runnable(){
            @Override
            public void run() {
                queryDevicePropertyData("Temperature",1);
                queryDevicePropertyData("Humidity",2);
            }
        }).start();
        setContentView(R.layout.activity_second);

    }

    public static String dateToStamp(String s) throws ParseException {
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse(s);
        long ts = date.getTime();
        res = String.valueOf(ts);
        return res;
    }

    public String getNowStamp(){
        return String.valueOf(System.currentTimeMillis());
    }

    public void drawTemperatureChart(JSONArray jsonArray) throws JSONException {
        List<String> mJsonDateX = new ArrayList<String>();
        List<Integer> mJsonDateY = new ArrayList<Integer>();

        JSONObject jsonObject;
        for(int i = 0; i < jsonArray.length(); i++){
            jsonObject = jsonArray.getJSONObject(i);
            mJsonDateX.add(stampToDate(String.valueOf(jsonObject.optLong("Time"))).substring(11));
            mJsonDateY.add(Integer.valueOf(jsonObject.optString("Value")));
        }

        List<PointValue> mPointValues = new ArrayList<PointValue>();
        List<AxisValue> mAxisXValues = new ArrayList<AxisValue>();
        List<AxisValue> mAxisYValues = new ArrayList<AxisValue>();

        LineChartView lineChart = (LineChartView)findViewById(R.id.chart1);

        for (int i = 0; i < mJsonDateX.size(); i++) {
            mAxisXValues.add(new AxisValue(i).setLabel(mJsonDateX.get(i)));
        }
        for (float i = 0; i <= 50; i+=5) {
            mAxisYValues.add(new AxisValue(i).setLabel(i+""));
        }

        for (int i = 0; i < mJsonDateY.size(); i++) {
            mPointValues.add(new PointValue(i, mJsonDateY.get(i)));
        }

        Line line = new Line(mPointValues).setColor(Color.parseColor("#FFCD41"));  //折线的颜色（橙色）
        List<Line> lines = new ArrayList<Line>();
        line.setShape(ValueShape.SQUARE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.DIAMOND）
        line.setCubic(false);//曲线是否平滑，即是曲线还是折线
        line.setFilled(false);//是否填充曲线的面积
        line.setStrokeWidth(2);
        line.setHasLabels(true);//曲线的数据坐标是否加上备注
        line.setHasLabelsOnlyForSelected(true);//点击数据坐标提示数据（设置了这个line.setHasLabels(true);就无效）
        line.setHasLines(true);//是否用线显示。如果为false 则没有曲线只有点显示
        line.setHasPoints(true);//是否显示圆点 如果为false 则没有原点只有点显示（每个数据点都是个大的圆点）
        lines.add(line);
        LineChartData data = new LineChartData();
        data.setLines(lines);

        //X坐标轴
        Axis axisX = new Axis(); //X轴
        axisX.setHasTiltedLabels(true);  //X坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.GRAY);  //设置字体颜色
        axisX.setTextSize(10);//设置字体大小
        axisX.setMaxLabelChars(5); //最多几个X轴坐标，意思就是你的缩放让X轴上数据的个数7<=x<=mAxisXValues.length
        axisX.setValues(mAxisXValues);  //填充X轴的坐标名称
        data.setAxisXBottom(axisX); //x 轴在底部
        axisX.setHasLines(true); //x 轴分割线

        // Y轴是根据数据的大小自动设置Y轴上限(在下面我会给出固定Y轴数据个数的解决方案)
        Axis axisY = new Axis();  //Y轴
        axisY.setName("温度");//y轴标注
        axisY.setTextSize(10);//设置字体大小
        axisY.setValues(mAxisYValues);
        data.setAxisYLeft(axisY);  //Y轴设置在左边


        //设置行为属性，支持缩放、滑动以及平移
        lineChart.setInteractive(true);
        lineChart.setZoomType(ZoomType.HORIZONTAL);
        lineChart.setMaxZoom((float) 5);//最大方法比例
        lineChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        lineChart.setLineChartData(data);
        lineChart.setVisibility(View.VISIBLE);

        Viewport v = new Viewport(lineChart.getMaximumViewport());
        v.bottom = 0;
        v.top = 50;
        lineChart.setMaximumViewport(v);

        v.left = mPointValues.size()-7;
        v.right = mPointValues.size()-1;
        lineChart.setCurrentViewport(v);
    }

    public void drawHumidityChart(JSONArray jsonArray) throws JSONException {
        List<String> mJsonDateX = new ArrayList<String>();
        List<Integer> mJsonDateY = new ArrayList<Integer>();

        JSONObject jsonObject;
        for(int i = 0; i < jsonArray.length(); i++){
            jsonObject = jsonArray.getJSONObject(i);
            mJsonDateX.add(stampToDate(String.valueOf(jsonObject.optLong("Time"))).substring(11));
            mJsonDateY.add(Integer.valueOf(jsonObject.optString("Value")));
        }

        List<PointValue> mPointValues = new ArrayList<PointValue>();
        List<AxisValue> mAxisXValues = new ArrayList<AxisValue>();
        List<AxisValue> mAxisYValues = new ArrayList<AxisValue>();

        LineChartView lineChart = (LineChartView)findViewById(R.id.chart2);

        for (int i = 0; i < mJsonDateX.size(); i++) {
            mAxisXValues.add(new AxisValue(i).setLabel(mJsonDateX.get(i)));
        }
        for (float i = 0; i <= 100; i+=5) {
            mAxisYValues.add(new AxisValue(i).setLabel(i+""));
        }

        for (int i = 0; i < mJsonDateY.size(); i++) {
            mPointValues.add(new PointValue(i, mJsonDateY.get(i)));
        }

        Line line = new Line(mPointValues).setColor(Color.parseColor("#FFCD41"));  //折线的颜色（橙色）
        List<Line> lines = new ArrayList<Line>();
        line.setShape(ValueShape.SQUARE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.DIAMOND）
        line.setCubic(false);//曲线是否平滑，即是曲线还是折线
        line.setFilled(false);//是否填充曲线的面积
        line.setStrokeWidth(2);
        line.setHasLabels(true);//曲线的数据坐标是否加上备注
        line.setHasLabelsOnlyForSelected(true);//点击数据坐标提示数据（设置了这个line.setHasLabels(true);就无效）
        line.setHasLines(true);//是否用线显示。如果为false 则没有曲线只有点显示
        line.setHasPoints(true);//是否显示圆点 如果为false 则没有原点只有点显示（每个数据点都是个大的圆点）
        lines.add(line);
        LineChartData data = new LineChartData();
        data.setLines(lines);

        //坐标轴
        Axis axisX = new Axis(); //X轴
        axisX.setHasTiltedLabels(true);  //X坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.GRAY);  //设置字体颜色
        axisX.setTextSize(10);//设置字体大小
        axisX.setMaxLabelChars(5); //最多几个X轴坐标，意思就是你的缩放让X轴上数据的个数7<=x<=mAxisXValues.length
        axisX.setValues(mAxisXValues);  //填充X轴的坐标名称
        data.setAxisXBottom(axisX); //x 轴在底部
        axisX.setHasLines(true); //x 轴分割线

        // Y轴是根据数据的大小自动设置Y轴上限(在下面我会给出固定Y轴数据个数的解决方案)
        Axis axisY = new Axis();  //Y轴
        axisY.setName("湿度");//y轴标注
        axisY.setTextSize(10);//设置字体大小
        axisY.setValues(mAxisYValues);
        data.setAxisYLeft(axisY);  //Y轴设置在左边


        //设置行为属性，支持缩放、滑动以及平移
        lineChart.setInteractive(true);
        lineChart.setZoomType(ZoomType.HORIZONTAL);
        lineChart.setMaxZoom((float) 5);//最大方法比例
        lineChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        lineChart.setLineChartData(data);
        lineChart.setVisibility(View.VISIBLE);

        Viewport v = new Viewport(lineChart.getMaximumViewport());
        v.bottom = 0;
        v.top = 100;
        lineChart.setMaximumViewport(v);
        v.left = mPointValues.size()-7;
        v.right = mPointValues.size()-1;
        lineChart.setCurrentViewport(v);
    }

    public void queryDevicePropertyData(String identifier,int msgWhat) {
        DefaultProfile profile = DefaultProfile.getProfile("cn-shanghai", "LTAI4FnP9CaEubeyK5F4EaeJ", "bd1cICKNbuhMmcXaIDhbRxaeOpx8s4");
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain("iot.cn-shanghai.aliyuncs.com");
        request.setVersion("2018-01-20");
        request.setAction("QueryDevicePropertyData");
        request.putQueryParameter("RegionId", "cn-shanghai");

        String today = stampToDate(getNowStamp());
        try {
            today = dateToStamp(today.substring(0,10)+" 00:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        request.putQueryParameter("StartTime", today);
        request.putQueryParameter("EndTime",   getNowStamp());
        request.putQueryParameter("Asc", "1");
        request.putQueryParameter("PageSize", "50");
        request.putQueryParameter("Identifier", identifier);
        request.putQueryParameter("ProductKey", "a1YSkpQ02ky");
        request.putQueryParameter("DeviceName", "led_text");
        Log.i(TAG,"数据返回：");
        try {
            final CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());
            Log.w(TAG,response.getData());

            Message msg = Message.obtain();
            msg.obj = response.getData();
            msg.what = msgWhat;
            handler.sendMessage(msg);

        } catch (ClientException e) {
            e.printStackTrace();
        }
    }

    public static String stampToDate(String s){
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long lt = new Long(s);
        Date date = new Date(lt);
        res = simpleDateFormat.format(date);
        return res;
    }

    public JSONArray getJsonArray(String stringJson){
        try {
            JSONObject jsonObject= new JSONObject(stringJson);
            jsonObject = jsonObject.getJSONObject("Data");
            jsonObject = jsonObject.getJSONObject("List");
            JSONArray jsonArray = jsonObject.getJSONArray("PropertyInfo");
            return jsonArray;
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(SecondActivity.this, "解析错误", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
