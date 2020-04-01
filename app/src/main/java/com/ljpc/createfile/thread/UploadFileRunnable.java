package com.ljpc.createfile.thread;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ljpc.createfile.handler.MainHandler;
import com.ljpc.createfile.utils.TimeUtil;
import com.ljpc.createfile.utils.UploadFileUtil;

import java.io.File;
import java.io.FileInputStream;

public class UploadFileRunnable implements Runnable{

    private Application context;
    private String filepath;
    private static final String TAG = UploadFileRunnable.class.getName();
    private String content;

    public UploadFileRunnable(Application context,@NonNull String content){
        this.context = context;
        this.content = content;
    }

    @Override
    public void run() {
        MainHandler handler = new MainHandler(context);
        Message m1 = Message.obtain();
        if (!content.isEmpty()) {
            String ctime = TimeUtil.getCurTime();
            filepath = context.getExternalFilesDir(null).getAbsolutePath()
                    + File.separator + ctime + ".txt";
            Log.e(TAG,"run: filepath="+filepath);
            File f = null;
            try {
                f = new File(filepath);
                if (f.exists()) {
                    f.delete();
                }
                UploadFileUtil.writeToPath(filepath, content);
                UploadFileUtil.uploadFile(filepath,context);
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                if (f.exists()){
                    f.delete();
                }
            }
        } else {
            m1.what = MainHandler.NO_CONTENT;
            handler.sendMessage(m1);
        }
    }
}
