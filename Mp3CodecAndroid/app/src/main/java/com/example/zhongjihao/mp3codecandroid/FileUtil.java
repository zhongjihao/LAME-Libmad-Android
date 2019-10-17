package com.example.zhongjihao.mp3codecandroid;

import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Created by zhongjihao100@163.com on 18-8-12.
 */
public class FileUtil {
    /**
     * 判断SD卡是否被挂载
     * @param sdcardPath
     * @return
     */
    public static boolean isMount(String sdcardPath) {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState(new File(sdcardPath)));
    }

    public static String getMP3FileName(long timeMillis){
        return String.format("%1$tY-%1$tm-%1$td_%1$tH_%1$tM_%1$tS_%1$tL.mp3", timeMillis);
    }

    public static File setOutPutFile(String dir,String fileName){
        boolean isSdcardOk = true;
        File directory = new File(dir);
        if (!directory.exists()) {
            isSdcardOk = directory.mkdirs();
        }
        Log.d("FileUtil","setOutPutFile----->isSdcardOk: "+isSdcardOk);
        if (isSdcardOk) {
            try {
                File file = new File(directory, fileName);
                if (!file.exists()) {
                    file.createNewFile();
                }
                return file;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }
}
