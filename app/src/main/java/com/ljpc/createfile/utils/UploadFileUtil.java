package com.ljpc.createfile.utils;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;

import com.ljpc.createfile.config.FtpConfig;
import com.ljpc.createfile.listener.FTPDataTransferListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Objects;

import it.sauronsoftware.ftp4j.FTPClient;

public class UploadFileUtil {

    public static void uploadFile(String filepath,Application application) throws Exception {
        File file = new File(filepath);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            uploadFile(fis,file.getName(),application);
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }finally {
            if (Objects.nonNull(fis)){
                fis.close();
            }
        }
    }

    public static void uploadFile(InputStream inputStream, String filename,Application application)
            throws Exception {
        FtpConfig config = FtpConfig.getFtpConfig();
        String ftpPassword = config.getFtpPassword();
        String userName = config.getFtpUserName();
        String host = config.getAddress();
        int port = config.getPort();
        if (Objects.isNull(userName) || Objects.isNull(ftpPassword) || Objects.isNull(host)) {
        } else {
            FTPClient client = null;
            try {
                client = new FTPClient();
                client.connect(host, port);
                client.login(userName, ftpPassword);
                FTPDataTransferListener listener = new FTPDataTransferListener(inputStream.available()
                        ,10,application,filename);
                client.upload(filename, inputStream, 0, 0, listener);
            }catch (Exception e){
                e.printStackTrace();
                throw new Exception(e.getMessage());
            }finally {
                if (Objects.nonNull(client)){
                    client.disconnect(true);
                }
            }
        }
    }

    public static void writeToPath(String filepath, String content) throws Exception {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(new File(filepath))));
            writer.write(content, 0, content.length());
            writer.flush();
        }catch (Exception e){
            throw new Exception(e.getMessage());
        }finally {
            if (!Objects.isNull(writer)) {
                writer.close();
            }
        }
    }

    public static String getFilenameFromUri(Context context, @NonNull Uri uri) {
        Cursor cursor = null;
        String summary = null;
        try {
            if (MediaStore.AUTHORITY.equals(uri.getAuthority())) {
                // Fetch the ringtone title from the media provider
                cursor = context.getContentResolver().query(uri,
                        new String[]{MediaStore.Audio.Media.TITLE}, null, null, null);
            } else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                cursor = context.getContentResolver().query(uri,
                        new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            }
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    summary = cursor.getString(0);
                }
            }
        } catch (SQLiteException sqle) {
            // Unknown title for the ringtone
        } catch (IllegalArgumentException iae) {
            // Some other error retrieving the column from the provider
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return summary;
    }

    public static String getRealFilePath(@NonNull final Application app, @NonNull final Uri uri ) {
        Cursor cursor = null;
        String data = null;
        if ( MediaStore.AUTHORITY.equals(uri.getAuthority()) ) {
            cursor = app.getContentResolver().query(uri,
                    new String[]{MediaStore.Audio.Media.DATA}, null, null, null);
        } else if ( ContentResolver.SCHEME_CONTENT.equals( uri.getScheme() ) ) {
            cursor = app.getContentResolver().query( uri,
                    new String[] { MediaStore.Files.FileColumns.DATA }, null, null, null );
        }
        if ( null != cursor ) {
            if ( cursor.moveToFirst() ) {
                data = cursor.getString(0);
            }
            cursor.close();
        }
        return data;
    }
}
