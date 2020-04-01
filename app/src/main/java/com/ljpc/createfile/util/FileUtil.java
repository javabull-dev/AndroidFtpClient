package com.ljpc.createfile.util;

import android.util.Log;

import com.ljpc.createfile.exception.IORuntimeException;

import java.io.File;
import java.io.IOException;

/**
 * Created by Android studio 3.5
 * Company: None
 *
 * @Author: ljpc
 * @Date: 2020-03-24
 * @Time: 11:36
 * @Blog:
 */
public class FileUtil {

    public static String TAG = FileUtil.class.getName();
    
    public File newTempFile(String prefix, String suffix){
        File ret = null;
        try {
            ret = File.createTempFile(prefix,suffix);
        } finally {
            return ret;
        }
    }

    public static File createTempFile(String prefix, String suffix, File dir, boolean isReCreat) throws IORuntimeException{
        int exceptionsCount = 0;
        while (true) {
            try {
                File file = File.createTempFile(prefix, suffix, dir).getCanonicalFile();
                if (isReCreat) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                    //noinspection ResultOfMethodCallIgnored
                    file.createNewFile();
                }
                return file;
            } catch (IOException ioex) { // fixes java.io.WinNTFileSystem.createFileExclusively access denied
                if (++exceptionsCount >= 50) {
                    //
                    Log.e(TAG, "createTempFile: 创建临时文件出错,错误->"+ioex.getMessage());
                    throw new IORuntimeException(ioex.getCause());
                }
            }
        }
    }
}
