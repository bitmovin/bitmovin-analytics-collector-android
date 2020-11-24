package com.bitmovin.analytics.bitmovin.player;

import android.util.Log;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.EventDataDecoratorPipeline;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.data.DRMInformation;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.data.EventDataDecorator;
import com.bitmovin.analytics.data.DeviceInformationEventDataDecorator;
import com.bitmovin.analytics.enums.PlayerType;
import com.bitmovin.analytics.enums.VideoStartFailedReason;
import com.bitmovin.analytics.error.ExceptionMapper;
import com.bitmovin.analytics.stateMachines.PlayerState;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BitmovinSdkAdapter implements PlayerAdapter, EventDataDecorator {
    private static final String TAG = "BitmovinPlayerAdapter";
    private final BitmovinAnalyticsConfig config;
    private final BitmovinPlayer bitmovinPlayer;
    private final DeviceInformationEventDataDecorator deviceInformationEventDataDecorator;
    private PlayerStateMachine stateMachine;
    private ExceptionMapper<ErrorEvent> exceptionMapper = new BitmovinPlayerExceptionMapper();
    private int totalDroppedVideoFrames;
    private boolean playerIsReady;
    private boolean isVideoAttemptedPlay = false;
    private DRMInformation drmInformation = null;

    public BitmovinSdkAdapter(BitmovinPlayer bitmovinPlayer, BitmovinAnalyticsConfig config, DeviceInformationEventDataDecorator deviceInformationEventDataDecorator, PlayerStateMachine stateMachine) {
        this.config = config;
        this.stateMachine = stateMachine;
        this.bitmovinPlayer = bitmovinPlayer;
        this.deviceInformationEventDataDecorator = deviceInformationEventDataDecorator;
    }

    public void init() {
        addPlayerListeners();
        checkAutoplayStartup();
        this.totalDroppedVideoFrames = 0;
        this.playerIsReady = false;
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
    public void decorate(@NotNull EventData data) {

        data.setAnalyticsVersion(BuildConfig.VERSION_NAME);
        data.setPlayer(PlayerType.BITMOVIN.toString());

        //duration
        double duration = bitmovinPlayer.getDuration();
        if (duration != Double.POSITIVE_INFINITY) {
            data.setVideoDuration((long) duration * Util.MILLISECONDS_IN_SECONDS);
        }

        //ad
        if (bitmovinPlayer.isAd()) {
            data.setAd(1);
        }

        //isLive
        data.setLive(Util.getIsLiveFromConfigOrPlayer(playerIsReady, config.isLive(), bitmovinPlayer.isLive()));

        //version
        data.setVersion(PlayerType.BITMOVIN.toString() + "-" + BitmovinUtil.getPlayerVersion());

        //isCasting
        data.setCasting(bitmovinPlayer.isCasting());

        // DroppedVideoFrames
        data.setDroppedFrames(this.totalDroppedVideoFrames);
        this.totalDroppedVideoFrames = 0;

        //streamFormat, mpdUrl, and m3u8Url
        if (bitmovinPlayer.getConfig() != null && bitmovinPlayer.getConfig().getSourceItem() != null) {
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
                    if (sourceItem.getProgressiveSources() != null && sourceItem.getProgressiveSources().size() > 0) {
                        data.setM3u8Url(sourceItem.getProgressiveSources().get(0).getUrl());
                    }
                    data.setStreamFormat(Util.PROGRESSIVE_STREAM_FORMAT);
                    break;
                case SMOOTH:
                    data.setStreamFormat(Util.SMOOTH_STREAM_FORMAT);
                    break;
            }
        }

        //video quality
        VideoQuality videoQuality = bitmovinPlayer.getPlaybackVideoData();
        if (videoQuality != null) {
            data.setVideoBitrate(videoQuality.getBitrate());
            data.setVideoPlaybackHeight(videoQuality.getHeight());
            data.setVideoPlaybackWidth(videoQuality.getWidth());
            data.setVideoCodec(videoQuality.getCodec());
        }

        //audio quality
        AudioQuality audioQuality = bitmovinPlayer.getPlaybackAudioData();
        if (audioQuality != null) {
            data.setAudioBitrate(audioQuality.getBitrate());
            data.setAudioCodec(audioQuality.getCodec());
        }

        //Subtitle info
        SubtitleTrack subtitle = bitmovinPlayer.getSubtitle();
        if (subtitle != null && subtitle.getId() != null) {
            data.setSubtitleLanguage(subtitle.getLanguage() != null ? subtitle.getLanguage() : subtitle.getLabel());
            data.setSubtitleEnabled(true);
        }

        //Audio language
        AudioTrack audioTrack = bitmovinPlayer.getAudio();
        if (audioTrack != null && audioTrack.getId() != null) {
            data.setAudioLanguage(audioTrack.getLanguage());
        }

        // DRM Information
        if (drmInformation != null) {
            data.setDrmType(drmInformation.getType());
        }
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
    public void registerEventDataDecorators(EventDataDecoratorPipeline pipeline) {
        pipeline.registerEventDataDecorator(this.deviceInformationEventDataDecorator);
        pipeline.registerEventDataDecorator(this);
    }

    @Override
    public long getPosition() {
        return (long) bitmovinPlayer.getCurrentTime() * Util.MILLISECONDS_IN_SECONDS;
    }

    @Nullable
    @Override
    public DRMInformation getDRMInformation() {
        return drmInformation;
    }

    @Override
    public void clearValues() {

    }

    /*
      Because of the late initialization of the Adapter we do not get the first couple of events
      so in case the player starts a video due to autoplay=true we need to transition into startup state manually
     */
    private void checkAutoplayStartup() {
        if (bitmovinPlayer.getConfig() != null) {
            PlaybackConfiguration playbackConfiguration = bitmovinPlayer.getConfig().getPlaybackConfiguration();
            SourceConfiguration source = bitmovinPlayer.getConfig().getSourceConfiguration();
            if (playbackConfiguration != null && source != null && source.getFirstSourceItem() != null && playbackConfiguration.isAutoplayEnabled()) {
                startup();
            }
        }
    }

    private void startup() {
        stateMachine.transitionState(PlayerState.STARTUP, getPosition());
        if (!bitmovinPlayer.isAd()) {
            isVideoAttemptedPlay = true;
        }
    }

    /**
     * Player Listeners
     */

    private OnSourceLoadedListener onSourceLoadedListener = new OnSourceLoadedListener() {
        @Override
        public void onSourceLoaded(SourceLoadedEvent sourceLoadedEvent) {
            Log.d(TAG, "On Source Loaded");
            isVideoAttemptedPlay = false;
        }
    };

    private OnSourceUnloadedListener onSourceUnloadedListener = new OnSourceUnloadedListener() {
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

    private OnDestroyListener onDestroyedListener = new OnDestroyListener() {
        @Override
        public void onDestroy(DestroyEvent destroyEvent) {
            try {
                Log.d(TAG, "On Destroy");
                if (!stateMachine.isStartupFinished() && isVideoAttemptedPlay) {
                      stateMachine.setVideoStartFailedReason(VideoStartFailedReason.PAGE_CLOSED);
                      stateMachine.transitionState(PlayerState.EXITBEFOREVIDEOSTART, getPosition());
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnPlaybackFinishedListener onPlaybackFinishedListener = new OnPlaybackFinishedListener() {
        @Override
        public void onPlaybackFinished(PlaybackFinishedEvent playbackFinishedEvent) {
            try {
                Log.d(TAG, "On Playback Finished Listener");

                long position = (bitmovinPlayer.getDuration() != Double.POSITIVE_INFINITY) ? (long) bitmovinPlayer.getDuration() * Util.MILLISECONDS_IN_SECONDS : getPosition();
                stateMachine.transitionState(PlayerState.PAUSE, position);
                stateMachine.disableHeartbeat();
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }

        }
    };

    private OnReadyListener onReadyListener = new OnReadyListener() {
        @Override
        public void onReady(ReadyEvent readyEvent) {
            Log.d(TAG, "On Ready Listener");
            playerIsReady = true;
        }
    };

    private OnPausedListener onPausedListener = new OnPausedListener() {
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

    private OnPlayListener onPlayListener = new OnPlayListener() {
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

    private OnPlayingListener onPlayingListener = new OnPlayingListener() {
        @Override
        public void onPlaying(PlayingEvent playingEvent) {
            try {
                Log.d(TAG, "On Playing Listener " + stateMachine.getCurrentState().toString());
                stateMachine.transitionState(PlayerState.PLAYING, getPosition());
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnSeekedListener onSeekedListener = new OnSeekedListener() {
        @Override
        public void onSeeked(SeekedEvent seekedEvent) {
            Log.d(TAG, "On Seeked Listener");
        }
    };

    private OnSeekListener onSeekListener = new OnSeekListener() {
        @Override
        public void onSeek(SeekEvent seekEvent) {
            try {
                Log.d(TAG, "On Seek Listener");
                if (stateMachine.getCurrentState() != PlayerState.SEEKING && stateMachine.isStartupFinished()) {
                    stateMachine.transitionState(PlayerState.SEEKING, getPosition());
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnStallEndedListener onStallEndedListener = new OnStallEndedListener() {
        @Override
        public void onStallEnded(StallEndedEvent stallEndedEvent) {
            try {
                Log.d(TAG, "On Stall Ended: " + String.valueOf(bitmovinPlayer.isPlaying()));
                if (stateMachine.isStartupFinished()) {
                    if (bitmovinPlayer.isPlaying() && stateMachine.getCurrentState() != PlayerState.PLAYING) {
                        stateMachine.transitionState(PlayerState.PLAYING, getPosition());
                    } else if (bitmovinPlayer.isPaused() && stateMachine.getCurrentState() != PlayerState.PAUSE) {
                        stateMachine.transitionState(PlayerState.PAUSE, getPosition());
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnAudioChangedListener onAudioChangedListener = new OnAudioChangedListener() {
        @Override
        public void onAudioChanged(AudioChangedEvent audioChangedEvent) {
            try {
                Log.d(TAG, "On AudioChanged: " + bitmovinPlayer.getAudio().getId());
                if ((stateMachine.getCurrentState() == PlayerState.PLAYING || stateMachine.getCurrentState() == PlayerState.PAUSE) && stateMachine.isStartupFinished()) {
                    PlayerState originalState = stateMachine.getCurrentState();
                    stateMachine.transitionState(PlayerState.AUDIOTRACKCHANGE, getPosition());
                    stateMachine.transitionState(originalState, getPosition());
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnSubtitleChangedListener onSubtitleChangedListener = new OnSubtitleChangedListener() {
        @Override
        public void onSubtitleChanged(SubtitleChangedEvent event) {
            try {
                Log.d(TAG, "On SubtitleChanged: " + bitmovinPlayer.getSubtitle().getId());
                if ((stateMachine.getCurrentState() == PlayerState.PLAYING || stateMachine.getCurrentState() == PlayerState.PAUSE) && stateMachine.isStartupFinished()) {
                    PlayerState originalState = stateMachine.getCurrentState();
                    stateMachine.transitionState(PlayerState.SUBTITLECHANGE, getPosition());
                    stateMachine.transitionState(originalState, getPosition());
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnStallStartedListener onStallStartedListener = new OnStallStartedListener() {
        @Override
        public void onStallStarted(StallStartedEvent stallStartedEvent) {
            try {
                Log.d(TAG, "On Stall Started Listener");
                if (stateMachine.getCurrentState() != PlayerState.SEEKING && stateMachine.isStartupFinished()) {
                    stateMachine.transitionState(PlayerState.BUFFERING, getPosition());
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnVideoPlaybackQualityChangedListener onVideoPlaybackQualityChangedListener = new OnVideoPlaybackQualityChangedListener() {
        @Override
        public void onVideoPlaybackQualityChanged(
                VideoPlaybackQualityChangedEvent videoPlaybackQualityChangedEvent) {
            try {
                Log.d(TAG, "On Video Quality Changed");
                if ((stateMachine.getCurrentState() == PlayerState.PLAYING || stateMachine.getCurrentState() == PlayerState.PAUSE)
                        && stateMachine.isStartupFinished() && stateMachine.isQualityChangeEventEnabled()) {
                    PlayerState originalState = stateMachine.getCurrentState();
                    stateMachine.transitionState(PlayerState.QUALITYCHANGE, getPosition());
                    stateMachine.transitionState(originalState, getPosition());
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnDroppedVideoFramesListener onDroppedVideoFramesListener = new OnDroppedVideoFramesListener() {
        @Override
        public void onDroppedVideoFrames(DroppedVideoFramesEvent droppedVideoFramesEvent) {
            try {
                totalDroppedVideoFrames += droppedVideoFramesEvent.getDroppedFrames();
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnAudioPlaybackQualityChangedListener onAudioPlaybackQualityChangedListener = new OnAudioPlaybackQualityChangedListener() {
        @Override
        public void onAudioPlaybackQualityChanged(
                AudioPlaybackQualityChangedEvent audioPlaybackQualityChangedEvent) {
            try {
                Log.d(TAG, "On Audio Quality Changed");
                if ((stateMachine.getCurrentState() == PlayerState.PLAYING || stateMachine.getCurrentState() == PlayerState.PAUSE)
                        && stateMachine.isStartupFinished() && stateMachine.isQualityChangeEventEnabled()) {
                    PlayerState originalState = stateMachine.getCurrentState();
                    AudioQuality oldQuality = audioPlaybackQualityChangedEvent.getOldAudioQuality();
                    AudioQuality newQuality = audioPlaybackQualityChangedEvent.getNewAudioQuality();
                    if (oldQuality != null && newQuality != null && oldQuality.getBitrate() == newQuality.getBitrate()) {
                        return;
                    }
                    stateMachine.transitionState(PlayerState.QUALITYCHANGE, getPosition());
                    stateMachine.transitionState(originalState, getPosition());
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnDownloadFinishedListener onDownloadFinishedListener = new OnDownloadFinishedListener() {
        @Override
        public void onDownloadFinished(DownloadFinishedEvent downloadFinishedEvent) {
            try {
                if (downloadFinishedEvent.getDownloadType().toString().contains("drm/license")) {
                    drmInformation = new DRMInformation(Double.valueOf(downloadFinishedEvent.getDownloadTime() * 1000).longValue(),
                            downloadFinishedEvent.getDownloadType().toString().replace("drm/license/", ""));
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnErrorListener onErrorListener = new OnErrorListener() {
        @Override
        public void onError(ErrorEvent errorEvent) {
            try {
                Log.d(TAG, "onPlayerError");
                long videoTime = getPosition();
                ErrorCode errorCode = exceptionMapper.map(errorEvent);

                stateMachine.setErrorCode(errorCode);
                if (!stateMachine.isStartupFinished() && isVideoAttemptedPlay) {
                    stateMachine.setVideoStartFailedReason(VideoStartFailedReason.PLAYER_ERROR);
                }
                stateMachine.transitionState(PlayerState.ERROR, videoTime);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnAdBreakStartedListener onAdBreakStartedListener = new OnAdBreakStartedListener() {
        @Override
        public void onAdBreakStarted(AdBreakStartedEvent adBreakStartedEvent) {
            try {
                stateMachine.startAd(getPosition());
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };

    private OnAdBreakFinishedListener onAdBreakFinishedListener = new OnAdBreakFinishedListener() {
        @Override
        public void onAdBreakFinished(AdBreakFinishedEvent adBreakFinishedEvent) {
            try {
             stateMachine.transitionState(PlayerState.ADFINISHED, getPosition());
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
    };
}
