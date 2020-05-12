#ifndef __LED_H
#define __LED_H
#include "sys.h"

//LED端口定义
#define LED0 PAout(6)	
#define LED1 PAout(7)	

void LED_Init(void);//LED初始化	
#endif
