package com.bitmovin.analytics.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Pair;

import com.bitmovin.analytics.BuildConfig;
import com.bitmovin.analytics.CollectorConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Util {
    public static final String DASH_STREAM_FORMAT = "dash";
    public static final String HLS_STREAM_FORMAT = "hls";
    public static final String PROGRESSIVE_STREAM_FORMAT = "progressive";
    public static final String SMOOTH_STREAM_FORMAT = "smooth";
    public static final int MILLISECONDS_IN_SECONDS = 1000;

    private static final Map<String, String> VIDEO_FORMAT_MIME_TYPE_MAP;
    public static final String PLAYER_TECH = "Android:Exoplayer";

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

    /**
     * Returns the time in ms since the system was booted, and guaranteed to be monotonic
     * Details here: https://developer.android.com/reference/android/os/SystemClock
     * @return The time in ms since the system was booted, and guaranteed to be monotonic.
     */
    public static long getElapsedTime() {
        return SystemClock.elapsedRealtime();
    }

    public static long getTimestamp() {
        return System.currentTimeMillis();
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

    public static Integer calculatePercentage(Long numerator, Long denominator, Boolean clamp) {
        if (denominator == null || denominator == 0 || numerator == null) {
            return null;
        }
        int result = Math.round((numerator.floatValue() / denominator.floatValue()) * 100);
        return clamp ? Math.min(result, 100) : result;
    }

    public static Pair<String, String> getHostnameAndPath(String uriString) {
        try {
            URI uri = new URI(uriString);
            return new Pair<>(uri.getHost(), uri.getPath());
        } catch(URISyntaxException ignored) {

        }
        return new Pair<>(null, null);
    }
    
    public static boolean getIsLiveFromConfigOrPlayer(boolean isPlayerReady, Boolean isLiveFromConfig, boolean isLiveFromPlayer) {
        if (isPlayerReady) {
            return isLiveFromPlayer;
        }
        return isLiveFromConfig != null ? isLiveFromConfig : false;
    }

    public static boolean isClassLoaded(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static int getDeviceVolume(Context context){
        AudioManager audioManager;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }
}
