package com.example.zhongjihao.mp3codecandroid;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by zhongjihao on 18-8-12.
 */

public class AudioGather {
    private static final String TAG = "AudioGather";
    private static AudioGather mAudioGather;
    private AudioRecord audioRecord;
    private short[] audioBuf;
    private int min_buffer_size;
    private AudioCodec pcmEncoder;
    //转换周期，录音每满160帧，进行一次转换
    private static final int FRAME_COUNT = 160;
    //声道数
    private static final int NUM_CHANNELS = 2;
    //默认采样率
    private static final int SAMPLE_RATE = 44100;
    //输出MP3的码率
    private static final int BITRATE = 32;
    //mode = 0,1,2,3 = stereo, jstereo, dual channel (not supported), mono
    private static final int MODE = 3;
    /*
       recommended:
           2     near-best quality, not too slow
           5     good quality, fast
           7     ok quality, really fast
    */
    private static final int QUALITY = 7;

    private Thread workThread;
    private volatile boolean loop = false;
    private File mp3File;
    private FileOutputStream os = null;
    private PcmCallback mCallback;

    private OnFinishListener finishListener;

    public interface OnFinishListener {
        void onFinish(String mp3SavePath);
    }

    public static AudioGather getInstance() {
        if (mAudioGather == null) {
            synchronized (AudioGather.class) {
                if (mAudioGather == null) {
                    mAudioGather = new AudioGather();
                }
            }
        }
        return mAudioGather;
    }

    private AudioGather() {

    }

    /*
         channelConfig：有立体声（CHANNEL_IN_STEREO）和单声道（CHANNEL_IN_MONO）两种。
                          但只有单声道（CHANNEL_IN_MONO）是所有设备都支持的。
         audioFormat ： 有ENCODING_PCM_16BIT和ENCODING_PCM_8BIT两种音频编码格式。
                        官方声明只有ENCODING_PCM_16BIT是所有设备都支持的。

     */

    public void prepareAudioRecord(int channelConfig,int pcmFormat) {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        //音频采样率，44100是目前的标准，但是某些设备仍然支持22050,16000,11025,8000,4000
        int[] sampleRates = {44100, 22050, 16000, 11025, 8000, 4000};
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        int aSampleRate = 44100;
        try {
            for (int sampleRate : sampleRates) {
                min_buffer_size = AudioRecord.getMinBufferSize(sampleRate, channelConfig, pcmFormat);
                int bytesPerFrame = 1;
                if(pcmFormat == AudioFormat.ENCODING_PCM_16BIT)
                    bytesPerFrame = 2;
                else if(pcmFormat == AudioFormat.ENCODING_PCM_8BIT)
                    bytesPerFrame = 1;
                //获取的AudioRecord最小缓冲区包含的帧数
                int frameSize = min_buffer_size / bytesPerFrame;
                //保证AudioRecord最小缓冲区的帧数是160的整数倍，否则会造成数据丢失
                if (frameSize % FRAME_COUNT != 0) {
                    frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
                }
                min_buffer_size = frameSize * bytesPerFrame;
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, pcmFormat, min_buffer_size);
                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    audioRecord = null;
                    Log.e(TAG, "====zhongjihao===initialized the mic failed");
                    continue;
                }
                aSampleRate = sampleRate;
                audioBuf = new short[min_buffer_size];
                Log.d(TAG, "====zhongjihao====min_buffer_size: " + min_buffer_size);
                break;
            }
        } catch (final Exception e) {
            Log.e(TAG, "AudioThread#run", e);
        }

        // Create and run thread used to encode data
        pcmEncoder = new AudioCodec(os, min_buffer_size);
        int numChannels = channelConfig == AudioFormat.CHANNEL_IN_MONO ? 1:2;
        pcmEncoder.initAudioEncoder(numChannels,aSampleRate, BITRATE, MODE, QUALITY);
        pcmEncoder.start();
        //给AudioRecord设置刷新监听，待录音帧数每次达到FRAME_COUNT，就通知转换线程转换一次数据
        audioRecord.setRecordPositionUpdateListener(pcmEncoder, pcmEncoder.getHandler());
        audioRecord.setPositionNotificationPeriod(FRAME_COUNT);
    }


    /**
     * 开始录音
     */
    public void startRecord(File mp3File) {
        if(loop)
            return;
        this.mp3File = mp3File;
        workThread = new Thread() {
            @Override
            public void run() {
                if (audioRecord != null) {
                    audioRecord.startRecording();
                }
                while (loop && !Thread.interrupted()) {
                    //读取音频数据到audioBuf
                    int size = audioRecord.read(audioBuf,0, min_buffer_size);
                    if (size > 0) {
                        // set audio data to encoder
                        // Log.d(TAG, "======zhongjihao========录音字节数:" + size);
                        if (mCallback != null) {
                            mCallback.addPcmData(audioBuf,size);
                        }
                    }
                }
                Log.d(TAG, "=====zhongjihao======Audio录音线程退出...");
            }
        };

        loop = true;
        workThread.start();
    }

    public void stopRecord() {
        Log.d(TAG, "run: ===zhongjihao====停止录音======");
        if(audioRecord != null)
            audioRecord.stop();
        loop = false;
        if(workThread != null)
            workThread.interrupt();
    }

    public void release() {
        if(audioRecord != null)
            audioRecord.release();
        audioRecord = null;
    }

    public void setCallback(PcmCallback callback) {
        this.mCallback = callback;
    }

    public interface PcmCallback {
        public void addPcmData(short[] pcmData,int dataSize);
    }
}
