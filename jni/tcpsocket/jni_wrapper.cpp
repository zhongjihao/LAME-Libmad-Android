#include "../baseclass/jni_register.h"
#include "./nativesocket/CMp3SendThread.h"
#include "./nativesocket/CMp3ReceiverThread.h"

static JavaVM              *g_vm      = NULL;
static jclass               g_cls     = NULL;
static jobject              g_obj     = NULL;
static CMp3SendThread      *p_send_th = NULL;
static CMp3ReceiverThread  *p_recv_th = NULL;

void JNI_initGlobalObject(JNIEnv *env,jobject jobj)
{
	jclass tmp = env ->GetObjectClass(jobj);
	g_cls = (jclass)env ->NewGlobalRef(tmp);
	g_obj = env ->NewGlobalRef(jobj);
}

void JNI_destroyGlobalObject(JNIEnv* env,jobject jobj)
{
	env ->DeleteGlobalRef(g_obj);
	env ->DeleteGlobalRef(g_cls);
	g_vm = NULL;
}

void JNI_TcpConnect(JNIEnv *env,jobject jobj,jstring remoteIp,jint port)
{
	if(p_send_th == NULL)
	{
		const char* ip = env ->GetStringUTFChars(remoteIp,NULL);
		p_send_th = new CMp3SendThread(ip,(int)port);
		env ->ReleaseStringUTFChars(remoteIp,ip);
		int ret = p_send_th ->initSocket();
        if(ret == -1)
		{
			LOGE("====JNI====tcpConnect  error========");
			delete p_send_th;
			p_send_th = NULL;
			ThrowErrnoException(env, "java/io/IOException", errno);
			return;
		}
	}
	else
	{
		jstring msg = env ->NewStringUTF("TCP Client已连接!");
		jclass cls = env ->GetObjectClass(jobj);
		jmethodID mid = env ->GetMethodID(cls,"showToast","(Ljava/lang/String;)V");
		if(mid == NULL)
		{
			env ->DeleteLocalRef(msg);
			env ->DeleteLocalRef(cls);
			ThrowException(env,"java/lang/RuntimeException","====JNI====Unable find method showToast======");
			return;
		}
		env ->CallVoidMethod(jobj,mid,msg);
		env ->DeleteLocalRef(msg);
		env ->DeleteLocalRef(cls);
	}
}

void JNI_setRecvDir(JNIEnv *env,jobject jobj,jstring sddir)
{
	if(p_recv_th != NULL)
	{
 	    const char* recvDir =  env->GetStringUTFChars(sddir, NULL);
        p_recv_th->setRecvDir(recvDir);
	    env->ReleaseStringUTFChars(sddir,recvDir);
	}
}

void JNI_setSendFilePath(JNIEnv *env,jobject jobj,jstring filepath)
{
	if(p_send_th != NULL)
	{
 	    const char* filename =  env->GetStringUTFChars(filepath, NULL);
		p_send_th ->setOutpath(filename);
	    env->ReleaseStringUTFChars(filepath,filename);
	}
}

void JNI_TcpBind(JNIEnv* env,jobject jobj,jstring localIp,jint port)
{
	if(p_recv_th == NULL)
	{
		const char* ip = env ->GetStringUTFChars(localIp,NULL);
		LOGD("=======JNI====TcpBind======ip:%s,port: %d",ip,port);
		p_recv_th = new CMp3ReceiverThread(ip,(int)port);
        env ->ReleaseStringUTFChars(localIp,ip);
		int ret = p_recv_th ->initSocket();
		if(ret == -1)
		{
			LOGE("====JNI====tcpBind  error========");
			delete p_recv_th;
			p_recv_th = NULL;
			ThrowErrnoException(env, "java/io/IOException", errno);
			return;
		}

	    if(!p_recv_th ->start())
	    {
			LOGE("===JNI====Receiver pThread  create failed======");
		    delete p_recv_th;
		    p_recv_th = NULL;
			ThrowException(env,"java/lang/RuntimeException","====JNI===Mp3ReceiverThread create failed====");
			return;
	    } 
	}
	else
	{
		jstring msg = env ->NewStringUTF("TCP Server已绑定!");
		jclass cls = env ->GetObjectClass(jobj);
		jmethodID mid = env ->GetMethodID(cls,"showToast","(Ljava/lang/String;)V");
		if(mid == NULL)
		{
			env ->DeleteLocalRef(msg);
			env ->DeleteLocalRef(cls);
			ThrowException(env,"java/lang/RuntimeException","====JNI====Unable find method showToast======");
			return;
		}
		env ->CallVoidMethod(jobj,mid,msg);
		env ->DeleteLocalRef(msg);
		env ->DeleteLocalRef(cls);
	}
}

void JNI_startSendThread(JNIEnv *env,jobject jobj)
{
	LOGD("===JNI===开始启动发送线程=======");
	if(p_send_th != NULL)
	{
		if(!p_send_th ->start())
		{
			LOGE("======JNI===sendThread create failed======");
			delete p_send_th;
			p_send_th = NULL;
			ThrowException(env,"java/lang/RuntimeException","====JNI===Mp3SendThread create failed====");
			return;
		}
	}
}

void JNI_destroyTcpSocket(JNIEnv *env,jobject jobj)
{
	if(p_send_th != NULL)
	{
		delete p_send_th;
		p_send_th = NULL;
	}
	if(p_recv_th != NULL)
	{
		delete p_recv_th;
		p_recv_th = NULL;
	}
}

JNIEnv* getJNIEnv(bool* needsDetach)
{
	JNIEnv* env = NULL;
	if(g_vm ->GetEnv((void**)&env,JNI_VERSION_1_4) != JNI_OK)
	{
		int status = g_vm ->AttachCurrentThread(&env,NULL);
		if(status < 0)
		{
			LOGE("=====JNI====getJNIEnv===failed to attach current thread=====");
			return NULL;
		}
		*needsDetach = true;
	}
	LOGD("=====JNI===getJNIEnv  Successfull=======");
	return env;
}

void JNI_RecvFileDoneNotify(char * recvfile)
{
	bool needsDetach = false;
	JNIEnv* env = getJNIEnv(&needsDetach);
	if(env != NULL)
	{
		if(g_obj != NULL)
		{
			jstring audiofile = env ->NewStringUTF(recvfile);
			jmethodID mid = env ->GetMethodID(g_cls,"recvAudioDone","(Ljava/lang/String;)V");
			env ->CallVoidMethod(g_obj,mid,audiofile);
			env ->DeleteLocalRef(audiofile);
		}
		if(needsDetach)
		{
			g_vm ->DetachCurrentThread();
		}
	}
}

//JAVA函数和C++函数映射关系表
static JNINativeMethod gMethods[] = {
	{ "tcpConnect","(Ljava/lang/String;I)V",(void*)JNI_TcpConnect},
	{ "tcpBind","(Ljava/lang/String;I)V",(void*)JNI_TcpBind},
	{ "setRecvDir","(Ljava/lang/String;)V",(void*)JNI_setRecvDir},
	{ "setSendFilePath","(Ljava/lang/String;)V",(void*)JNI_setSendFilePath},
	{ "startSendThread","()V",(void*)JNI_startSendThread},
	{ "destroyTcpSocket","()V",(void*)JNI_destroyTcpSocket},
	{ "initGlobalObject","()V",(void*)JNI_initGlobalObject},
	{ "destroyGlobalObject","()V",(void*)JNI_destroyGlobalObject},
};

//定义java中类全名
static const char* classPathName = "com/mp3/android/nativeJNI/NativeTcpSocket";

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

