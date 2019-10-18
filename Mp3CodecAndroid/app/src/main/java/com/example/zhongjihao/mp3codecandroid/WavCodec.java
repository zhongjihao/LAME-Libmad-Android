package com.example.zhongjihao.mp3codecandroid;

import android.media.AudioRecord;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.zhongjihao.mp3codecandroid.wavcodec.WavCoderWrap;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhongjihao100@163.com on 18-8-12.
 */
public class WavCodec extends Thread implements AudioGather.PcmCallback,AudioRecord.OnRecordPositionUpdateListener {
    private static final String TAG = "WavCodec";
    //用于存取待转换的PCM数据
    private LinkedBlockingQueue<PcmBuffer> audioQueue;
    private StopHandler handler;
    private CountDownLatch handlerInitLatch = new CountDownLatch(1);
    private static final int PROCESS_STOP = 1;
    private String wavPath;

    public static class StopHandler extends Handler {
        WeakReference<WavCodec> sr;

        public StopHandler(WavCodec stateReceiver){
            sr = new WeakReference<>(stateReceiver);
        }

        @Override
        public void handleMessage(Message msg) {
            WavCodec codec = sr.get();
            if (codec == null) {
                return;
            }

            if (msg.what == PROCESS_STOP) {
                //录音停止后，将剩余的PCM数据转换完毕
                for (;codec.encoderData() > 0;);
                removeCallbacksAndMessages(null);
                codec.flush();
                codec.audioQueue.clear();
                Log.d(TAG, "=====zhongjihao======Audio 编码线程 退出...");
                getLooper().quit();
            }
        }
    }

    public WavCodec(String wavPath){
        this.wavPath = wavPath;
        audioQueue = new LinkedBlockingQueue<>();
        WavCoderWrap.newInstance().createWavEncoder(wavPath);
    }

    public void initWavEncoder(int numChannels, int sampleRate,int bytesPerSample) {
        WavCoderWrap.newInstance().initWavEncoder(numChannels,sampleRate,bytesPerSample);
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new StopHandler(this);
        handlerInitLatch.countDown();
        Looper.loop();
    }

    public Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, "Error when waiting handle to init");
        }
        return handler;
    }

    public void sendStopMessage() {
        handler.sendEmptyMessage(PROCESS_STOP);
    }

    /**
     * 添加音频数据
     *
     * @param rawData
     */
    public void addPcmData(short[] rawData, int readSize) {
        try {
            if (audioQueue != null)
                audioQueue.put(new PcmBuffer(rawData,readSize));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMarkerReached(AudioRecord recorder) {
        // Do nothing
    }

    @Override
    public void onPeriodicNotification(AudioRecord recorder) {
        //由AudioRecord进行回调，满足帧数，通知数据编码
        encoderData();
    }

    //从缓存区audioQueue里获取待编码的PCM数据，编码为MP3数据,并写入文件
    private int encoderData() {
        if(audioQueue != null && !audioQueue.isEmpty()) {
            try {
                PcmBuffer data = audioQueue.take();
                short[] buffer = data.getData();
                int readSize = data.getReadSize();
                Log.d(TAG, "======zhongjihao====要编码的Audio数据大小:" + readSize);
                if (readSize > 0) {
                    WavCoderWrap.newInstance().encodePcmToWav(buffer,readSize);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    //Flush all data left in lame buffer to file
    private void flush() {
        WavCoderWrap.newInstance().encodeFlush();
        WavCoderWrap.newInstance().destroyWavEncoder();
    }

}
