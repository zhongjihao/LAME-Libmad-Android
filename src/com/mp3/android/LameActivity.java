
package com.mp3.android;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import com.mp3.android.nativeJNI.NativeMP3Decoder;
import com.mp3.android.nativeJNI.NativeMp3Encoder;
import com.mp3.android.nativeJNI.NativeMp3Encoder.EncoderDoneNotify;
import com.mp3.android.nativeJNI.NativeTcpSocket;
import com.mp3.android.nativeJNI.NativeTcpSocket.RecvDoneNotify;
import com.samsung.sample.lame4android.R;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class LameActivity extends Activity implements OnClickListener,EncoderDoneNotify,RecvDoneNotify{
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
    // 作为TCP Client端
    private final static String localIP = "192.168.1.109";
    private final static int localPort = 7788;
    private final static String remoteIP = "192.168.1.106";
    private final static int remotePort = 7799;

    // 作为TCP Server端
//     private final static String localIP = "192.168.1.106";
//     private final static int localPort = 7799;
//     private final static String remoteIP = "192.168.1.109";
//     private final static int remotePort = 7788;

    private boolean mIsRecording = false;
    private File mRawFile;
    private File mEncodedFile;
    private String recvFile;

    private Button recordBtn;
    private Button tcpConBtn;
    private Button tcpBindBtn;
    private LinearLayout local_music_llayout;
    private Button playLocalMp3Btn;
    private Button pauseLocalMp3Btn;
    private Button stopLocalMp3Btn;
    private LinearLayout remote_music_llayout;
    private Button playRemoteMp3Btn;
    private Button pauseRemoteMp3Btn;
    private Button stopRemoteMp3Btn;

    private NativeMp3Encoder mp3Encoder;
    private NativeMP3Decoder mp3Decoder;
    private NativeTcpSocket  tcpsocket;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lame);
        mp3Encoder = new NativeMp3Encoder(this);
        tcpsocket = new NativeTcpSocket(this,this);
        handler = new Homehandle(this);
        initRecorder();
        tcpsocket.initGlobalObject();
        mp3Encoder.initGlobalObject();
        mp3Encoder.initEncoder(NUM_CHANNELS, SAMPLE_RATE, BITRATE, MODE, QUALITY);

        recordBtn = (Button) findViewById(R.id.startRecordBtn);
        recordBtn.setText(startRecordingLabel);
        tcpConBtn = (Button) findViewById(R.id.tcpConnectBtn);
        tcpBindBtn = (Button) findViewById(R.id.tcpBindBtn);
        local_music_llayout = (LinearLayout)findViewById(R.id.local_music_llayout);
        local_music_llayout.setVisibility(View.GONE);
        playLocalMp3Btn = (Button) findViewById(R.id.playLocalBtn);
        pauseLocalMp3Btn = (Button)findViewById(R.id.pauseLocalBtn);
        stopLocalMp3Btn = (Button)findViewById(R.id.stopLocalBtn);
        remote_music_llayout = (LinearLayout)findViewById(R.id.remote_music_llayout);
        remote_music_llayout.setVisibility(View.GONE);
        playRemoteMp3Btn = (Button) findViewById(R.id.playRemoteBtn);
        pauseRemoteMp3Btn = (Button)findViewById(R.id.pauseRemoteBtn);
        stopRemoteMp3Btn = (Button)findViewById(R.id.stopRemoteBtn);

        recordBtn.setOnClickListener(this);
        tcpConBtn.setOnClickListener(this);
        tcpBindBtn.setOnClickListener(this);
        playLocalMp3Btn.setOnClickListener(this);
        pauseLocalMp3Btn.setOnClickListener(this);
        stopLocalMp3Btn.setOnClickListener(this);
        playRemoteMp3Btn.setOnClickListener(this);
        pauseRemoteMp3Btn.setOnClickListener(this);
        stopRemoteMp3Btn.setOnClickListener(this);
        
        // TCP Client
//        tcpConBtn.setVisibility(View.VISIBLE);
//        tcpBindBtn.setVisibility(View.GONE);

        // TCP Server
//         tcpConBtn.setVisibility(View.GONE);
//         tcpBindBtn.setVisibility(View.VISIBLE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_lame, menu);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRecorder.release();
        mp3Encoder.destroyEncoder();
        mp3Encoder.destroyGlobalObject();
        if(mAudioTrack != null){
            mAudioTrack.stop();
            mAudioTrack.release();// 关闭并释放资源
        }    
        mThreadFlag = false;// 音频线程停止
        if(mp3Decoder != null){
            mp3Decoder.closeAudioFile();
        }    
        tcpsocket.destroyGlobalObject();
        tcpsocket.destroyTcpSocket();
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
                    Toast.makeText(LameActivity.this, e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                } finally {
                    if (output != null) {
                        try {
                            output.flush();
                        } catch (IOException e) {
                            Toast.makeText(LameActivity.this, e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        } finally {
                            try {
                                output.close();
                            } catch (IOException e) {
                                Toast.makeText(LameActivity.this,
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

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        switch (vid) {
        case R.id.startRecordBtn:
            if (!mIsRecording) {
                recordBtn.setText(stopRecordingLabel);
                mIsRecording = true;
                mRecorder.startRecording();
                mRawFile = getFile("raw");
                startBufferedWrite(mRawFile);
            } else {
                recordBtn.setText(startRecordingLabel);
                mRecorder.stop();
                mIsRecording = false;    
                mEncodedFile = getFile("mp3");
                int result = mp3Encoder.encodeFile(mRawFile.getAbsolutePath(),
                        mEncodedFile.getAbsolutePath());
                if (result == 0) {
                    Toast.makeText(LameActivity.this,
                            "Encoded to " + mEncodedFile.getName(),
                            Toast.LENGTH_SHORT).show();
                }
            }
            break;
        case R.id.tcpConnectBtn:
            tcpsocket.tcpConnect(remoteIP, remotePort);
            break;
        case R.id.tcpBindBtn:       
            tcpsocket.tcpBind(localIP, localPort);
            tcpsocket.setRecvDir(Environment
                    .getExternalStorageDirectory().getAbsolutePath());
            break;
        case R.id.playLocalBtn:
            if(isStopPlay){
                ret = mp3Decoder.initAudioPlayer(mEncodedFile.getAbsolutePath(), 0);
                isStopPlay = false;
            }
            if (ret == -1) {
                Log.e("zhongjihao", "==========Couldn't open file " + mEncodedFile.getAbsolutePath());
                Toast.makeText(this, "Couldn't open file " + mEncodedFile.getAbsolutePath(),
                        Toast.LENGTH_SHORT).show();
            } else {
                if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED) {
                    if (playDone) {
                        mp3Decoder.rePlayAudioFile();
                    }
                    mAudioTrack.play();
                    playDone = false;
                } else if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
                    mAudioTrack.play();
                } else {
                    Toast.makeText(getApplicationContext(), "Already in play",
                            Toast.LENGTH_SHORT).show();
                }
            }
            break;
        case R.id.pauseLocalBtn:
        case R.id.pauseRemoteBtn:
            if (ret == -1) {
                Log.e("zhongjihao", "========Couldn't open file " + mEncodedFile.getAbsolutePath());
                Toast.makeText(this, "Couldn't open file " + mEncodedFile.getAbsolutePath(),
                        Toast.LENGTH_SHORT).show();
            } else {
                if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    mAudioTrack.pause();
                } else {
                    Toast.makeText(getApplicationContext(), "Already pause",
                            Toast.LENGTH_SHORT).show();
                }
            }
            break;
        case R.id.stopLocalBtn:
        case R.id.stopRemoteBtn:
            if (ret == -1) {
                Log.e("zhongjihao", "========Couldn't open file " + mEncodedFile.getAbsolutePath());
                Toast.makeText(this, "Couldn't open file " + mEncodedFile.getAbsolutePath(),
                        Toast.LENGTH_SHORT).show();
            } else {
                if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING
                        ||mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
                    isStopPlay = true;
                    mAudioTrack.stop();
                    mp3Decoder.closeAudioFile();
                } else if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED) {
                    Toast.makeText(getApplicationContext(), "Already stop",
                            Toast.LENGTH_SHORT).show();
                }
            }
            break;
        case R.id.playRemoteBtn:
            if(isStopPlay){
                ret = mp3Decoder.initAudioPlayer(recvFile, 0);
                isStopPlay = false;
            }
            if (ret == -1) {
                Log.e("zhongjihao", "==========Couldn't open file " + recvFile);
                Toast.makeText(this, "Couldn't open file " + recvFile,
                        Toast.LENGTH_SHORT).show();
            } else {
                if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED) {
                    if (playDone) {
                        mp3Decoder.rePlayAudioFile();
                    }
                    mAudioTrack.play();
                    playDone = false;
                } else if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
                    mAudioTrack.play();
                } else {
                    Toast.makeText(getApplicationContext(), "Already in play",
                            Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }

    }

    @Override
    public void encoderNotify() {
        Log.d("LameActivity", "========encoderNotify()=======");
        handler.sendEmptyMessage(ENCODER_DONE);
        //编码完后,先不要启动发送线程
//        tcpsocket.setSendFilePath(mEncodedFile.getAbsolutePath());
//        tcpsocket.startSendThread(); 
    }
    
    private static class Homehandle extends Handler {
        private WeakReference<LameActivity> wref;

        public Homehandle(LameActivity act) {
            wref = new WeakReference<LameActivity>(act);
        }

        @Override
        public void handleMessage(Message msg) {
            final LameActivity act = wref.get();
            if (act == null) {
                return;
            }
            switch (msg.what) {
            case ENCODER_DONE:
                   Toast.makeText(act, "录音文件已编码完毕!", Toast.LENGTH_SHORT).show();
                   act.local_music_llayout.setVisibility(View.VISIBLE);
                   if(act.mp3Decoder == null){
                           act.mp3Decoder = new NativeMP3Decoder();
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
                                                       + "====播放的文件位置: ========" + act.playCurrentPos);
                                               if (act.playCurrentPos == act.mp3Decoder.getAudioFileSize() ||act.playCurrentPos == 0) {
                                                   act.mAudioTrack.stop();
                                                   act.playCurrentPos = 0;
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
                   }
                   break;
            case RECV_DONE:
                Toast.makeText(act, "收到新的语音消息!", Toast.LENGTH_SHORT).show();
                act.remote_music_llayout.setVisibility(View.VISIBLE);
                if(act.mp3Decoder == null){
                    act.mp3Decoder = new NativeMP3Decoder();
                    act.ret = act.mp3Decoder.initAudioPlayer(act.recvFile, 0);
                    if (act.ret == -1) {
                        Log.e("zhongjihao", "====Couldn't open file '" + act.recvFile);
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
                                                + "====播放的文件位置: ========" + act.playCurrentPos);
                                        if (act.playCurrentPos == act.mp3Decoder.getAudioFileSize()) {
                                            act.mAudioTrack.stop();
                                            act.playCurrentPos = 0;
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
                act.ret = act.mp3Decoder.initAudioPlayer(act.recvFile, 0);
            }
                break;
            case PLAY_DONE:
                Toast.makeText(act, "play done", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    @Override
    public void audioRecvDone(String audioFile) {
        Log.d("LameActivity", "========audioRecvDone()=======");
        recvFile = audioFile;
        handler.sendEmptyMessage(RECV_DONE);
    }
}
