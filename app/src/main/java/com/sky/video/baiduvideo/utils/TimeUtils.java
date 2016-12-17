package com.sky.video.baiduvideo.utils;

/**
 * Created by BlueSky on 16/12/17.
 */

public class TimeUtils {
    //毫秒转成HH:MM:SS
    public static String formatSecond(int second) {
        int hh = second / 3600;
        int mm = second % 3600 / 60;
        int ss = second % 60;
        String strTemp = null;
        if (0 != hh) {
            strTemp = String.format("%02d:%02d:%02d", hh, mm, ss);
        } else {
            strTemp = String.format("%02d:%02d", mm, ss);
        }
        return strTemp;
    }

}
