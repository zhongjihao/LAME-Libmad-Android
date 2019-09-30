/*************************************************************************
    > File Name: CMp3Encoder.cpp
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年03月28日 星期六 17时57分24秒
 ************************************************************************/
#define LOG_TAG "Mp3Encoder"

#include "CMp3Encoder.h"
#include "../../baseclass/log.h"

CMp3Encoder::CMp3Encoder():lame(NULL)
{
	LOGD("%s: ======zhongjihao=======",__FUNCTION__);
}

CMp3Encoder::~CMp3Encoder()
{
	LOGD("%s: ========zhongjihao=====",__FUNCTION__);
    destroyEncoder();
}

int CMp3Encoder::initEncoder(int channelnum,int samplerate,int brate,int mode,int quality)
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
	LOGD("%s:====zhongjihao====Init parameters===",__FUNCTION__);
	lame_set_num_channels(lame, channelnum);
	LOGD("%s:====zhongjihao===Number of channels: %d", __FUNCTION__,channelnum);
	lame_set_in_samplerate(lame, samplerate);
	LOGD("%s:====zhongjihao===Sample rate: %d", __FUNCTION__,samplerate);
	lame_set_brate(lame, brate);
	LOGD("%s:====zhongjihao===Bitrate: %d", __FUNCTION__,brate);
	lame_set_mode(lame, *(reinterpret_cast<MPEG_mode*>(&mode)));
	LOGD("%s:====zhongjihao===Mode: %d", __FUNCTION__,mode);
	lame_set_quality(lame, quality);
	LOGD("%s:====zhongjihao===Quality: %d", __FUNCTION__,quality);
												
	int res = lame_init_params(lame);
	LOGD("%s:====zhongjihao===Init returned: %d", __FUNCTION__,res);
	return res;
}


int CMp3Encoder::encoder(short* pcm_l,short* pcm_r,int nsamples,unsigned char* mp3buf, int mp3buf_size)
{
	int nb_total = 0;
	LOGD("%s:====zhongjihao===Encoding started",__FUNCTION__);
	nb_total = lame_encode_buffer(lame, pcm_l, pcm_r, nsamples, mp3buf,mp3buf_size);
	LOGD("%s:====zhongjihao===Encoded %d bytes", __FUNCTION__,nb_total);
    return nb_total;
}

int CMp3Encoder::flush(unsigned char* mp3buf, int mp3buf_size)
{
	int nb_total = lame_encode_flush(lame, mp3buf, mp3buf_size);
	LOGD("%s:====zhongjihao====Flushed %d bytes", __FUNCTION__,nb_total);
}

void CMp3Encoder::destroyEncoder()
{
	int res = lame_close(lame);
	lame = NULL;
	LOGD("%s:=====zhongjihao===Deinit returned: %d", __FUNCTION__,res);
}

