package com.bitmovin.bitmovinanalyticscollector.utils;

import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.util.Locale;

/**
 * Created by zachmanc on 12/15/17.
 */

public class DeviceInfo {

    public static String getLocale(){
        return Resources.getSystem().getConfiguration().locale.toString();
    }

    public static long timestamp(){
        return System.currentTimeMillis();
    }



}
