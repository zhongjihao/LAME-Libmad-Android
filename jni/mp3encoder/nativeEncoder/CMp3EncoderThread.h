/*************************************************************************
    > File Name: CMp3EncoderThread.h
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年03月28日 星期六 17时49分27秒
 ************************************************************************/

#ifndef _CMP3_ENCODER_THREAD_H_
#define _CMP3_ENCODER_THREAD_H_

#include <unistd.h>
#include <strings.h>
#include "../libmp3lame/lame.h"
#include "../../baseclass/Thread.h"


#define BUFFER_SIZE 8192
#define be_short(s) ((short) ((unsigned short) (s) << 8) | ((unsigned short) (s) >> 8))


class CMp3EncoderThread : public Thread
{
private:
	FILE* in_fp;
	FILE* out_fp;
	char* rawFile;       //录音的文件名
	char* encodedFile;   //编码后的文件名
	lame_t lame;
public:
	CMp3EncoderThread();
	~CMp3EncoderThread();
	virtual void run();
    int init_lame(int channelnum,int samplerate,int brate,int mode,int quality);
	void setEncoderFile(const char* infile,const char* outfile);
private:
	CMp3EncoderThread(CMp3EncoderThread& oth);
	CMp3EncoderThread& operator=(const CMp3EncoderThread& oth);
	int encoder();
	void destroy_lame();
	int openFile();
	static int read_samples(FILE* input_file,short* input);
	friend void threadExitHandle(void *d);
};

//extern pthread_mutex_t g_mutex;
//extern pthread_cond_t g_cond;
extern void JNI_Mp3EncoderDoneCallback();

#endif

