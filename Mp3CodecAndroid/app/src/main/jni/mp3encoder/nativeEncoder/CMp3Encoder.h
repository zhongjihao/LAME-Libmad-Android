/*************************************************************************
    > File Name: CMp3Encoder.h
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年03月28日 星期六 17时49分27秒
 ************************************************************************/

#ifndef _CMP3_ENCODER_H_
#define _CMP3_ENCODER_H_

#include <unistd.h>
#include <cstring>
#include "../libmp3lame/lame.h"


#define BUFFER_SIZE 8192
#define be_short(s) ((short) ((unsigned short) (s) << 8) | ((unsigned short) (s) >> 8))


class CMp3Encoder
{
private:
	lame_t lame;
public:
	CMp3Encoder();
	~CMp3Encoder();
    int initEncoder(int inChannelNum,int inSamplerate,int outSamplerate,int outBitrate,int mode,int quality);
	int encoder(short* pcm_l,short* pcm_r,int nsamples,unsigned char *mp3buf, int mp3buf_size);
	int flush(unsigned char *mp3buf, int mp3buf_size);
private:
	CMp3Encoder(CMp3Encoder& oth);
	CMp3Encoder& operator=(const CMp3Encoder& oth);
};


#endif

