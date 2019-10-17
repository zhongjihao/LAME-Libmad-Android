package com.example.zhongjihao.mp3codecandroid;

/**
 * Created by zhongjihao100@163.com on 18-8-26.
 */

public class PcmBuffer {
    private short[] rawData;
    private int readSize;

    public PcmBuffer(short[] rawData, int readSize) {
        this.rawData = rawData.clone();
        this.readSize = readSize;
    }

    public short[] getData() {
        return rawData;
    }

    public int getReadSize() {
        return readSize;
    }
}
