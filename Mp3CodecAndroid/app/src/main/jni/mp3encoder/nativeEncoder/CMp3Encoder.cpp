/*************************************************************************
    > File Name: CMp3Encoder.cpp
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年03月28日 星期六 17时57分24秒
 ************************************************************************/
#define LOG_TAG "Mp3Encoder"

#include "CMp3Encoder.h"
#include "../../baseclass/log.h"

CMp3Encoder::CMp3Encoder():lame(NULL),pcmPath(NULL),mp3Path(NULL),pcmFd(NULL),mp3Fd(NULL)
{
	LOGD("%s: ======zhongjihao=======",__FUNCTION__);
}

CMp3Encoder::~CMp3Encoder()
{
	int res = lame_close(lame);
	lame = NULL;
	LOGD("%s:=====zhongjihao===Deinit returned: %d", __FUNCTION__,res);
}

int CMp3Encoder::initEncoder(int inChannelNum,int inSamplerate,int outSamplerate,int outBitrate,int mode,int quality)
{
	if(lame != NULL)
	{
		lame_close(lame);
		lame = NULL;
	}
	lame = lame_init();
	if(lame == NULL)
	{
		LOGE("%s:====zhongjihao===lame_init failed===",__FUNCTION__);
		return -1;
	}
	LOGD("%s:====zhongjihao====Init parameters===",__FUNCTION__);
	lame_set_num_channels(lame, inChannelNum);//输入流的声道
	LOGD("%s:====zhongjihao===Number of channels: %d", __FUNCTION__,inChannelNum);
	lame_set_in_samplerate(lame, inSamplerate);
	LOGD("%s:====zhongjihao===InSample rate: %d", __FUNCTION__,inSamplerate);
	lame_set_out_samplerate(lame, outSamplerate);
	LOGD("%s:====zhongjihao===OutSample rate: %d", __FUNCTION__,outSamplerate);
	lame_set_brate(lame, outBitrate);
	LOGD("%s:====zhongjihao===Bitrate: %d", __FUNCTION__,outBitrate);
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
	LOGD("%s:E: ====zhongjihao====Flushed mp3addr: %p  mp3bytes: %d", __FUNCTION__,mp3buf,mp3buf_size);
	int nb_total = lame_encode_flush(lame, mp3buf, mp3buf_size);
	LOGD("%s:X: ====zhongjihao====Flushed %d bytes", __FUNCTION__,nb_total);
    return nb_total;
}

void CMp3Encoder::setEncoderSource(const char* infile,const char* outfile)
{
	int rawFileLen = strlen(infile);
	int encodedFileLen = strlen(outfile);
	pcmPath = new char[rawFileLen+1];
	mp3Path = new char[encodedFileLen+1];
	bzero(pcmPath,rawFileLen+1);
	bzero(mp3Path,encodedFileLen+1);
	memcpy(pcmPath,infile,rawFileLen);
	memcpy(mp3Path,outfile,encodedFileLen);
}

int CMp3Encoder::encoder()
{
	pcmFd = fopen(pcmPath,"rb");
	if(pcmFd == NULL){
		LOGE("%s:====zhongjihao====open %s failed!", __FUNCTION__,pcmPath);
		return -1;
	}

	mp3Fd = fopen(mp3Path,"wb");
	if(mp3Fd == NULL){
		LOGE("%s:====zhongjihao====open %s failed!", __FUNCTION__,mp3Path);
		return -1;
	}
}

int CMp3Encoder::read_samples(FILE *input_file, short *input)
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

