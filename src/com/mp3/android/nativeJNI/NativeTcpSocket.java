package com.mp3.android.nativeJNI;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class NativeTcpSocket {
    private RecvDoneNotify callback;
    static{
        System.loadLibrary("tcpsocket");
    }
    private Context context;
    
    public NativeTcpSocket(Context cxt,RecvDoneNotify listen) {
        this.context = cxt;
        this.callback = listen;
    }

    private void showToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
    
    public interface RecvDoneNotify{
        public void audioRecvDone(String audioFile);
    }
    
    private void recvAudioDone(String audioFile){
        Log.d("NativeMp3Encoder", "========recvAudioDone()========");
        callback.audioRecvDone(audioFile);
    }
    
    public native void initGlobalObject();
    public native void destroyGlobalObject();
    public native void tcpBind(String ip, int port);
    public native void tcpConnect(String ip, int port);
    public native void setRecvDir(String recvDir);
    public native void setSendFilePath(String filepath);
    public native void startSendThread();
    public native void destroyTcpSocket();
}
