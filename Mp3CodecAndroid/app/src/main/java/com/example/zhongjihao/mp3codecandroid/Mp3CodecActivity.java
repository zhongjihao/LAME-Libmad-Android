package com.example.zhongjihao.mp3codecandroid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.zhongjihao.mp3codecandroid.mp3codec.IMP3EncoderDoneNotify;


/**
 * Created by zhongjihao100@163.com on 18-8-12.
 */
public class Mp3CodecActivity extends AppCompatActivity implements View.OnClickListener,IMP3EncoderDoneNotify {
    private static final String TAG = "Mp3CodecActivity";
    private boolean hasPermission;
    private AudioGather audioRecord;
    private Mp3Play mp3Play;

    private Button startRecordMP3Btn;
    private Button stopRecordMP3Btn;
    private Button startRecordWavBtn;
    private Button stopRecordWavBtn;
    private Button playMp3Btn;
    private Button pauseMp3Btn;
    private Button stopMp3Btn;
    private Button rePlayMp3Btn;
    private String mp3Path;

    private static final int TARGET_PERMISSION_REQUEST = 100;

    // 要申请的权限
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp3_codec);

        startRecordMP3Btn = (Button) findViewById(R.id.StartRecordMP3);
        stopRecordMP3Btn = (Button) findViewById(R.id.StopRecordMP3);
        startRecordWavBtn = (Button) findViewById(R.id.StartRecordWAV);
        stopRecordWavBtn = (Button) findViewById(R.id.StopRecordWAV);
        playMp3Btn = (Button)findViewById(R.id.playMp3);
        pauseMp3Btn = (Button)findViewById(R.id.pauseMp3);
        stopMp3Btn = (Button)findViewById(R.id.stopMp3);
        rePlayMp3Btn = (Button)findViewById(R.id.rePlayMp3);

        hasPermission = false;
        audioRecord = AudioGather.getInstance();
        mp3Play = new Mp3Play(this);

        startRecordMP3Btn.setOnClickListener(this);
        stopRecordMP3Btn.setOnClickListener(this);
        startRecordWavBtn.setOnClickListener(this);
        stopRecordWavBtn.setOnClickListener(this);
        playMp3Btn.setOnClickListener(this);
        pauseMp3Btn.setOnClickListener(this);
        stopMp3Btn.setOnClickListener(this);
        rePlayMp3Btn.setOnClickListener(this);

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

    private void startMP3Record(){
        if(audioRecord != null && audioRecord.isRecording()){
            audioRecord.stopRecord();
        }
        audioRecord.prepareAudioRecord();
        final MP3Encoder mp3Encoder = new MP3Encoder();
        audioRecord.setMp3Encoder(mp3Encoder);
        mp3Encoder.setOutputPath(Environment.getExternalStorageDirectory()+"/"+"audio_dir",FileUtil.getMP3FileName(System.currentTimeMillis()));
        mp3Path = mp3Encoder.getMp3Path();
        mp3Encoder.initMP3Encoder(audioRecord.getaChannelCount(),audioRecord.getaSampleRate(),audioRecord.getaSampleRate(),MP3Encoder.BITRATE,MP3Encoder.MODE,MP3Encoder.QUALITY,audioRecord.getMin_buffer_size());
        //启动MP3编码线程
        mp3Encoder.start();
        //设置AudioRecord录制监听
        audioRecord.setRecordListener(mp3Encoder, mp3Encoder.getHandler(),AudioGather.FRAME_COUNT);
        audioRecord.setCallback(new AudioGather.PcmCallback() {
            @Override
            public void addPcmData(short[] pcmData, int elementNum) {
                mp3Encoder.addPcmData(pcmData,elementNum);
            }
        });
        audioRecord.startRecord();
    }

    private void startWAVRecord(){
        if(audioRecord != null && audioRecord.isRecording()){
            audioRecord.stopRecord();
        }
        audioRecord.prepareAudioRecord();
        final WavCoder wavCoder = new WavCoder();
        audioRecord.setWavCoder(wavCoder);
        wavCoder.setOutputPath(Environment.getExternalStorageDirectory()+"/"+"audio_dir/"+FileUtil.getWavFileName(System.currentTimeMillis()));
        wavCoder.initWavEncoder(audioRecord.getaChannelCount(),audioRecord.getaSampleRate(),AudioGather.AUDIO_FORMAT.getBytesPerFrame());
        //启动WAV编码线程
        wavCoder.start();
        //设置AudioRecord录制监听
        audioRecord.setRecordListener(wavCoder,wavCoder.getHandler(),AudioGather.FRAME_COUNT);
        audioRecord.setCallback(new AudioGather.PcmCallback() {
            @Override
            public void addPcmData(short[] pcmData, int elementNum) {
                wavCoder.addPcmData(pcmData,elementNum);
            }
        });
        audioRecord.startRecord();
    }

    @Override
    public void encoderMp3Done(){
        Log.d(TAG,"PCM file encode MP3 file Done!");

    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        switch (vid){
            case R.id.StartRecordMP3:{
                startMP3Record();
                break;
            }
            case R.id.StopRecordMP3:{
                if(audioRecord != null && audioRecord.isRecording()){
                    audioRecord.stopRecord();
                }
                break;
            }
            case R.id.StartRecordWAV:{
                startWAVRecord();
                break;
            }
            case R.id.StopRecordWAV:{
                if(audioRecord != null && audioRecord.isRecording()){
                    audioRecord.stopRecord();
                }
                break;
            }
            case R.id.playMp3:{
                if(TextUtils.isEmpty(mp3Path)){
                    Toast.makeText(this, "请先录制MP3,然后再播放!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if(audioRecord != null && audioRecord.isRecording()){
                    audioRecord.stopRecord();
                }
                mp3Play.setPlaySource(mp3Path);
                mp3Play.prepare();
                mp3Play.startPlayMP3();
                break;
            }
            case R.id.pauseMp3:{
                if(audioRecord != null && audioRecord.isRecording()){
                    audioRecord.stopRecord();
                }
                mp3Play.pausePlayMP3();
                break;
            }
            case R.id.rePlayMp3:{
                if(audioRecord != null && audioRecord.isRecording()){
                    audioRecord.stopRecord();
                }
                mp3Play.rePlayMP3();
                break;
            }
            case R.id.stopMp3:{
                if(audioRecord != null && audioRecord.isRecording()){
                    audioRecord.stopRecord();
                }
                mp3Play.stopPlayMP3();
                mp3Play.release();
                break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(audioRecord != null && audioRecord.isRecording()){
            audioRecord.stopRecord();
        }
        if(mp3Play != null){
            mp3Play.release();
        }
        System.exit(0);
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
                startRecordMP3Btn.setEnabled(true);
                startRecordWavBtn.setEnabled(true);
                hasPermission = true;
            }
        }else{
            startRecordMP3Btn.setEnabled(false);
            startRecordWavBtn.setEnabled(false);
            hasPermission = false;
            Toast.makeText(this, getText(R.string.no_permission_tips), Toast.LENGTH_SHORT)
                    .show();
        }
    }

}
