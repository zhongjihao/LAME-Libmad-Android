//
// Created by zhongjihao100@163.com on 20/10/19.
//

#define LOG_TAG "WavEncoder"

#include "WavEncoder.h"
#include "../baseclass/log.h"
#include <string.h>

WavEncoder::WavEncoder(const char* wavPath)
{
    LOGD("%s: ======zhongjihao=======",__FUNCTION__);
    wavFile = fopen(wavPath,"w+");
    if(wavFile == NULL)
    {
        LOGE("%s: create wav file error\n",__FUNCTION__);
    }
}

WavEncoder::~WavEncoder()
{
    LOGD("%s: ========zhongjihao=====",__FUNCTION__);
    if(wavFile != NULL){
        fclose(wavFile);
        wavFile = NULL;
    }
}

void WavEncoder::initWavEncoder(int channelNum,int samplerate,int bytesPerSample)
{
    LOGD("%s: ========zhongjihao=====",__FUNCTION__);
    //WAVE_HEADER
    memcpy(wavHEADER.ChunkID,"RIFF",strlen("RIFF"));
    memcpy(wavHEADER.Format,"WAVE",strlen("WAVE"));

    //WAVE_FMT
    wavFMT.SampleRate = samplerate;
    wavFMT.NumChannels = channelNum;
    wavFMT.BitsPerSample = bytesPerSample*8;
    wavFMT.ByteRate = wavFMT.SampleRate * wavFMT.NumChannels * wavFMT.BitsPerSample / 8;
    memcpy(wavFMT.Subchunk1ID,"fmt ",strlen("fmt "));
    wavFMT.Subchunk1Size = sizeof(WAVE_FMT) - sizeof(wavFMT.Subchunk1ID) - sizeof(wavFMT.Subchunk1Size);
    wavFMT.BlockAlign = wavFMT.NumChannels * wavFMT.BitsPerSample / 8;
    wavFMT.AudioFormat = 1;

    //WAVE_DATA
    memcpy(wavDATA.Subchunk2ID,"data",strlen("data"));
    wavDATA.Subchunk2Size = 0;

    if(wavFile != NULL){
        fseek(wavFile,sizeof(WAVE_HEADER)+sizeof(WAVE_FMT)+sizeof(WAVE_DATA),SEEK_SET);
    }
}

void WavEncoder::writePcmData(short* pcmData,int nsamples)
{
    if(wavFile != NULL){
        wavDATA.Subchunk2Size += nsamples*sizeof(short);
        fwrite(pcmData,sizeof(short),nsamples,wavFile);
    }
}

void WavEncoder::writeWavHeader()
{
    wavHEADER.ChunkSize = 36 + wavDATA.Subchunk2Size;
    if(wavFile != NULL){
        rewind(wavFile);
        fwrite(&wavHEADER,sizeof(WAVE_HEADER),1,wavFile);
        fwrite(&wavFMT,sizeof(WAVE_FMT),1,wavFile);
        fwrite(&wavDATA,sizeof(WAVE_DATA),1,wavFile);
    }
}
