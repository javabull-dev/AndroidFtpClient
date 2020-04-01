package com.ljpc.createfile.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Android studio 3.5
 * Company: None
 *
 * @Author: ljpc
 * @Date: 2020-03-24
 * @Time: 11:48
 * @Blog:
 */
public class TimeUtil {

    public static String getCurrentTime(){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
        Date date = new Date(System.currentTimeMillis());
        return format.format(date);
    }
}
