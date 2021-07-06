package com.bitmovin.analytics;

import static com.bitmovin.analytics.utils.DataSerializer.serialize;

import android.content.Context;
import android.util.Log;
import com.bitmovin.analytics.adapters.AdAdapter;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.config.SourceMetadata;
import com.bitmovin.analytics.data.AdEventData;
import com.bitmovin.analytics.data.BackendFactory;
import com.bitmovin.analytics.data.CustomData;
import com.bitmovin.analytics.data.DebuggingEventDataDispatcher;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.data.EventDataFactory;
import com.bitmovin.analytics.data.IEventDataDispatcher;
import com.bitmovin.analytics.data.RandomizedUserIdIdProvider;
import com.bitmovin.analytics.data.SecureSettingsAndroidIdUserIdProvider;
import com.bitmovin.analytics.data.SimpleEventDataDispatcher;
import com.bitmovin.analytics.data.UserIdProvider;
import com.bitmovin.analytics.data.manipulators.ManifestUrlEventDataManipulator;
import com.bitmovin.analytics.enums.VideoStartFailedReason;
import com.bitmovin.analytics.features.Feature;
import com.bitmovin.analytics.features.FeatureManager;
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener;
import com.bitmovin.analytics.license.LicenseCallback;
import com.bitmovin.analytics.stateMachines.PlayerState;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
import com.bitmovin.analytics.stateMachines.StateMachineListener;
import com.bitmovin.analytics.utils.Util;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An analytics plugin that sends video playback analytics to Bitmovin Analytics servers. Currently
 * supports analytics of ExoPlayer video players
 */
public class BitmovinAnalytics implements StateMachineListener, LicenseCallback, ImpressionIdProvider {

    private static final String TAG = "BitmovinAnalytics";

    private FeatureManager featureManager = new FeatureManager();
    private EventBus eventBus = new EventBus();

    protected final BitmovinAnalyticsConfig bitmovinAnalyticsConfig;
    protected PlayerAdapter playerAdapter;
    protected AdAdapter adAdapter;
    protected PlayerStateMachine playerStateMachine;
    protected BitmovinAdAnalytics adAnalytics;
    protected IEventDataDispatcher eventDataDispatcher;
    protected Context context;
    private final UserIdProvider userIdProvider;
    private final EventDataFactory eventDataFactory;

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     * @param context {@link Context}
     */
    public BitmovinAnalytics(BitmovinAnalyticsConfig bitmovinAnalyticsConfig, Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        Log.d(TAG, "Initializing Bitmovin Analytics with Key: " + bitmovinAnalyticsConfig.getKey());
        this.context = context;
        this.userIdProvider =
                bitmovinAnalyticsConfig.getRandomizeUserId()
                        ? new RandomizedUserIdIdProvider()
                        : new SecureSettingsAndroidIdUserIdProvider(context);
        this.bitmovinAnalyticsConfig = bitmovinAnalyticsConfig;
        this.eventDataFactory = new EventDataFactory(bitmovinAnalyticsConfig, this.userIdProvider);
        this.playerStateMachine = new PlayerStateMachine(this.bitmovinAnalyticsConfig, this);
        this.playerStateMachine.addListener(this);
        IEventDataDispatcher innerEventDataDispatcher =
                new SimpleEventDataDispatcher(
                        this.bitmovinAnalyticsConfig, this.context, this, new BackendFactory());
        this.eventDataDispatcher =
                new DebuggingEventDataDispatcher(innerEventDataDispatcher, debugCallback);
        if (this.bitmovinAnalyticsConfig.getAds()) {
            this.adAnalytics = new BitmovinAdAnalytics(this);
        }
    }

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     * @deprecated Please use {@link #BitmovinAnalytics(BitmovinAnalyticsConfig, Context)} and pass
     *     {@link Context} seperately.
     */
    @Deprecated
    public BitmovinAnalytics(BitmovinAnalyticsConfig bitmovinAnalyticsConfig) {
        this(bitmovinAnalyticsConfig, bitmovinAnalyticsConfig.getContext());
    }

    /**
     * Attach a player instance to this analytics plugin. After this is completed, BitmovinAnalytics
     * will start monitoring and sending analytics data based on the attached player adapter.
     *
     * <p>To attach a different player instance, simply call this method again.
     */
    protected void attach(PlayerAdapter adapter) {
        detachPlayer();
        eventDataDispatcher.enable();
        this.playerAdapter = adapter;
        Collection<Feature<?>> features = this.playerAdapter.init();
        this.featureManager.registerFeatures(features);

        // this.registerEventDataManipulators(prePipelineManipulator);
        this.playerAdapter.registerEventDataManipulators(eventDataFactory);
        this.eventDataFactory.registerEventDataManipulator(
                new ManifestUrlEventDataManipulator(
                        this.playerAdapter, this.bitmovinAnalyticsConfig));
        // this.registerEventDataManipulators(postPipelineManipulator);
    }

    protected void attachAd(AdAdapter adapter) {
        detachAd();
        this.adAdapter = adapter;
    }

    /** Detach the current player that is being used with Bitmovin Analytics. */
    public void detachPlayer() {
        detachAd();

        featureManager.unregisterFeatures();
        eventBus.notify(
                OnAnalyticsReleasingEventListener.class,
                OnAnalyticsReleasingEventListener::onReleasing);

        if (playerAdapter != null) {
            playerAdapter.release();
        }

        if (playerStateMachine != null) {
            playerStateMachine.resetStateMachine();
        }
        eventDataDispatcher.disable();
        eventDataFactory.clearEventDataManipulators();
    }

    private void detachAd() {
        if (adAdapter != null) {
            adAdapter.release();
        }
    }

    public EventData createEventData() {
        return eventDataFactory.create(
                playerStateMachine.getImpressionId(),
                playerAdapter.getCurrentSourceMetadata(),
                playerAdapter.getDeviceInformationProvider().getDeviceInformation());
    }

    @Override
    public void onStartup(long videoStartupTime, long playerStartupTime) {
        Log.d(TAG, String.format("onStartup %s", playerStateMachine.getImpressionId()));
        EventData data = createEventData();
        data.setSupportedVideoCodecs(Util.getSupportedVideoFormats());
        data.setState("startup");
        data.setDuration(videoStartupTime + playerStartupTime);
        data.setVideoStartupTime(videoStartupTime);

        data.setDrmLoadTime(playerAdapter.getDRMDownloadTime());

        data.setPlayerStartupTime(playerStartupTime);
        data.setStartupTime(videoStartupTime + playerStartupTime);

        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
        sendEventData(data);
    }

    @Override
    public void onPauseExit(long duration) {
        Log.d(TAG, String.format("onPauseExit %s", playerStateMachine.getImpressionId()));
        EventData data = createEventData();
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
        EventData data = createEventData();
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
        EventData data = createEventData();
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
        EventData data = createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setVideoTimeStart(playerStateMachine.getVideoTimeEnd());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());

        if (playerStateMachine.getVideoStartFailedReason() != null) {
            data.setVideoStartFailedReason(
                    playerStateMachine.getVideoStartFailedReason().getReason());
            data.setVideoStartFailed(true);
        }

        data.setErrorCode(errorCode.getErrorCode());
        data.setErrorMessage(errorCode.getDescription());
        data.setErrorData(serialize(errorCode.getErrorData()));
        sendEventData(data);
    }

    @Override
    public void onSeekComplete(long duration) {
        Log.d(TAG, String.format("onSeekComplete %s", playerStateMachine.getImpressionId()));
        EventData data = createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setSeeked(duration);
        data.setDuration(duration);
        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
        sendEventData(data);
    }

    @Override
    public void onHeartbeat(long duration) {
        Log.d(
                TAG,
                String.format(
                        "onHeartbeat %s %s",
                        playerStateMachine.getCurrentState().toString().toLowerCase(),
                        playerStateMachine.getImpressionId()));
        EventData data = createEventData();
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
        EventData data = createEventData();
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

    @Override
    public void onSubtitleChange() {
        Log.d(TAG, String.format("onSubtitleChange %s", playerStateMachine.getImpressionId()));
        EventData data = createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(0);
        sendEventData(data);
        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
    }

    @Override
    public void onAudioTrackChange() {
        Log.d(TAG, String.format("onAudioTrackChange %s", playerStateMachine.getImpressionId()));
        EventData data = createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(0);
        sendEventData(data);
        data.setVideoTimeStart(playerStateMachine.getVideoTimeStart());
        data.setVideoTimeEnd(playerStateMachine.getVideoTimeEnd());
    }

    @Override
    public void onVideoStartFailed() {
        VideoStartFailedReason videoStartFailedReason =
                playerStateMachine.getVideoStartFailedReason();
        if (videoStartFailedReason == null) {
            videoStartFailedReason = VideoStartFailedReason.UNKNOWN;
        }

        EventData data = createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setVideoStartFailed(true);
        ErrorCode errorCode = videoStartFailedReason.getErrorCode();
        if (errorCode != null) {
            data.setErrorCode(errorCode.getErrorCode());
            data.setErrorMessage(errorCode.getDescription());
            data.setErrorData(serialize(errorCode.getErrorData()));
            eventBus.notify(
                    OnErrorDetailEventListener.class,
                    listener ->
                            listener.onError(
                                    Util.getTimestamp(),
                                    errorCode.getErrorCode(),
                                    errorCode.getDescription(),
                                    null));
        }
        data.setVideoStartFailedReason(videoStartFailedReason.getReason());
        sendEventData(data);
        this.detachPlayer();
    }

    public final void resetSourceRelatedState() {
        if (this.eventDataDispatcher != null) {
            this.eventDataDispatcher.resetSourceRelatedState();
        }

        featureManager.resetFeatures();
        // TODO reset features and prepare for new source

        if (this.playerAdapter != null) {
            this.playerAdapter.resetSourceRelatedState();
        }
    }

    public CustomData getCustomData() {
        SourceMetadata sourceMetadata = playerAdapter.getCurrentSourceMetadata();
        if (sourceMetadata != null) {
            return SourceMetadataExtension.Companion.getCustomData(sourceMetadata);
        }
        return this.bitmovinAnalyticsConfig.getCustomData();
    }

    public void setCustomData(CustomData customData) {
        CustomDataHelpers.Setter customDataSetter = this.bitmovinAnalyticsConfig::setCustomData;

        SourceMetadata sourceMetadata = playerAdapter.getCurrentSourceMetadata();

        if (sourceMetadata != null) {
            customDataSetter =
                    (changedCustomData) ->
                            SourceMetadataExtension.Companion.setCustomData(
                                    sourceMetadata, changedCustomData);
        }

        this.playerStateMachine.changeCustomData(getPosition(), customData, customDataSetter);
    }

    public void setCustomDataOnce(CustomData customData) {
        if (playerAdapter == null) {
            Log.d(TAG, "Custom data could not be set because player is not attached");
            return;
        }
        CustomDataHelpers.Getter customDataGetter = this.bitmovinAnalyticsConfig::getCustomData;
        CustomDataHelpers.Setter customDataSetter = this.bitmovinAnalyticsConfig::setCustomData;

        SourceMetadata sourceMetadata = playerAdapter.getCurrentSourceMetadata();

        if (sourceMetadata != null) {
            customDataGetter =
                    () -> SourceMetadataExtension.Companion.getCustomData(sourceMetadata);
            customDataSetter =
                    (changedCustomData) ->
                            SourceMetadataExtension.Companion.setCustomData(
                                    sourceMetadata, changedCustomData);
        }

        CustomData currentCustomData = customDataGetter.getCustomData();
        customDataSetter.setCustomData(customData);
        EventData eventData = createEventData();
        eventData.setState(PlayerState.CUSTOMDATACHANGE.toString().toLowerCase());
        sendEventData(eventData);
        customDataSetter.setCustomData(currentCustomData);
    }

    public void sendEventData(EventData data) {
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

    public Observable<OnAnalyticsReleasingEventListener> getOnAnalyticsReleasingObservable() {
        return eventBus.get(OnAnalyticsReleasingEventListener.class);
    }

    public Observable<OnErrorDetailEventListener> getOnErrorDetailObservable() {
        return eventBus.get(OnErrorDetailEventListener.class);
    }

    @Override
    public void configureFeatures(boolean authenticated, @Nullable Map<String, String> settings) {
        if (settings == null) {
            settings = new HashMap<>();
        }
        featureManager.configureFeatures(authenticated, settings);
    }

    @Override
    public void authenticationCompleted(boolean success) {
        if (!success) {
            detachPlayer();
        }
    }

    public void addDebugListener(DebugListener listener) {
        eventBus.get(DebugListener.class).subscribe(listener);
    }

    public void removeDebugListener(DebugListener listener) {
        eventBus.get(DebugListener.class).unsubscribe(listener);
    }

    @NotNull
    @Override
    public String getImpressionId() {
        return this.playerStateMachine.getImpressionId();
    }

    public interface DebugListener {
        void onDispatchEventData(EventData data);

        void onDispatchAdEventData(AdEventData data);

        void onMessage(String message);
    }

    private DebugCallback debugCallback =
            new DebugCallback() {
                @Override
                public void dispatchEventData(@NotNull EventData data) {
                    eventBus.notify(
                            DebugListener.class, listener -> listener.onDispatchEventData(data));
                }

                @Override
                public void dispatchAdEventData(@NotNull AdEventData data) {
                    eventBus.notify(
                            DebugListener.class, listener -> listener.onDispatchAdEventData(data));
                }

                @Override
                public void message(@NotNull String message) {
                    eventBus.notify(DebugListener.class, listener -> listener.onMessage(message));
                }
            };
}
