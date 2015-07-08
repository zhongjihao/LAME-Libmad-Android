#include "../baseclass/jni_register.h"
#include "./nativeEncoder/CMp3EncoderThread.h"

static CMp3EncoderThread   *p_encode_th = NULL;
static JavaVM* g_vm = NULL;
static jclass g_cls = NULL;
static jobject g_obj = NULL;

void JNI_initGlobalObject(JNIEnv *env,jobject jobj)
{
	env ->GetJavaVM(&g_vm);
	jclass tem = env ->GetObjectClass(jobj);
	g_cls = (jclass)env ->NewGlobalRef(tem);
	g_obj = env ->NewGlobalRef(jobj);
}

void JNI_destroyGlobalObject(JNIEnv *env,jobject jobj)
{
	env ->DeleteGlobalRef(g_obj);
	env ->DeleteGlobalRef(g_cls);
	g_vm = NULL;

}

void JNI_initEncoder(JNIEnv *env,jobject jobj, jint in_num_channels, jint in_samplerate, jint in_brate,jint in_mode, jint in_quality) 
{
	if(p_encode_th == NULL)
	{
		p_encode_th = new CMp3EncoderThread;
	    int ret_code = p_encode_th ->init_lame(in_num_channels,in_samplerate,in_brate,in_mode,in_quality);
        if(ret_code < 0)
	    {
			LOGE("=====error during initialization======");
		    delete p_encode_th;
		    p_encode_th = NULL;
			//异常
			ThrowException(env,"java/lang/RuntimeException","=====JNI===Init lamemp3 failed=====");
			return;
	    }
	    LOGD("====JNI===init lamemp3 successfull==========");
	}
}

void JNI_destroyEncoder(JNIEnv *env, jobject jobj) 
{
	LOGD("====JNI=====destroyEncoder======");
	if(p_encode_th != NULL)
	{	
		delete p_encode_th;
		p_encode_th = NULL;
	}
}

jint JNI_encodeFile(JNIEnv *env,jobject jobj, jstring in_source_path, jstring in_target_path) 
{
	const char *source_path = NULL, *target_path = NULL;
	source_path = env->GetStringUTFChars(in_source_path, NULL);
	target_path = env->GetStringUTFChars(in_target_path, NULL);
    
	p_encode_th ->setEncoderFile(source_path,target_path);

    LOGD("====JNI===开始启动编码线程=====");
	if(!p_encode_th ->start())
	{
		LOGE("=======encoder pThread  create failed======");
		delete p_encode_th;
		p_encode_th = NULL;
        env->ReleaseStringUTFChars(in_source_path,source_path);
	    env->ReleaseStringUTFChars(in_target_path,target_path);
		ThrowException(env,"java/lang/RuntimeException","===JNI===Mp3EncoderThread create failed===");
		return -1;
	} 
    env->ReleaseStringUTFChars(in_source_path,source_path);
	env->ReleaseStringUTFChars(in_target_path,target_path);
	return 0;
}

JNIEnv* getJNIEnv(bool* needsDetach)
{
	JNIEnv* env = NULL;
	if(g_vm ->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK)
	{
		int status = g_vm ->AttachCurrentThread(&env, NULL);
		if(status < 0)
		{
			LOGE("====JNI======getJNIEnv======failed to attach current thread=======");
			return NULL;
		}
	    *needsDetach = true;
	}
	LOGD("====JNI====getJNIEnv=======Success======");
	return env;
}

void JNI_Mp3EncoderDoneCallback()
{
	bool needsDetach = false;
	JNIEnv* env = getJNIEnv(&needsDetach);
	if(env != NULL)
	{
		if(g_obj != NULL)
		{
			LOGD("=========JNI===调用上层java接口==1=====");
			jmethodID mid = env ->GetMethodID(g_cls,"mp3EncoderDone","()V");
			LOGD("===JNI====调用上层java=====2===========");
			env ->CallVoidMethod(g_obj,mid);
			LOGD("=====JNI===调用上层完毕=====3========");
		}
		if(needsDetach)
		{
			g_vm ->DetachCurrentThread();
		}
	}
}

//JAVA函数和C++函数映射关系表
static JNINativeMethod gMethods[] = {
	{ "initGlobalObject","()V",(void*)JNI_initGlobalObject},
	{ "destroyGlobalObject","()V",(void*)JNI_destroyGlobalObject},
	{ "initEncoder","(IIIII)V",(void*)JNI_initEncoder},
	{ "encodeFile","(Ljava/lang/String;Ljava/lang/String;)I",(void*)JNI_encodeFile},
    { "destroyEncoder","()V",(void*)JNI_destroyEncoder},
};

//定义java中类全名
static const char* classPathName = "com/mp3/android/nativeJNI/NativeMp3Encoder";

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

