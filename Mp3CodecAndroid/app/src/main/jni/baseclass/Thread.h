/*************************************************************************
    > File Name: Thread.h
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年03月27日 星期五 18时41分50秒
 ************************************************************************/

#ifndef _THREAD_H_
#define _THREAD_H_

#include <pthread.h>

class Thread
{
private:
	pthread_t       tid;
private:
	Thread(Thread& oth);
	Thread& operator=(const Thread& oth);
	void run1();                        //内部函数
	static void* threadFunc(void* pvoid); //线程运行的函数指针
public:
	Thread();
	virtual ~Thread();
	virtual void run() = 0;  //线程运行实体
	bool start();            //开始执行线程
	void join();             //等待线程直至退出
};

#endif
