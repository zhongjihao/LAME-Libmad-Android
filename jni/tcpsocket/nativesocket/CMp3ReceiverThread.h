/*************************************************************************
    > File Name: CMp3ReceiverThread.h
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年06月29日 星期一 14时16分31秒
 ************************************************************************/
#ifndef _CMP3_RECEVIER_THREAD_H_
#define _CMP3_RECEVIER_THREAD_H_

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <fcntl.h>
#include <unistd.h>
#include "../../baseclass/Thread.h"

#define FILE_NAME_LEN 256

class CMp3ReceiverThread : public Thread
{
private:
	int m_sfd;
	int m_cfd;
	char outpath[FILE_NAME_LEN]; //文件存放目录
	bool isStop;
	char m_ip[20];
	short m_port;
	static char targetFile[FILE_NAME_LEN];//保存的文件全路经
private:
	CMp3ReceiverThread(CMp3ReceiverThread& oth);
	CMp3ReceiverThread& operator=(const CMp3ReceiverThread& oth);
    int receiverData();
public:
	CMp3ReceiverThread(const char* ip,int port);
	~CMp3ReceiverThread();
	int initSocket();
	void setRecvDir(const char* sddir);
	virtual void run();
	friend void threadExitHandle(void *d);
};

extern void JNI_RecvFileDoneNotify(char *targetFile);

#endif

