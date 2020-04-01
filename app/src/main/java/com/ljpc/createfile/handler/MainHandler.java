package com.ljpc.createfile.handler;

import android.app.Application;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ljpc.createfile.component.MainActivityComponentManager;

import java.util.Objects;

public class MainHandler extends Handler {
    public static final String TAG = MainHandler.class.getName();
    public static final int UPLOAD_FAIL = 0,NO_CONTENT=1,UPLOAD_FINISH=2
            ,UPDATE_PROGRESS=3;
    private Application context;

    public MainHandler(Application application){
        super(application.getMainLooper());
        this.context = application;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        Object obj = msg.obj;
        switch (msg.what) {
            case UPLOAD_FAIL:
                showToast("上传失败");
                updateData(obj);
                break;
            case NO_CONTENT:
                showToast("没有内容");
                break;
            case UPLOAD_FINISH:
                updateData(obj);
                break;
            case UPDATE_PROGRESS:
                updateData(obj);
                break;
            default:
                break;
        }
    }

    private void showToast(@NonNull final String msg){
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private void updateData(Object o){
        if (o instanceof String){
            String text = (String) o;
            EditText board = (EditText)MainActivityComponentManager.get("contentBoard");
            if (Objects.nonNull(board)){
                board.append(text);
            }
        }
    }
}
