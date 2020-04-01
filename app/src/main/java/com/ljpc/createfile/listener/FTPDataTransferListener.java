package com.ljpc.createfile.listener;

import android.app.Application;
import android.os.Message;
import android.util.Log;

import com.ljpc.createfile.handler.MainHandler;

public class FTPDataTransferListener implements
        it.sauronsoftware.ftp4j.FTPDataTransferListener {
    public static final String TAG = FTPDataTransferListener.class.getName();
    private int size;
    private int count = 0;
    private int frequency;
    private float pass = 0;
    Application application;
    private String filename;

    /**
     * @param filesize:文件大小
     * @param frequency：刷新频率
     */
    public FTPDataTransferListener(int filesize, int frequency, Application application, String filename) {
        this.size = filesize;
        this.frequency = frequency;
        this.application = application;
        this.filename = filename;
    }

    @Override
    public void started() {
        Log.e(TAG, "started: 开始上传 filename="+filename);
    }

    @Override
    public void transferred(int var1) {
        MainHandler handler = new MainHandler(application);
        if (count == frequency) {
            int pro = (int) ((pass / size) * 100);
            Message message = Message.obtain();
            message.obj = String.format("文件:[%s] 已上传:%s%%\n",filename, String.valueOf(pro));
            message.what = MainHandler.UPDATE_PROGRESS;
            handler.sendMessage(message);
            count = 0;
        }
        count++;
        pass += var1;
    }

    @Override
    public void completed() {
        MainHandler handler = new MainHandler(application);
        //上传结束，结束progress
        Message message = Message.obtain();
        message.obj = String.format("文件:[%s]上传完成\n",filename);
        message.what = MainHandler.UPLOAD_FINISH;
        handler.sendMessage(message);
        Log.e(TAG, "completed: 上传完成 filename="+filename);
    }

    @Override
    public void aborted() {
        failed();
    }

    @Override
    public void failed() {
        //上传失败
        MainHandler handler = new MainHandler(application);
        Message message = Message.obtain();
        message.obj = String.format("文件:[%s]上传失败\n",filename);
        message.what = MainHandler.UPLOAD_FAIL;
        handler.sendMessage(message);
    }
}
