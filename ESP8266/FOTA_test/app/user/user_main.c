/*
 * ESPRESSIF MIT License
 *
 * Copyright (c) 2016 <ESPRESSIF SYSTEMS (SHANGHAI) PTE LTD>
 *
 * Permission is hereby granted for use on ESPRESSIF SYSTEMS ESP8266 only, in which case,
 * it is free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
#include "ets_sys.h"
#include "osapi.h"
#include "os_type.h"
#include "eagle_soc.h"

#include "driver/uart.h"  //串口0需要的头文件
#include "gpio.h"  //端口控制需要的头文件
#include "driver/key.h"
#include "user_interface.h"
#include "user_config.h"
#include "modules/fota_upgrade.h"
#include "modules/wifi.h"

#if ((SPI_FLASH_SIZE_MAP == 0) || (SPI_FLASH_SIZE_MAP == 1))
#error "The flash map is not supported"
#elif (SPI_FLASH_SIZE_MAP == 2)
#define SYSTEM_PARTITION_OTA_SIZE							0x6A000
#define SYSTEM_PARTITION_OTA_2_ADDR							0x81000
#define SYSTEM_PARTITION_RF_CAL_ADDR						0xfb000
#define SYSTEM_PARTITION_PHY_DATA_ADDR						0xfc000
#define SYSTEM_PARTITION_SYSTEM_PARAMETER_ADDR				0xfd000
#define SYSTEM_PARTITION_CUSTOMER_PRIV_PARAM_ADDR           0x7c000
#elif (SPI_FLASH_SIZE_MAP == 3)
#define SYSTEM_PARTITION_OTA_SIZE							0x6A000
#define SYSTEM_PARTITION_OTA_2_ADDR							0x81000
#define SYSTEM_PARTITION_RF_CAL_ADDR						0x1fb000
#define SYSTEM_PARTITION_PHY_DATA_ADDR						0x1fc000
#define SYSTEM_PARTITION_SYSTEM_PARAMETER_ADDR				0x1fd000
#define SYSTEM_PARTITION_CUSTOMER_PRIV_PARAM_ADDR           0x7c000
#elif (SPI_FLASH_SIZE_MAP == 4)
#define SYSTEM_PARTITION_OTA_SIZE							0x6A000
#define SYSTEM_PARTITION_OTA_2_ADDR							0x81000
#define SYSTEM_PARTITION_RF_CAL_ADDR						0x3fb000
#define SYSTEM_PARTITION_PHY_DATA_ADDR						0x3fc000
#define SYSTEM_PARTITION_SYSTEM_PARAMETER_ADDR				0x3fd000
#define SYSTEM_PARTITION_CUSTOMER_PRIV_PARAM_ADDR           0x7c000
#elif (SPI_FLASH_SIZE_MAP == 5)
#define SYSTEM_PARTITION_OTA_SIZE							0x6A000
#define SYSTEM_PARTITION_OTA_2_ADDR							0x101000
#define SYSTEM_PARTITION_RF_CAL_ADDR						0x1fb000
#define SYSTEM_PARTITION_PHY_DATA_ADDR						0x1fc000
#define SYSTEM_PARTITION_SYSTEM_PARAMETER_ADDR				0x1fd000
#define SYSTEM_PARTITION_CUSTOMER_PRIV_PARAM_ADDR           0xfc000
#elif (SPI_FLASH_SIZE_MAP == 6)
#define SYSTEM_PARTITION_OTA_SIZE							0x6A000
#define SYSTEM_PARTITION_OTA_2_ADDR							0x101000
#define SYSTEM_PARTITION_RF_CAL_ADDR						0x3fb000
#define SYSTEM_PARTITION_PHY_DATA_ADDR						0x3fc000
#define SYSTEM_PARTITION_SYSTEM_PARAMETER_ADDR				0x3fd000
#define SYSTEM_PARTITION_CUSTOMER_PRIV_PARAM_ADDR           0xfc000
#else
#error "The flash map is not supported"
#endif

#define SYSTEM_PARTITION_CUSTOMER_PRIV_PARAM                SYSTEM_PARTITION_CUSTOMER_BEGIN

uint32 priv_param_start_sec;

/*********************************************************************************
 * @brief      开关输入引脚配置
 ********************************************************************************/
#define SWITCH_Pin_NUM         0
#define SWITCH_Pin_FUNC        FUNC_GPIO0
#define SWITCH_Pin_MUX         PERIPHS_IO_MUX_GPIO0_U

#define SWITCH_Pin_Rd_Init()   GPIO_DIS_OUTPUT(SWITCH_Pin_NUM)
#define SWITCH_Pin_Wr_Init()   GPIO_OUTPUT_SET(SWITCH_Pin_NUM,0)
#define SWITCH_Pin_Set_High()  GPIO_OUTPUT_SET(SWITCH_Pin_NUM,1)
#define SWITCH_Pin_Set_Low()   GPIO_OUTPUT_SET(SWITCH_Pin_NUM,0)
#define SWITCH_Pin_State       ( GPIO_INPUT_GET(SWITCH_Pin_NUM) != 0 )

/*********************************************************************************
 * @brief      按键相关变量
 ********************************************************************************/
static struct keys_param switch_param;
static struct single_key_param *switch_signle;
static bool status = true;


static const partition_item_t at_partition_table[] = {
    { SYSTEM_PARTITION_BOOTLOADER, 						0x0, 												0x1000},
    { SYSTEM_PARTITION_OTA_1,   						0x1000, 											SYSTEM_PARTITION_OTA_SIZE},
    { SYSTEM_PARTITION_OTA_2,   						SYSTEM_PARTITION_OTA_2_ADDR, 						SYSTEM_PARTITION_OTA_SIZE},
    { SYSTEM_PARTITION_RF_CAL,  						SYSTEM_PARTITION_RF_CAL_ADDR, 						0x1000},
    { SYSTEM_PARTITION_PHY_DATA, 						SYSTEM_PARTITION_PHY_DATA_ADDR, 					0x1000},
    { SYSTEM_PARTITION_SYSTEM_PARAMETER, 				SYSTEM_PARTITION_SYSTEM_PARAMETER_ADDR, 			0x3000},
    { SYSTEM_PARTITION_CUSTOMER_PRIV_PARAM,             SYSTEM_PARTITION_CUSTOMER_PRIV_PARAM_ADDR,          0x1000},
};

void ICACHE_FLASH_ATTR user_pre_init(void)
{
    if(!system_partition_table_regist(at_partition_table, sizeof(at_partition_table)/sizeof(at_partition_table[0]),SPI_FLASH_SIZE_MAP)) {
		INFO("[IMALIUBO] system_partition_table_regist fail\r\n");
		while(1);
	}
}

os_timer_t wifistate_checktimer;

void ICACHE_FLASH_ATTR
WifiStatus_Check(void){
    uint8 wifiStatus;
    wifiStatus = wifi_station_get_connect_status();
    if (wifiStatus == STATION_GOT_IP) {
        INFO(" WiFi connection is successful!\r\n");
        os_timer_disarm(&wifistate_checktimer);
        struct ip_info local_ip;
        wifi_get_ip_info(STATION_IF,&local_ip);

        //连接好wifi后更新user.bin
        fota_init();
    }else{
        INFO(" WiFi connection failed!\r\n");
    }
}


void ICACHE_FLASH_ATTR
wifiConnectCb(uint8_t status){

    os_timer_disarm(&wifistate_checktimer);
    //WifiStatus_Check中将会进行版本升级
    os_timer_setfn(&wifistate_checktimer, (os_timer_func_t *) WifiStatus_Check,NULL);
    os_timer_arm(&wifistate_checktimer, 2000, true);
}

/*********************************************************************************
 * @brief      按键短按回调函数
 ********************************************************************************/
static void Switch_ShortPress_Handler( void )
{
	os_printf("--------Switch_ShortPress_Handler---------\n");
    if( status == true )
    {
        GPIO_OUTPUT_SET(GPIO_ID_PIN(2), 1);
        os_printf("---------status = true--------\n");

        status = false;
    }
    else
    {
        GPIO_OUTPUT_SET(GPIO_ID_PIN(2), 0);
        os_printf("---------status = false--------\n");
        os_printf("-----------try fota -----------\n");
        wifi_set_opmode(STATION_MODE);
        WIFI_Connect(STA_SSID, STA_PASS, wifiConnectCb);

        status = true;
    }
}


/*********************************************************************************
 * @brief      按键初始化函数
 ********************************************************************************/
void drv_Switch_Init( void )
{
    switch_signle = key_init_single( SWITCH_Pin_NUM, SWITCH_Pin_MUX,
                                     SWITCH_Pin_FUNC,
                                     NULL,
                                     &Switch_ShortPress_Handler );
    switch_param.key_num = 1;
    switch_param.single_key = &switch_signle;

    key_init( &switch_param );
}


//wifi账号密码、TCP连接IP和端口在user_config.h中修改
void ICACHE_FLASH_ATTR
user_init(void)
{

    //wifi_set_opmode(STATION_MODE);

    INFO("[IMALIUBO] SDK version:%s\n", system_get_sdk_version());

    uart_init_3(115200,115200);

    PIN_FUNC_SELECT(PERIPHS_IO_MUX_GPIO2_U, FUNC_GPIO2);//nodemcu的板载LED
    GPIO_OUTPUT_SET(GPIO_ID_PIN(2), 0);
    PIN_FUNC_SELECT(PERIPHS_IO_MUX_GPIO0_U, FUNC_GPIO0);//GPIO0为nodemcu板载按键
    GPIO_DIS_OUTPUT(GPIO_ID_PIN(0));//GPIO0为高


    INFO(" \r\n[IMALIUBO] SDK version:%s\r\n", system_get_sdk_version());
    INFO("[IMALIUBO] system_get_free_heap_size=%d\r\n",system_get_free_heap_size());

    if (system_upgrade_userbin_check() == UPGRADE_FW_BIN1) {
        INFO("\r\nnow bin:---------user1---------- \r\n");
    } else if (system_upgrade_userbin_check() == UPGRADE_FW_BIN2) {
        INFO("\r\nnow bin:---------user2---------- \r\n");
    }

    //wifiConnectCb中将会检查升级
    //WIFI_Connect(STA_SSID, STA_PASS, wifiConnectCb);

    INFO("[IAMLIUBO] system_free_size = %d\n", system_get_free_heap_size());
    
    drv_Switch_Init();

}

