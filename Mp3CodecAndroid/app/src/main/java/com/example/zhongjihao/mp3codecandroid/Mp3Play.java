package com.example.zhongjihao.mp3codecandroid;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import android.widget.Toast;

import com.example.zhongjihao.mp3codecandroid.mp3codec.Mp3DecoderJni;

/**
 * Created by zhongjihao100@163.com on 18-8-12.
 */
public class Mp3Play {
    private final static String TAG = "Mp3Play";
    private Context context;
    private Thread mThread;
    private short[] audioBuffer;
    private AudioTrack mAudioTrack;
    private boolean mIsRunning;
    private int playCurrentPos = 0;
    private int samplerate;
    private int mAudioMinBufSize;
    private String path;

    private Mp3DecoderJni mp3Decoder;

    public Mp3Play(Context context){
        this.context = context;
    }

    public void prepare() {
        if(mp3Decoder!= null){
            Toast.makeText(context, "请先停止播放",
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "mp3 decoder already prepare!");
            return;
        }
        mp3Decoder = new Mp3DecoderJni();
        int ret = mp3Decoder.initAudioPlayer(path, 0);
        Log.d(TAG, "prepare------>ret: "+ret+"    mAudioMinBufSize: "+mAudioMinBufSize);
        if (ret >0 ) {
            samplerate = mp3Decoder.getAudioSamplerate();
            samplerate = samplerate / 2;
            Log.d(TAG, "prepare------>samplerate: " + samplerate);
            // 声音文件一秒钟buffer的大小
            mAudioMinBufSize = AudioTrack.getMinBufferSize(samplerate,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);

            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, // 指定在流的类型
                    // STREAM_ALARM：警告声
                    // STREAM_MUSCI：音乐声，例如music等
                    // STREAM_RING：铃声
                    // STREAM_SYSTEM：系统声音
                    // STREAM_VOCIE_CALL：电话声音

                    samplerate,// 设置音频数据的采样率
                    AudioFormat.CHANNEL_IN_STEREO,// 设置输出声道为双声道
                    AudioFormat.ENCODING_PCM_16BIT,// 设置音频数据块是8位还是16位
                    mAudioMinBufSize, AudioTrack.MODE_STREAM);// 设置模式类型，在这里设置为流类型
            // AudioTrack中有MODE_STATIC和MODE_STREAM两种分类。
            // STREAM方式表示由用户通过write方式把数据一次一次的写到audiotrack中。
            // 这种方式的缺点就是JAVA层和Native层不断地交换数据，效率损失较大。
            // 而STATIC方式表示是一开始创建的时候，就把音频数据放到一个固定的buffer，然后直接传给audiotrack，
            // 后续就不用一次次得write了。AudioTrack会自己播放这个buffer中的数据。
            // 这种方法对于铃声等体积较小的文件比较合适。

            audioBuffer = new short[mAudioMinBufSize];
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mIsRunning) {
                        if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PAUSED
                                && mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                            // ****从libmad处获取data******/
                            playCurrentPos = mp3Decoder.getAudioBuf(
                                    audioBuffer, mAudioMinBufSize);
                            mAudioTrack.write(audioBuffer, 0, mAudioMinBufSize);
                            Log.d(TAG, "播放缓冲大小:  " + mAudioMinBufSize
                                    + " ,播放的文件位置: " + playCurrentPos+" ,文件大小: "+mp3Decoder.getAudioFileSize());
                            if (playCurrentPos == 0) {
                                mAudioTrack.stop();
                                mp3Decoder.rePlayAudioFile();
                            }
                        } else {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            mIsRunning = true;
            mThread.start();
        }
    }

    public void setPlaySource(String path){
        this.path = path;
    }

    public void pausePlayMP3(){
        if(mAudioTrack == null){
            Toast.makeText(context, "请先播放",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mAudioTrack.pause();
        } else {
            Toast.makeText(context, "Already pause",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void startPlayMP3(){
        if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED || (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED)) {
            mAudioTrack.play();
        }else {
            Toast.makeText(context, "Already in play",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void rePlayMP3(){
        if(mAudioTrack == null){
            Toast.makeText(context, "请先播放",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED || (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED)) {
            mp3Decoder.rePlayAudioFile();
            mAudioTrack.play();
        } else {
            Toast.makeText(context, "Already in play",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void stopPlayMP3(){
        if(mAudioTrack == null){
            Toast.makeText(context, "请先播放",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING
                ||mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
            mAudioTrack.stop();
        } else if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED) {
            Toast.makeText(context, "Already stop",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void release(){
        if(mAudioTrack != null){
            mAudioTrack.stop();
            mAudioTrack.release();// 关闭并释放资源
            mAudioTrack = null;
        }
        if(mp3Decoder != null){
            mp3Decoder.closeAudioFile();
            mp3Decoder = null;
        }
        mIsRunning = false;// 音频线程停止
    }

}
