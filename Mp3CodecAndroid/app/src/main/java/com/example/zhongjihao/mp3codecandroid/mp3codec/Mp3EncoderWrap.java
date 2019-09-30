package com.example.zhongjihao.mp3codecandroid.mp3codec;

/**
 * Created by zhongjihao on 18-8-25.
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

    public void createEncoder() {
        cPtr = Mp3EncoderJni.createMp3Encoder();
    }

    public int initMp3Encoder(int numChannels, int sampleRate,int bitRate, int mode, int quality) {
        return Mp3EncoderJni.initMp3Encoder(cPtr,numChannels, sampleRate,bitRate,mode,quality);
    }

    public int encodePcmToMp3(short[] buffer_l, short[] buffer_r, int samples, byte[] mp3buf) {
        int encoderByte = Mp3EncoderJni.encodePcmToMp3(cPtr,buffer_l, buffer_r,samples,mp3buf);
        return encoderByte;
    }

    public int encodeFlush(byte[] mp3buf) {
        int encoderByte = Mp3EncoderJni.encodeFlush(cPtr,mp3buf);
        return encoderByte;
    }

    public void destroyMp3Encoder() {
        if (cPtr != 0) {
            Mp3EncoderJni.destroyMp3Encoder(cPtr);
        }
        mInstance = null;
    }

}
