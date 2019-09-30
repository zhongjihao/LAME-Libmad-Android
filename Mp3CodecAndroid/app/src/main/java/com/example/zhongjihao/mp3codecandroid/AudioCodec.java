package com.example.zhongjihao.mp3codecandroid;

import android.media.AudioRecord;
import android.os.Looper;
import android.util.Log;
import android.os.Handler;
import android.os.Message;

import com.example.zhongjihao.mp3codecandroid.mp3codec.Mp3EncoderWrap;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhongjihao on 18-8-12.
 **/

public class AudioCodec extends Thread implements AudioGather.PcmCallback,AudioRecord.OnRecordPositionUpdateListener {
    private static final String TAG = "AudioCodec";
    //用于存取待转换的PCM数据
    private List<PcmBuffer> audioQueue = Collections.synchronizedList(new LinkedList<PcmBuffer>());
    private FileOutputStream mp3File;
    private byte[] mp3Buffer;
    private StopHandler handler;
    private CountDownLatch handlerInitLatch = new CountDownLatch(1);
    public static final int PROCESS_STOP = 1;

    public static class StopHandler extends Handler {
        WeakReference<AudioCodec> sr;

        public WorkerHandler(AudioCodec stateReceiver) {
            sr = new WeakReference<AudioCodec>(stateReceiver);
        }

        @Override
        public void handleMessage(Message msg) {
            AudioCodec codec = sr.get();
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


    public AudioCodec(FileOutputStream os,int bufferSize) {
        mp3File = os;
        //官方规定了计算公式：7200 + (1.25 * buffer_l.length)
        mp3Buffer =  new byte[(int) (7200 + (bufferSize * 2 * 1.25))];
        Mp3EncoderWrap.newInstance().createEncoder();
    }


    public void initAudioEncoder(int numChannels, int sampleRate, int bitRate, int mode, int quality) {
        Mp3EncoderWrap.newInstance().initMp3Encoder(numChannels,sampleRate,bitRate,mode,quality);
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
        if(audioQueue != null && audioQueue.size() > 0) {
            try {
                PcmBuffer data = audioQueue.remove(0);
                short[] buffer = data.getData();
                int readSize = data.getReadSize();
                Log.d(TAG, "======zhongjihao====要编码的Audio数据大小:" + data.getReadSize());
                if (readSize > 0) {
                    int encodedSize =  Mp3EncoderWrap.newInstance().encodePcmToMp3(buffer, buffer, readSize, mp3Buffer);
                    if (encodedSize < 0) {
                        Log.e(TAG, "===zhongjihao====Lame encoded size: " + encodedSize);
                    }
                    try {
                        mp3File.write(mp3Buffer, 0, encodedSize);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "===zhongjihao====Unable to write to file");
                    }
                    return readSize;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        return 0;
    }

    private void flush() {
        final int flushResult =  Mp3EncoderWrap.newInstance().encodeFlush(mp3Buffer);

        if (flushResult > 0) {
            try {
                mp3File.write(mp3Buffer, 0, flushResult);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

}
