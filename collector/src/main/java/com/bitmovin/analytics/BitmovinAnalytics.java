package com.bitmovin.analytics;

import android.content.Context;
import android.util.Log;

import com.bitmovin.analytics.adapters.AdAdapter;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.data.AdEventData;
import com.bitmovin.analytics.data.DRMInformation;
import com.bitmovin.analytics.data.DebuggingEventDataDispatcher;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.data.IEventDataDispatcher;
import com.bitmovin.analytics.data.SimpleEventDataDispatcher;
import com.bitmovin.analytics.enums.VideoStartFailedReason;
import com.bitmovin.analytics.features.EventEmitter;
import com.bitmovin.analytics.features.EventSource;
import com.bitmovin.analytics.features.FeatureManager;
import com.bitmovin.analytics.features.errordetails.ErrorDetailEventListener;
import com.bitmovin.analytics.license.LicenseCallback;

import com.bitmovin.analytics.stateMachines.PlayerEvent;
import com.bitmovin.analytics.stateMachines.PlayerState;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
import com.bitmovin.analytics.stateMachines.StateMachineListener;
import com.bitmovin.analytics.utils.Util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

import static com.bitmovin.analytics.utils.DataSerializer.serialize;

/**
 * An analytics plugin that sends video playback analytics to Bitmovin Analytics servers. Currently
 * supports analytics of ExoPlayer video players
 */
public class BitmovinAnalytics implements StateMachineListener, LicenseCallback, EventSource<ErrorDetailEventListener>
{

    private static final String TAG = "BitmovinAnalytics";

//    private List<DebugListener> debugListeners = new ArrayList<>();
    private FeatureManager featureManager = new FeatureManager();
    private EventEmitter eventEmitter = new EventEmitter();

    protected final BitmovinAnalyticsConfig bitmovinAnalyticsConfig;
    protected PlayerAdapter playerAdapter;
    protected AdAdapter adAdapter;
    protected PlayerStateMachine playerStateMachine;
    protected BitmovinAdAnalytics adAnalytics;
    protected IEventDataDispatcher eventDataDispatcher;
    protected Context context;

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     * @param context                 {@link Context}
     */
    public BitmovinAnalytics(BitmovinAnalyticsConfig bitmovinAnalyticsConfig, Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        Log.d(TAG, "Initializing Bitmovin Analytics with Key: " + bitmovinAnalyticsConfig.getKey());
        this.context = context;
        this.bitmovinAnalyticsConfig = bitmovinAnalyticsConfig;
        this.playerStateMachine = new PlayerStateMachine(this.bitmovinAnalyticsConfig, this);
        this.playerStateMachine.addListener(this);
        IEventDataDispatcher innerEventDataDispatcher = new SimpleEventDataDispatcher(this.bitmovinAnalyticsConfig, this.context, this);
        this.eventDataDispatcher = new DebuggingEventDataDispatcher(innerEventDataDispatcher, debugCallback);
        if (this.bitmovinAnalyticsConfig.getAds()) {
            this.adAnalytics = new BitmovinAdAnalytics(this);
        }
    }

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     * @deprecated Please use {@link #BitmovinAnalytics(BitmovinAnalyticsConfig, Context)} and pass {@link Context} seperately.
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
        this.playerAdapter.init();
    }

    protected void attachAd(AdAdapter adapter) {
        detachAd();
        this.adAdapter = adapter;
    }

    /**
     * Detach the current player that is being used with Bitmovin Analytics.
     */
    public void detachPlayer() {
        detachAd();

        featureManager.unregisterFeatures();
        if (playerAdapter != null) {
            playerAdapter.release();
        }

        if (playerStateMachine != null) {
            playerStateMachine.resetStateMachine();
        }
        eventDataDispatcher.disable();
    }

    private void detachAd() {
        if (adAdapter != null) {
            adAdapter.release();
        }
    }


    @Override
    public void onStartup(long duration) {
        Log.d(TAG, String.format("onStartup %s", playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setSupportedVideoCodecs(Util.getSupportedVideoFormats());
        data.setState("startup");
        data.setDuration(duration);
        data.setVideoStartupTime(duration);

        DRMInformation drmInfo = playerAdapter.getDRMInformation();
        if (drmInfo != null) {
            data.setDrmType(drmInfo.getType());
            data.setDrmLoadTime(drmInfo.getLoadTime());
        }

        //Setting a startup time of 1 to workaround dashboard issue
        data.setPlayerStartupTime(1);
        data.setStartupTime(duration + 1);

        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
        sendEventData(data, playerStateMachine.getCurrentState(),  PlayerEvent.STARTUP);
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
        sendEventData(data, playerStateMachine.getCurrentState(), PlayerEvent.PAUSED);
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
        sendEventData(data, playerStateMachine.getCurrentState(), PlayerEvent.PLAYED);
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
        sendEventData(data, playerStateMachine.getCurrentState(), PlayerEvent.BUFFERED);
    }

    @Override
    public void onError(ErrorCode errorCode) {
        Log.d(TAG, String.format("onError %s", playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setVideoTimeStart(playerStateMachine.getVideoTimeEnd());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());

        if (playerStateMachine.getVideoStartFailedReason() != null) {
            data.setVideoStartFailedReason(playerStateMachine.getVideoStartFailedReason().getReason());
            data.setVideoStartFailed(true);
        }

        data.setErrorCode(errorCode.getErrorCode());
        data.setErrorMessage(errorCode.getDescription());
        data.setErrorData(serialize(errorCode.getErrorData()));
        sendEventData(data, playerStateMachine.getCurrentState(), PlayerEvent.ERROR);
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
        sendEventData(data, playerStateMachine.getCurrentState(), PlayerEvent.SEEKED);
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
        sendEventData(data, playerStateMachine.getCurrentState(), PlayerEvent.HEARTBEAT);
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
        sendEventData(data, playerStateMachine.getCurrentState(), PlayerEvent.QUALITY_CHANGED);
        data.setVideoTimeStart(playerStateMachine.getVideoTimeEnd());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
    }

    @Override
    public void onVideoChange() {
        Log.d(TAG, "onVideoChange");
    }

    @Override
    public void onSubtitleChange() {
        Log.d(TAG, String.format("onSubtitleChange %s", playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(0);
        sendEventData(data, playerStateMachine.getCurrentState(), PlayerEvent.SUBTITLE_CHANGED);
        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
    }

    @Override
    public void onAudioTrackChange() {
        Log.d(TAG, String.format("onAudioTrackChange %s", playerStateMachine.getImpressionId()));
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(0);
        sendEventData(data, playerStateMachine.getCurrentState(), PlayerEvent.AUDIO_CHANGED);
        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
    }

    @Override
    public void onVideoStartFailed() {
        VideoStartFailedReason videoStartFailedReason = playerStateMachine.getVideoStartFailedReason();
        if(videoStartFailedReason == null) {
            videoStartFailedReason = VideoStartFailedReason.UNKNOWN;
        }

        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setVideoStartFailed(true);
        ErrorCode errorCode = videoStartFailedReason.getErrorCode();
        if(errorCode != null) {
            data.setErrorCode(errorCode.getErrorCode());
            data.setErrorMessage(errorCode.getDescription());
            data.setErrorData(serialize(errorCode.getErrorData()));
            eventEmitter.emit(ErrorDetailEventListener.class, listener -> listener.onError(Util.getTimestamp(), errorCode.getErrorCode(), errorCode.getDescription(), null));
        }
        data.setVideoStartFailedReason(videoStartFailedReason.getReason());
        sendEventData(data, playerStateMachine.getCurrentState(), PlayerEvent.ERROR);
        this.detachPlayer();
    }


    public void sendEventData(EventData data, PlayerState from, PlayerEvent event) {
        this.featureManager.decorateSample(data, from, event);
        this.eventDataDispatcher.add(data);
        this.playerAdapter.clearValues();
    }

    public void sendAdEventData(AdEventData data) {
        this.eventDataDispatcher.addAd(data);
    }

    public long getPosition() {
        if (playerAdapter == null) {
            return 0;
        }
        return playerAdapter.getPosition();
    }

    @Override
    public void configureFeatures(boolean authenticated, Map<String, String> settings, Collection<EventData> samples, Collection<AdEventData> adSamples)
    {
        featureManager.configureFeatures(authenticated, settings, samples, adSamples);
    }

    @Override
    public void authenticationCompleted(boolean success)
    {
        if (!success) {
            detachPlayer();
        }
    }

    public void addDebugListener(DebugListener listener) {
        eventEmitter.addEventListener(listener);
    }

    public void removeDebugListener(DebugListener listener) {
        eventEmitter.removeEventListener(listener);
    }

    @Override
    public void addEventListener(ErrorDetailEventListener errorDetailsEventListener)
    {
        eventEmitter.addEventListener(errorDetailsEventListener);
    }

    @Override
    public void removeEventListener(ErrorDetailEventListener errorDetailsEventListener)
    {
        eventEmitter.removeEventListener(errorDetailsEventListener);
    }

    public interface DebugListener {
        void onDispatchEventData(EventData data);

        void onDispatchAdEventData(AdEventData data);

        void onMessage(String message);
    }

    private DebugCallback debugCallback = new DebugCallback() {
        @Override
        public void dispatchEventData(@NotNull EventData data) {
            eventEmitter.emit(DebugListener.class, listener -> listener.onDispatchEventData(data));
        }

        @Override
        public void dispatchAdEventData(@NotNull AdEventData data) {
            eventEmitter.emit(DebugListener.class, listener -> listener.onDispatchAdEventData(data));
        }

        @Override
        public void message(@NotNull String message) {
            eventEmitter.emit(DebugListener.class, listener -> listener.onMessage(message));
        }
    };
}
