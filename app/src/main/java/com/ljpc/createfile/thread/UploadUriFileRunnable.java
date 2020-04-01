package com.ljpc.createfile.thread;

import android.app.Application;
import android.net.Uri;
import android.os.Message;
import android.util.Log;

import com.ljpc.createfile.handler.MainHandler;
import com.ljpc.createfile.utils.UploadFileUtil;

import java.io.InputStream;

public class UploadUriFileRunnable implements Runnable {
    public static final String  TAG = UploadUriFileRunnable.class.getName();
    private Application context;
    private Uri uri;

    public UploadUriFileRunnable(Application context,Uri uri){
        this.context = context;
        this.uri = uri;
    }

    @Override
    public void run() {
        String fn = UploadFileUtil.getFilenameFromUri(context,uri);
        //文件，打开文件，显示文件内容在 mContentEdit中，并保存文件路径 和 文件名
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            UploadFileUtil.uploadFile(inputStream,fn,context);
        } catch (Exception e) {
            Message message = Message.obtain();
            message.what = MainHandler.UPLOAD_FAIL;
            new MainHandler(context).sendMessage(message);
            Log.e(TAG,"UploadUriFileRunnable: error="+e.getMessage());
        }
    }
}
