package com.example.zhongjihao.mp3codecandroid.mp3codec;


/**
 * Created by zhongjihao on 18-8-12.
 */


public class Mp3EncoderJni {

    static {
        System.loadLibrary("mp3encoder");
    }

    public native static long createMp3Encoder();
    /*
      mono单声，所以numChannels是 1
      sampleRate 默认44100
      bitRate, MP3 file will be encoded with bit rate 32kbps
      quality,recommended:
           2     near-best quality, not too slow
           5     good quality, fast
           7     ok quality, really fast
      mode = 0,1,2,3 = stereo, jstereo, dual channel (not supported), mono
     */
    public native static int initMp3Encoder(long cPtr,int numChannels, int sampleRate,
                                   int bitRate, int mode, int quality);
    public native static void destroyMp3Encoder(long cPtr);
    /*
     buffer_l ： 左声道数据
     buffer_r：  右声道数据
     samples ：每个声道输入数据大小
     mp3buf ：用于接收编码后的数据。7200 + (1.25 * buffer_l.length)

     左右声道 ：当前声道选的是单声道，因此两边传入一样的buffer
     输入数据大小 ：录音线程读取到buffer中的数据不一定是占满的，所以read方法会返回当前大小size，即前size个数据是有效的音频数据，
                  后面的数据是以前留下的废数据。 这个size同样需要传入到Lame编码器中用于编码。
     mp3buf大小：官方规定了计算公式：7200 + (1.25 * buffer_l.length)

    */
    public native static int encodePcmToMp3(long cPtr,short[] buffer_l, short[] buffer_r, int samples, byte[] mp3buf);
    /*
      将MP3结尾信息写入buffer中。
      传入参数：mp3buf至少7200字节
     */
    public native static int encodeFlush(long cPtr,byte[] mp3buf);
}
