package com.bitmovin.analytics;

import android.content.Context;
import android.util.Log;
import com.bitmovin.analytics.adapters.AdAdapter;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.config.SourceMetadata;
import com.bitmovin.analytics.data.AdEventData;
import com.bitmovin.analytics.data.BackendFactory;
import com.bitmovin.analytics.data.CustomData;
import com.bitmovin.analytics.data.DebuggingEventDataDispatcher;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.data.EventDataFactory;
import com.bitmovin.analytics.data.IEventDataDispatcher;
import com.bitmovin.analytics.data.RandomizedUserIdIdProvider;
import com.bitmovin.analytics.data.SecureSettingsAndroidIdUserIdProvider;
import com.bitmovin.analytics.data.SimpleEventDataDispatcher;
import com.bitmovin.analytics.data.UserIdProvider;
import com.bitmovin.analytics.data.manipulators.ManifestUrlEventDataManipulator;
import com.bitmovin.analytics.features.Feature;
import com.bitmovin.analytics.features.FeatureManager;
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener;
import com.bitmovin.analytics.license.FeatureConfigContainer;
import com.bitmovin.analytics.license.LicenseCallback;
import com.bitmovin.analytics.stateMachines.DefaultStateMachineListener;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
import com.bitmovin.analytics.stateMachines.PlayerStates;
import com.bitmovin.analytics.stateMachines.StateMachineListener;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An analytics plugin that sends video playback analytics to Bitmovin Analytics servers. Currently
 * supports analytics of ExoPlayer video players
 */
public class BitmovinAnalytics implements LicenseCallback, ImpressionIdProvider {

    private static final String TAG = "BitmovinAnalytics";

    private FeatureManager<FeatureConfigContainer> featureManager = new FeatureManager<>();
    private final EventBus eventBus = new EventBus();

    private final BitmovinAnalyticsConfig bitmovinAnalyticsConfig;
    @Nullable private PlayerAdapter playerAdapter;
    @Nullable private StateMachineListener stateMachineListener;

    private final PlayerStateMachine playerStateMachine;
    private final IEventDataDispatcher eventDataDispatcher;
    private final Context context;
    private final UserIdProvider userIdProvider;
    private final EventDataFactory eventDataFactory;

    private BitmovinAdAnalytics adAnalytics;

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
        IEventDataDispatcher innerEventDataDispatcher =
                new SimpleEventDataDispatcher(
                        this.bitmovinAnalyticsConfig, this.context, this, new BackendFactory());
        this.eventDataDispatcher =
                new DebuggingEventDataDispatcher(innerEventDataDispatcher, debugCallback);
        if (this.bitmovinAnalyticsConfig.getAds()) {
            this.adAnalytics = new BitmovinAdAnalytics(this);
        }
    }

    public Context getContext() {
        return context;
    }

    public BitmovinAnalyticsConfig getConfig() {
        return bitmovinAnalyticsConfig;
    }

    public PlayerStateMachine getPlayerStateMachine() {
        return playerStateMachine;
    }

    public EventDataFactory getEventDataFactory() {
        return eventDataFactory;
    }

    /**
     * Attach a player instance to this analytics plugin. After this is completed, BitmovinAnalytics
     * will start monitoring and sending analytics data based on the attached player adapter.
     *
     * <p>To attach a different player instance, simply call this method again.
     */
    protected void attach(PlayerAdapter adapter) {
        detachPlayer();

        this.stateMachineListener =
                new DefaultStateMachineListener(
                        this, adapter, eventBus.get(OnErrorDetailEventListener.class));
        this.playerStateMachine.addListener(this.stateMachineListener);

        eventDataDispatcher.enable();
        this.playerAdapter = adapter;
        Collection<Feature<FeatureConfigContainer, ?>> features = this.playerAdapter.init();
        this.featureManager.registerFeatures(features);

        // this.registerEventDataManipulators(prePipelineManipulator);
        this.playerAdapter.registerEventDataManipulators(eventDataFactory);
        this.eventDataFactory.registerEventDataManipulator(
                new ManifestUrlEventDataManipulator(
                        this.playerAdapter, this.bitmovinAnalyticsConfig));
        // this.registerEventDataManipulators(postPipelineManipulator);

        tryAttachAd(adapter);
    }

    private void tryAttachAd(PlayerAdapter adapter) {
        if (adAnalytics == null) {
            return;
        }
        AdAdapter adAdapter = adapter.createAdAdapter();
        if (adAdapter == null) {
            return;
        }
        adAnalytics.attachAdapter(adapter, adAdapter);
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
            if (stateMachineListener != null) {
                playerStateMachine.removeListener(stateMachineListener);
            }

            playerStateMachine.resetStateMachine();
        }
        eventDataDispatcher.disable();
        eventDataFactory.clearEventDataManipulators();
    }

    private void detachAd() {
        if (adAnalytics != null) {
            adAnalytics.detachAdapter();
        }
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
        EventData eventData = playerAdapter.createEventData();
        eventData.setState(PlayerStates.CUSTOMDATACHANGE.getName());
        sendEventData(eventData);
        customDataSetter.setCustomData(currentCustomData);
    }

    public void sendEventData(EventData data) {
        this.eventDataDispatcher.add(data);
        if (this.playerAdapter != null) {
            this.playerAdapter.clearValues();
        }
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
    public void configureFeatures(
            boolean authenticated, @Nullable FeatureConfigContainer featureConfigs) {
        featureManager.configureFeatures(authenticated, featureConfigs);
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
