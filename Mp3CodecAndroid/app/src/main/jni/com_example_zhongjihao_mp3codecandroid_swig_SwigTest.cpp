/**
 * Created by zhongjihao on 18-8-12.
 */

#include "com_example_zhongjihao_mp3codecandroid_swig_SwigTest.h"


JNIEXPORT jstring JNICALL Java_com_example_zhongjihao_mp3codecandroid_swig_SwigTest_SayHello
  (JNIEnv *env, jclass jcls)
{
    return env ->NewStringUTF("SWIG测试");
}

