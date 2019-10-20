//
// Created by zhongjihao100@163.com on 20/10/19.
//

#ifndef MP3CODECANDROID_WAVENCODER_H
#define MP3CODECANDROID_WAVENCODER_H

#include <stdio.h>

typedef struct WAVE_HEADER{
    char           ChunkID[4];         //内容为"RIFF"
    unsigned int   ChunkSize;          //存储文件的字节数（不包含ChunkID和ChunkSize这8个字节）
    char           Format[4];          //内容为"WAVE"
}WAVE_HEADER;

typedef struct WAVE_FMT{
    char             Subchunk1ID[4];      //内容为"fmt "
    unsigned int     Subchunk1Size;       //存储该子块的字节数,为16（不含前面的Subchunk1ID和Subchunk1Size这8个字节
    unsigned short   AudioFormat;         //存储音频文件的编码格式，例如若为PCM则其存储值为1，若为其他非PCM格式的则有一定的压缩
    unsigned short   NumChannels;         //通道数，单通道(Mono)值为1，双通道(Stereo)值为2
    unsigned int     SampleRate;          //采样率，如8k，44.1k等
    unsigned int     ByteRate;            //每秒存储的字节数，其值=SampleRate * NumChannels * BitsPerSample/8
    unsigned short   BlockAlign;          //块对齐大小，其值=NumChannels * BitsPerSample/8
    unsigned short   BitsPerSample;       //每个采样点的bit数，一般为8,16,32等
}WAVE_FMT;

typedef struct WAVE_DATA{
    char         Subchunk2ID[4];       //内容为“data”
    unsigned int Subchunk2Size;        //PCM原始裸数据字节数
}WAVE_DATA;


class WavEncoder
{
private:
    FILE* wavFile;
    WAVE_HEADER wavHEADER;
    WAVE_FMT    wavFMT;
    WAVE_DATA   wavDATA;
private:
    WavEncoder(WavEncoder& oth);
    WavEncoder& operator=(const WavEncoder& oth);
public:
    WavEncoder(const char* wavPath);
    ~WavEncoder();
    void initWavEncoder(int channelNum,int samplerate,int bytesPerSample);
    void writePcmData(short* pcmData,int nsamples);
    void writeWavHeader();
};

#endif //MP3CODECANDROID_WAVENCODER_H
