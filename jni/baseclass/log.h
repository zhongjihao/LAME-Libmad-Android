/*************************************************************************
    > File Name: log.h
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年06月29日 星期一 12时37分23秒
 ************************************************************************/

#ifndef _LOG_H_
#define _LOG_H_

#include <android/log.h>

#define LOG_TAG "MP3_ENCODER_DECODER"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#endif
