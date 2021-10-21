package com.bitmovin.analytics;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import com.bitmovin.analytics.data.CustomData;
import com.bitmovin.analytics.enums.CDNProvider;
import com.bitmovin.analytics.enums.PlayerType;

public class BitmovinAnalyticsConfig implements Parcelable {
    private String cdnProvider;
    private String customData1;
    private String customData2;
    private String customData3;
    private String customData4;
    private String customData5;
    private String customData6;
    private String customData7;
    private String customData8;
    private String customData9;
    private String customData10;
    private String customData11;
    private String customData12;
    private String customData13;
    private String customData14;
    private String customData15;
    private String customData16;
    private String customData17;
    private String customData18;
    private String customData19;
    private String customData20;
    private String customData21;
    private String customData22;
    private String customData23;
    private String customData24;
    private String customData25;

    private String customUserId;
    private String experimentName;
    private String mpdUrl;
    private String m3u8Url;
    private int heartbeatInterval = 59700;
    private String key;
    private String title;
    private String path;
    private String playerKey;
    private PlayerType playerType;
    private String videoId;
    private Boolean ads = true;
    private Context context;
    private Boolean isLive;
    private Boolean randomizeUserId = false;
    private CollectorConfig config = new CollectorConfig();

    public static final Creator<BitmovinAnalyticsConfig> CREATOR =
            new Creator<BitmovinAnalyticsConfig>() {
                @Override
                public BitmovinAnalyticsConfig createFromParcel(Parcel in) {
                    return new BitmovinAnalyticsConfig(in);
                }

                @Override
                public BitmovinAnalyticsConfig[] newArray(int size) {
                    return new BitmovinAnalyticsConfig[size];
                }
            };

    @Deprecated
    public BitmovinAnalyticsConfig(String key, Context context) {
        this(key);
        this.context = context;
    }

    @Deprecated
    public BitmovinAnalyticsConfig(String key, String playerKey, Context context) {
        this(key, playerKey);
        this.context = context;
    }

    public BitmovinAnalyticsConfig(String key) {
        this.key = key;
        this.playerKey = "";
    }

    public BitmovinAnalyticsConfig(String key, String playerKey) {
        this.key = key;
        this.playerKey = playerKey;
    }

    protected BitmovinAnalyticsConfig(Parcel in) {
        cdnProvider = in.readString();
        customData1 = in.readString();
        customData2 = in.readString();
        customData3 = in.readString();
        customData4 = in.readString();
        customData5 = in.readString();
        customData6 = in.readString();
        customData7 = in.readString();
        customData8 = in.readString();
        customData9 = in.readString();
        customData10 = in.readString();
        customData11 = in.readString();
        customData12 = in.readString();
        customData13 = in.readString();
        customData14 = in.readString();
        customData15 = in.readString();
        customData16 = in.readString();
        customData17 = in.readString();
        customData18 = in.readString();
        customData19 = in.readString();
        customData20 = in.readString();
        customData21 = in.readString();
        customData22 = in.readString();
        customData23 = in.readString();
        customData24 = in.readString();
        customData25 = in.readString();
        customUserId = in.readString();
        experimentName = in.readString();
        mpdUrl = in.readString();
        m3u8Url = in.readString();
        heartbeatInterval = in.readInt();
        key = in.readString();
        title = in.readString();
        path = in.readString();
        playerKey = in.readString();
        playerType = in.readParcelable(PlayerType.class.getClassLoader());
        videoId = in.readString();
        isLive = (Boolean) in.readSerializable();
        config = in.readParcelable(CollectorConfig.class.getClassLoader());
        ads = in.readInt() == 1;
        randomizeUserId = (Boolean) in.readSerializable();
    }

    public BitmovinAnalyticsConfig() {}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(cdnProvider);
        dest.writeString(customData1);
        dest.writeString(customData2);
        dest.writeString(customData3);
        dest.writeString(customData4);
        dest.writeString(customData5);
        dest.writeString(customData6);
        dest.writeString(customData7);
        dest.writeString(customData8);
        dest.writeString(customData9);
        dest.writeString(customData10);
        dest.writeString(customData11);
        dest.writeString(customData12);
        dest.writeString(customData13);
        dest.writeString(customData14);
        dest.writeString(customData15);
        dest.writeString(customData16);
        dest.writeString(customData17);
        dest.writeString(customData18);
        dest.writeString(customData19);
        dest.writeString(customData20);
        dest.writeString(customData21);
        dest.writeString(customData22);
        dest.writeString(customData23);
        dest.writeString(customData24);
        dest.writeString(customData25);
        dest.writeString(customUserId);
        dest.writeString(experimentName);
        dest.writeString(mpdUrl);
        dest.writeString(m3u8Url);
        dest.writeInt(heartbeatInterval);
        dest.writeString(key);
        dest.writeString(title);
        dest.writeString(path);
        dest.writeString(playerKey);
        dest.writeParcelable(playerType, flags);
        dest.writeString(videoId);
        dest.writeSerializable(isLive);
        dest.writeParcelable(config, config.describeContents());
        dest.writeInt(ads ? 1 : 0);
        dest.writeSerializable(randomizeUserId);
    }

    @Override
    public int describeContents() {
        return 0;
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

    public String getCustomData6() {
        return customData6;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData6
     */
    public void setCustomData6(String customData6) {
        this.customData6 = customData6;
    }

    public String getCustomData7() {
        return customData7;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData7
     */
    public void setCustomData7(String customData7) {
        this.customData7 = customData7;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData8
     */
    public void setCustomData8(String customData8) {
        this.customData8 = customData8;
    }

    public String getCustomData8() {
        return customData8;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData9
     */
    public void setCustomData9(String customData9) {
        this.customData9 = customData9;
    }

    public String getCustomData9() {
        return customData9;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData10
     */
    public void setCustomData10(String customData10) {
        this.customData10 = customData10;
    }

    public String getCustomData10() {
        return customData10;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData11
     */
    public void setCustomData11(String customData11) {
        this.customData11 = customData11;
    }

    public String getCustomData11() {
        return customData11;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData12
     */
    public void setCustomData12(String customData12) {
        this.customData12 = customData12;
    }

    public String getCustomData12() {
        return customData12;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData13
     */
    public void setCustomData13(String customData13) {
        this.customData13 = customData13;
    }

    public String getCustomData13() {
        return customData13;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData14
     */
    public void setCustomData14(String customData14) {
        this.customData14 = customData14;
    }

    public String getCustomData14() {
        return customData14;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData15
     */
    public void setCustomData15(String customData15) {
        this.customData15 = customData15;
    }

    public String getCustomData15() {
        return customData15;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData16
     */
    public void setCustomData16(String customData16) {
        this.customData16 = customData16;
    }

    public String getCustomData16() {
        return customData16;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData17
     */
    public void setCustomData17(String customData17) {
        this.customData17 = customData17;
    }

    public String getCustomData17() {
        return customData17;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData18
     */
    public void setCustomData18(String customData18) {
        this.customData18 = customData18;
    }

    public String getCustomData18() {
        return customData18;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData19
     */
    public void setCustomData19(String customData19) {
        this.customData19 = customData19;
    }

    public String getCustomData19() {
        return customData19;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData20
     */
    public void setCustomData20(String customData20) {
        this.customData20 = customData20;
    }

    public String getCustomData20() {
        return customData20;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData21
     */
    public void setCustomData21(String customData21) {
        this.customData21 = customData21;
    }

    public String getCustomData21() {
        return customData21;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData22
     */
    public void setCustomData22(String customData22) {
        this.customData22 = customData22;
    }

    public String getCustomData22() {
        return customData22;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData23
     */
    public void setCustomData23(String customData23) {
        this.customData23 = customData23;
    }

    public String getCustomData23() {
        return customData23;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData24
     */
    public void setCustomData24(String customData24) {
        this.customData24 = customData24;
    }

    public String getCustomData24() {
        return customData24;
    }

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     *
     * @param customData25
     */
    public void setCustomData25(String customData25) {
        this.customData25 = customData25;
    }

    public String getCustomData25() {
        return customData25;
    }

    public String getExperimentName() {
        return experimentName;
    }

    /** A/B Test Experiment Name */
    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    /**
     * Human readable title of the video asset currently playing
     *
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * Human readable title of the video asset currently playing
     *
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Set MPD URL recorded in analytics. If not set explicitly the collector will retrieve
     * available information from the player.
     *
     * @param mpdUrl
     */
    public void setMpdUrl(String mpdUrl) {
        this.mpdUrl = mpdUrl;
    }

    public String getMpdUrl() {
        return this.mpdUrl;
    }

    /**
     * Set M3U8 URL recorded in analytics. If not set explicitly the collector will retrieve
     * available information from the player.
     *
     * @param m3u8Url
     */
    public void setM3u8Url(String m3u8Url) {
        this.m3u8Url = m3u8Url;
    }

    public String getM3u8Url() {
        return this.m3u8Url;
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

    public Context getContext() {
        return context;
    }

    /**
     * Configuration options for the Analytics collector
     *
     * @return collector configuration {@link CollectorConfig}
     */
    public CollectorConfig getConfig() {
        return config;
    }

    /**
     * Returns a value indicating if ads tracking is enabled
     *
     * @return
     */
    public Boolean getAds() {
        return ads;
    }

    /**
     * Enable or disable ads tracking
     *
     * @param ads
     */
    public void setAds(Boolean ads) {
        this.ads = ads;
    }

    /**
     * Returns true if the stream is marked as live before stream metadata is available.
     *
     * @return
     */
    public Boolean isLive() {
        return isLive;
    }

    /**
     * Mark the stream as live before stream metadata is available.
     *
     * @param live
     */
    public void setIsLive(Boolean live) {
        isLive = live;
    }

    /**
     * Returns true if random UserId value will be generated
     *
     * @return
     */
    public Boolean getRandomizeUserId() {
        return randomizeUserId;
    }

    /**
     * Generate random UserId value for session
     *
     * @param randomizeUserId
     */
    public void setRandomizeUserId(Boolean randomizeUserId) {
        this.randomizeUserId = randomizeUserId;
    }

    protected CustomData getCustomData() {
        return new CustomData(
                this.getCustomData1(),
                this.getCustomData2(),
                this.getCustomData3(),
                this.getCustomData4(),
                this.getCustomData5(),
                this.getCustomData6(),
                this.getCustomData7(),
                this.getCustomData8(),
                this.getCustomData9(),
                this.getCustomData10(),
                this.getCustomData11(),
                this.getCustomData12(),
                this.getCustomData13(),
                this.getCustomData14(),
                this.getCustomData15(),
                this.getCustomData16(),
                this.getCustomData17(),
                this.getCustomData18(),
                this.getCustomData19(),
                this.getCustomData20(),
                this.getCustomData21(),
                this.getCustomData22(),
                this.getCustomData23(),
                this.getCustomData24(),
                this.getCustomData25(),
                this.getExperimentName());
    }

    protected void setCustomData(CustomData customData) {
        this.setCustomData1(customData.getCustomData1());
        this.setCustomData2(customData.getCustomData2());
        this.setCustomData3(customData.getCustomData3());
        this.setCustomData4(customData.getCustomData4());
        this.setCustomData5(customData.getCustomData5());
        this.setCustomData6(customData.getCustomData6());
        this.setCustomData7(customData.getCustomData7());
        this.setCustomData8(customData.getCustomData8());
        this.setCustomData9(customData.getCustomData9());
        this.setCustomData10(customData.getCustomData10());
        this.setCustomData11(customData.getCustomData11());
        this.setCustomData12(customData.getCustomData12());
        this.setCustomData13(customData.getCustomData13());
        this.setCustomData14(customData.getCustomData14());
        this.setCustomData15(customData.getCustomData15());
        this.setCustomData16(customData.getCustomData16());
        this.setCustomData17(customData.getCustomData17());
        this.setCustomData18(customData.getCustomData18());
        this.setCustomData19(customData.getCustomData19());
        this.setCustomData20(customData.getCustomData20());
        this.setCustomData21(customData.getCustomData21());
        this.setCustomData22(customData.getCustomData22());
        this.setCustomData23(customData.getCustomData23());
        this.setCustomData24(customData.getCustomData24());
        this.setCustomData25(customData.getCustomData25());
        this.setExperimentName(customData.getExperimentName());
    }
}
