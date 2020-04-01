package com.ljpc.createfile.message;

import android.app.Application;
import android.os.Handler;
import android.widget.Toast;

public class PushToMainMessage {

    public static void pushMessage(final Application app, final String msg){
        Handler handler = new Handler(app.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(app, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void pushUpdateProgressData(final Application application,final int progress){
        Handler handler = new Handler(application.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {

            }
        });
    }
}
