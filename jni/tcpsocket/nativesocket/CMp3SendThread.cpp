/*************************************************************************
    > File Name: CMp3SendThread.cpp
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年03月27日 星期五 12时53分23秒
 ************************************************************************/
#include "CMp3SendThread.h"

CMp3SendThread::CMp3SendThread(const char* ip,int port):filepath(NULL)
{
	LOGD("======CMp3SendThread======");
	bzero(m_ip,sizeof(m_ip));
	memcpy(m_ip,ip,strlen(ip));
	m_port = port;
	m_sfd = -1;
}

CMp3SendThread::~CMp3SendThread()
{
	if(m_sfd != -1)
		close(m_sfd);
	LOGD("=========~CMp3SendThread()=======");
}

int CMp3SendThread::initSocket()
{
	m_sfd = socket(AF_INET,SOCK_STREAM,6);
	if(m_sfd == -1)
	{
		LOGD("====CMp3SendThread====建立socket失败========");
		perror("=====CMp3SendThread====socket()============");
		return m_sfd;
	}
	LOGD("========建立socket成功=======");
	struct sockaddr_in dr;
	bzero(&dr,sizeof(dr));
	dr.sin_family = AF_INET;
	dr.sin_port = htons(m_port);
    inet_aton(m_ip,&dr.sin_addr);
	int ret = connect(m_sfd,(struct sockaddr*)&dr,sizeof(dr));
	if(ret == -1)
	{
		LOGD("=====CMp3SendThread======connect连接失败=========");
		perror("=====CMp3SendThread===connect()====");
		close(m_sfd);
		m_sfd = -1;
		return ret;
	}
	LOGD("=======connect连接成功===========ip: %s,port: %d",m_ip,m_port);
	return m_sfd;
}

int CMp3SendThread::openFile(const char* filename)
{
	return open(filename,O_RDONLY);
}

int CMp3SendThread::sendData(const char* file)
{
	LOGD("=======CMp3SendThread====sendData======");
	char *name = strrchr(file,'/'); 
	int len = strlen(name);//取得文件名长度
	int ret;
	//发送文件名长度
	ret = send(m_sfd,&len,sizeof(len),0);
	if(ret == -1)
	{
		LOGE("=======CMp3SendThread=====发送文件名长度失败========");
		perror("=====CMp3SendThread=====send()文件名长度=====");
		close(m_sfd);
		m_sfd = -1;
		return ret;
	}
	
	//发送文件名
	ret = send(m_sfd,name,len,0);
	if(ret == -1)
	{
		LOGE("=====CMp3SendThread========send发送文件名%s失败======",name);
        perror("===CMp3SendThread=======send()文件名============");
		close(m_sfd);
		m_sfd = -1;
        return ret;
	}
	LOGD("===CMp3SendThread======send发送文件名成功=======file: %s",file);

	int ffd = openFile(file);
	if(ffd == -1)
	{
		LOGE("====CMp3SendThread=====open打开文件%s失败=====",file);
		perror("=====CMp3SendThread======open()文件======");
		close(m_sfd);
		m_sfd = -1;
		return ffd;
	}
    char buf[SEND_BUFFER_SIZE];
	int size = 0;
	int count = 0;
	
	//循环发送文件内容
    while(1)
	{
	   memset(buf,0,sizeof(buf));
       size = read(ffd,buf,sizeof(buf)-1);
	   if(size == -1)
	   {
		   LOGE("====CMp3SendThread=====read出错============");
		   perror("===CMp3SendThread=====read()======");
		   close(m_sfd);
		   m_sfd = -1;
		   break;
	   }
	   if(size == 0) break;
	   if(size > 0)
	   {
		   ret = send(m_sfd,&size,sizeof(size),0);
		   if(ret == -1)
		   {
			   LOGE("====CMp3SendThread===send发送文件内容长度失败===");
			   perror("====CMp3SendThread===send()文件内容长度=====");
			   close(m_sfd);
			   m_sfd = -1;
			   break;
		   }
		   ret = send(m_sfd,buf,size,0);
		   LOGD("======CMp3SendThread===每次发送的字节数: %d",ret);
		   if(ret == -1)
		   {
			   LOGE("====CMp3SendThread=====send发送文件内容失败=======");
			   perror("====CMp3SendThread=====send()文件内容============");
			   close(m_sfd);
			   m_sfd = -1;
			   break;
		   }		   
		   count += ret;
	   }
	}
	//读到文件末尾,发送文件结束标志0
	int end = 0;
	send(m_sfd,&end,sizeof(end),0);
	LOGD("======CMp3SendThread====发送文件结束标志0===");
	
    close(ffd);
	return count;
}

void CMp3SendThread::setOutpath(const char* outpath)
{
	if(filepath != NULL)
	{
		delete[] filepath;
		filepath = NULL;
	}
	int len = strlen(outpath);
	filepath =  new char[len+1];
	memset(filepath,0,len+1);
	memcpy(filepath,outpath,len);
}

void CMp3SendThread::run()
{
	LOGD("========CMp3SendThred====执行发送线程=======");
//	pthread_mutex_lock(&g_mutex);
//	pthread_cond_wait(&g_cond,&g_mutex);
	int send_count = sendData(filepath);
//	pthread_mutex_unlock(&g_mutex);
    LOGD("======发送的数据大小总共为: %d========",send_count);
}
