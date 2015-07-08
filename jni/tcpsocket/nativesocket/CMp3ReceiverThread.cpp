/*************************************************************************
    > File Name: CMp3ReceiverThread.cpp
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年06月29日 星期一 14时40分54秒
 ************************************************************************/
#include "CMp3ReceiverThread.h"

char CMp3ReceiverThread::targetFile[FILE_NAME_LEN] = {0};

CMp3ReceiverThread::CMp3ReceiverThread(const char*ip,int port)
{
	LOGD("=======CMp3ReceiverThread()======");
	bzero(m_ip,sizeof(m_ip));
	memcpy(m_ip,ip,strlen(ip));
	m_port = port;
	m_sfd = -1;
	m_cfd = -1;
	isStop = false;
}

CMp3ReceiverThread::~CMp3ReceiverThread()
{
	if(m_cfd != -1)
		close(m_cfd);
	if(m_sfd != -1)
		close(m_sfd);
	isStop = true;
	LOGD("===========~CMp3ReceiverThread()=========");
}

int CMp3ReceiverThread::initSocket()
{
	m_sfd = socket(AF_INET,SOCK_STREAM,6);
	if(m_sfd == -1)
	{
		LOGE("=====CMp3ReceiverThread====建立socket失败====");
		perror("====CMp3ReceiverThread===socket()====");
		return m_sfd;
	}
	LOGD("========CMp3ReceiverThread====socket成功======ip: %s,port: %d",m_ip,m_port);
	struct sockaddr_in dr;
	bzero(&dr,sizeof(dr));
	dr.sin_family = AF_INET;
	dr.sin_port = htons(m_port);
	inet_aton(m_ip,&dr.sin_addr);

	int ret = bind(m_sfd,(struct sockaddr*)&dr,sizeof(dr));
	if(ret == -1)
	{
		LOGE("====CMp3ReceiverThread====bind失败=======");
		perror("===CMp3ReceiverThread====bind()======");
		close(m_sfd);
		m_sfd = -1;
		return ret;
	}
	LOGD("======CMp3ReceiverThread=====bind成功======ip: %s,port: %d",m_ip,m_port);

	ret = listen(m_sfd,10);
	if(ret == -1)
	{
		LOGE("=======CMp3ReceiverThread=====listen失败==========");
		perror("=====CMp3ReceiverThread=====listen()======");
		close(m_sfd);
		m_sfd = -1;
		return ret;
	}
    LOGD("=====CMp3ReceiverThread======listen成功======");
	return m_sfd;
}

int CMp3ReceiverThread::receiverData()
{
    if(m_cfd == -1)
	{
		m_cfd = accept(m_sfd,NULL,NULL);
	    if(m_cfd == -1)
	    {
			LOGE("====CMp3ReceiverThread=====accept失败==========");
		    perror("===CMp3ReceiverThread====accept()==========");
		    close(m_sfd);
			m_sfd = -1;
			isStop = true;
		    return m_cfd;
	    }
	    LOGD("=====CMp3ReceiverThread=====accept成功=====");
	}

	int recv_len;
	char buf[1024];
	char filename[100];
	bzero(filename,sizeof(filename));
	int total = 0;
	int ret;
//	char savePath[256];
	LOGD("=======CMp3ReceiverThread====receiverData===接收文件名长度============");
    ret = recv(m_cfd,&recv_len,sizeof(recv_len),MSG_WAITALL);
	if(ret == 0)
	{
		LOGD("=====CMp3ReceiverThread=======receiverData====接收文件名长度时有客户退出===========");
        close(m_cfd);
		m_cfd = -1;
		return ret;
	}
	if(ret == -1)
	{
		LOGE("====CMp3ReceiverThread========receiverData====接收文件名长度时网络故障===========");
		perror("==CMp3ReceiverThread========recv()文件名长度=======");
		close(m_cfd);
		close(m_sfd);
		m_cfd = -1;
		m_sfd = -1;
		isStop = true;
		return ret;
	}
    LOGD("=======CMp3ReceiverThread=====receiverData===接收文件名=======");
	ret = recv(m_cfd,filename,recv_len,MSG_WAITALL);
    if(ret == 0)
	{
		LOGD("=====CMp3ReceiverThread=======receiverData====接收文件名时有客户退出===========");
        close(m_cfd);
		m_cfd = -1;
		return ret;
	}
    if(ret == -1)
	{
		LOGE("====CMp3ReceiverThread========receiverData====接收文件名时网络故障===========");
		perror("==CMp3ReceiverThread========recv()文件名=======");
		close(m_cfd);
		close(m_sfd);
		m_cfd = -1;
		m_sfd = -1;
		isStop = true;
		return ret;
	}
	LOGD("=======CMp3ReceiverThread====目录: %s, 接收的文件名: %s,接受文件名长度: %d",outpath,filename,recv_len);
//	bzero(savePath,sizeof(savePath));
    bzero(targetFile,sizeof(targetFile));
//	memcpy(savePath,outpath,strlen(outpath));
    memcpy(targetFile,outpath,strlen(outpath));
//	strcat(savePath,filename);
    strcat(targetFile,filename);
//	int ffd = open(savePath,O_RDWR|O_CREAT,0666);
    int ffd = open(targetFile,O_RDWR|O_CREAT,0666);
    if(ffd == -1)
	{
		LOGE("=====CMp3ReceiverThread====open文件失败=======savePath: %s",targetFile);
		perror("===CMp3ReceiverThread====open()======");
		close(m_cfd);
		close(m_sfd);
		m_cfd = -1;
		m_sfd = -1;
		isStop = true;
        return ffd; 
	}
	LOGD("=====CMp3ReceiverThread====open文件成功=====文件名: %s",targetFile);
	while(1)
	{	
		ret = recv(m_cfd,&recv_len,sizeof(recv_len),MSG_WAITALL);
		LOGD("=====CMp3ReceiverThread====接收的字节数: %d",recv_len);
		if(ret == 0)
		{
			LOGD("======CMp3ReceiverThread===接收数据长度时有客户退出====");
			close(m_cfd);
			m_cfd = -1;
			break;
		}
        if(ret == -1)
		{
			LOGE("=====CMp3ReceiverThread=====接收数据长度时网络故障=======");
			perror("===CMp3ReceiverThread======recv数据长度=================");
			close(m_cfd);
			close(m_sfd);
			m_cfd = -1;
			m_sfd = -1;
			isStop = true;
			break;
		}
        if(recv_len == 0)
		{
			LOGD("=====CMp3ReceiverThread====收到文件结束标志============");
			break;
		}
		memset(buf,0,sizeof(buf));
		ret = recv(m_cfd,buf,recv_len,MSG_WAITALL);
		if(ret >0)
		{
			int r = write(ffd,buf,recv_len);
			if(r == -1)
			{
                LOGE("====CMp3ReceiverThread===写入文件出错==========");
				perror("====CMp3ReceiverThread==write()======================");
				close(m_cfd);
				close(m_sfd);
				m_cfd = -1;
				m_sfd = -1;
				isStop = true;
				break;
            }
			total += r;
		}
		if(ret == 0)
		{
			LOGD("===CMp3ReceiverThread======接受数据内容时有客户退出=======");
			close(m_cfd);
			m_cfd = -1;
			break;
		}
		if(ret == -1)
		{
			LOGE("===CMp3ReceiverThread=====接收数据内容时网络故障==========");
			perror("===CMp3ReceiverThread===recv数据内容====================");
	        close(m_cfd);
			close(m_sfd);
			m_sfd = -1;
			m_cfd = -1;
			isStop = true;
			break;
		}
	}
    close(ffd);
	return total;
}

void threadExitHandle(void *d)
{
	LOGD("======CMp3ReceiverThread======threadExitHandle========");
	JNI_RecvFileDoneNotify(CMp3ReceiverThread::targetFile);
}

void CMp3ReceiverThread::run()
{
	while(!isStop)
	{
		LOGD("==========CMp3ReceiverThread====run=====");
		pthread_cleanup_push(threadExitHandle,NULL);
		int count = receiverData();
        LOGD("====CMp3ReceiverThread====总共接收的字节数:%d========",count);
		pthread_cleanup_pop(1);
	}
}

void CMp3ReceiverThread::setRecvDir(const char* sddir)
{
	int len = strlen(sddir);
	bzero(outpath,sizeof(outpath));
	memcpy(outpath,sddir,len);
}
