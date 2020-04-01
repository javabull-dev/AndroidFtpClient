package com.ljpc.createfile.thread;

import android.app.Application;
import android.os.Message;
import android.util.Log;

import com.ljpc.createfile.handler.MainHandler;
import com.ljpc.createfile.util.FileUtil;
import com.ljpc.createfile.util.TimeUtil;
import com.ljpc.createfile.utils.UploadFileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;

/**
 * 上传日志板的内容至ftp服务器
 * 1.将日志板的内容保存至系统的临时文件区
 * 2.将临时文件上传ftp服务器
 */
public class UploadContentRunnable implements Runnable{

    private Application context;
    private String content;
    private String filepath;
    private MainHandler handler;

    private static final String TAG = UploadFileRunnable.class.getName();

    public UploadContentRunnable(Application context, String content){
        this.context = context;
        this.content = content;
        handler = new MainHandler(context);
    }

    @Override
    public void run() {
        boolean success = true;
        String time = TimeUtil.getCurrentTime();
        File tempFile = FileUtil.createTempFile(time, ".txt", null, true);

        if (Objects.nonNull(tempFile)){
            //写入临时文件
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));
                writer.write(content,0,content.length());
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
                Message msg = Message.obtain();
                msg.what = MainHandler.UPLOAD_FAIL;
                handler.sendMessage(msg);
            } finally {
                if (Objects.nonNull(writer)){
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //上传ftp服务器
            try {
                UploadFileUtil.uploadFile(tempFile.getAbsolutePath(),context);
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
                Message msg = Message.obtain();
                msg.what = MainHandler.UPLOAD_FAIL;
                handler.sendMessage(msg);
            }

            if (success){
                Message msg = Message.obtain();
                msg.what = MainHandler.UPLOAD_FINISH;
                handler.sendMessage(msg);
            }

            if (tempFile.exists()){
                if (tempFile.delete()){
                    Log.e(TAG, "删除文件成功.....");
                }else {
                    Log.e(TAG, "删除文件失败.....");
                }
            }
        }else {
            Log.e(TAG,"run: 创建临时文件失败");
        }
    }
}
