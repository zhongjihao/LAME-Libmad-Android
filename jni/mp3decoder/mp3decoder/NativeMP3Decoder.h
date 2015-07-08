/*************************************************************************
    > File Name: NativeMP3Decoder.h
    > Author: zhongjihao
    > Mail: zhongjihao100@163.com 
    > Created Time: 2015年07月03日 星期五 17时19分54秒
 ************************************************************************/
#ifndef _NATIVE_MP3_DECODER_H
#define _NATIVE_MP3_DECODER_H

#include <stdio.h>  
#include <string.h>  
#include <stdlib.h> 
#include "mad/mad.h"
#include "NativeFileOperator.h"
#include "../../baseclass/log.h"


#define SHRT_MAX_SHORT (32767)
#define INPUT_BUFFER_SIZE	8192*5 /*(8192/4) */
#define OUTPUT_BUFFER_SIZE	8192 /* Must be an integer multiple of 4. */

/** 
* Struct holding the pointer to a wave file. 
*/
typedef struct  
{
	int size;  
	int64_t fileStartPos;  
	int fd;  
	struct mad_stream stream;  
	struct mad_frame frame;  
	struct mad_synth synth;  
	mad_timer_t timer;  
	int leftSamples;  
	int offset;  
	unsigned char inputBuffer[INPUT_BUFFER_SIZE];  
} MP3FileHandle;

/** static WaveFileHandle array **/ 
static MP3FileHandle* handles[100];

static MP3FileHandle* Handle;  
unsigned int g_Samplerate; 

int NativeMP3Decoder_init(const char* filepath,unsigned long start);
int NativeMP3Decoder_readSamples(short *target, int size);
void NativeMP3Decoder_closeAudioFile();
int NativeMP3Decoder_getAudioSamplerate();
int getAudioFileSize();
void rePlayAudioFile();

#endif

