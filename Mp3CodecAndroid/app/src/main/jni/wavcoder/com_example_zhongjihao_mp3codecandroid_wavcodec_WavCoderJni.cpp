/*************************************************************************
    > File Name: com_example_zhongjihao_mp3codecandroid_wavcodec_WavCoderJni.cpp
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com
    > Created Time: 2019年10月20日 星期日 16时50分27秒
 ************************************************************************/

#define LOG_TAG "WavCoder-JNI"

#include "WavEncoder.h"
#include "com_example_zhongjihao_mp3codecandroid_wavcodec_WavCoderJni.h"
#include "../baseclass/log.h"

JNIEXPORT jlong JNICALL Java_com_example_zhongjihao_mp3codecandroid_wavcodec_WavCoderJni_createWavEncoder
  (JNIEnv *env, jclass jcls, jstring jwavPath)
{
    const char* wavfile = env ->GetStringUTFChars(jwavPath, NULL);
    WavEncoder* p_codec = new WavEncoder(wavfile);
    env ->ReleaseStringUTFChars(jwavPath, wavfile);
    return reinterpret_cast<long> (p_codec);
}


JNIEXPORT void JNICALL Java_com_example_zhongjihao_mp3codecandroid_wavcodec_WavCoderJni_initWavEncoder
  (JNIEnv *env, jclass jcls, jlong jptr, jint jchannelNum, jint jsampleRate, jint jbytesPerSample)
{
    LOGD("%s: ===zhongjihao=====",__FUNCTION__);
    WavEncoder* p_codec = reinterpret_cast<WavEncoder *> (jptr);
    p_codec->initWavEncoder((int)jchannelNum,(int)jsampleRate,(int)jbytesPerSample);
}


JNIEXPORT void JNICALL Java_com_example_zhongjihao_mp3codecandroid_wavcodec_WavCoderJni_writePcmData
  (JNIEnv *env, jclass jcls, jlong jptr, jshortArray jpcmArray, jint jshortNum)
{
    jshort* buffer = env->GetShortArrayElements(jpcmArray, NULL);
    WavEncoder* p_codec = reinterpret_cast<WavEncoder *> (jptr);
    p_codec->writePcmData(buffer,jshortNum);
    env->ReleaseShortArrayElements(jpcmArray, buffer, 0);
}

JNIEXPORT void JNICALL Java_com_example_zhongjihao_mp3codecandroid_wavcodec_WavCoderJni_writeWavHeader
  (JNIEnv *env, jclass jcls, jlong jptr)
{
    LOGD("%s: ===zhongjihao=====",__FUNCTION__);
    WavEncoder* p_codec = reinterpret_cast<WavEncoder *> (jptr);
    p_codec->writeWavHeader();
}

JNIEXPORT void JNICALL Java_com_example_zhongjihao_mp3codecandroid_wavcodec_WavCoderJni_destroyWavEncoder
  (JNIEnv *env, jclass jcls, jlong jptr)
{
    LOGD("%s: ===zhongjihao=====",__FUNCTION__);
    WavEncoder* p_codec = reinterpret_cast<WavEncoder *> (jptr);
    delete p_codec;
}
