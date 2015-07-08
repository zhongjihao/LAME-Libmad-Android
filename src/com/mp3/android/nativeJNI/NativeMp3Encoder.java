
package com.mp3.android.nativeJNI;

import android.util.Log;

public class NativeMp3Encoder {
    private EncoderDoneNotify callback;
    static {
        System.loadLibrary("mp3encoder");
    }
    
    public NativeMp3Encoder(EncoderDoneNotify notify) {
        callback = notify;
    }
    
    public interface EncoderDoneNotify{
        public void encoderNotify();
    }
    
    private void mp3EncoderDone(){
        Log.d("NativeMp3Encoder", "========mp3EncoderDone()========");
        callback.encoderNotify();
    }
    public native void initGlobalObject();
    public native void destroyGlobalObject();
    public native void initEncoder(int numChannels, int sampleRate,
            int bitRate, int mode, int quality);
    public native void destroyEncoder();
    public native int encodeFile(String sourcePath, String targetPath);
}
