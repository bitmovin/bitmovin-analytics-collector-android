package com.bitmovin.analytics.bitmovin.player;

import android.util.Log;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.bitmovin.player.config.BitmovinAnalyticsSourceConfigProvider;
import com.bitmovin.analytics.data.DeviceInformationProvider;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.data.manipulators.EventDataManipulator;
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline;
import com.bitmovin.analytics.enums.DRMType;
import com.bitmovin.analytics.enums.PlayerType;
import com.bitmovin.analytics.enums.VideoStartFailedReason;
import com.bitmovin.analytics.error.ExceptionMapper;
import com.bitmovin.analytics.features.Feature;
import com.bitmovin.analytics.features.FeatureFactory;
import com.bitmovin.analytics.stateMachines.PlayerState;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
import com.bitmovin.analytics.utils.Util;
import com.bitmovin.player.api.PlaybackConfig;
import com.bitmovin.player.api.Player;
import com.bitmovin.player.api.deficiency.ErrorEvent;
import com.bitmovin.player.api.drm.ClearKeyConfig;
import com.bitmovin.player.api.drm.DrmConfig;
import com.bitmovin.player.api.drm.WidevineConfig;
import com.bitmovin.player.api.event.EventListener;
import com.bitmovin.player.api.event.PlayerEvent;
import com.bitmovin.player.api.event.SourceEvent;
import com.bitmovin.player.api.media.audio.AudioTrack;
import com.bitmovin.player.api.media.audio.quality.AudioQuality;
import com.bitmovin.player.api.media.subtitle.SubtitleTrack;
import com.bitmovin.player.api.media.video.quality.VideoQuality;
import com.bitmovin.player.api.source.Source;
import com.bitmovin.player.api.source.SourceConfig;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class BitmovinSdkAdapter implements PlayerAdapter, EventDataManipulator {
    private static final String TAG = "BitmovinPlayerAdapter";
    private final BitmovinAnalyticsConfig config;
    private Player bitmovinPlayer;
    private final DeviceInformationProvider deviceInformationProvider;
    private PlayerStateMachine stateMachine;
    private ExceptionMapper<ErrorEvent> exceptionMapper = new BitmovinPlayerExceptionMapper();
    private int totalDroppedVideoFrames;
    private boolean isVideoAttemptedPlay = false;
    private FeatureFactory featureFactory;
    private BitmovinAnalyticsSourceConfigProvider sourceConfigProvider;
    private SourceSwitchHandler sourceSwitchHandler;

    private Long drmDownloadTime = null;

    public BitmovinSdkAdapter(
            Player bitmovinPlayer,
            BitmovinAnalyticsConfig config,
            DeviceInformationProvider deviceInformationProvider,
            PlayerStateMachine stateMachine,
            FeatureFactory featureFactory,
            BitmovinAnalyticsSourceConfigProvider sourceConfigProvider) {
        this.featureFactory = featureFactory;
        this.config = config;
        this.stateMachine = stateMachine;
        this.bitmovinPlayer = bitmovinPlayer;
        this.deviceInformationProvider = deviceInformationProvider;
        this.sourceConfigProvider = sourceConfigProvider;
        this.sourceSwitchHandler =
                new SourceSwitchHandler(config, sourceConfigProvider, stateMachine, bitmovinPlayer);
    }

    public Collection<Feature<?>> init() {
        addPlayerListeners();
        checkAutoplayStartup();
        this.reset();
        this.sourceSwitchHandler.init();
        return featureFactory.createFeatures();
    }

    private void addPlayerListeners() {
        Log.d(TAG, "Adding Player Listeners");

        this.bitmovinPlayer.on(SourceEvent.Loaded.class, this.sourceEventLoadedListener);
        this.bitmovinPlayer.on(SourceEvent.Unloaded.class, this.sourceEventUnloadedListener);

        this.bitmovinPlayer.on(PlayerEvent.Play.class, this.playerEventPlayListener);
        this.bitmovinPlayer.on(PlayerEvent.Playing.class, this.playerEventPlayingListener);
        this.bitmovinPlayer.on(PlayerEvent.Paused.class, this.playerEventPausedListener);
        this.bitmovinPlayer.on(PlayerEvent.StallEnded.class, this.playerEventStallEndedListener);
        this.bitmovinPlayer.on(PlayerEvent.Seeked.class, this.playerEventSeekedListener);
        this.bitmovinPlayer.on(PlayerEvent.Seek.class, this.playerEventSeekListener);
        this.bitmovinPlayer.on(
                PlayerEvent.StallStarted.class, this.playerEventStallStartedListener);
        this.bitmovinPlayer.on(
                PlayerEvent.PlaybackFinished.class, this.playerEventPlaybackFinishedListener);
        this.bitmovinPlayer.on(
                PlayerEvent.VideoPlaybackQualityChanged.class,
                this.playerEventVideoPlaybackQualityChangedListener);
        this.bitmovinPlayer.on(
                PlayerEvent.AudioPlaybackQualityChanged.class,
                this.playerEventAudioPlaybackQualityChangedListener);
        this.bitmovinPlayer.on(
                PlayerEvent.DroppedVideoFrames.class, this.playerEventDroppedVideoFramesListener);
        this.bitmovinPlayer.on(
                SourceEvent.SubtitleChanged.class, this.sourceEventSubtitleChangedListener);
        this.bitmovinPlayer.on(
                SourceEvent.AudioChanged.class, this.sourceEventAudioChangedListener);
        this.bitmovinPlayer.on(
                SourceEvent.DownloadFinished.class, this.sourceEventDownloadFinishedListener);
        this.bitmovinPlayer.on(PlayerEvent.Destroy.class, this.playerEventDestroyListener);

        this.bitmovinPlayer.on(PlayerEvent.Error.class, this.playerErrorEventListener);
        this.bitmovinPlayer.on(SourceEvent.Error.class, this.sourceErrorEventListener);

        this.bitmovinPlayer.on(
                PlayerEvent.AdBreakStarted.class, this.playerEventAdBreakStartedListener);
        this.bitmovinPlayer.on(
                PlayerEvent.AdBreakFinished.class, this.playerEventAdBreakFinishedListener);
        this.bitmovinPlayer.on(PlayerEvent.TimeChanged.class, this.playerEventTimeChangedListener);
    }

    private void removePlayerListener() {
        Log.d(TAG, "Removing Player Listeners");
        this.bitmovinPlayer.off(this.sourceEventLoadedListener);
        this.bitmovinPlayer.off(this.sourceEventUnloadedListener);

        this.bitmovinPlayer.off(this.playerEventPlayListener);
        this.bitmovinPlayer.off(this.playerEventPlayingListener);
        this.bitmovinPlayer.off(this.playerEventPausedListener);
        this.bitmovinPlayer.off(this.playerEventStallEndedListener);
        this.bitmovinPlayer.off(this.playerEventSeekedListener);
        this.bitmovinPlayer.off(this.playerEventStallStartedListener);
        this.bitmovinPlayer.off(this.playerEventSeekListener);
        this.bitmovinPlayer.off(this.playerEventPlaybackFinishedListener);
        this.bitmovinPlayer.off(this.playerEventVideoPlaybackQualityChangedListener);
        this.bitmovinPlayer.off(this.playerEventAudioPlaybackQualityChangedListener);
        this.bitmovinPlayer.off(this.playerEventDroppedVideoFramesListener);
        this.bitmovinPlayer.off(this.sourceEventSubtitleChangedListener);
        this.bitmovinPlayer.off(this.sourceEventAudioChangedListener);
        this.bitmovinPlayer.off(this.sourceEventDownloadFinishedListener);
        this.bitmovinPlayer.off(this.playerEventDestroyListener);

        this.bitmovinPlayer.off(this.sourceErrorEventListener);
        this.bitmovinPlayer.off(this.playerErrorEventListener);

        this.bitmovinPlayer.off(this.playerEventAdBreakStartedListener);
        this.bitmovinPlayer.off(this.playerEventAdBreakFinishedListener);
        this.bitmovinPlayer.off(this.playerEventTimeChangedListener);
    }

    @Override
    public void manipulate(@NotNull EventData data) {
        // duration and isLive, streamFormat, mpdUrl, and m3u8Url
        Source source = bitmovinPlayer.getSource();
        if (source != null) {
            double duration = source.getDuration();
            if (duration == -1) {
                // Source duration is not available yet, fallback to BitmovinAnalyticsConfig
                data.setLive(config.isLive() != null && config.isLive());
            } else {
                if (duration == Double.POSITIVE_INFINITY) {
                    data.setLive(true);
                } else {
                    data.setLive(false);
                    data.setVideoDuration((long) duration * Util.MILLISECONDS_IN_SECONDS);
                }
            }

            SourceConfig sourceConfig = source.getConfig();

            switch (sourceConfig.getType()) {
                case Hls:
                    data.setM3u8Url(sourceConfig.getUrl());
                    data.setStreamFormat(Util.HLS_STREAM_FORMAT);
                    break;
                case Dash:
                    data.setMpdUrl(sourceConfig.getUrl());
                    data.setStreamFormat(Util.DASH_STREAM_FORMAT);
                    break;
                case Progressive:
                    data.setProgUrl(sourceConfig.getUrl());
                    data.setStreamFormat(Util.PROGRESSIVE_STREAM_FORMAT);
                    break;
                case Smooth:
                    data.setStreamFormat(Util.SMOOTH_STREAM_FORMAT);
                    break;
            }

            DrmConfig drmConfig = sourceConfig.getDrmConfig();
            if (drmConfig instanceof WidevineConfig) {
                data.setDrmType(DRMType.WIDEVINE.getValue());
            } else if (drmConfig instanceof ClearKeyConfig) {
                data.setDrmType(DRMType.CLEARKEY.getValue());
            } else if (drmConfig != null) {
                Log.d(TAG, "Warning: unknown DRM Type " + drmConfig.getClass().getSimpleName());
            }

        } else {
            // player active Source is not available
            data.setLive(config.isLive() != null && config.isLive());
        }

        data.setPlayer(PlayerType.BITMOVIN.toString());

        // ad
        if (bitmovinPlayer.isAd()) {
            data.setAd(1);
        }

        // version
        data.setVersion(PlayerType.BITMOVIN.toString() + "-" + BitmovinUtil.getPlayerVersion());

        // isCasting
        data.setCasting(bitmovinPlayer.isCasting());

        // DroppedVideoFrames
        data.setDroppedFrames(this.totalDroppedVideoFrames);
        this.totalDroppedVideoFrames = 0;

        // video quality
        VideoQuality videoQuality = bitmovinPlayer.getPlaybackVideoData();
        if (videoQuality != null) {
            data.setVideoBitrate(videoQuality.getBitrate());
            data.setVideoPlaybackHeight(videoQuality.getHeight());
            data.setVideoPlaybackWidth(videoQuality.getWidth());
            data.setVideoCodec(videoQuality.getCodec());
        }

        // audio quality
        AudioQuality audioQuality = bitmovinPlayer.getPlaybackAudioData();
        if (audioQuality != null) {
            data.setAudioBitrate(audioQuality.getBitrate());
            data.setAudioCodec(audioQuality.getCodec());
        }

        // Subtitle info
        SubtitleTrack subtitle = bitmovinPlayer.getSubtitle();
        if (subtitle != null && subtitle.getId() != null) {
            data.setSubtitleLanguage(
                    subtitle.getLanguage() != null ? subtitle.getLanguage() : subtitle.getLabel());
            data.setSubtitleEnabled(true);
        }

        // Audio language
        AudioTrack audioTrack = bitmovinPlayer.getAudio();
        if (audioTrack != null && audioTrack.getId() != null) {
            data.setAudioLanguage(audioTrack.getLanguage());
        }
    }

    @Override
    public void release() {
        if (bitmovinPlayer != null) {
            removePlayerListener();
            this.sourceSwitchHandler.destroy();
        }
        this.reset();
        this.stateMachine.resetStateMachine();
    }

    @Override
    public void reset() {
        this.totalDroppedVideoFrames = 0;
        this.drmDownloadTime = null;
    }

    @Override
    public void registerEventDataManipulators(EventDataManipulatorPipeline pipeline) {
        pipeline.registerEventDataManipulator(this);
    }

    @Override
    public long getPosition() {
        return BitmovinUtil.getPositionFromPlayer(bitmovinPlayer);
    }

    @Override
    public Long getDRMDownloadTime() {
        return drmDownloadTime;
    }

    @Override
    public DeviceInformationProvider getDeviceInformationProvider() {
        return this.deviceInformationProvider;
    }

    @Override
    public void clearValues() {}

    /*
     * Because of the late initialization of the Adapter we do not get the first
     * couple of events so in case the player starts a video due to autoplay=true we
     * need to transition into startup state manually
     */
    private void checkAutoplayStartup() {
        PlaybackConfig playbackConfig = bitmovinPlayer.getConfig().getPlaybackConfig();
        Source source = bitmovinPlayer.getSource();

        if (source != null && playbackConfig.isAutoplayEnabled()) {
            Log.d(TAG, "Detected Autoplay going to startup");
            startup();
        }
    }

    private void startup() {
        stateMachine.transitionState(PlayerState.STARTUP, getPosition());
        if (!bitmovinPlayer.isAd()) {
            isVideoAttemptedPlay = true;
        }
    }

    private final EventListener<SourceEvent.Loaded> sourceEventLoadedListener =
            (event) -> {
                Log.d(TAG, "On Source Loaded");
                isVideoAttemptedPlay = false;
            };

    private final EventListener<SourceEvent.Unloaded> sourceEventUnloadedListener =
            (event) -> {
                try {
                    Log.d(TAG, "On Source Unloaded");
                    stateMachine.resetStateMachine();
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<PlayerEvent.Destroy> playerEventDestroyListener =
            (event) -> {
                try {
                    Log.d(TAG, "On Destroy");
                    if (!stateMachine.isStartupFinished() && isVideoAttemptedPlay) {
                        stateMachine.setVideoStartFailedReason(VideoStartFailedReason.PAGE_CLOSED);
                        stateMachine.transitionState(
                                PlayerState.EXITBEFOREVIDEOSTART, getPosition());
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<PlayerEvent.PlaybackFinished> playerEventPlaybackFinishedListener =
            (event) -> {
                try {
                    Log.d(TAG, "On Playback Finished Listener");

                    long position =
                            (bitmovinPlayer.getDuration() != Double.POSITIVE_INFINITY)
                                    ? (long) bitmovinPlayer.getDuration()
                                            * Util.MILLISECONDS_IN_SECONDS
                                    : getPosition();
                    stateMachine.transitionState(PlayerState.PAUSE, position);
                    stateMachine.disableHeartbeat();
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<PlayerEvent.Paused> playerEventPausedListener =
            (event) -> {
                try {
                    Log.d(TAG, "On Pause Listener");
                    stateMachine.pause(getPosition());
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<PlayerEvent.Play> playerEventPlayListener =
            event -> {
                try {
                    Log.d(TAG, "On Play Listener");
                    if (!stateMachine.isStartupFinished()) {
                        startup();
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<PlayerEvent.Playing> playerEventPlayingListener =
            (event) -> {
                try {
                    Log.d(TAG, "On Playing Listener " + stateMachine.getCurrentState().toString());
                    stateMachine.transitionState(PlayerState.PLAYING, getPosition());
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<PlayerEvent.TimeChanged> playerEventTimeChangedListener =
            (event) -> {
                try {
                    if (!bitmovinPlayer.isStalled()) {
                        stateMachine.transitionState(PlayerState.PLAYING, getPosition());
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<PlayerEvent.Seeked> playerEventSeekedListener =
            (event) -> {
                Log.d(TAG, "On Seeked Listener");
            };

    private final EventListener<PlayerEvent.Seek> playerEventSeekListener =
            (event) -> {
                try {
                    Log.d(TAG, "On Seek Listener");
                    if (stateMachine.getCurrentState() != PlayerState.SEEKING
                            && stateMachine.isStartupFinished()) {
                        stateMachine.transitionState(PlayerState.SEEKING, getPosition());
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<PlayerEvent.StallEnded> playerEventStallEndedListener =
            (event) -> {
                try {
                    Log.d(TAG, "On Stall Ended: " + String.valueOf(bitmovinPlayer.isPlaying()));
                    if (stateMachine.isStartupFinished()) {
                        if (bitmovinPlayer.isPlaying()
                                && stateMachine.getCurrentState() != PlayerState.PLAYING) {
                            stateMachine.transitionState(PlayerState.PLAYING, getPosition());
                        } else if (bitmovinPlayer.isPaused()
                                && stateMachine.getCurrentState() != PlayerState.PAUSE) {
                            stateMachine.transitionState(PlayerState.PAUSE, getPosition());
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<SourceEvent.AudioChanged> sourceEventAudioChangedListener =
            (event) -> {
                try {
                    Log.d(TAG, "On AudioChanged: " + bitmovinPlayer.getAudio().getId());
                    if ((stateMachine.getCurrentState() == PlayerState.PLAYING
                                    || stateMachine.getCurrentState() == PlayerState.PAUSE)
                            && stateMachine.isStartupFinished()) {
                        PlayerState originalState = stateMachine.getCurrentState();
                        stateMachine.transitionState(PlayerState.AUDIOTRACKCHANGE, getPosition());
                        stateMachine.transitionState(originalState, getPosition());
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<SourceEvent.SubtitleChanged> sourceEventSubtitleChangedListener =
            (event) -> {
                try {
                    Log.d(TAG, "On SubtitleChanged: " + bitmovinPlayer.getSubtitle().getId());
                    if ((stateMachine.getCurrentState() == PlayerState.PLAYING
                                    || stateMachine.getCurrentState() == PlayerState.PAUSE)
                            && stateMachine.isStartupFinished()) {
                        PlayerState originalState = stateMachine.getCurrentState();
                        stateMachine.transitionState(PlayerState.SUBTITLECHANGE, getPosition());
                        stateMachine.transitionState(originalState, getPosition());
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<PlayerEvent.StallStarted> playerEventStallStartedListener =
            (event) -> {
                try {
                    Log.d(TAG, "On Stall Started Listener isPlaying:" + bitmovinPlayer.isPlaying());
                    if (stateMachine.getCurrentState() != PlayerState.SEEKING
                            && stateMachine.isStartupFinished()) {
                        stateMachine.transitionState(PlayerState.BUFFERING, getPosition());
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    // TODO TSA events with bitrate 0 at the end of playing a source
    // should they be ignored?
    private final EventListener<PlayerEvent.VideoPlaybackQualityChanged>
            playerEventVideoPlaybackQualityChangedListener =
                    (event) -> {
                        try {
                            Log.d(TAG, "On Video Quality Changed");
                            if ((stateMachine.getCurrentState() == PlayerState.PLAYING
                                            || stateMachine.getCurrentState() == PlayerState.PAUSE)
                                    && stateMachine.isStartupFinished()
                                    && stateMachine.isQualityChangeEventEnabled()) {
                                PlayerState originalState = stateMachine.getCurrentState();
                                stateMachine.transitionState(
                                        PlayerState.QUALITYCHANGE, getPosition());
                                stateMachine.transitionState(originalState, getPosition());
                            }
                        } catch (Exception e) {
                            Log.d(TAG, e.getMessage(), e);
                        }
                    };

    private final EventListener<PlayerEvent.DroppedVideoFrames>
            playerEventDroppedVideoFramesListener =
                    (event) -> {
                        try {
                            totalDroppedVideoFrames += event.getDroppedFrames();
                        } catch (Exception e) {
                            Log.d(TAG, e.getMessage(), e);
                        }
                    };

    private final EventListener<PlayerEvent.AudioPlaybackQualityChanged>
            playerEventAudioPlaybackQualityChangedListener =
                    (event) -> {
                        try {
                            Log.d(TAG, "On Audio Quality Changed");
                            if ((stateMachine.getCurrentState() == PlayerState.PLAYING
                                            || stateMachine.getCurrentState() == PlayerState.PAUSE)
                                    && stateMachine.isStartupFinished()
                                    && stateMachine.isQualityChangeEventEnabled()) {
                                PlayerState originalState = stateMachine.getCurrentState();
                                AudioQuality oldQuality = event.getOldAudioQuality();
                                AudioQuality newQuality = event.getNewAudioQuality();
                                if (oldQuality != null
                                        && newQuality != null
                                        && oldQuality.getBitrate() == newQuality.getBitrate()) {
                                    return;
                                }
                                stateMachine.transitionState(
                                        PlayerState.QUALITYCHANGE, getPosition());
                                stateMachine.transitionState(originalState, getPosition());
                            }
                        } catch (Exception e) {
                            Log.d(TAG, e.getMessage(), e);
                        }
                    };

    private final EventListener<SourceEvent.DownloadFinished> sourceEventDownloadFinishedListener =
            (event) -> {
                try {
                    if (event.getDownloadType().toString().contains("drm/license")) {
                        drmDownloadTime =
                                Double.valueOf(event.getDownloadTime() * 1000).longValue();
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<PlayerEvent.Error> playerErrorEventListener =
            (event) -> {
                try {
                    Log.d(TAG, "onPlayerError");
                    long videoTime = getPosition();
                    ErrorCode errorCode = exceptionMapper.map(event);

                    stateMachine.setErrorCode(errorCode);
                    if (!stateMachine.isStartupFinished() && isVideoAttemptedPlay) {
                        stateMachine.setVideoStartFailedReason(VideoStartFailedReason.PLAYER_ERROR);
                    }
                    stateMachine.transitionState(PlayerState.ERROR, videoTime);
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<SourceEvent.Error> sourceErrorEventListener =
            (event) -> {
                try {
                    Log.d(TAG, "onPlayerError");
                    long videoTime = getPosition();
                    ErrorCode errorCode = exceptionMapper.map(event);

                    stateMachine.setErrorCode(errorCode);
                    if (!stateMachine.isStartupFinished() && isVideoAttemptedPlay) {
                        stateMachine.setVideoStartFailedReason(VideoStartFailedReason.PLAYER_ERROR);
                    }
                    stateMachine.transitionState(PlayerState.ERROR, videoTime);
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<PlayerEvent.AdBreakStarted> playerEventAdBreakStartedListener =
            (event) -> {
                try {
                    stateMachine.startAd(getPosition());
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };

    private final EventListener<PlayerEvent.AdBreakFinished> playerEventAdBreakFinishedListener =
            (event) -> {
                try {
                    stateMachine.transitionState(PlayerState.ADFINISHED, getPosition());
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            };
}
