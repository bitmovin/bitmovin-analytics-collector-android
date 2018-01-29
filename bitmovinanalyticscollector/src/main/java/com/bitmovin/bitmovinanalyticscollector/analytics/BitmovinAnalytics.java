package com.bitmovin.bitmovinanalyticscollector.analytics;

import android.util.Log;

import com.bitmovin.bitmovinanalyticscollector.adapters.ExoPlayerAdapter;
import com.bitmovin.bitmovinanalyticscollector.adapters.PlayerAdapter;
import com.bitmovin.bitmovinanalyticscollector.data.ErrorCode;
import com.bitmovin.bitmovinanalyticscollector.data.EventData;
import com.bitmovin.bitmovinanalyticscollector.data.IEventDataDispatcher;
import com.bitmovin.bitmovinanalyticscollector.data.SimpleEventDataDispatcher;
import com.bitmovin.bitmovinanalyticscollector.stateMachines.PlayerStateMachine;
import com.bitmovin.bitmovinanalyticscollector.stateMachines.StateMachineListener;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;

/**
 * An analytics plugin that sends video playback analytics to Bitmovin Analytics servers. Currently
 * supports analytics of ExoPlayer video players
 */
public class BitmovinAnalytics implements StateMachineListener {
    private static final String TAG = "BitmovinAnalytics";

    private final BitmovinAnalyticsConfig bitmovinAnalyticsConfig;
    private PlayerAdapter playerAdapter;
    private PlayerStateMachine playerStateMachine;
    private IEventDataDispatcher eventDataDispatcher;

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     */
    public BitmovinAnalytics(BitmovinAnalyticsConfig bitmovinAnalyticsConfig) {
        this.bitmovinAnalyticsConfig = bitmovinAnalyticsConfig;
        this.playerStateMachine = new PlayerStateMachine(this.bitmovinAnalyticsConfig);
        this.playerStateMachine.addListener(this);
        this.eventDataDispatcher = new SimpleEventDataDispatcher(this.bitmovinAnalyticsConfig);
    }

    /**
     * Attach a player instance to this analytics plugin. After this is completed, BitmovinAnalytics
     * will start monitoring and sending analytics data based on the attached player instance.
     * <p>
     * To attach a different player instance, simply call this method again.
     *
     * @param exoPlayer
     */
    public void attachPlayer(ExoPlayer exoPlayer) {
        detachPlayer();
        this.playerAdapter = new ExoPlayerAdapter(exoPlayer, bitmovinAnalyticsConfig, playerStateMachine);
    }

    /**
     * Detach the current player that is being used with Bitmovin Analytics.
     * <p>
     * For ExoPlayer implementations: Call this method when you call ExoPlayer's
     * {@link SimpleExoPlayer#release()} )} method
     */
    public void detachPlayer() {
        if (this.playerAdapter != null) {
            this.playerAdapter.release();
        }
        if (this.playerStateMachine != null) {
            this.playerStateMachine.resetStateMachine();
        }
    }

    @Override
    public void onSetup() {
        Log.d(TAG, String.format("onSetup %s", playerStateMachine.getImpressionId()));
    }

    @Override
    public void onStartup(long duration) {
        Log.d(TAG, String.format("onStartup %s", playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setState("startup");
        data.setDuration(duration);
        data.setVideoStartupTime(duration);
        data.setStartupTime(duration);
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
        Log.d(TAG, String.format("onHeartbeat %s %s", playerStateMachine.getCurrentState().toString().toLowerCase(), playerStateMachine.getImpressionId()));
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
}
