/*************************************************************************
    > File Name: Thread.cpp
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年03月27日 星期五 19时46分54秒
 ************************************************************************/

#define LOG_TAG "Thread"

#include "Thread.h"
#include "log.h"


//pthread_mutex_t g_mutex;
//pthread_cond_t g_cond;

Thread::Thread()
{
}

Thread::~Thread()
{
}

bool Thread::start()
{
	LOGD("========创建线程=========");
	return (pthread_create(&tid,NULL,threadFunc,this) == 0);
}

void Thread::join()
{
	LOGD("========join =======%lu",tid);
	if(tid > 0)
	{
		pthread_join(tid,NULL);
	}
}

void* Thread::threadFunc(void* pvoid)
{
	Thread* pThread = static_cast<Thread*>(pvoid);
	pThread ->run1();
	return NULL;
}

void Thread::run1()
{
	run();
}


