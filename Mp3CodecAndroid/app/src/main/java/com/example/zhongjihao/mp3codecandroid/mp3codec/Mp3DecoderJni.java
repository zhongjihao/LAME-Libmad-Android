package com.example.zhongjihao.mp3codecandroid.mp3codec;

/**
 * Created by zhongjihao on 18-8-12.
 */

public class Mp3DecoderJni {
    static {
        System.loadLibrary("mp3decoder");
    }

    public Mp3DecoderJni() {

    }

    public native int initAudioPlayer(String file, int StartAddr);

    public native int getAudioBuf(short[] audioBuffer, int numSamples);

    public native int getAudioSamplerate();

    public native int getAudioFileSize();

    public native void rePlayAudioFile();

    public native void closeAudioFile();
}
