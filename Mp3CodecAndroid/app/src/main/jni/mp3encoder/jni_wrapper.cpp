/*************************************************************************
    > File Name: jni_wrapper.cpp
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com
    > Created Time: 2015年03月28日 星期六 16时50分27秒
 ************************************************************************/

#define LOG_TAG "Mp3Encoder_JNI"

#include "../baseclass/jni_register.h"
#include "./nativeEncoder/CMp3Encoder.h"
#include "../baseclass/log.h"


jlong JNI_createEncoder(JNIEnv *env,jclass jcls)
{
	CMp3Encoder* p_encode = new CMp3Encoder;
	return reinterpret_cast<long> (p_encode);
}

jint JNI_initEncoder(JNIEnv *env,jclass jcls, jlong jcPtr, jint in_num_channels, jint inSamplerate, jint outSamplerate,jint outBitrate,jint in_mode, jint in_quality)
{
	CMp3Encoder* p_encode = reinterpret_cast<CMp3Encoder *> (jcPtr);
	int ret_code = p_encode->initEncoder((int)in_num_channels,(int)inSamplerate,(int)outSamplerate,(int)outBitrate,(int)in_mode,(int)in_quality);

	LOGD("%s: ===zhongjihao===init lamemp3===ret_code: %d",__FUNCTION__,ret_code);
	return ret_code;
}

void JNI_destroyEncoder(JNIEnv *env, jclass jcls,jlong jcPtr)
{
	LOGD("%s: ====zhongjihao==========",__FUNCTION__);
	CMp3Encoder* p_encode = reinterpret_cast<CMp3Encoder *> (jcPtr);
	delete p_encode;
}

jint JNI_encodePcmToMp3(JNIEnv *env,jclass jcls, jlong jcPtr,jshortArray jbuffer_l, jshortArray jbuffer_r, jint jsamples, jbyteArray jmp3buf)
{
	jshort* buffer_l = env->GetShortArrayElements(jbuffer_l, NULL);
	jshort* buffer_r = env->GetShortArrayElements(jbuffer_r, NULL);
	jbyte*  mp3buf = env->GetByteArrayElements(jmp3buf, NULL);
	const jsize mp3buf_size = env->GetArrayLength(jmp3buf);

	CMp3Encoder* p_encode = reinterpret_cast<CMp3Encoder *> (jcPtr);
	int encoderBytes = p_encode ->encoder(buffer_l, buffer_r, jsamples, (unsigned char*)mp3buf, mp3buf_size);

	env->ReleaseShortArrayElements(jbuffer_l, buffer_l, 0);
	env->ReleaseShortArrayElements(jbuffer_r, buffer_r, 0);
	env->ReleaseByteArrayElements(jmp3buf, mp3buf, 0);

	LOGD("%s: ====zhongjihao======encoderBytes: %d",__FUNCTION__,encoderBytes);
	return encoderBytes;
}

jint JNI_encodeFlush(JNIEnv *env,jclass jcls, jlong jcPtr, jbyteArray jmp3buf)
{
	jbyte*  mp3buf = env->GetByteArrayElements(jmp3buf, NULL);
	const jsize mp3buf_size = env->GetArrayLength(jmp3buf);

	CMp3Encoder* p_encode = reinterpret_cast<CMp3Encoder *> (jcPtr);
    int encoderBytes = p_encode ->flush((unsigned char*)mp3buf, mp3buf_size);
    env->ReleaseByteArrayElements(jmp3buf, mp3buf, 0);
	LOGD("%s: ====zhongjihao===flush===encoderBytes: %d",__FUNCTION__,encoderBytes);
	return encoderBytes;
}


//JAVA函数和C++函数映射关系表
static JNINativeMethod gMethods[] = {
	{ "createMp3Encoder","()J",(void*)JNI_createEncoder},
	{ "initMp3Encoder","(JIIIIII)I",(void*)JNI_initEncoder},
	{ "encodePcmToMp3","(J[S[SI[B)I",(void*)JNI_encodePcmToMp3},
	{ "encodeFlush","(J[B)I",(void*)JNI_encodeFlush},
    { "destroyMp3Encoder","(J)V",(void*)JNI_destroyEncoder},
};

//定义java中类全名
static const char* classPathName = "com/example/zhongjihao/mp3codecandroid/mp3codec/Mp3EncoderJni";

typedef union{
	JNIEnv* env;
	void* venv;
}UnionJNIEnvToVoid;

//JNI库加载时自动调用该函数
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm,void* reserved)
{
	UnionJNIEnvToVoid uenv;
	JNIEnv* env = NULL;
	//获得JNI版本
	if(vm->GetEnv((void**)&uenv.venv,JNI_VERSION_1_4) != JNI_OK)
	{
		return -1;
	}
	env = uenv.env;
	//注册java函数
	if(registerNatives(env,classPathName,gMethods,sizeof(gMethods)/sizeof(gMethods[0])) != JNI_TRUE)
	{
		return -1;
	}
	return JNI_VERSION_1_4;
}

