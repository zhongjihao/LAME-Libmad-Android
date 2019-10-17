package com.example.zhongjihao.mp3codecandroid;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;

/**
 * Created by zhongjihao100@163.com on 18-8-12.
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
    private int aChannelCount;
    //采样率
    private int aSampleRate;
    //输出MP3的码率
    private static final int BITRATE = 32;
    /**
     * channelConfig：有立体声（CHANNEL_IN_STEREO）和单声道（CHANNEL_IN_MONO）两种。
     但只有单声道（CHANNEL_IN_MONO）是所有设备都支持的。
     audioFormat ： 有ENCODING_PCM_16BIT和ENCODING_PCM_8BIT两种音频编码格式。
     官方声明只有ENCODING_PCM_16BIT是所有设备都支持的。
     */
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final PCMFormat AUDIO_FORMAT = PCMFormat.PCM_16BIT;
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
    private PcmCallback mCallback;
    private boolean initAudioRecord = false;
    private boolean initAudioEncoder = false;

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

    public void prepareAudioRecord() {
        if(initAudioRecord){
            Log.d(TAG,"AudioRecord inited");
            return;
        }
        //音频采样率，44100是目前的标准，但是某些设备仍然支持22050,16000,11025,8000,4000
        int[] sampleRates = {44100, 22050, 16000, 11025, 8000, 4000};

        try {
            for (int sampleRate : sampleRates) {
                min_buffer_size = AudioRecord.getMinBufferSize(sampleRate,CHANNEL_CONFIG,AUDIO_FORMAT.getAudioFormat());
                int bytesPerFrame = AUDIO_FORMAT.getBytesPerFrame();
                //获取的AudioRecord最小缓冲区包含的帧数
                int frameSize = min_buffer_size / bytesPerFrame;
                //保证AudioRecord最小缓冲区的帧数是160的整数倍，否则会造成数据丢失
                if (frameSize % FRAME_COUNT != 0) {
                    frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
                }
                min_buffer_size = frameSize * bytesPerFrame;
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, CHANNEL_CONFIG,AUDIO_FORMAT.getAudioFormat(), min_buffer_size);
                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    audioRecord = null;
                    Log.e(TAG, "====zhongjihao===initialized the mic failed");
                    continue;
                }
                aSampleRate = sampleRate;
                audioBuf = new short[frameSize];
                initAudioRecord = true;
                Log.d(TAG, "====zhongjihao====min_buffer_size: " + min_buffer_size+"  aSampleRate: "+aSampleRate);
                break;
            }
        } catch (final Exception e) {
            Log.e(TAG, "AudioThread#run", e);
        }
    }

    public void initAudioEncoder(String dir, String fileName) {
        if (!initAudioRecord) {
            Log.e(TAG, "AudioRecord is not inited");
            return;
        }

        if (initAudioEncoder) {
            Log.e(TAG, "AudioEncoder is inited");
            return;
        }
        // Create and run thread used to encode data
        File file = FileUtil.setOutPutFile(dir, fileName);
        if (file == null) {
            Log.e(TAG, "initAudioEncoder----dir: " + dir + "  fileName: " + fileName + "  create error");
            return;
        }

        pcmEncoder = new AudioCodec(file, min_buffer_size/2);
        aChannelCount = CHANNEL_CONFIG == AudioFormat.CHANNEL_IN_STEREO ? 2 : 1;
        pcmEncoder.initAudioEncoder(aChannelCount, aSampleRate, aSampleRate, BITRATE, MODE, QUALITY);
        pcmEncoder.start();
        //给AudioRecord设置刷新监听，待录音帧数每次达到FRAME_COUNT，就通知转换线程转换一次数据
        audioRecord.setRecordPositionUpdateListener(pcmEncoder, pcmEncoder.getHandler());
        audioRecord.setPositionNotificationPeriod(FRAME_COUNT);
        mCallback = pcmEncoder;
        initAudioEncoder = true;
    }

    /**
     * 开始录音
     */
    public void startRecord() {
        if(loop)
            return;
        workThread = new Thread() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                if (audioRecord != null) {
                    audioRecord.startRecording();
                }
                while (loop && !Thread.interrupted()) {
                    //读取音频数据到audioBuf
                    int size = audioRecord.read(audioBuf,0, min_buffer_size/2);
                    if (size > 0) {
                        // set audio data to encoder
                        // Log.d(TAG, "======zhongjihao========录音short个数:" + size);
                        if (mCallback != null) {
                            mCallback.addPcmData(audioBuf,size);
                        }
                    }
                }
                Log.d(TAG, "=====zhongjihao======AudioRecord采集结束=====");
                if(audioRecord != null){
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                }

                // stop the encoding thread and try to wait
                // until the thread finishes its job
                pcmEncoder.sendStopMessage();
                try {
                    Log.d(TAG, "=====zhongjihao======等待Audio编码线程退出...");
                    pcmEncoder.join();
                    Log.d(TAG, "=====zhongjihao======Audio录音线程结束...");
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        };

        loop = true;
        workThread.start();
    }

    public void stopRecord() {
        Log.d(TAG, "===zhongjihao====stopRecord======");
        loop = false;
        initAudioRecord = false;
        initAudioEncoder = false;

    }

    public interface PcmCallback {
        public void addPcmData(short[] pcmData,int elementNum);
    }
}
