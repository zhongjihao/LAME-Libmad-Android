package com.example.zhongjihao.mp3codecandroid.wavcodec;

/**
 * Created by zhongjihao100@163.com on 18-8-12.
 */

public class WavCoderJni {
    static {
        System.loadLibrary("wavcoder");
    }

    public native static long createWavEncoder(String wavPath);

    public native static void initWavEncoder(long cPtr,int channelNum, int sampleRate,int bytesPerSample);

    public native static void writePcmData(long cPtr,short[] buffer, int samples);

    public native static void writeWavHeader(long cPtr);

    public native static void destroyWavEncoder(long cPtr);
}
