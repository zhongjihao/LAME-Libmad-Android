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
public class WavCoder extends Thread implements AudioGather.PcmCallback,AudioRecord.OnRecordPositionUpdateListener {
    private static final String TAG = "WavCoder";
    //用于存取待转换的PCM数据
    private LinkedBlockingQueue<PcmBuffer> audioQueue;
    private StopHandler handler;
    private CountDownLatch handlerInitLatch = new CountDownLatch(1);
    private static final int PROCESS_STOP = 1;
    private String wavPath;
    private boolean isEncodering = false;

    public static class StopHandler extends Handler {
        WeakReference<WavCoder> sr;

        public StopHandler(WavCoder stateReceiver){
            sr = new WeakReference<>(stateReceiver);
        }

        @Override
        public void handleMessage(Message msg) {
            WavCoder codec = sr.get();
            if (codec == null) {
                return;
            }

            if (msg.what == PROCESS_STOP) {
                //录音停止后，将剩余的PCM数据转换完毕
                for (;codec.encoderData() > 0;);
                removeCallbacksAndMessages(null);
                codec.flush();
                codec.audioQueue.clear();
                Log.d(TAG, "=====zhongjihao======Wav编码线程开始退出...");
                getLooper().quit();
            }
        }
    }

    public void setOutputPath(String wavPath){
        this.wavPath = wavPath;
    }

    public WavCoder(){
        audioQueue = new LinkedBlockingQueue<>();
    }

    public void initWavEncoder(int numChannels, int sampleRate,int bytesPerSample) {
        Log.d(TAG, "initWAVEncoder");
        WavCoderWrap.newInstance().createWavEncoder(wavPath);
        WavCoderWrap.newInstance().initWavEncoder(numChannels,sampleRate,bytesPerSample);
    }

    @Override
    public void run() {
        isEncodering = true;
        Looper.prepare();
        handler = new StopHandler(this);
        handlerInitLatch.countDown();
        Looper.loop();
        isEncodering = false;
        Log.d(TAG, "=====zhongjihao======WAV编码线程已经退出...");
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

    public void stopWavCoder() {
        handler.sendEmptyMessage(PROCESS_STOP);
    }

    public boolean isEncodering() {
        return isEncodering;
    }

    /**
     * 添加音频数据
     *
     * @param rawData
     */
    public void addPcmData(short[] rawData, int readSize) {
        try {
            Log.d(TAG, "======addPcmData===readSize: "+readSize);
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
        Log.d(TAG, "======onPeriodicNotification===");
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
                    WavCoderWrap.newInstance().writePcmData(buffer,readSize);
                    return readSize;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    //Flush all data left in lame buffer to file
    private void flush() {
        WavCoderWrap.newInstance().writeWavHeader();
        WavCoderWrap.newInstance().destroyWavEncoder();
    }

}
