#ifndef _JNI_REGISTER_H_
#define _JNI_REGISTER_H_

#include <jni.h>
#include <string.h>
#include <strings.h>
#include <errno.h>

#define MAX_LOG_MESSAGE_LENGTH 256 

//通过异常类和异常信息抛出异常
static void ThrowException(JNIEnv* env, const char* className, const char* message)
{
	jclass clazz = env->FindClass(className);
	if (clazz != NULL) 
	{
		env->ThrowNew(clazz, message);
		env->DeleteLocalRef(clazz); 
	}
}

//通过异常类和错误号抛出异常
static void ThrowErrnoException(JNIEnv* env, const char* className,int errnum)
{
	char buffer[MAX_LOG_MESSAGE_LENGTH];
	bzero(buffer,sizeof(buffer));
    //通过错误号获得错误消息
    if(-1 == strerror_r(errnum,buffer,MAX_LOG_MESSAGE_LENGTH))
	{
		bzero(buffer,sizeof(buffer));
		strerror_r(errno, buffer, MAX_LOG_MESSAGE_LENGTH); 
	}
	ThrowException(env, className, buffer);
}

static jint registerNativeMethods(JNIEnv* env,const char* className,JNINativeMethod* gMethods,int numMethods)
{
	jclass clazz;
	clazz = env ->FindClass(className);
	if(clazz == NULL)
	{
		return JNI_FALSE;
	}
	if(env->RegisterNatives(clazz,gMethods,numMethods) < 0)
	{
		env ->DeleteLocalRef(clazz);
		return JNI_FALSE;
	}
	env ->DeleteLocalRef(clazz);
	return JNI_TRUE;
}

static jint registerNatives(JNIEnv* env,const char* classPathName,JNINativeMethod *gMethods,int methodsNum)
{
	if(!registerNativeMethods(env,classPathName,gMethods,methodsNum))
	{
		return JNI_FALSE;
	}
	return JNI_TRUE;
}


#endif
