package com.ljpc.createfile.thread;

import android.app.Activity;
import android.os.Message;

import com.ljpc.createfile.config.FtpConfig;
import com.ljpc.createfile.handler.RemoteFileHandler;

import java.io.IOException;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

public class ObtainRemoteFileRunnable implements Runnable {
    private Activity activity;

    public ObtainRemoteFileRunnable(Activity activity){
        this.activity = activity;
    }

    @Override
    public void run() {
        RemoteFileHandler handler = new RemoteFileHandler(activity);
        FtpConfig ftpConfig = FtpConfig.getFtpConfig();
        String name = ftpConfig.getFtpUserName();
        String password = ftpConfig.getFtpPassword();
        String address = ftpConfig.getAddress();
        int port = ftpConfig.getPort();
        FTPClient client = new FTPClient();
        try {
            client.connect(address,port);
            client.login(name,password);
            FTPFile[] files = client.list();
            for (FTPFile file:files){
                file.getName();
                file.getType();
            }
            Message message = Message.obtain();
            message.what = RemoteFileHandler.OBTAIN_SUCCESS;
            message.obj = files;
            handler.sendMessage(message);
        } catch (Exception e) {
            Message message = Message.obtain();
            message.what = RemoteFileHandler.OBTAIN_SUCCESS;
            message.obj = e.getMessage();
            handler.sendMessage(message);
        }finally {
            if (client.isConnected()){
                try {
                    client.disconnect(true);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (FTPIllegalReplyException e) {
                    e.printStackTrace();
                } catch (FTPException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
