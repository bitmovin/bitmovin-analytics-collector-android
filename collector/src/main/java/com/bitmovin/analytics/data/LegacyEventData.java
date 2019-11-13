package com.bitmovin.analytics.data;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.BuildConfig;
import com.bitmovin.analytics.utils.Util;

import java.util.List;

@Deprecated
public class LegacyEventData {
    private String domain;
    private String path = "";
    private String language;
    private String userAgent;
    private int screenWidth;
    private int screenHeight;
    private boolean isLive;
    private boolean isCasting;
    private long videoDuration = 0;
    private long time = Util.getTimeStamp();
    private int videoWindowWidth = 0;
    private int videoWindowHeight = 0;
    private int droppedFrames = 0;
    private long played = 0;
    private long buffered = 0;
    private long paused = 0;
    private int ad = 0;
    private long seeked = 0;
    private int videoPlaybackWidth = 0;
    private int videoPlaybackHeight = 0;
    private int videoBitrate = 0;
    private int audioBitrate = 0;
    private long videoTimeStart = 0;
    private long videoTimeEnd = 0;
    private long videoStartupTime = 0;
    private long duration = 0;
    private long startupTime = 0;
    private String analyticsVersion;
    private String key;
    private String playerKey;
    private String player;
    private String cdnProvider;
    private String videoId;
    private String videoTitle;
    private String customUserId;
    private String customData1;
    private String customData2;
    private String customData3;
    private String customData4;
    private String customData5;
    private String experimentName;
    private String userId;
    private String impressionId;
    private String state;
    private Integer errorCode;
    private String errorMessage;
    private int playerStartupTime;
    private int pageLoadType = 1;
    private int pageLoadTime;
    private String version;
    private String playerTech;
    private String streamFormat;
    private String mpdUrl;
    private String m3u8Url;
    private String progUrl;
    private boolean isMuted = false;
    private int sequenceNumber = 0;
    private String platform = "android";
    private String videoCodec;
    private String audioCodec;
    private List<String> supportedVideoCodecs;
    private DeviceInformation deviceInformation = new DeviceInformation();
    private boolean subtitleEnabled = false;
    private String subtitleLanguage = null;
    private String audioLanguage = null;

    public LegacyEventData(BitmovinAnalyticsConfig bitmovinAnalyticsConfig, Context context, String impressionId, String userAgent) {
        this.analyticsVersion = BuildConfig.VERSION_NAME;
        this.key = bitmovinAnalyticsConfig.getKey();
        this.playerKey = bitmovinAnalyticsConfig.getPlayerKey();
        this.videoId = bitmovinAnalyticsConfig.getVideoId();
        this.videoTitle = bitmovinAnalyticsConfig.getTitle();
        this.userId = Util.getUserId(context);
        this.customUserId = bitmovinAnalyticsConfig.getCustomUserId();
        this.customData1 = bitmovinAnalyticsConfig.getCustomData1();
        this.customData2 = bitmovinAnalyticsConfig.getCustomData2();
        this.customData3 = bitmovinAnalyticsConfig.getCustomData3();
        this.customData4 = bitmovinAnalyticsConfig.getCustomData4();
        this.customData5 = bitmovinAnalyticsConfig.getCustomData5();
        this.path = bitmovinAnalyticsConfig.getPath();
        this.experimentName = bitmovinAnalyticsConfig.getExperimentName();
        this.playerTech = Util.PLAYER_TECH;
        this.setUserAgent(userAgent);
        this.impressionId = impressionId;

        if (bitmovinAnalyticsConfig.getCdnProvider() != null) {
            this.cdnProvider = bitmovinAnalyticsConfig.getCdnProvider().toString();
        }

        if (bitmovinAnalyticsConfig.getPlayerType() != null) {
            this.player = bitmovinAnalyticsConfig.getPlayerType().toString();
        }

        if (context != null) {
            this.domain = context.getPackageName();
            this.screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            this.screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            this.language = Util.getLocale();
        }
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public void setCasting(boolean casting) {
        isCasting = casting;
    }

    public void setVideoDuration(long videoDuration) {
        this.videoDuration = videoDuration;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setVideoWindowWidth(int videoWindowWidth) {
        this.videoWindowWidth = videoWindowWidth;
    }

    public void setVideoWindowHeight(int videoWindowHeight) {
        this.videoWindowHeight = videoWindowHeight;
    }

    public void setDroppedFrames(int droppedFrames) {
        this.droppedFrames = droppedFrames;
    }

    public void setPlayed(long played) {
        this.played = played;
    }

    public void setBuffered(long buffered) {
        this.buffered = buffered;
    }

    public void setPaused(long paused) {
        this.paused = paused;
    }

    public void setAd(int ad) {
        this.ad = ad;
    }

    public void setSeeked(long seeked) {
        this.seeked = seeked;
    }

    public void setVideoPlaybackWidth(int videoPlaybackWidth) {
        this.videoPlaybackWidth = videoPlaybackWidth;
    }

    public void setVideoPlaybackHeight(int videoPlaybackHeight) {
        this.videoPlaybackHeight = videoPlaybackHeight;
    }

    public void setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public void setAudioBitrate(int audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public void setVideoTimeStart(long videoTimeStart) {
        this.videoTimeStart = videoTimeStart;
    }

    public void setVideoTimeEnd(long videoTimeEnd) {
        this.videoTimeEnd = videoTimeEnd;
    }

    public void setVideoStartupTime(long videoStartupTime) {
        this.videoStartupTime = videoStartupTime;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setStartupTime(long startupTime) {
        this.startupTime = startupTime;
    }

    public void setAnalyticsVersion(String analyticsVersion) {
        this.analyticsVersion = analyticsVersion;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setPlayerKey(String playerKey) {
        this.playerKey = playerKey;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public void setCdnProvider(String cdnProvider) {
        this.cdnProvider = cdnProvider;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
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

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setImpressionId(String impressionId) {
        this.impressionId = impressionId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setPlayerStartupTime(int playerStartupTime) {
        this.playerStartupTime = playerStartupTime;
    }

    public void setPageLoadType(int pageLoadType) {
        this.pageLoadType = pageLoadType;
    }

    public void setPageLoadTime(int pageLoadTime) {
        this.pageLoadTime = pageLoadTime;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPlayerTech(String playerTech) {
        this.playerTech = playerTech;
    }

    public void setStreamFormat(String streamFormat) {
        this.streamFormat = streamFormat;
    }

    public void setMpdUrl(String mpdUrl) {
        this.mpdUrl = mpdUrl;
    }

    public void setM3u8Url(String m3u8Url) {
        this.m3u8Url = m3u8Url;
    }

    public void setProgUrl(String progUrl) {
        this.progUrl = progUrl;
    }

    public void setMuted(boolean muted) {
        this.isMuted = muted;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public void setSupportedVideoCodecs(List<String> supportedVideoCodecs) {
        this.supportedVideoCodecs = supportedVideoCodecs;
    }

    public String getDomain() {
        return domain;
    }

    public String getPath() {
        return path;
    }

    public String getLanguage() {
        return language;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public boolean isLive() {
        return isLive;
    }

    public boolean isCasting() {
        return isCasting;
    }

    public long getVideoDuration() {
        return videoDuration;
    }

    public long getTime() {
        return time;
    }

    public int getVideoWindowWidth() {
        return videoWindowWidth;
    }

    public int getVideoWindowHeight() {
        return videoWindowHeight;
    }

    public int getDroppedFrames() {
        return droppedFrames;
    }

    public long getPlayed() {
        return played;
    }

    public long getBuffered() {
        return buffered;
    }

    public long getPaused() {
        return paused;
    }

    public int getAd() {
        return ad;
    }

    public long getSeeked() {
        return seeked;
    }

    public int getVideoPlaybackWidth() {
        return videoPlaybackWidth;
    }

    public int getVideoPlaybackHeight() {
        return videoPlaybackHeight;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }

    public long getVideoTimeStart() {
        return videoTimeStart;
    }

    public long getVideoTimeEnd() {
        return videoTimeEnd;
    }

    public long getVideoStartupTime() {
        return videoStartupTime;
    }

    public long getDuration() {
        return duration;
    }

    public long getStartupTime() {
        return startupTime;
    }

    public String getAnalyticsVersion() {
        return analyticsVersion;
    }

    public String getKey() {
        return key;
    }

    public String getPlayerKey() {
        return playerKey;
    }

    public String getPlayer() {
        return player;
    }

    public String getCdnProvider() {
        return cdnProvider;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getVideoTitle() {
        return videoTitle;
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

    public String getExperimentName() {
        return experimentName;
    }

    public String getUserId() {
        return userId;
    }

    public String getImpressionId() {
        return impressionId;
    }

    public String getState() {
        return state;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getPlayerStartupTime() {
        return playerStartupTime;
    }

    public int getPageLoadType() {
        return pageLoadType;
    }

    public int getPageLoadTime() {
        return pageLoadTime;
    }

    public String getVersion() {
        return version;
    }

    public String getPlayerTech() {
        return playerTech;
    }

    public String getStreamFormat() {
        return streamFormat;
    }

    public String getMpdUrl() {
        return mpdUrl;
    }

    public String getM3u8Url() {
        return m3u8Url;
    }

    public String getProgUrl() {
        return progUrl;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public String getPlatform() {
        return platform;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public List<String> getSupportedVideoCodec() {
        return supportedVideoCodecs;
    }

    public DeviceInformation getDeviceInformation() {
        return deviceInformation;
    }
}
