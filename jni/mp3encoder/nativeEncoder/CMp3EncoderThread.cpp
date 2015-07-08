/*************************************************************************
    > File Name: CMp3EncoderThread.cpp
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年03月28日 星期六 17时57分24秒
 ************************************************************************/
#include "CMp3EncoderThread.h"

CMp3EncoderThread::CMp3EncoderThread():lame(NULL),in_fp(NULL),out_fp(NULL)
{
	LOGD("========CMp3EncoderThread()=============");	
}

CMp3EncoderThread::~CMp3EncoderThread()
{
	LOGD("========~CMp3EncoderThread()===========");
	if(in_fp != NULL)
	{
		fclose(in_fp);
		in_fp = NULL;
	}
	if(out_fp != NULL)
	{
		fclose(out_fp);
		out_fp = NULL;
	}
	destroy_lame();
}

int CMp3EncoderThread::init_lame(int channelnum,int samplerate,int brate,int mode,int quality)
{
	if(lame != NULL)
	{
		lame_close(lame);
		lame = NULL;
	}
	lame = lame_init();
	if(lame == NULL)
	{
		return -1;
	}
	LOGD("Init parameters:");
	lame_set_num_channels(lame, channelnum);
	LOGD("Number of channels: %d", channelnum);
	lame_set_in_samplerate(lame, samplerate);
	LOGD("Sample rate: %d", samplerate);
	lame_set_brate(lame, brate);
	LOGD("Bitrate: %d", brate);
	lame_set_mode(lame, *(reinterpret_cast<MPEG_mode*>(&mode)));
	LOGD("Mode: %d", mode);
	lame_set_quality(lame, quality);
	LOGD("Quality: %d", quality);
												
	int res = lame_init_params(lame);
	LOGD("Init returned: %d", res);	
	return res;
}

int CMp3EncoderThread::openFile()
{
	in_fp = fopen(rawFile,"rb");
	if(in_fp == NULL)
		return -1;
	out_fp = fopen(encodedFile,"wb");
	if(out_fp == NULL)
		return -1;
	return 0;
}

void CMp3EncoderThread::setEncoderFile(const char* infile,const char* outfile)
{
	int rawFileLen = strlen(infile);
	int encodedFileLen = strlen(outfile);
	rawFile = new char[rawFileLen+1];
	encodedFile = new char[encodedFileLen+1];
	bzero(rawFile,rawFileLen+1);
	bzero(encodedFile,encodedFileLen+1);
	memcpy(rawFile,infile,rawFileLen);
	memcpy(encodedFile,outfile,encodedFileLen);
}

int CMp3EncoderThread::encoder()
{
	short input[BUFFER_SIZE];
	unsigned char output[BUFFER_SIZE];
	int nb_read = 0;
	int nb_write = 0;
	int nb_total = 0;

	LOGD("Encoding started");
	while (nb_read = read_samples(in_fp, input)) 
	{
        nb_write = lame_encode_buffer(lame, input, input, nb_read, 
				                      output,BUFFER_SIZE);
	    fwrite(output, nb_write, 1, out_fp);
		nb_total += nb_write;
	}
	LOGD("Encoded %d bytes", nb_total);
									
	nb_write = lame_encode_flush(lame, output, BUFFER_SIZE);
	fwrite(output, nb_write, 1, out_fp);
	LOGD("Flushed %d bytes", nb_write);
			
	if(in_fp != NULL)
	{
		fclose(in_fp);
		unlink(rawFile);
		delete[] rawFile;
		rawFile = NULL;
		in_fp = NULL;
	}
	if(out_fp != NULL)
	{
		fclose(out_fp);
		delete[] encodedFile;
		encodedFile = NULL;
		out_fp = NULL;
	}
    return nb_total+nb_write;
}

int CMp3EncoderThread::read_samples(FILE *input_file, short *input) 
{
	int nb_read;
	nb_read = fread(input, 1, sizeof(short), input_file) / sizeof(short);

	int i = 0;
	while (i < nb_read) 
	{
		input[i] = be_short(input[i]);
		i++;
	}
	return nb_read;
}

void CMp3EncoderThread::destroy_lame()
{
	int res = lame_close(lame);
	lame = NULL;
	LOGD("Deinit returned: %d", res);
}

void threadExitHandle(void *d)
{
	LOGD("======CMp3EncoderThread exit callback========");
	JNI_Mp3EncoderDoneCallback();
}

void CMp3EncoderThread::run()
{
	LOGD("======CMp3EncoderThread====执行编码线程===1===");
//	pthread_mutex_lock(&g_mutex);
    pthread_cleanup_push(threadExitHandle,NULL);
    int ret = openFile();
	if(ret == -1)
	{
		LOGD("====CMp3EncoderThread===打开文件失败====");
//	    pthread_cond_signal(&g_cond);
//	    pthread_mutex_unlock(&g_mutex);
		return;
	}
	LOGD("======CMp3EncoderThread====执行编码线程===2===");
	int count = encoder();
	LOGD("======CMp3EncoderThread====执行编码线程===3===");
//	pthread_cond_signal(&g_cond);
//	pthread_mutex_unlock(&g_mutex);
	LOGD("====编码的字节数:%d",count);
	pthread_exit(NULL);
	pthread_cleanup_pop(0);
}

