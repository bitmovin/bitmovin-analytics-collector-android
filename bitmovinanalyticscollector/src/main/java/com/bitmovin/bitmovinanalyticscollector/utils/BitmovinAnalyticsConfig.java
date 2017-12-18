package com.bitmovin.bitmovinanalyticscollector.utils;

import android.content.Context;

import com.bitmovin.bitmovinanalyticscollector.enums.CDNProvider;
import com.bitmovin.bitmovinanalyticscollector.enums.Player;

/**
 * Created by zachmanc on 12/14/17.
 */

public class BitmovinAnalyticsConfig {
    public static final String analyticsUrl = "https://analytics-ingress-global.bitmovin.com/analytics";
    private String key;
    private String playerKey;
    private CDNProvider cdnProvider;
    private Player player;
    private String videoId;
    private String customUserId;
    private String customData1;
    private String customData2;
    private String customData3;
    private String customData4;
    private String customData5;
    private String userId;
    private Context context;

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

    public CDNProvider getCdnProvider() {
        return cdnProvider;
    }

    public Player getPlayer() {
        return player;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getCustomUserId() {
        return customUserId;
    }

    public String getCustomData1() {
        return customData1;
    }

    public String getCustomData2() {
        return customData2;
    }

    public String getCustomData3() {
        return customData3;
    }

    public String getCustomData4() {
        return customData4;
    }

    public String getCustomData5() {
        return customData5;
    }

    public String getUserId() {
        return userId;
    }

    public Context getContext() { return context; }

    public void setContext(Context context) { this.context = context; }

    public void setCdnProvider(CDNProvider cdnProvider) { this.cdnProvider = cdnProvider; }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public void setCustomUserId(String customUserId) {
        this.customUserId = customUserId;
    }

    public void setCustomData1(String customData1) {
        this.customData1 = customData1;
    }

    public void setCustomData2(String customData2) {
        this.customData2 = customData2;
    }

    public void setCustomData3(String customData3) {
        this.customData3 = customData3;
    }

    public void setCustomData4(String customData4) {
        this.customData4 = customData4;
    }

    public void setCustomData5(String customData5) {
        this.customData5 = customData5;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
