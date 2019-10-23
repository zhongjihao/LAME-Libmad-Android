/*************************************************************************
    > File Name: jni_mad_mp3_wrap.cpp
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年07月03日 星期五 20时07分54秒
 ************************************************************************/

#define LOG_TAG "JNI_MAD_MP3"

#include "../baseclass/jni_register.h"
#include "../baseclass/log.h"

#ifdef __cplusplus
extern "C" {
#endif
    int NativeMP3Decoder_readSamples(short *target, int size);  
    void NativeMP3Decoder_closeAudioFile();  
    int NativeMP3Decoder_getAudioSamplerate();  
    int NativeMP3Decoder_init(const char * filepath,unsigned long start);
	int getAudioFileSize();
	void rePlayAudioFile();
#ifdef __cplusplus
}
#endif

jint JNI_libmad_NativeMP3Decoder_initAudioPlayer(JNIEnv *env, jobject obj, jstring jfilePath,jint startPos)
{
	const char* filePath = env->GetStringUTFChars(jfilePath, NULL);
    int ret = NativeMP3Decoder_init(filePath,startPos);
	if(ret == -1)
	{
		LOGE("%s: mp3 mad init failed",__FUNCTION__);
		env ->ReleaseStringUTFChars(jfilePath,filePath);
		ThrowErrnoException(env, "java/io/IOException", errno);
		return (jint)ret;
	}
	LOGD("%s: mp3 mad init ok----->ret: %d,filePath: %s,startPos: %d",__FUNCTION__,ret,filePath,startPos);
	env ->ReleaseStringUTFChars(jfilePath,filePath);
	return (jint)ret;
}

jint JNI_libmad_NativeMP3Decoder_getAudioBuf(JNIEnv *env, jobject obj ,jshortArray jaudioBuf,jint jlen)
{
	int bufsize = 0;
	int ret = 0;
    if(jaudioBuf != NULL)
	{
		bufsize = env->GetArrayLength(jaudioBuf);
        jshort *buffer = env->GetShortArrayElements(jaudioBuf,NULL);
		memset(buffer,0,bufsize*2);
		ret = NativeMP3Decoder_readSamples(buffer, jlen);
		env->ReleaseShortArrayElements(jaudioBuf,buffer,0);
	}
	else
	{
		LOGE("%s: 传入的数组为空",__FUNCTION__);
	}
	return ret;
}

jint JNI_libmad_NativeMP3Decoder_getAudioSamplerate(JNIEnv *env,jobject obj)
{
	return NativeMP3Decoder_getAudioSamplerate();
}

jint JNI_getAudioFileSize(JNIEnv *env,jobject obj)
{
	return (jint)getAudioFileSize();
}

void JNI_rePlayAudioFile(JNIEnv *env,jobject obj)
{
	rePlayAudioFile();
}

void JNI_libmad_NativeMP3Decoder_closeAudioFile(JNIEnv *env,jobject obj)
{
	NativeMP3Decoder_closeAudioFile();
}


//JAVA函数和C++函数映射关系表
static JNINativeMethod gMethods[] = {
	{"initAudioPlayer","(Ljava/lang/String;I)I",(void*)JNI_libmad_NativeMP3Decoder_initAudioPlayer},
	{"getAudioBuf","([SI)I",(void*)JNI_libmad_NativeMP3Decoder_getAudioBuf},
    {"getAudioSamplerate","()I",(void*)JNI_libmad_NativeMP3Decoder_getAudioSamplerate},
    {"closeAudioFile","()V",(void*)JNI_libmad_NativeMP3Decoder_closeAudioFile},
	{"getAudioFileSize","()I",(void*)JNI_getAudioFileSize},
	{"rePlayAudioFile","()V",(void*)JNI_rePlayAudioFile},
 };

//定义java中类全名
static const char* classPathName = "com/example/zhongjihao/mp3codecandroid/mp3codec/Mp3DecoderJni";

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

