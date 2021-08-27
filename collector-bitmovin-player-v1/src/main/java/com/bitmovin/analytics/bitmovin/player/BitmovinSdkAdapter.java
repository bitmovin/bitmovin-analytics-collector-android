package com.bitmovin.analytics.bitmovin.player;

import android.util.Log;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.adapters.AdAdapter;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.config.SourceMetadata;
import com.bitmovin.analytics.data.DeviceInformationProvider;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.data.manipulators.EventDataManipulator;
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline;
import com.bitmovin.analytics.enums.CastTech;
import com.bitmovin.analytics.enums.PlayerType;
import com.bitmovin.analytics.enums.VideoStartFailedReason;
import com.bitmovin.analytics.error.ExceptionMapper;
import com.bitmovin.analytics.features.Feature;
import com.bitmovin.analytics.features.FeatureFactory;
import com.bitmovin.analytics.license.FeatureConfigContainer;
import com.bitmovin.analytics.stateMachines.PlayerState;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
import com.bitmovin.analytics.stateMachines.PlayerStates;
import com.bitmovin.analytics.utils.Util;
import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.api.event.data.AdBreakFinishedEvent;
import com.bitmovin.player.api.event.data.AdBreakStartedEvent;
import com.bitmovin.player.api.event.data.AudioChangedEvent;
import com.bitmovin.player.api.event.data.AudioPlaybackQualityChangedEvent;
import com.bitmovin.player.api.event.data.DestroyEvent;
import com.bitmovin.player.api.event.data.DownloadFinishedEvent;
import com.bitmovin.player.api.event.data.DroppedVideoFramesEvent;
import com.bitmovin.player.api.event.data.ErrorEvent;
import com.bitmovin.player.api.event.data.PausedEvent;
import com.bitmovin.player.api.event.data.PlayEvent;
import com.bitmovin.player.api.event.data.PlaybackFinishedEvent;
import com.bitmovin.player.api.event.data.PlayingEvent;
import com.bitmovin.player.api.event.data.ReadyEvent;
import com.bitmovin.player.api.event.data.SeekEvent;
import com.bitmovin.player.api.event.data.SeekedEvent;
import com.bitmovin.player.api.event.data.SourceLoadedEvent;
import com.bitmovin.player.api.event.data.SourceUnloadedEvent;
import com.bitmovin.player.api.event.data.StallEndedEvent;
import com.bitmovin.player.api.event.data.StallStartedEvent;
import com.bitmovin.player.api.event.data.SubtitleChangedEvent;
import com.bitmovin.player.api.event.data.VideoPlaybackQualityChangedEvent;
import com.bitmovin.player.api.event.listener.OnAdBreakFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdBreakStartedListener;
import com.bitmovin.player.api.event.listener.OnAudioChangedListener;
import com.bitmovin.player.api.event.listener.OnAudioPlaybackQualityChangedListener;
import com.bitmovin.player.api.event.listener.OnDestroyListener;
import com.bitmovin.player.api.event.listener.OnDownloadFinishedListener;
import com.bitmovin.player.api.event.listener.OnDroppedVideoFramesListener;
import com.bitmovin.player.api.event.listener.OnErrorListener;
import com.bitmovin.player.api.event.listener.OnPausedListener;
import com.bitmovin.player.api.event.listener.OnPlayListener;
import com.bitmovin.player.api.event.listener.OnPlaybackFinishedListener;
import com.bitmovin.player.api.event.listener.OnPlayingListener;
import com.bitmovin.player.api.event.listener.OnReadyListener;
import com.bitmovin.player.api.event.listener.OnSeekListener;
import com.bitmovin.player.api.event.listener.OnSeekedListener;
import com.bitmovin.player.api.event.listener.OnSourceLoadedListener;
import com.bitmovin.player.api.event.listener.OnSourceUnloadedListener;
import com.bitmovin.player.api.event.listener.OnStallEndedListener;
import com.bitmovin.player.api.event.listener.OnStallStartedListener;
import com.bitmovin.player.api.event.listener.OnSubtitleChangedListener;
import com.bitmovin.player.api.event.listener.OnVideoPlaybackQualityChangedListener;
import com.bitmovin.player.config.PlaybackConfiguration;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.config.quality.AudioQuality;
import com.bitmovin.player.config.quality.VideoQuality;
import com.bitmovin.player.config.track.AudioTrack;
import com.bitmovin.player.config.track.SubtitleTrack;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class BitmovinSdkAdapter implements PlayerAdapter, EventDataManipulator {
    private static final String TAG = "BitmovinPlayerAdapter";
    private final BitmovinAnalyticsConfig config;
    private final BitmovinPlayer bitmovinPlayer;
    private final DeviceInformationProvider deviceInformationProvider;
    private PlayerStateMachine stateMachine;
    private ExceptionMapper<ErrorEvent> exceptionMapper = new BitmovinPlayerExceptionMapper();
    private int totalDroppedVideoFrames;
    private boolean playerIsReady;
    private boolean isVideoAttemptedPlay = false;
    private FeatureFactory featureFactory;

    private Long drmDownloadTime = null;
    private String drmType = null;

    public BitmovinSdkAdapter(
            BitmovinPlayer bitmovinPlayer,
            BitmovinAnalyticsConfig config,
            DeviceInformationProvider deviceInformationProvider,
            PlayerStateMachine stateMachine,
            FeatureFactory featureFactory) {
        this.featureFactory = featureFactory;
        this.config = config;
        this.stateMachine = stateMachine;
        this.bitmovinPlayer = bitmovinPlayer;
        this.deviceInformationProvider = deviceInformationProvider;
    }

    public Collection<Feature<FeatureConfigContainer, ?>> init() {
        addPlayerListeners();
        checkAutoplayStartup();
        this.totalDroppedVideoFrames = 0;
        this.playerIsReady = false;
        return featureFactory.createFeatures();
    }

    @Override
    public SourceMetadata getCurrentSourceMetadata() {
        /* Adapter doesn't support source-specific metadata */
        return null;
    }

    private void addPlayerListeners() {
        Log.d(TAG, "Adding Player Listeners");
        this.bitmovinPlayer.addEventListener(onSourceLoadedListener);
        this.bitmovinPlayer.addEventListener(onSourceUnloadedListener);

        this.bitmovinPlayer.addEventListener(onPlayListener);
        this.bitmovinPlayer.addEventListener(onPlayingListener);
        this.bitmovinPlayer.addEventListener(onPausedListener);
        this.bitmovinPlayer.addEventListener(onStallEndedListener);
        this.bitmovinPlayer.addEventListener(onSeekedListener);
        this.bitmovinPlayer.addEventListener(onSeekListener);
        this.bitmovinPlayer.addEventListener(onStallStartedListener);
        this.bitmovinPlayer.addEventListener(onPlaybackFinishedListener);
        this.bitmovinPlayer.addEventListener(onReadyListener);
        this.bitmovinPlayer.addEventListener(onVideoPlaybackQualityChangedListener);
        this.bitmovinPlayer.addEventListener(onAudioPlaybackQualityChangedListener);
        this.bitmovinPlayer.addEventListener(onDroppedVideoFramesListener);
        this.bitmovinPlayer.addEventListener(onSubtitleChangedListener);
        this.bitmovinPlayer.addEventListener(onAudioChangedListener);
        this.bitmovinPlayer.addEventListener(onDownloadFinishedListener);
        this.bitmovinPlayer.addEventListener(onDestroyedListener);

        this.bitmovinPlayer.addEventListener(onErrorListener);

        this.bitmovinPlayer.addEventListener(onAdBreakStartedListener);
        this.bitmovinPlayer.addEventListener(onAdBreakFinishedListener);
    }

    private void removePlayerListener() {
        Log.d(TAG, "Removing Player Listeners");
        this.bitmovinPlayer.removeEventListener(onSourceLoadedListener);
        this.bitmovinPlayer.removeEventListener(onSourceUnloadedListener);

        this.bitmovinPlayer.removeEventListener(onPlayListener);
        this.bitmovinPlayer.removeEventListener(onPlayingListener);
        this.bitmovinPlayer.removeEventListener(onPausedListener);
        this.bitmovinPlayer.removeEventListener(onStallEndedListener);
        this.bitmovinPlayer.removeEventListener(onSeekedListener);
        this.bitmovinPlayer.removeEventListener(onStallStartedListener);
        this.bitmovinPlayer.removeEventListener(onSeekListener);
        this.bitmovinPlayer.removeEventListener(onPlaybackFinishedListener);
        this.bitmovinPlayer.removeEventListener(onReadyListener);
        this.bitmovinPlayer.removeEventListener(onVideoPlaybackQualityChangedListener);
        this.bitmovinPlayer.removeEventListener(onAudioPlaybackQualityChangedListener);
        this.bitmovinPlayer.removeEventListener(onDroppedVideoFramesListener);
        this.bitmovinPlayer.removeEventListener(onErrorListener);
        this.bitmovinPlayer.removeEventListener(onSubtitleChangedListener);
        this.bitmovinPlayer.removeEventListener(onAudioChangedListener);
        this.bitmovinPlayer.removeEventListener(onDownloadFinishedListener);
        this.bitmovinPlayer.removeEventListener(onDestroyedListener);

        this.bitmovinPlayer.removeEventListener(onAdBreakStartedListener);
        this.bitmovinPlayer.removeEventListener(onAdBreakFinishedListener);
    }

    @Override
    public void manipulate(@NotNull EventData data) {
        data.setPlayer(PlayerType.BITMOVIN.toString());

        // duration
        double duration = bitmovinPlayer.getDuration();
        if (duration != Double.POSITIVE_INFINITY) {
            data.setVideoDuration((long) duration * Util.MILLISECONDS_IN_SECONDS);
        }

        // ad
        if (bitmovinPlayer.isAd()) {
            data.setAd(1);
        }

        // isLive
        data.setLive(
                Util.getIsLiveFromConfigOrPlayer(
                        playerIsReady, config.isLive(), bitmovinPlayer.isLive()));

        // version
        data.setVersion(PlayerType.BITMOVIN.toString() + "-" + BitmovinUtil.getPlayerVersion());

        // isCasting
        data.setCasting(bitmovinPlayer.isCasting());
        if (bitmovinPlayer.isCasting()) {
            data.setCastTech(CastTech.GoogleCast.getValue());
        }

        // DroppedVideoFrames
        data.setDroppedFrames(this.totalDroppedVideoFrames);
        this.totalDroppedVideoFrames = 0;

        // streamFormat, mpdUrl, and m3u8Url
        if (bitmovinPlayer.getConfig() != null
                && bitmovinPlayer.getConfig().getSourceItem() != null) {
            SourceItem sourceItem = bitmovinPlayer.getConfig().getSourceItem();
            switch (sourceItem.getType()) {
                case HLS:
                    if (sourceItem.getHlsSource() != null) {
                        data.setM3u8Url(sourceItem.getHlsSource().getUrl());
                    }
                    data.setStreamFormat(Util.HLS_STREAM_FORMAT);
                    break;
                case DASH:
                    if (sourceItem.getDashSource() != null) {
                        data.setMpdUrl(sourceItem.getDashSource().getUrl());
                    }
                    data.setStreamFormat(Util.DASH_STREAM_FORMAT);
                    break;
                case PROGRESSIVE:
                    if (sourceItem.getProgressiveSources() != null
                            && sourceItem.getProgressiveSources().size() > 0) {
                        data.setProgUrl(sourceItem.getProgressiveSources().get(0).getUrl());
                    }
                    data.setStreamFormat(Util.PROGRESSIVE_STREAM_FORMAT);
                    break;
                case SMOOTH:
                    data.setStreamFormat(Util.SMOOTH_STREAM_FORMAT);
                    break;
            }
        }

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

        // DRM Information
        data.setDrmType(drmType);
    }

    @Override
    public void release() {
        playerIsReady = false;
        if (bitmovinPlayer != null) {
            removePlayerListener();
        }
        stateMachine.resetStateMachine();
    }

    @Override
    public void resetSourceRelatedState() {
        // no Playlist transition event in older version of Bitmovin Player collector
        // (v1)
    }

    @Override
    public void registerEventDataManipulators(EventDataManipulatorPipeline pipeline) {
        pipeline.registerEventDataManipulator(this);
    }

    @Override
    public long getPosition() {
        return BitmovinUtil.getCurrentTimeInMs(bitmovinPlayer);
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

    @Override
    public AdAdapter createAdAdapter() {
        return new BitmovinSdkAdAdapter(this.bitmovinPlayer);
    }

    /*
     * Because of the late initialization of the Adapter we do not get the first
     * couple of events so in case the player starts a video due to autoplay=true we
     * need to transition into startup state manually
     */
    private void checkAutoplayStartup() {
        if (bitmovinPlayer.getConfig() != null) {
            PlaybackConfiguration playbackConfiguration =
                    bitmovinPlayer.getConfig().getPlaybackConfiguration();
            SourceConfiguration source = bitmovinPlayer.getConfig().getSourceConfiguration();
            if (playbackConfiguration != null
                    && source != null
                    && source.getFirstSourceItem() != null
                    && playbackConfiguration.isAutoplayEnabled()) {
                startup();
            }
        }
    }

    private void startup() {
        stateMachine.transitionState(PlayerStates.STARTUP, getPosition());
        if (!bitmovinPlayer.isAd()) {
            // if ad is playing as first thing we prevent from sending the
            // VideoStartFailedReason.PAGE_CLOSED / VideoStartFailedReason.PLAYER_ERROR
            // because actual video is not playing yet
            isVideoAttemptedPlay = true;
        }
    }

    /** Player Listeners */
    private OnSourceLoadedListener onSourceLoadedListener =
            new OnSourceLoadedListener() {
                @Override
                public void onSourceLoaded(SourceLoadedEvent sourceLoadedEvent) {
                    Log.d(TAG, "On Source Loaded");
                    isVideoAttemptedPlay = false;
                }
            };

    private OnSourceUnloadedListener onSourceUnloadedListener =
            new OnSourceUnloadedListener() {
                @Override
                public void onSourceUnloaded(SourceUnloadedEvent sourceUnloadedEvent) {
                    try {
                        Log.d(TAG, "On Source Unloaded");
                        stateMachine.resetStateMachine();
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnDestroyListener onDestroyedListener =
            new OnDestroyListener() {
                @Override
                public void onDestroy(DestroyEvent destroyEvent) {
                    try {
                        Log.d(TAG, "On Destroy");
                        if (!stateMachine.isStartupFinished() && isVideoAttemptedPlay) {
                            stateMachine.setVideoStartFailedReason(
                                    VideoStartFailedReason.PAGE_CLOSED);
                            stateMachine.transitionState(
                                    PlayerStates.EXITBEFOREVIDEOSTART, getPosition());
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnPlaybackFinishedListener onPlaybackFinishedListener =
            new OnPlaybackFinishedListener() {
                @Override
                public void onPlaybackFinished(PlaybackFinishedEvent playbackFinishedEvent) {
                    try {
                        Log.d(TAG, "On Playback Finished Listener");

                        // if it's life stream we are using currentPosition of playback as videoTime
                        long videoTime =
                                (bitmovinPlayer.getDuration() != Double.POSITIVE_INFINITY)
                                        ? Util.secondsToMillis(bitmovinPlayer.getDuration())
                                        : getPosition();
                        stateMachine.transitionState(PlayerStates.PAUSE, videoTime);
                        stateMachine.disableHeartbeat();
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnReadyListener onReadyListener =
            new OnReadyListener() {
                @Override
                public void onReady(ReadyEvent readyEvent) {
                    Log.d(TAG, "On Ready Listener");
                    playerIsReady = true;
                }
            };

    private OnPausedListener onPausedListener =
            new OnPausedListener() {
                @Override
                public void onPaused(PausedEvent pausedEvent) {
                    try {
                        Log.d(TAG, "On Pause Listener");
                        stateMachine.pause(getPosition());
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnPlayListener onPlayListener =
            new OnPlayListener() {
                @Override
                public void onPlay(PlayEvent playEvent) {
                    try {
                        Log.d(TAG, "On Play Listener");
                        if (!stateMachine.isStartupFinished()) {
                            startup();
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnPlayingListener onPlayingListener =
            new OnPlayingListener() {
                @Override
                public void onPlaying(PlayingEvent playingEvent) {
                    try {
                        Log.d(
                                TAG,
                                "On Playing Listener " + stateMachine.getCurrentState().getName());
                        stateMachine.transitionState(PlayerStates.PLAYING, getPosition());
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnSeekedListener onSeekedListener =
            new OnSeekedListener() {
                @Override
                public void onSeeked(SeekedEvent seekedEvent) {
                    Log.d(TAG, "On Seeked Listener");
                }
            };

    private OnSeekListener onSeekListener =
            new OnSeekListener() {
                @Override
                public void onSeek(SeekEvent seekEvent) {
                    try {
                        Log.d(TAG, "On Seek Listener");
                        if (stateMachine.isStartupFinished()) {
                            stateMachine.transitionState(PlayerStates.SEEKING, getPosition());
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnStallEndedListener onStallEndedListener =
            new OnStallEndedListener() {
                @Override
                public void onStallEnded(StallEndedEvent stallEndedEvent) {
                    try {
                        Log.d(TAG, "On Stall Ended: " + bitmovinPlayer.isPlaying());
                        if (!stateMachine.isStartupFinished()) {
                            return;
                        }

                        if (bitmovinPlayer.isPlaying()
                                && stateMachine.getCurrentState() != PlayerStates.PLAYING) {
                            stateMachine.transitionState(PlayerStates.PLAYING, getPosition());
                        } else if (bitmovinPlayer.isPaused()
                                && stateMachine.getCurrentState() != PlayerStates.PAUSE) {
                            stateMachine.transitionState(PlayerStates.PAUSE, getPosition());
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnAudioChangedListener onAudioChangedListener =
            new OnAudioChangedListener() {
                @Override
                public void onAudioChanged(AudioChangedEvent audioChangedEvent) {
                    try {
                        Log.d(TAG, "On AudioChanged");
                        if (!stateMachine.isStartupFinished()) {
                            return;
                        }

                        if (stateMachine.getCurrentState() != PlayerStates.PLAYING
                                && stateMachine.getCurrentState() != PlayerStates.PAUSE) {
                            return;
                        }

                        PlayerState<?> originalState = stateMachine.getCurrentState();
                        stateMachine.transitionState(PlayerStates.AUDIOTRACKCHANGE, getPosition());
                        stateMachine.transitionState(originalState, getPosition());
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnSubtitleChangedListener onSubtitleChangedListener =
            new OnSubtitleChangedListener() {
                @Override
                public void onSubtitleChanged(SubtitleChangedEvent event) {
                    try {
                        Log.d(TAG, "On SubtitleChanged");
                        if (!stateMachine.isStartupFinished()) {
                            return;
                        }

                        if (stateMachine.getCurrentState() != PlayerStates.PLAYING
                                && stateMachine.getCurrentState() != PlayerStates.PAUSE) {
                            return;
                        }

                        PlayerState<?> originalState = stateMachine.getCurrentState();
                        stateMachine.transitionState(PlayerStates.SUBTITLECHANGE, getPosition());
                        stateMachine.transitionState(originalState, getPosition());
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnStallStartedListener onStallStartedListener =
            new OnStallStartedListener() {
                @Override
                public void onStallStarted(StallStartedEvent stallStartedEvent) {
                    try {
                        Log.d(TAG, "On Stall Started Listener");
                        if (!stateMachine.isStartupFinished()) {
                            return;
                        }

                        // if stalling is triggered by a seeking event
                        // we count the buffering time towards the seeking time
                        if (stateMachine.getCurrentState() != PlayerStates.SEEKING) {
                            stateMachine.transitionState(PlayerStates.BUFFERING, getPosition());
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnVideoPlaybackQualityChangedListener onVideoPlaybackQualityChangedListener =
            new OnVideoPlaybackQualityChangedListener() {
                @Override
                public void onVideoPlaybackQualityChanged(
                        VideoPlaybackQualityChangedEvent videoPlaybackQualityChangedEvent) {
                    try {
                        Log.d(TAG, "On Video Quality Changed");
                        if (!stateMachine.isStartupFinished()) {
                            return;
                        }

                        if (!stateMachine.isQualityChangeEventEnabled()) {
                            return;
                        }

                        if (stateMachine.getCurrentState() != PlayerStates.PLAYING
                                && stateMachine.getCurrentState() != PlayerStates.PAUSE) {
                            return;
                        }

                        PlayerState<?> originalState = stateMachine.getCurrentState();
                        stateMachine.transitionState(PlayerStates.QUALITYCHANGE, getPosition());
                        stateMachine.transitionState(originalState, getPosition());
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnDroppedVideoFramesListener onDroppedVideoFramesListener =
            new OnDroppedVideoFramesListener() {
                @Override
                public void onDroppedVideoFrames(DroppedVideoFramesEvent droppedVideoFramesEvent) {
                    try {
                        totalDroppedVideoFrames += droppedVideoFramesEvent.getDroppedFrames();
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnAudioPlaybackQualityChangedListener onAudioPlaybackQualityChangedListener =
            new OnAudioPlaybackQualityChangedListener() {
                @Override
                public void onAudioPlaybackQualityChanged(
                        AudioPlaybackQualityChangedEvent audioPlaybackQualityChangedEvent) {
                    try {
                        Log.d(TAG, "On Audio Quality Changed");
                        if (!stateMachine.isStartupFinished()) {
                            return;
                        }

                        if (!stateMachine.isQualityChangeEventEnabled()) {
                            return;
                        }

                        if (stateMachine.getCurrentState() != PlayerStates.PLAYING
                                && stateMachine.getCurrentState() != PlayerStates.PAUSE) {
                            return;
                        }

                        PlayerState<?> originalState = stateMachine.getCurrentState();
                        AudioQuality oldQuality =
                                audioPlaybackQualityChangedEvent.getOldAudioQuality();
                        AudioQuality newQuality =
                                audioPlaybackQualityChangedEvent.getNewAudioQuality();
                        if (oldQuality != null
                                && newQuality != null
                                && oldQuality.getBitrate() == newQuality.getBitrate()) {
                            return;
                        }
                        stateMachine.transitionState(PlayerStates.QUALITYCHANGE, getPosition());
                        stateMachine.transitionState(originalState, getPosition());
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnDownloadFinishedListener onDownloadFinishedListener =
            new OnDownloadFinishedListener() {
                @Override
                public void onDownloadFinished(DownloadFinishedEvent downloadFinishedEvent) {
                    try {
                        if (downloadFinishedEvent
                                .getDownloadType()
                                .toString()
                                .contains("drm/license")) {
                            drmDownloadTime =
                                    Util.secondsToMillis(downloadFinishedEvent.getDownloadTime());
                            drmType =
                                    downloadFinishedEvent
                                            .getDownloadType()
                                            .toString()
                                            .replace("drm/license/", "");
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnErrorListener onErrorListener =
            new OnErrorListener() {
                @Override
                public void onError(ErrorEvent errorEvent) {
                    try {
                        Log.d(TAG, "onPlayerError");
                        long videoTime = getPosition();
                        ErrorCode errorCode = exceptionMapper.map(errorEvent);

                        if (!stateMachine.isStartupFinished() && isVideoAttemptedPlay) {
                            stateMachine.setVideoStartFailedReason(
                                    VideoStartFailedReason.PLAYER_ERROR);
                        }
                        stateMachine.error(videoTime, errorCode);
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnAdBreakStartedListener onAdBreakStartedListener =
            new OnAdBreakStartedListener() {
                @Override
                public void onAdBreakStarted(AdBreakStartedEvent adBreakStartedEvent) {
                    try {
                        stateMachine.startAd(getPosition());
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };

    private OnAdBreakFinishedListener onAdBreakFinishedListener =
            new OnAdBreakFinishedListener() {
                @Override
                public void onAdBreakFinished(AdBreakFinishedEvent adBreakFinishedEvent) {
                    try {
                        stateMachine.transitionState(PlayerStates.ADFINISHED, getPosition());
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage(), e);
                    }
                }
            };
}
