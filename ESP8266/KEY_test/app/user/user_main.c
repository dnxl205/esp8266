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

//#include "ip_addr.h"
//#include "espconn.h"
//#include "mem.h"
//#include "smartconfig.h"
//#include "airkiss.h"

#define INFO( format, ... ) os_printf( format, ## __VA_ARGS__ )

#define SWITCH_Pin_NUM         0
#define SWITCH_Pin_FUNC        FUNC_GPIO0
#define SWITCH_Pin_MUX         PERIPHS_IO_MUX_GPIO0_U

#define SWITCH_Pin_Rd_Init()   GPIO_DIS_OUTPUT(SWITCH_Pin_NUM)
#define SWITCH_Pin_Wr_Init()   GPIO_OUTPUT_SET(SWITCH_Pin_NUM,0)
#define SWITCH_Pin_Set_High()  GPIO_OUTPUT_SET(SWITCH_Pin_NUM,1)
#define SWITCH_Pin_Set_Low()   GPIO_OUTPUT_SET(SWITCH_Pin_NUM,0)
#define SWITCH_Pin_State       ( GPIO_INPUT_GET(SWITCH_Pin_NUM) != 0 )


//#define DEVICE_TYPE 		"gh_9e2cff3dfa51" //wechat public number
//#define DEVICE_ID 			"122475" //model ID
//
//#define DEFAULT_LAN_PORT 	12476

#if ((SPI_FLASH_SIZE_MAP == 0) || (SPI_FLASH_SIZE_MAP == 1))
#error "The flash map is not supported"
#elif (SPI_FLASH_SIZE_MAP == 2)
#define SYSTEM_PARTITION_OTA_SIZE							0x6A000
#define SYSTEM_PARTITION_OTA_2_ADDR							0x81000
#define SYSTEM_PARTITION_RF_CAL_ADDR						0xfb000
#define SYSTEM_PARTITION_PHY_DATA_ADDR						0xfc000
#define SYSTEM_PARTITION_SYSTEM_PARAMETER_ADDR				0xfd000
#elif (SPI_FLASH_SIZE_MAP == 3)
#define SYSTEM_PARTITION_OTA_SIZE							0x6A000
#define SYSTEM_PARTITION_OTA_2_ADDR							0x81000
#define SYSTEM_PARTITION_RF_CAL_ADDR						0x1fb000
#define SYSTEM_PARTITION_PHY_DATA_ADDR						0x1fc000
#define SYSTEM_PARTITION_SYSTEM_PARAMETER_ADDR				0x1fd000
#elif (SPI_FLASH_SIZE_MAP == 4)
#define SYSTEM_PARTITION_OTA_SIZE							0x6A000
#define SYSTEM_PARTITION_OTA_2_ADDR							0x81000
#define SYSTEM_PARTITION_RF_CAL_ADDR						0x3fb000
#define SYSTEM_PARTITION_PHY_DATA_ADDR						0x3fc000
#define SYSTEM_PARTITION_SYSTEM_PARAMETER_ADDR				0x3fd000
#elif (SPI_FLASH_SIZE_MAP == 5)
#define SYSTEM_PARTITION_OTA_SIZE							0x6A000
#define SYSTEM_PARTITION_OTA_2_ADDR							0x101000
#define SYSTEM_PARTITION_RF_CAL_ADDR						0x1fb000
#define SYSTEM_PARTITION_PHY_DATA_ADDR						0x1fc000
#define SYSTEM_PARTITION_SYSTEM_PARAMETER_ADDR				0x1fd000
#elif (SPI_FLASH_SIZE_MAP == 6)
#define SYSTEM_PARTITION_OTA_SIZE							0x6A000
#define SYSTEM_PARTITION_OTA_2_ADDR							0x101000
#define SYSTEM_PARTITION_RF_CAL_ADDR						0x3fb000
#define SYSTEM_PARTITION_PHY_DATA_ADDR						0x3fc000
#define SYSTEM_PARTITION_SYSTEM_PARAMETER_ADDR				0x3fd000
#else
#error "The flash map is not supported"
#endif

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
};

static void Switch_ShortPress_Handler( void )
{
	os_printf("--------------Switch_ShortPress_Handler------------------\n");
    if( status == true )
    {
        status = false;
        GPIO_DIS_OUTPUT(GPIO_ID_PIN(1));
        os_printf("---------status = false--------\n");
    }
    else
    {
        status = true;
        GPIO_DIS_OUTPUT(GPIO_ID_PIN(0));
        os_printf("---------status = true--------\n");
    }
}

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


void ICACHE_FLASH_ATTR user_pre_init(void)
{
    if(!system_partition_table_regist(at_partition_table, sizeof(at_partition_table)/sizeof(at_partition_table[0]),SPI_FLASH_SIZE_MAP)) {
		os_printf("system_partition_table_regist fail\r\n");
		while(1);
	}
}

void delay_ms(uint16 x)
{
	for(;x>0;x--)
	{
		system_soft_wdt_feed();//这里我们喂下看门狗  ，不让看门狗复位
		os_delay_us(1000);
	}
}


void ICACHE_FLASH_ATTR
user_init(void)
{
    uart_init_3(115200,115200);

    os_printf("SDK version:%s\n", system_get_sdk_version());
    os_printf("--------------Hello World------------------\n");

    PIN_FUNC_SELECT(PERIPHS_IO_MUX_GPIO2_U, FUNC_GPIO2);//nodemcu的板载LED
    GPIO_OUTPUT_SET(GPIO_ID_PIN(2), 0);

    PIN_FUNC_SELECT(PERIPHS_IO_MUX_GPIO0_U, FUNC_GPIO0);//GPIO0为nodemcu板载按键
    GPIO_DIS_OUTPUT(GPIO_ID_PIN(0));//GPIO0为高

    INFO(" \r\n[IMALIUBO] SDK version:%s\r\n", system_get_sdk_version());

    if (system_upgrade_userbin_check() == UPGRADE_FW_BIN1) {
        INFO("\r\nnow bin:---------user1---------- \r\n");
    } else if (system_upgrade_userbin_check() == UPGRADE_FW_BIN2) {
        INFO("\r\nnow bin:---------user2---------- \r\n");
    }

    drv_Switch_Init();

}
