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


static JavaVM* g_vm = NULL;

static struct Mp3EncoderCb{
    jclass    callback_cls;
    jobject   callback_obj;
    jmethodID callback_mid;
}mp3EncoderCb;

jlong JNI_createEncoder(JNIEnv *env,jclass jcls)
{
	CMp3Encoder* p_encode = new CMp3Encoder;
	return reinterpret_cast<long> (p_encode);
}

void JNI_OnRegisterCallback(JNIEnv *env, jclass jcls, jobject jobj)
{
    LOGD("%s:E   ",__FUNCTION__);
    jclass tmp = env ->GetObjectClass(jobj); //可以在子线程中使用
    mp3EncoderCb.callback_cls = (jclass)env->NewGlobalRef(tmp);

    //JNI 函数参数中 jobject 或者它的子类，其参数都是 local reference。
    //Local reference 只在这个 JNI函数中有效，JNI函数返回后，引用的对象就被释放，它的生命周期就结束了。
    //若要留着日后使用，则需根据这个 local reference 创建 global reference。
    //Global reference 不会被系统自动释放，它仅当被程序明确调用 DeleteGlobalRef 时才被回收。（JNI多线程机制）
    mp3EncoderCb.callback_obj = env ->NewGlobalRef(jobj);
    LOGD("%s:X   ",__FUNCTION__);
}

void JNI_OnDestroyCallback(JNIEnv *env, jclass jcls)
{
    LOGD("%s:E   ",__FUNCTION__);
    env ->DeleteGlobalRef(mp3EncoderCb.callback_obj);
    env ->DeleteGlobalRef(mp3EncoderCb.callback_cls);
    g_vm = NULL;
    LOGD("%s:X   ",__FUNCTION__);
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

jint JNI_encodePcmDataToMp3(JNIEnv *env,jclass jcls, jlong jcPtr,jshortArray jbuffer_l, jshortArray jbuffer_r, jint jsamples, jbyteArray jmp3buf)
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

jint JNI_pcmfileConvertMP3file(JNIEnv *env,jclass jcls, jlong jcPtr, jstring jpcmpath, jstring jmp3path)
{
    const char *pcmpath = NULL, *mp3path = NULL;
    pcmpath = env->GetStringUTFChars(jpcmpath, NULL);
    mp3path = env->GetStringUTFChars(jmp3path, NULL);

    p_encode_th ->setEncoderFile(pcmpath,mp3path);

    LOGD("====JNI===开始启动编码线程=====");
    if(!p_encode_th ->start())
    {
        LOGE("=======encoder pThread  create failed======");
        delete p_encode_th;
        p_encode_th = NULL;
        env->ReleaseStringUTFChars(jpcmpath,pcmpath);
        env->ReleaseStringUTFChars(jmp3path,mp3path);
        ThrowException(env,"java/lang/RuntimeException","===JNI===Mp3EncoderThread create failed===");
        return -1;
    }
    env->ReleaseStringUTFChars(jpcmpath,pcmpath);
    env->ReleaseStringUTFChars(jmp3path,mp3path);
    return 0;
}


//JAVA函数和C++函数映射关系表
static JNINativeMethod gMethods[] = {
    { "registerCallback","(Lcom/example/zhongjihao/mp3codecandroid/mp3codec/IMP3EncoderDoneNotify;)V",(void*)JNI_OnRegisterCallback},
    { "destroyCallback","()V",(void*)JNI_OnDestroyCallback},
	{ "createMp3Encoder","()J",(void*)JNI_createEncoder},
	{ "initMp3Encoder","(JIIIIII)I",(void*)JNI_initEncoder},
	{ "encodePcmDataToMp3","(J[S[SI[B)I",(void*)JNI_encodePcmDataToMp3},
	{ "encodeFlush","(J[B)I",(void*)JNI_encodeFlush},
    { "pcmfileConvertMP3file","(JLjava/lang/String;Ljava/lang/String;)I",(void*)JNI_pcmfileConvertMP3file},
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
    g_vm = vm;
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

