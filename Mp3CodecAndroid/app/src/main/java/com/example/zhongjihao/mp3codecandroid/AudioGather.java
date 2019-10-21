package com.example.zhongjihao.mp3codecandroid;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;


/**
 * Created by zhongjihao100@163.com on 18-8-12.
 */

public class AudioGather {
    private static final String TAG = "AudioGather";
    private static final int MAX_VOLUME = 2000;
    private static AudioGather mAudioGather;
    private AudioRecord audioRecord;
    private short[] audioBuf;
    private int min_buffer_size;
    //转换周期，录音每满160帧，进行一次转换
    public static final int FRAME_COUNT = 160;
    //声道数
    private int aChannelCount;
    //采样率
    private int aSampleRate;

    /**
     * channelConfig：有立体声（CHANNEL_IN_STEREO）和单声道（CHANNEL_IN_MONO）两种。
     但只有单声道（CHANNEL_IN_MONO）是所有设备都支持的。
     audioFormat ： 有ENCODING_PCM_16BIT和ENCODING_PCM_8BIT两种音频编码格式。
     官方声明只有ENCODING_PCM_16BIT是所有设备都支持的。
     */
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final PCMFormat AUDIO_FORMAT = PCMFormat.PCM_16BIT;

    private Thread workThread;
    private volatile boolean isRecording = false;
    private PcmCallback mCallback;
    private boolean initAudioRecord = false;
    private int mVolume;
    private MP3Encoder mp3Encoder;
    private WavCoder wavCoder;


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
        if(audioRecord != null){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
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
                Log.d(TAG, "prepareAudioRecord done------>min_buffer_size: " + min_buffer_size+"  aSampleRate: "+aSampleRate);
                break;
            }
        } catch (final Exception e) {
            Log.e(TAG, "AudioThread#run", e);
        }
    }

    public int getMin_buffer_size() {
        return min_buffer_size;
    }

    public int getaChannelCount() {
        aChannelCount = CHANNEL_CONFIG == AudioFormat.CHANNEL_IN_STEREO ? 2 : 1;
        return aChannelCount;
    }

    public int getaSampleRate() {
        return aSampleRate;
    }

    public void setRecordListener(AudioRecord.OnRecordPositionUpdateListener listener, android.os.Handler handler, int FRAME_COUNT){
        //给AudioRecord设置刷新监听，待录音帧数每次达到FRAME_COUNT，就通知转换线程转换一次数据
        audioRecord.setRecordPositionUpdateListener(listener, handler);
        audioRecord.setPositionNotificationPeriod(FRAME_COUNT);
    }

    public void setMp3Encoder(MP3Encoder mp3Encoder){
        this.mp3Encoder = mp3Encoder;
    }

    public void setWavCoder(WavCoder wavCoder){
        this.wavCoder = wavCoder;
    }

    /**
     * 开始录音
     */
    public void startRecord() {
        if(isRecording)
            return;
        workThread = new Thread() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                Log.d(TAG, "=====zhongjihao===run----->audioRecord: "+audioRecord);
                if (audioRecord != null) {
                    audioRecord.startRecording();
                }
                while (isRecording && !Thread.interrupted()) {
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
                if(audioRecord != null){
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                }
                // stop the encoding thread and try to wait
                // until the thread finishes its job
                if(mp3Encoder != null && mp3Encoder.isEncodering()){
                    mp3Encoder.stopMP3Encoder();
                    try {
                        Log.d(TAG, "=====zhongjihao======等待MP3编码线程退出...");
                        mp3Encoder.join();
                        Log.d(TAG, "=====zhongjihao=======MP3编码线程已经退出...");
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }else if(wavCoder != null && wavCoder.isEncodering()){
                    wavCoder.stopWavCoder();
                    try {
                        Log.d(TAG, "=====zhongjihao======等待WAV编码线程退出...");
                        wavCoder.join();
                        Log.d(TAG, "=====zhongjihao=======WAV编码线程已经退出...");
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "=====zhongjihao======AudioRecord采集线程退出....");
            }
        };

        isRecording = true;
        workThread.start();
    }

    public void stopRecord() {
        Log.d(TAG, "===zhongjihao====stopRecord======");
        isRecording = false;
        initAudioRecord = false;
    }

    public boolean isRecording() {
        return isRecording;
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

    public void setCallback(PcmCallback callback) {
        this.mCallback = callback;
    }

    public interface PcmCallback {
        public void addPcmData(short[] pcmData,int elementNum);
    }
}
