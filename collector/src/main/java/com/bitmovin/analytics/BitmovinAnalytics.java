package com.bitmovin.analytics;

import android.content.Context;
import android.util.Log;

import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.data.IEventDataDispatcher;
import com.bitmovin.analytics.data.SimpleEventDataDispatcher;
import com.bitmovin.analytics.license.DefaultLicenseProvider;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
import com.bitmovin.analytics.stateMachines.StateMachineListener;
import com.bitmovin.analytics.license.OnLicenseValidated;
import com.bitmovin.analytics.utils.Util;

/**
 * An analytics plugin that sends video playback analytics to Bitmovin Analytics servers. Currently
 * supports analytics of ExoPlayer video players
 */
public class BitmovinAnalytics implements StateMachineListener, OnLicenseValidated {

    private static final String TAG = "BitmovinAnalytics";

    protected final BitmovinAnalyticsConfig bitmovinAnalyticsConfig;
    protected PlayerAdapter playerAdapter;
    protected PlayerStateMachine playerStateMachine;
    protected IEventDataDispatcher eventDataDispatcher;
    protected Context context;
    protected DefaultLicenseProvider licenseProvider;

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     * @param context {@link Context}
     */
    public BitmovinAnalytics(BitmovinAnalyticsConfig bitmovinAnalyticsConfig, Context context) {
        this.context = context;
        this.bitmovinAnalyticsConfig = bitmovinAnalyticsConfig;
        this.licenseProvider = new DefaultLicenseProvider();
        this.playerStateMachine = new PlayerStateMachine(this.bitmovinAnalyticsConfig, this);
        this.playerStateMachine.addListener(this);
        this.eventDataDispatcher = new SimpleEventDataDispatcher(this.bitmovinAnalyticsConfig, this.context, this, licenseProvider);
    }

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     */
    @Deprecated
    public BitmovinAnalytics(BitmovinAnalyticsConfig bitmovinAnalyticsConfig) {
        this(bitmovinAnalyticsConfig, bitmovinAnalyticsConfig.getContext());
    }

    /**
     * Attach a player instance to this analytics plugin. After this is completed, BitmovinAnalytics
     * will start monitoring and sending analytics data based on the attached player adapter.
     * <p>
     * To attach a different player instance, simply call this method again.
     */
    protected void attach(PlayerAdapter adapter) {
        detachPlayer();
        eventDataDispatcher.enable();
        this.playerAdapter = adapter;
    }

    /**
     * Detach the current player that is being used with Bitmovin Analytics.
     */
    public void detachPlayer() {
        if (playerAdapter != null) {
            playerAdapter.release();
        }

        if (playerStateMachine != null) {
            playerStateMachine.resetStateMachine();
        }
        eventDataDispatcher.disable();
    }

    @Override
    public void onSetup() {
        Log.d(TAG, String.format("onSetup %s", playerStateMachine.getImpressionId()));
    }

    @Override
    public void onStartup(long duration) {
        Log.d(TAG, String.format("onStartup %s", playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setSupportedVideoCodecs(Util.getSupportedVideoFormats());
        data.setState("startup");
        data.setDuration(duration);
        data.setVideoStartupTime(duration);

        //Setting a startup time of 1 to workaround dashboard issue
        data.setPlayerStartupTime(1);
        data.setStartupTime(duration + 1);

        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
        sendEventData(data);
    }

    @Override
    public void onPauseExit(long duration) {
        Log.d(TAG, String.format("onPauseExit %s", playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(duration);
        data.setPaused(duration);
        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
        sendEventData(data);
    }

    @Override
    public void onPlayExit(long duration) {
        Log.d(TAG, String.format("onPlayExit %s", playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(duration);
        data.setPlayed(duration);
        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
        sendEventData(data);
    }

    @Override
    public void onRebuffering(long duration) {
        Log.d(TAG, String.format("onRebuffering %s", playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(duration);
        data.setBuffered(duration);
        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
        sendEventData(data);
    }

    @Override
    public void onError(ErrorCode errorCode) {
        Log.d(TAG, String.format("onError %s", playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setVideoTimeStart(playerStateMachine.getVideoTimeEnd());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
        data.setErrorCode(errorCode.getErrorCode());
        data.setErrorMessage(errorCode.getDescription());
        sendEventData(data);
    }

    @Override
    public void onSeekComplete(long duration) {
        Log.d(TAG, String.format("onSeekComplete %s", playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setSeeked(duration);
        data.setDuration(duration);
        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
        sendEventData(data);
    }

    @Override
    public void onHeartbeat(long duration) {
        Log.d(TAG, String
                .format("onHeartbeat %s %s", playerStateMachine.getCurrentState().toString().toLowerCase(),
                        playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(duration);

        switch (playerStateMachine.getCurrentState()) {
            case PLAYING:
                data.setPlayed(duration);
                break;
            case PAUSE:
                data.setPaused(duration);
                break;
            case BUFFERING:
                data.setBuffered(duration);
                break;

        }

        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
        sendEventData(data);
    }

    @Override
    public void onAd() {
        Log.d(TAG, "onAd");
    }

    @Override
    public void onMute() {
        Log.d(TAG, "onMute");
    }

    @Override
    public void onUnmute() {
        Log.d(TAG, "onUnmute");
    }

    @Override
    public void onUpdateSample() {
        Log.d(TAG, "onUpdateSample");
    }

    @Override
    public void onQualityChange() {
        Log.d(TAG, String.format("onQualityChange %s", playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(0);
        sendEventData(data);
        data.setVideoTimeStart(playerStateMachine.getVideoTimeEnd());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
    }

    @Override
    public void onVideoChange() {
        Log.d(TAG, "onVideoChange");
    }

    public void sendEventData(EventData data) {
        this.eventDataDispatcher.add(data);
    }

    public long getPosition() {
        if (playerAdapter == null) {
            return 0;
        }
        return playerAdapter.getPosition();
    }

    @Override
    public void validationCompleted(boolean success, String key) {
        if (!success) {
            detachPlayer();
        }
    }
}
