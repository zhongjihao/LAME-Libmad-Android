package com.example.zhongjihao.mp3codecandroid.mp3codec;

/**
 * Created by zhongjihao100@163.com on 18-8-25.
 */

public class Mp3EncoderWrap {
    private long cPtr;
    private static Mp3EncoderWrap mInstance;
    private static Object lockobj = new Object();

    private Mp3EncoderWrap() {
        cPtr = 0;
    }

    public static Mp3EncoderWrap newInstance() {
        synchronized (lockobj) {
            if (mInstance == null) {
                mInstance = new Mp3EncoderWrap();
            }
        }
        return mInstance;
    }

    public void registerCallback(IMP3EncoderDoneNotify cb){
        Mp3EncoderJni.registerCallback(cb);
    }

    public void destroyCallback(){
        Mp3EncoderJni.destroyCallback();
    }

    public void createEncoder() {
        if(cPtr == 0){
            cPtr = Mp3EncoderJni.createMp3Encoder();
        }
    }

    public int initMp3Encoder(int inChannelNum, int inSampleRate,int outSampleRate,int outBitrate, int mode, int quality) {
        if(cPtr != 0){
            return Mp3EncoderJni.initMp3Encoder(cPtr,inChannelNum, inSampleRate,outSampleRate,outBitrate,mode,quality);
        }else {
            return -1;
        }
    }

    public int encodePcmDataToMp3(short[] buffer_l, short[] buffer_r, int samples, byte[] mp3buf) {
        int encoderByte = 0;
        if(cPtr != 0){
            encoderByte = Mp3EncoderJni.encodePcmDataToMp3(cPtr,buffer_l, buffer_r,samples,mp3buf);
        }
        return encoderByte;
    }

    public int pcmfileConvertMP3file(String pcmPath, String mp3Path) {
        int ret = 0;
        if(cPtr != 0){
            ret = Mp3EncoderJni.pcmfileConvertMP3file(cPtr,pcmPath, mp3Path);
        }
        return ret;
    }

    public int encodeFlush(byte[] mp3buf) {
        int encoderByte = 0;
        if(cPtr != 0){
            encoderByte = Mp3EncoderJni.encodeFlush(cPtr,mp3buf);
        }
        return encoderByte;
    }

    public void destroyMp3Encoder() {
        if (cPtr != 0) {
            Mp3EncoderJni.destroyMp3Encoder(cPtr);
        }
        cPtr = 0;
        mInstance = null;
    }

}
