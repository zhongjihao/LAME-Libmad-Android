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
    private static final int MAX_VOLUME = 2000;
    public static final int RECORD_MP3 = 1;
    public static final int RECORD_WAV = 2;
    private AudioRecord audioRecord;
    private short[] audioBuf;
    private int min_buffer_size;
    private MP3Codec mp3Encoder;
    private WavCodec wavEncoder;
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
    private int mVolume;
    private String outputDir;
    private String recordFileName;

    public AudioGather(String dir, String fileName) {
        this.outputDir = dir;
        this.recordFileName = fileName;
    }

    private void prepareAudioRecord() {
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

    private void initMP3Encoder() {
        if (!initAudioRecord) {
            Log.e(TAG, "AudioRecord is not inited");
            return;
        }

        if (initAudioEncoder) {
            Log.e(TAG, "AudioEncoder is inited");
            return;
        }

        File file = FileUtil.setOutPutFile(outputDir, recordFileName);
        if (file == null) {
            Log.e(TAG, "initAudioEncoder----outputDir: " + outputDir + "  fileName: " + recordFileName + "  create error");
            return;
        }
        // Create and run thread used to encode data
        mp3Encoder = new MP3Codec(file, min_buffer_size/2);
        aChannelCount = CHANNEL_CONFIG == AudioFormat.CHANNEL_IN_STEREO ? 2 : 1;
        mp3Encoder.initAudioEncoder(aChannelCount, aSampleRate, aSampleRate, BITRATE, MODE, QUALITY);
        mp3Encoder.start();
        //给AudioRecord设置刷新监听，待录音帧数每次达到FRAME_COUNT，就通知转换线程转换一次数据
        audioRecord.setRecordPositionUpdateListener(mp3Encoder, mp3Encoder.getHandler());
        audioRecord.setPositionNotificationPeriod(FRAME_COUNT);
        mCallback = mp3Encoder;
        initAudioEncoder = true;
    }

    private void initWAVEncoder() {
        if (!initAudioRecord) {
            Log.e(TAG, "AudioRecord is not inited");
            return;
        }

        if (initAudioEncoder) {
            Log.e(TAG, "AudioEncoder is inited");
            return;
        }
        // Create and run thread used to encode data
        wavEncoder = new WavCodec(outputDir+"/"+recordFileName);
        aChannelCount = CHANNEL_CONFIG == AudioFormat.CHANNEL_IN_STEREO ? 2 : 1;
        wavEncoder.initWavEncoder(aChannelCount, aSampleRate,AUDIO_FORMAT.getBytesPerFrame());
        wavEncoder.start();
        //给AudioRecord设置刷新监听，待录音帧数每次达到FRAME_COUNT，就通知转换线程转换一次数据
        audioRecord.setRecordPositionUpdateListener(wavEncoder, wavEncoder.getHandler());
        audioRecord.setPositionNotificationPeriod(FRAME_COUNT);
        mCallback = wavEncoder;
        initAudioEncoder = true;
    }

    /**
     * 开始录音
     */
    public void startRecord(final int recordType) {
        if(loop)
            return;
        prepareAudioRecord();

        if(recordType == RECORD_MP3){
            initMP3Encoder();
        }else if(recordType == RECORD_WAV){
            initWAVEncoder();
        }

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
                            calculateRealVolume(audioBuf, size);
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
                if(recordType == RECORD_MP3){
                    mp3Encoder.sendStopMessage();
                    try {
                        Log.d(TAG, "=====zhongjihao======等待MP3编码线程退出...");
                        mp3Encoder.join();
                        Log.d(TAG, "=====zhongjihao======Audio录音线程结束...");
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }else if(recordType == RECORD_WAV){
                    wavEncoder.sendStopMessage();
                    try {
                        Log.d(TAG, "=====zhongjihao======等待WAV编码线程退出...");
                        wavEncoder.join();
                        Log.d(TAG, "=====zhongjihao======Audio录音线程结束...");
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
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

    /**
     * 此计算方法来自samsung开发范例
     *
     * @param buffer buffer
     * @param readSize readSize
     */
    private void calculateRealVolume(short[] buffer, int readSize) {
        double sum = 0;
        for (int i = 0; i < readSize; i++) {
            sum += buffer[i] * buffer[i];
        }
        if (readSize > 0) {
            double amplitude = sum / readSize;
            mVolume = (int) Math.sqrt(amplitude);
        }
    }

    /**
     * 获取真实的音量。 [算法来自三星]
     * @return 真实音量
     */
    public int getRealVolume() {
        return mVolume;
    }

    /**
     * 获取相对音量。 超过最大值时取最大值。
     * @return 音量
     */
    public int getVolume(){
        if (mVolume >= MAX_VOLUME) {
            return MAX_VOLUME;
        }
        return mVolume;
    }

    public interface PcmCallback {
        public void addPcmData(short[] pcmData,int elementNum);
    }
}
