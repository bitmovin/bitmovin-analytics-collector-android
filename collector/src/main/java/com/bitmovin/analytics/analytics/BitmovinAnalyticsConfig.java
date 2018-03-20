package com.bitmovin.analytics.analytics;

import android.content.Context;

import com.bitmovin.analytics.enums.CDNProvider;
import com.bitmovin.analytics.enums.PlayerType;

public class BitmovinAnalyticsConfig {
    public static final String analyticsUrl = "https://analytics-ingress-global.bitmovin.com/analytics";
    public static final String licenseUrl = "https://analytics-ingress-global.bitmovin.com/licensing";

    private String key;
    private String playerKey;
    private String cdnProvider;
    private String videoId;
    private PlayerType playerType;
    private String customUserId;
    private String customData1;
    private String customData2;
    private String customData3;
    private String customData4;
    private String customData5;
    private String experimentName;
    private String path;
    private Context context;
    private int heartbeatInterval = 59700;

    public BitmovinAnalyticsConfig(String key, Context context) {
        this.key = key;
        this.playerKey = "";
        this.context = context;
    }

    public BitmovinAnalyticsConfig(String key, String playerKey, Context context) {
        this.key = key;
        this.playerKey = playerKey;
        this.context = context;
    }

    public static String getAnalyticsUrl() {
        return analyticsUrl;
    }

    public String getKey() {
        return key;
    }

    public String getPlayerKey() {
        return playerKey;
    }

    public String getCdnProvider() {
        return cdnProvider;
    }

    /**
     * CDN Provider used to play out Content
     *
     * @param cdnProvider {@link CDNProvider}
     */
    public void setCdnProvider(String cdnProvider) {
        this.cdnProvider = cdnProvider;
    }

    public String getVideoId() {
        return videoId;
    }

    /**
     * ID of the Video in the Customer System
     *
     * @param videoId
     */
    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getCustomUserId() {
        return customUserId;
    }

    /**
     * User-ID in the Customer System
     *
     * @param customUserId
     */
    public void setCustomUserId(String customUserId) {
        this.customUserId = customUserId;
    }

    public String getCustomData1() {
        return customData1;
    }

    /**
     * Optional free-form data
     *
     * @param customData1
     */
    public void setCustomData1(String customData1) {
        this.customData1 = customData1;
    }

    public String getCustomData2() {
        return customData2;
    }

    /**
     * Optional free-form data
     *
     * @param customData2
     */
    public void setCustomData2(String customData2) {
        this.customData2 = customData2;
    }

    public String getCustomData3() {
        return customData3;
    }

    /**
     * Optional free-form data
     *
     * @param customData3
     */
    public void setCustomData3(String customData3) {
        this.customData3 = customData3;
    }

    public String getCustomData4() {
        return customData4;
    }

    /**
     * Optional free-form data
     *
     * @param customData4
     */
    public void setCustomData4(String customData4) {
        this.customData4 = customData4;
    }

    public String getCustomData5() {
        return customData5;
    }

    /**
     * Optional free-form data
     *
     * @param customData5
     */
    public void setCustomData5(String customData5) {
        this.customData5 = customData5;
    }

    public Context getContext() {
        return context;
    }

    /* Android Context. Needed to grab the package name, application name, device information, ANDROID_ID, and window information */
    public void setContext(Context context) {
        this.context = context;
    }

    public String getExperimentName() {
        return experimentName;
    }

    /**
     * A/B Test Experiment Name
     */
    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    /**
     * PlayerType that the current video is being played back with.
     *
     * @param playerType {@link PlayerType}
     */
    public void setPlayerType(PlayerType playerType) {
        this.playerType = playerType;
    }

    public String getPath() {
        return path;
    }

    /**
     * Breadcrumb path
     *
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    /**
     * The frequency that heartbeats should be sent, in milliseconds
     *
     * @param heartbeatInterval
     */
    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
}
