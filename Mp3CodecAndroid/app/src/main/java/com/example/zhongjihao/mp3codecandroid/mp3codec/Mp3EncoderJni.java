package com.example.zhongjihao.mp3codecandroid.mp3codec;


/**
 * Created by zhongjihao100@163.com on 18-8-12.
 */


public class Mp3EncoderJni {

    static {
        System.loadLibrary("mp3encoder");
    }

    //注册MP3编码完成回调
    public native static void registerCallback(IMP3EncoderDoneNotify cb);

    //销毁MP3编码完成回调
    public native static void destroyCallback();

    public native static long createMp3Encoder();
    /**
     * Initialize LAME.
     *
     * inChannelNum , number of channels in input stream.
           mono单声，所以numChannels是 1. stereo，所以numChannels是 2
      inSampleRate ,input sample rate in Hz.
      outSampleRate ,output sample rate in Hz.
      outBitrate, MP3 file will be encoded with bit rate 32kbps
      quality,recommended:
           2     near-best quality, not too slow
           5     good quality, fast
           7     ok quality, really fast
      mode = 0,1,2,3 = stereo, jstereo, dual channel (not supported), mono
     */
    public native static int initMp3Encoder(long cPtr,int inChannelNum, int inSampleRate,int outSampleRate,
                                   int outBitrate, int mode, int quality);

    /**
     * Encode buffer to mp3.
     *
     buffer_l ： 左声道数据(PCM data for left channel)
     buffer_r：  右声道数据(PCM data for right channel)
     samples ：每个声道输入数据short num(number of samples per channel)
     mp3buf ：用于接收编码后的数据。(result encoded MP3 stream. You must specified "7200 + (1.25 * buffer_l.length)" length array)

     @return <p>number of bytes output in mp3buf. Can be 0.</p>
      *         <p>-1: mp3buf was too small</p>
      *         <p>-2: malloc() problem</p>
      *         <p>-3: lame_init_params() not called</p>
      *         -4: psycho acoustic problems
     左右声道 ：当前声道选的是单声道，因此两边传入一样的buffer
     输入数据大小 ：录音线程读取到buffer中的数据不一定是占满的，所以read方法会返回当前大小size，即前size个数据是有效的音频数据，
                  后面的数据是以前留下的废数据。 这个size同样需要传入到Lame编码器中用于编码。
     mp3buf大小：官方规定了计算公式：7200 + (1.25 * buffer_l.length)

    */
    public native static int encodePcmDataToMp3(long cPtr,short[] buffer_l, short[] buffer_r, int samples, byte[] mp3buf);

    /**
     * * Flush LAME buffer.
     *
     * REQUIRED:
     * lame_encode_flush will flush the intenal PCM buffers, padding with
     * 0's to make sure the final frame is complete, and then flush
     * the internal MP3 buffers, and thus may return a
     * final few mp3 frames.  'mp3buf' should be at least 7200 bytes long
     * to hold all possible emitted data.
     *
     * will also write id3v1 tags (if any) into the bitstream
     *
     * return code = number of bytes output to mp3buf. Can be 0
     * @param mp3buf
     *            result encoded MP3 stream. You must specified at least 7200
     *            bytes.
     * @return number of bytes output to mp3buf. Can be 0.
     *
     * 将MP3结尾信息写入buffer中。
     * 传入参数：mp3buf至少7200字节
     */
    public native static int encodeFlush(long cPtr,byte[] mp3buf);

    //PCM文件编码为MP3文件
    public native static int pcmfileConvertMP3file(long cPtr,String pcmPath, String mp3Path);

    /**
     * Close LAME.
     */
    public native static void destroyMp3Encoder(long cPtr);
}
