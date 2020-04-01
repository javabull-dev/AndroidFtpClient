package com.ljpc.createfile.config;

import android.content.Context;
import android.util.Log;

import com.ljpc.createfile.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FtpConfig {
    public static final String TAG= FtpConfig.class.getName();
    public static final int FTP = 0,SFTP=1;
    private String name;
    private String ftpUserName;
    private String ftpPassword;
    private int protol;
    private String address;
    private int port;
    private static FtpConfig config=null;

    public static FtpConfig getFtpConfig(){
        if (config==null){
            config = new FtpConfig();
        }
        return config;
    }

    public static void loadParam(Context context) {
        Properties properties = new Properties();
        String absolutePath = context.getExternalFilesDir(null).getAbsolutePath();
        File file = new File(absolutePath + File.separator + "config.properties");
        InputStream inputStream = null;
        if (!file.exists()) {
            inputStream = context.getResources().openRawResource(R.raw.config);
        } else {
            try {
                inputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (Objects.nonNull(inputStream)) {
            try {
                FtpConfig ftpConfig = FtpConfig.getFtpConfig();
                properties.load(inputStream);
                ftpConfig.setAddress(properties.getProperty("ftpaddress"));
                ftpConfig.setPort(Integer.valueOf(properties.getProperty("ftpport")));
                ftpConfig.setProtol(properties.getProperty(
                        "ftpprotol").equals("ftp")?FtpConfig.FTP:FtpConfig.SFTP);
                ftpConfig.setName(properties.getProperty("ftpname"));
                ftpConfig.setFtpPassword(properties.getProperty("ftppassword"));
                ftpConfig.setFtpUserName(properties.getProperty("ftpusername"));
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "loadParam: 加载配置文件失败");
            }
        } else {
            Log.e(TAG, "loadParam: 加载配置文件失败,配置文件缺失");
        }
    }

    public static void saveParam(Context context) {
        Properties properties = new Properties();
        String absolutePath = context.getExternalFilesDir(null).getAbsolutePath();
        File file = new File(absolutePath + File.separator + "config.properties");
        FtpConfig ftpConfig = FtpConfig.getFtpConfig();
        properties.setProperty("ftpaddress", ftpConfig.getAddress());
        properties.setProperty("ftpport", String.valueOf(ftpConfig.getPort()));
        properties.setProperty("ftpprotol", ftpConfig.getProtol()==FtpConfig.FTP?"ftp":"sftp");
        properties.setProperty("ftpname", ftpConfig.getName());
        properties.setProperty("ftppassword", ftpConfig.getFtpPassword());
        properties.setProperty("ftpusername", ftpConfig.getFtpUserName());
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            properties.store(new FileOutputStream(file), "");
        } catch (Exception e) {
            Log.e(TAG, "saveParam: 保存配置文件失败");
        }
    }
}
