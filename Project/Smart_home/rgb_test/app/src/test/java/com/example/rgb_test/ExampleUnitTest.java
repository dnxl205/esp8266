package com.example.rgb_test;

import android.util.Log;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
//阿里云端api测试成功
public class ExampleUnitTest {
    public static void main(String[] args) {
        System.out.println("001");

        DefaultProfile profile = DefaultProfile.getProfile("cn-shanghai", "LTAI4FnP9CaEubeyK5F4EaeJ", "bd1cICKNbuhMmcXaIDhbRxaeOpx8s4");
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain("iot.cn-shanghai.aliyuncs.com");
        request.setVersion("2018-01-20");
        request.setAction("QueryDevicePropertyData");
        request.putQueryParameter("RegionId", "cn-shanghai");
        request.putQueryParameter("StartTime", "1583985734000");
        request.putQueryParameter("EndTime", "1585801262910");
        request.putQueryParameter("Asc", "0");
        request.putQueryParameter("PageSize", "50");
        request.putQueryParameter("Identifier", "Temperature");
        request.putQueryParameter("ProductKey", "a1YSkpQ02ky");
        request.putQueryParameter("DeviceName", "led_text");
        try {
            CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }
}