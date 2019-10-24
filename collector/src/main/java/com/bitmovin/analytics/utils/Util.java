package com.bitmovin.analytics.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.provider.Settings;

import com.bitmovin.analytics.BuildConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Util {
    public static final String DASH_STREAM_FORMAT = "dash";
    public static final String HLS_STREAM_FORMAT = "hls";
    public static final String PROGRESSIVE_STREAM_FORMAT = "progressive";
    public static final String SMOOTH_STREAM_FORMAT = "smooth";
    public static final int MILLISECONDS_IN_SECONDS = 1000;

    private static final Map<String, String> VIDEO_FORMAT_MIME_TYPE_MAP;

    static {
        VIDEO_FORMAT_MIME_TYPE_MAP = new HashMap<>();
        VIDEO_FORMAT_MIME_TYPE_MAP.put("avc", "video/avc");
        VIDEO_FORMAT_MIME_TYPE_MAP.put("hevc", "video/hevc");
        VIDEO_FORMAT_MIME_TYPE_MAP.put("vp9", "video/x-vnd.on2.vp9");
    }


    public static String getUUID() {
        return UUID.randomUUID().toString();
    }

    public static String getUserId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    public static String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public static long getTimeStamp() {
        return System.currentTimeMillis();
    }

    public static String getPlayerTech() {
        return "Android:Exoplayer";
    }

    public static String getLocale() {
        return Resources.getSystem().getConfiguration().locale.toString();
    }

    public static List<String> getSupportedVideoFormats() {
        List<String> codecs = new ArrayList<>();
        for (String format:
             VIDEO_FORMAT_MIME_TYPE_MAP.keySet()) {
            if(isMimeTypeSupported(VIDEO_FORMAT_MIME_TYPE_MAP.get(format))) {
                codecs.add(format);
            }
        }
        return codecs;
    }

    @SuppressWarnings("deprecation")
    public static boolean isMimeTypeSupported(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getDeviceOrientation(Context context) {
        int deviceOrientation = context.getResources().getConfiguration().orientation;
        switch (deviceOrientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                return "portrait";
            case Configuration.ORIENTATION_LANDSCAPE:
                return "landscape";
            default:
                return "";
        }
    }
}
