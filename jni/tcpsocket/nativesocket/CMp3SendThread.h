/*************************************************************************
    > File Name: CMp3SendThread.h
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年03月27日 星期五 11时40分53秒
 ************************************************************************/

#ifndef _CMP3_SEND_THREAD_H_
#define _CMP3_SEND_THREAD_H_

#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include <strings.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <fcntl.h>
#include "../../baseclass/Thread.h"

#define SEND_BUFFER_SIZE 128

class CMp3SendThread : public Thread
{
private:
	int m_sfd;
	char* filepath;
	char m_ip[20];
	short m_port;
private:
	CMp3SendThread(CMp3SendThread& oth);
	CMp3SendThread& operator=(const CMp3SendThread& oth);
	int openFile(const char* filename);
	int sendData(const char* file);
public:
	CMp3SendThread(const char* ip,int port);
	~CMp3SendThread();
	virtual void run();
	int initSocket();
	void setOutpath(const char* file);
};

//extern pthread_mutex_t g_mutex;
//extern pthread_cond_t g_cond;

#endif

