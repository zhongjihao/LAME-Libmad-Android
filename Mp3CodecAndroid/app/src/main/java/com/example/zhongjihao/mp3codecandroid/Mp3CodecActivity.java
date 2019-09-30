package com.example.zhongjihao.mp3codecandroid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.os.Message;
import android.os.Handler;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zhongjihao.mp3codecandroid.mp3codec.Mp3DecoderJni;
import com.example.zhongjihao.mp3codecandroid.mp3codec.Mp3EncoderJni;
import com.example.zhongjihao.mp3codecandroid.mp3codec.Mp3EncoderWrap;
import com.example.zhongjihao.mp3codecandroid.swig.SwigTest;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class Mp3CodecActivity extends AppCompatActivity {

    private boolean hasPermission;
    private final static int PLAY_DONE = 100;
    private static final int ENCODER_DONE = 10;
    private static final int RECV_DONE = 11;
    public static final int NUM_CHANNELS = 1;
    public static final int SAMPLE_RATE = 16000;
    public static final int BITRATE = 128;
    public static final int MODE = 1;
    public static final int QUALITY = 2;
    private AudioRecord mRecorder;
    private short[] mBuffer;
    private final String startRecordingLabel = "Start recording";
    private final String stopRecordingLabel = "Stop recording";

    private boolean mIsRecording = false;
    private File mRawFile;
    private File mEncodedFile;


    private Mp3EncoderJni mp3Encoder;
    private Mp3DecoderJni mp3Decoder;
    private Homehandle handler = null;

    private Thread mThread;
    private short[] audioBuffer;
    private AudioTrack mAudioTrack;
    private int ret;
    private boolean mThreadFlag;
    private int playCurrentPos = 0;
    private boolean playDone = false;
    private boolean isStopPlay = false;
    private int samplerate;
    private int mAudioMinBufSize;

    private static final int TARGET_PERMISSION_REQUEST = 100;

    // 要申请的权限
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp3_codec);


        TextView recordBtn = (Button) findViewById(R.id.recordBtn);


        hasPermission = false;


        handler = new Homehandle(this);
        initRecorder();
        Mp3EncoderWrap.newInstance().initMp3Encoder(NUM_CHANNELS, SAMPLE_RATE, BITRATE, MODE, QUALITY);


        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            for (int i = 0; i < permissions.length; i++) {
                int result = ContextCompat.checkSelfPermission(this, permissions[i]);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (result != PackageManager.PERMISSION_GRANTED) {
                    hasPermission = false;
                    break;
                } else
                    hasPermission = true;
            }
            if(!hasPermission){
                // 如果没有授予权限，就去提示用户请求
                ActivityCompat.requestPermissions(this,
                        permissions, TARGET_PERMISSION_REQUEST);
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRecorder.release();
        Mp3EncoderWrap.newInstance().destroyMp3Encoder();
        if(mAudioTrack != null){
            mAudioTrack.stop();
            mAudioTrack.release();// 关闭并释放资源
        }
        mThreadFlag = false;// 音频线程停止
        if(mp3Decoder != null){
            mp3Decoder.closeAudioFile();
        }

        System.exit(0);
    }

    private void initRecorder() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mBuffer = new short[bufferSize];
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
    }

    private void initAudioPlayer() {
        samplerate = mp3Decoder.getAudioSamplerate();
        Log.d("zhongjihao", "==========samplerate = " + samplerate);
        samplerate = samplerate / 2;
        // 声音文件一秒钟buffer的大小
        mAudioMinBufSize = AudioTrack.getMinBufferSize(samplerate,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, // 指定在流的类型
                // STREAM_ALARM：警告声
                // STREAM_MUSCI：音乐声，例如music等
                // STREAM_RING：铃声
                // STREAM_SYSTEM：系统声音
                // STREAM_VOCIE_CALL：电话声音

                samplerate,// 设置音频数据的采样率
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,// 设置输出声道为双声道立体声
                AudioFormat.ENCODING_PCM_16BIT,// 设置音频数据块是8位还是16位
                mAudioMinBufSize, AudioTrack.MODE_STREAM);// 设置模式类型，在这里设置为流类型
        // AudioTrack中有MODE_STATIC和MODE_STREAM两种分类。
        // STREAM方式表示由用户通过write方式把数据一次一次得写到audiotrack中。
        // 这种方式的缺点就是JAVA层和Native层不断地交换数据，效率损失较大。
        // 而STATIC方式表示是一开始创建的时候，就把音频数据放到一个固定的buffer，然后直接传给audiotrack，
        // 后续就不用一次次得write了。AudioTrack会自己播放这个buffer中的数据。
        // 这种方法对于铃声等体积较小的文件比较合适。
    }


    private void startBufferedWrite(final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream output = null;
                try {
                    output = new DataOutputStream(new BufferedOutputStream(
                            new FileOutputStream(file)));
                    while (mIsRecording) {
                        int readSize = mRecorder.read(mBuffer, 0,
                                mBuffer.length);
                        for (int i = 0; i < readSize; i++) {
                            output.writeShort(mBuffer[i]);
                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(Mp3CodecActivity.this, e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                } finally {
                    if (output != null) {
                        try {
                            output.flush();
                        } catch (IOException e) {
                            Toast.makeText(Mp3CodecActivity.this, e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        } finally {
                            try {
                                output.close();
                            } catch (IOException e) {
                                Toast.makeText(Mp3CodecActivity.this,
                                        e.getMessage(), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    private File getFile(final String suffix) {
        Time time = new Time();
        time.setToNow();
        return new File(Environment.getExternalStorageDirectory(),
                time.format("%Y%m%d%H%M%S") + "." + suffix);
    }


    private static class Homehandle extends Handler {
        private WeakReference<Mp3CodecActivity> wref;

        public Homehandle(Mp3CodecActivity act) {
            wref = new WeakReference<Mp3CodecActivity>(act);
        }

        @Override
        public void handleMessage(Message msg) {
            final Mp3CodecActivity act = wref.get();
            if (act == null) {
                return;
            }
            switch (msg.what) {
                case ENCODER_DONE:
                    Toast.makeText(act, "录音文件已编码完毕!", Toast.LENGTH_SHORT).show();
                    if(act.mp3Decoder == null){
                        act.mp3Decoder = new Mp3DecoderJni();
                        act.ret = act.mp3Decoder.initAudioPlayer(act.mEncodedFile.getAbsolutePath(), 0);
                        if (act.ret == -1) {
                            Log.e("zhongjihao", "====Couldn't open file '" + act.mEncodedFile.getAbsolutePath());
                        } else {
                            act.mThreadFlag = true;
                            act.initAudioPlayer();
                            act.audioBuffer = new short[1024 * 1024];
                            act.mThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while (act.mThreadFlag) {
                                        if (act.mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PAUSED
                                                && act.mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                                            // ****从libmad处获取data******/
                                            act.playCurrentPos = act.mp3Decoder.getAudioBuf(
                                                    act.audioBuffer, act.mAudioMinBufSize);
                                            act.mAudioTrack.write(act.audioBuffer, 0, act.mAudioMinBufSize);
                                            Log.d("", "====播放缓冲大小:  " + act.mAudioMinBufSize
                                                    + "====播放的文件位置: ========" + act.playCurrentPos+"=========文件大小: "+act.mp3Decoder.getAudioFileSize());
                                            if (act.playCurrentPos == 0) {
                                                act.mAudioTrack.stop();
                                                act.handler.sendEmptyMessage(PLAY_DONE);
                                                act.playDone = true;
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
                            act.mThread.start();
                        }
                    }else{
                        act.ret = act.mp3Decoder.initAudioPlayer(act.mEncodedFile.getAbsolutePath(), 0);
                        act.initAudioPlayer();
                    }
                    break;
                case PLAY_DONE:
                    Toast.makeText(act, "play done", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            finish();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)) {
            if(requestCode == TARGET_PERMISSION_REQUEST){
               // btnStart.setEnabled(true);
                hasPermission = true;
            }
        }else{
           // btnStart.setEnabled(false);
            hasPermission = false;
//            Toast.makeText(this, getText(R.string.no_permission_tips), Toast.LENGTH_SHORT)
//                    .show();
        }
    }

}
