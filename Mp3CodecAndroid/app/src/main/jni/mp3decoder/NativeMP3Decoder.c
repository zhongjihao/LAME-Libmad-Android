/*************************************************************************
    > File Name: NativeMP3Decoder.c
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年07月03日 星期五 14时13分15秒
 ************************************************************************/

#define LOG_TAG "MP3_Decoder"

#include "../baseclass/log.h"
#include "NativeMP3Decoder.h"

static inline int readNextFrame(MP3FileHandle* mp3);
/** 
* Seeks a free handle in the handles array and returns its index or -1 if no handle could be found 
*/ 
static int findFreeHandle()
{
	int i = 0;
	for(i = 0; i < 100; i++)
	{
		if(handles[i] == 0)
		{
			return i;
		}
	}
	return -1;
}

static inline void closeHandle()  
{
	file_close(Handle->fd);  
	mad_synth_finish(&Handle->synth);  
	mad_frame_finish(&Handle->frame);  
	mad_stream_finish(&Handle->stream);  
	free(Handle);  
	Handle = NULL;  
}  

static inline signed short fixedToShort(mad_fixed_t Fixed)  
{
	if(Fixed >= MAD_F_ONE)
		return (SHRT_MAX_SHORT);  
	if(Fixed <= -MAD_F_ONE)
		return (-SHRT_MAX_SHORT);  
			  
	Fixed = Fixed>>(MAD_F_FRACBITS-15);  
	return ((signed short)Fixed);  
}  

int  NativeMP3Decoder_init(const char * filepath,unsigned long start)
{
	LOGD("%s: filePath: %s, start: %lu",__FUNCTION__,filepath,start);
    int fd = file_open(filepath,_FMODE_READ);
	if(fd == -1)
	{
		perror("===NativeMP3Decoder_init===open()=====");
		LOGE("%s: 打开文件失败",__FUNCTION__);
		return -1;
	}
	if(fd == -2)
	{
		LOGE("%s: file_open参数指定错误",__FUNCTION__);
		return 0;
	}
	MP3FileHandle* mp3Handle = (MP3FileHandle*)malloc(sizeof(MP3FileHandle));
    memset(mp3Handle,0,sizeof(MP3FileHandle));  
	mp3Handle->fd = fd;  
	mp3Handle->fileStartPos = start; 
	struct stat st;  
	int ret = fstat(fd, &st);
	if(ret < 0)
	{
		LOGE("%s: 获取文件大小失败",__FUNCTION__);
		perror("NativeMP3Decoder_init===fstat()");
		free(mp3Handle);
		file_close(fd);
		return -1;
	}
	mp3Handle->size = st.st_size; 
    file_seek(mp3Handle->fd,start,SEEK_SET);
    
	mad_stream_init(&mp3Handle->stream);  
	mad_frame_init(&mp3Handle->frame);  
    mad_synth_init(&mp3Handle->synth);  
	mad_timer_reset(&mp3Handle->timer);  

    Handle = mp3Handle;
	readNextFrame(Handle); 
    g_Samplerate = Handle->frame.header.samplerate; 
	LOGD("%s: 采样率: %d, 文件大小: %ld",__FUNCTION__,g_Samplerate,st.st_size);
	return 1;
}

static inline int readNextFrame(MP3FileHandle* mp3)
{
	do
	{
		if(mp3->stream.buffer == 0 || mp3->stream.error == MAD_ERROR_BUFLEN)
		{
			int inputBufferSize = 0;
            if(mp3->stream.next_frame != 0)
			{
				int leftOver = mp3->stream.bufend - mp3->stream.next_frame; 
				int i;
				for(i= 0;i<leftOver;i++)
				{
					mp3->inputBuffer[i] = mp3->stream.next_frame[i];
				}
				int readBytes = file_read(mp3->fd, mp3->inputBuffer+leftOver,INPUT_BUFFER_SIZE-leftOver);
                if(readBytes == 0)
					return 0;  
                inputBufferSize = leftOver + readBytes;
				mp3 ->fileStartPos += readBytes;
			}
			else
			{
				int readBytes = file_read(mp3->fd,mp3->inputBuffer,INPUT_BUFFER_SIZE);
				if(readBytes == 0)
					return 0;
				inputBufferSize = readBytes;
				mp3 ->fileStartPos += readBytes;
			}
            mad_stream_buffer(&mp3->stream,mp3->inputBuffer,inputBufferSize);  
			mp3->stream.error = MAD_ERROR_NONE;
		}
        
		if(mad_frame_decode(&mp3->frame,&mp3->stream))
		{
			 if( mp3->stream.error == MAD_ERROR_BUFLEN ||(MAD_RECOVERABLE(mp3->stream.error)))
				 continue;
			 else
				 return 0;
		}
		else
			break;
	}while(1);

    mad_timer_add(&mp3->timer, mp3->frame.header.duration);  
    mad_synth_frame(&mp3->synth,&mp3->frame);  
	mp3->leftSamples = mp3->synth.pcm.length;  
	mp3->offset = 0;  
				  
	return -1;  
}

int NativeMP3Decoder_readSamples(short *target, int size)
{
	MP3FileHandle* mp3 = Handle;
	int pos =0;  
	int idx = 0;
    
	while(idx != size)
	{
		 if(mp3->leftSamples > 0)
		 {
			 for( ;idx < size && mp3->offset < mp3->synth.pcm.length; mp3->leftSamples--, mp3->offset++ )
			 {
				  int value = fixedToShort(mp3->synth.pcm.samples[0][mp3->offset]);
                  if(MAD_NCHANNELS(&mp3->frame.header) == 2)
				  {
					  value += fixedToShort(mp3->synth.pcm.samples[1][mp3->offset]);  
					  value /= 2;  
				  }
				  target[idx++] = value; 
			 }
		 }
		 else
		 {
		     file_seek(mp3->fd,0,SEEK_CUR);
             int result = readNextFrame(mp3);
			 if(result == 0)
			 {
				 LOGD("%s: readNextFrame----->result: %d",__FUNCTION__,result);
				 return 0;
			 }
		 }
	}
    
    if(idx > size)
	{
		LOGD("%s: idx: %d, size: %d",__FUNCTION__,idx,size);
		return 0;
	}
	return mp3 ->fileStartPos; 
}

int NativeMP3Decoder_getAudioSamplerate()  
{
	return g_Samplerate;		  
}  

int getAudioFileSize()
{
	if(Handle != NULL)
	{
		return Handle->size;
	}
	return 0;
}

void rePlayAudioFile()
{
	Handle ->fileStartPos = 0;
    file_seek(Handle->fd,-1*(Handle->size),SEEK_END);
}

void  NativeMP3Decoder_closeAudioFile()  
{
	if(Handle != NULL)  
	{
		closeHandle();  
	}  
}

