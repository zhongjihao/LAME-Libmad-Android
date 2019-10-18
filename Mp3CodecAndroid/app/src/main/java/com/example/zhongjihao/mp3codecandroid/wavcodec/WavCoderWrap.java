package com.example.zhongjihao.mp3codecandroid.wavcodec;


/**
 * Created by zhongjihao100@163.com on 18-8-12.
 */

public class WavCoderWrap {
    private long cPtr;
    private static WavCoderWrap mInstance;
    private static Object lockobj = new Object();

    private WavCoderWrap(){
        cPtr = 0;
    }

    public static WavCoderWrap newInstance() {
        synchronized (lockobj) {
            if (mInstance == null) {
                mInstance = new WavCoderWrap();
            }
        }
        return mInstance;
    }

    public void createWavEncoder(String wavPath) {
        if(cPtr == 0){
            cPtr = WavCoderJni.createWavEncoder(wavPath);
        }
    }

    public void initWavEncoder(int inChannelNum,int sampleRate,int bytesPerSample) {
        if(cPtr != 0){
            WavCoderJni.initWavEncoder(cPtr,inChannelNum, sampleRate,bytesPerSample);
        }
    }

    public void encodePcmToWav(short[] buffer,int samples) {
        if(cPtr != 0){
            WavCoderJni.encodePcmToWav(cPtr,buffer,samples);
        }
    }

    public void encodeFlush() {
        if(cPtr != 0){
            WavCoderJni.encodeFlush(cPtr);
        }
    }

    public void destroyWavEncoder() {
        if (cPtr != 0) {
            WavCoderJni.destroyWavEncoder(cPtr);
        }
        cPtr = 0;
        mInstance = null;
    }

}
