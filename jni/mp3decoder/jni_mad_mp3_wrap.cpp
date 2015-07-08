/*************************************************************************
    > File Name: jni_mad_mp3_wrap.cpp
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年07月03日 星期五 20时07分54秒
 ************************************************************************/
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

jint JNI_libmad_NativeMP3Decoder_initAudioPlayer(JNIEnv *env, jobject obj, jstring file,jint startAddr)
{
	const char* fileString = env->GetStringUTFChars(file, NULL);  
    int ret = NativeMP3Decoder_init(fileString,startAddr); 
	if(ret == -1)
	{
		LOGE("=======JNI===initAudioPlayer===error===");
		env ->ReleaseStringUTFChars(file,fileString);
		ThrowErrnoException(env, "java/io/IOException", errno);
		return (jint)ret;
	}
	env ->ReleaseStringUTFChars(file,fileString);
	return (jint)ret;
}

jint JNI_libmad_NativeMP3Decoder_getAudioBuf(JNIEnv *env, jobject obj ,jshortArray audioBuf,jint len)
{
	int bufsize = 0;
	int ret = 0;
    if(audioBuf != NULL)
	{
		bufsize = env->GetArrayLength(audioBuf);  
        jshort *_buf = env->GetShortArrayElements(audioBuf,NULL);  
		memset(_buf,0,bufsize*2);  
		ret = NativeMP3Decoder_readSamples(_buf, len);  
		env->ReleaseShortArrayElements(audioBuf,_buf,0);  
	}
	else
	{
		LOGE("====JNI===传入的数组为空====");
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
static const char* classPathName = "com/mp3/android/nativeJNI/NativeMP3Decoder";

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

