package com.bitmovin.analytics.bitmovin.player;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.Log;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.data.DRMInformation;
import com.bitmovin.analytics.data.DeviceInformationProvider;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.data.EventDataFactory;
import com.bitmovin.analytics.data.UserIdProvider;
import com.bitmovin.analytics.enums.PlayerType;
import com.bitmovin.analytics.enums.VideoStartFailedReason;
import com.bitmovin.analytics.error.ExceptionMapper;
import com.bitmovin.analytics.stateMachines.PlayerState;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
import com.bitmovin.analytics.utils.Util;
import com.bitmovin.player.BitmovinPlayer;
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
import com.bitmovin.player.api.event.listener.OnAudioChangedListener;
import com.bitmovin.player.api.event.listener.OnAudioPlaybackQualityChangedListener;
import com.bitmovin.player.api.event.listener.OnDownloadFinishedListener;
import com.bitmovin.player.api.event.listener.OnDroppedVideoFramesListener;
import com.bitmovin.player.api.event.listener.OnErrorListener;
import com.bitmovin.player.api.event.listener.OnPausedListener;
import com.bitmovin.player.api.event.listener.OnPlayListener;
import com.bitmovin.player.api.event.listener.OnPlayingListener;
import com.bitmovin.player.api.event.listener.OnPlaybackFinishedListener;
import com.bitmovin.player.api.event.listener.OnReadyListener;
import com.bitmovin.player.api.event.listener.OnSeekListener;
import com.bitmovin.player.api.event.listener.OnSeekedListener;
import com.bitmovin.player.api.event.listener.OnSourceLoadedListener;
import com.bitmovin.player.api.event.listener.OnSourceUnloadedListener;
import com.bitmovin.player.api.event.listener.OnStallEndedListener;
import com.bitmovin.player.api.event.listener.OnStallStartedListener;
import com.bitmovin.player.api.event.listener.OnSubtitleChangedListener;
import com.bitmovin.player.api.event.listener.OnVideoPlaybackQualityChangedListener;
import com.bitmovin.player.api.event.listener.OnDestroyListener;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.config.quality.AudioQuality;
import com.bitmovin.player.config.quality.VideoQuality;
import com.bitmovin.player.config.track.AudioTrack;
import com.bitmovin.player.config.track.SubtitleTrack;

import org.jetbrains.annotations.Nullable;

public class BitmovinSdkAdapter implements PlayerAdapter {
    private static final String TAG = "BitmovinPlayerAdapter";
    private final BitmovinAnalyticsConfig config;
    private final BitmovinPlayer bitmovinPlayer;
    private final EventDataFactory factory;
    private PlayerStateMachine stateMachine;
    private ExceptionMapper<ErrorEvent> exceptionMapper = new BitmovinPlayerExceptionMapper();
    private int totalDroppedVideoFrames;
    private boolean playerIsReady;
    private boolean isVideoPlayed = false;
    private boolean isVideoAttemptedPlay = false;
    private DRMInformation drmInformation = null;

    public BitmovinSdkAdapter(BitmovinPlayer bitmovinPlayer, BitmovinAnalyticsConfig config, Context context, PlayerStateMachine stateMachine) {
        this.config = config;
        this.stateMachine = stateMachine;
        this.bitmovinPlayer = bitmovinPlayer;
        this.totalDroppedVideoFrames = 0;
        this.playerIsReady = false;
        this.factory = new EventDataFactory(config, context, new DeviceInformationProvider(context, getUserAgent(context)), new UserIdProvider(context));
        addPlayerListeners();
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
    }

    private String getUserAgent(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        String applicationName = "Unknown";
        if (stringId == 0 && applicationInfo.nonLocalizedLabel != null) {
            applicationInfo.nonLocalizedLabel.toString();
        }
        String versionName;
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException var5) {
            versionName = "?";
        }

        return applicationName + "/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE + ") " + "BitmovinPlayer/" + BitmovinUtil.getPlayerVersion();
    }

    @Override
    public EventData createEventData() {
        EventData data = factory.build(stateMachine.getImpressionId());

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
            data.setDrmLoadTime(drmInformation.getLoadTime());
        }

        return data;
    }

    @Override
    public void release() {
        playerIsReady = false;
        if (bitmovinPlayer != null) {
            removePlayerListener();
        }
    }

    @Override
    public long getPosition() {
        return (long) bitmovinPlayer.getCurrentTime() * Util.MILLISECONDS_IN_SECONDS;
    }

    @Override
    public void clearValues() {

    }

    /**
     * Player Listeners
     */

    private OnSourceLoadedListener onSourceLoadedListener = new OnSourceLoadedListener() {
        @Override
        public void onSourceLoaded(SourceLoadedEvent sourceLoadedEvent) {
            Log.d(TAG, "On Source Loaded");
            isVideoPlayed = false;
            isVideoAttemptedPlay = false;
        }
    };

    private OnSourceUnloadedListener onSourceUnloadedListener = new OnSourceUnloadedListener() {
        @Override
        public void onSourceUnloaded(SourceUnloadedEvent sourceUnloadedEvent) {
            Log.d(TAG, "On Source Unloaded");
            stateMachine.resetStateMachine();
        }
    };

    private OnDestroyListener onDestroyedListener = new OnDestroyListener() {
        @Override
        public void onDestroy(DestroyEvent destroyEvent) {
            Log.d(TAG, "On Destroy");
            if (!isVideoPlayed && isVideoAttemptedPlay) {
                stateMachine.setVideoStartFailedReason(VideoStartFailedReason.PAGE_CLOSED);
                stateMachine.transitionState(PlayerState.EXITBEFOREVIDEOSTART, getPosition());
            }
        }
    };

    private OnPlaybackFinishedListener onPlaybackFinishedListener = new OnPlaybackFinishedListener() {
        @Override
        public void onPlaybackFinished(PlaybackFinishedEvent playbackFinishedEvent) {
            Log.d(TAG, "On Playback Finished Listener");

            long position = (bitmovinPlayer.getDuration() != Double.POSITIVE_INFINITY) ? (long) bitmovinPlayer.getDuration() * Util.MILLISECONDS_IN_SECONDS : getPosition();
            stateMachine.transitionState(PlayerState.PAUSE, position);
            stateMachine.disableHeartbeat();

        }
    };

    private OnPausedListener onPausedListener = new OnPausedListener() {
        @Override
        public void onPaused(PausedEvent pausedEvent) {
            Log.d(TAG, "On Pause Listener");
            //Do not transition to a paused state unless a firstReadyTimestamp has been set. This will be set by the onReadyListener and prevents the player from showing inaccurate startup times
            if (stateMachine.getElapsedTimeFirstReady() != 0) {
                stateMachine.transitionState(PlayerState.PAUSE, getPosition());
            }
        }
    };

    private OnPlayListener onPlayListener = new OnPlayListener() {
        @Override
        public void onPlay(PlayEvent playEvent) {
            Log.d(TAG, "On Play Listener");
            if (!isVideoPlayed && !bitmovinPlayer.isAd()) {
                videoStartTimeout.start();
                isVideoAttemptedPlay = true;
            }
        }
    };

    private OnPlayingListener onPlayingListener = new OnPlayingListener() {
        @Override
        public void onPlaying(PlayingEvent playingEvent) {
            Log.d(TAG, "On Playing Listener " + stateMachine.getCurrentState().toString());
            //Do not transition to a playing state unless a firstReadyTimestamp has been set. This will be set by the onReadyListener and prevents the player from showing inaccurate startup times when autoplay is enabled
            if (stateMachine.getElapsedTimeFirstReady() != 0) {
                stateMachine.transitionState(PlayerState.PLAYING, getPosition());
            }
            if (!isVideoPlayed && !bitmovinPlayer.isAd()) {
                isVideoPlayed = true;
                videoStartTimeout.cancel();
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
            Log.d(TAG, "On Seek Listener");
            if (stateMachine.getCurrentState() != PlayerState.SEEKING && stateMachine.getElapsedTimeFirstReady() != 0) {
                stateMachine.transitionState(PlayerState.SEEKING, getPosition());
            }
        }
    };

    private OnStallEndedListener onStallEndedListener = new OnStallEndedListener() {
        @Override
        public void onStallEnded(StallEndedEvent stallEndedEvent) {
            Log.d(TAG, "On Stall Ended: " + String.valueOf(bitmovinPlayer.isPlaying()));
            if (stateMachine.getElapsedTimeFirstReady() != 0) {
                if (bitmovinPlayer.isPlaying() && stateMachine.getCurrentState() != PlayerState.PLAYING) {
                    stateMachine.transitionState(PlayerState.PLAYING, getPosition());
                } else if (bitmovinPlayer.isPaused() && stateMachine.getCurrentState() != PlayerState.PAUSE) {
                    stateMachine.transitionState(PlayerState.PAUSE, getPosition());
                }
            }
        }
    };

    private OnAudioChangedListener onAudioChangedListener = new OnAudioChangedListener() {
        @Override
        public void onAudioChanged(AudioChangedEvent audioChangedEvent) {
            Log.d(TAG, "On AudioChanged: " + bitmovinPlayer.getAudio().getId());
            if ((stateMachine.getCurrentState() == PlayerState.PLAYING || stateMachine.getCurrentState() == PlayerState.PAUSE) && stateMachine.getElapsedTimeFirstReady() != 0) {
                PlayerState originalState = stateMachine.getCurrentState();
                stateMachine.transitionState(PlayerState.AUDIOTRACKCHANGE, getPosition());
                stateMachine.transitionState(originalState, getPosition());
            }
        }
    };

    private OnSubtitleChangedListener onSubtitleChangedListener = new OnSubtitleChangedListener() {
        @Override
        public void onSubtitleChanged(SubtitleChangedEvent event) {
            Log.d(TAG, "On SubtitleChanged: " + bitmovinPlayer.getSubtitle().getId());
            if ((stateMachine.getCurrentState() == PlayerState.PLAYING || stateMachine.getCurrentState() == PlayerState.PAUSE) && stateMachine.getElapsedTimeFirstReady() != 0) {
                PlayerState originalState = stateMachine.getCurrentState();
                stateMachine.transitionState(PlayerState.SUBTITLECHANGE, getPosition());
                stateMachine.transitionState(originalState, getPosition());
            }
        }
    };

    private OnStallStartedListener onStallStartedListener = new OnStallStartedListener() {
        @Override
        public void onStallStarted(StallStartedEvent stallStartedEvent) {
            Log.d(TAG, "On Stall Started Listener");
            if (stateMachine.getCurrentState() != PlayerState.SEEKING && stateMachine.getElapsedTimeFirstReady() != 0) {
                stateMachine.transitionState(PlayerState.BUFFERING, getPosition());
            }
        }
    };

    private OnVideoPlaybackQualityChangedListener onVideoPlaybackQualityChangedListener = new OnVideoPlaybackQualityChangedListener() {
        @Override
        public void onVideoPlaybackQualityChanged(
                VideoPlaybackQualityChangedEvent videoPlaybackQualityChangedEvent) {
            Log.d(TAG, "On Video Quality Changed");
            if ((stateMachine.getCurrentState() == PlayerState.PLAYING || stateMachine.getCurrentState() == PlayerState.PAUSE) && stateMachine.getElapsedTimeFirstReady() != 0) {
                PlayerState originalState = stateMachine.getCurrentState();
                stateMachine.transitionState(PlayerState.QUALITYCHANGE, getPosition());
                stateMachine.transitionState(originalState, getPosition());
            }
        }
    };

    private OnDroppedVideoFramesListener onDroppedVideoFramesListener = new OnDroppedVideoFramesListener() {
        @Override
        public void onDroppedVideoFrames(DroppedVideoFramesEvent droppedVideoFramesEvent) {
            totalDroppedVideoFrames += droppedVideoFramesEvent.getDroppedFrames();
        }
    };

    private OnAudioPlaybackQualityChangedListener onAudioPlaybackQualityChangedListener = new OnAudioPlaybackQualityChangedListener() {
        @Override
        public void onAudioPlaybackQualityChanged(
                AudioPlaybackQualityChangedEvent audioPlaybackQualityChangedEvent) {
            Log.d(TAG, "On Audio Quality Changed");
            if ((stateMachine.getCurrentState() == PlayerState.PLAYING || stateMachine.getCurrentState() == PlayerState.PAUSE) && stateMachine.getElapsedTimeFirstReady() != 0) {
                PlayerState originalState = stateMachine.getCurrentState();
                stateMachine.transitionState(PlayerState.QUALITYCHANGE, getPosition());
                stateMachine.transitionState(originalState, getPosition());
            }
        }
    };

    private OnDownloadFinishedListener onDownloadFinishedListener = new OnDownloadFinishedListener() {
        @Override
        public void onDownloadFinished(DownloadFinishedEvent downloadFinishedEvent) {
            if (downloadFinishedEvent.getDownloadType().toString().contains("drm/license")) {
                drmInformation = new DRMInformation(Double.valueOf(downloadFinishedEvent.getDownloadTime() * 1000).longValue(),
                        downloadFinishedEvent.getDownloadType().toString().replace("drm/license/", ""));
            }
        }
    };

    private OnErrorListener onErrorListener = new OnErrorListener() {
        @Override
        public void onError(ErrorEvent errorEvent) {
            Log.d(TAG, "onPlayerError");
            long videoTime = getPosition();
            ErrorCode errorCode = exceptionMapper.map(errorEvent);

            stateMachine.setErrorCode(errorCode);
            if (!isVideoPlayed && isVideoAttemptedPlay) {
                videoStartTimeout.cancel();
                stateMachine.setVideoStartFailedReason(VideoStartFailedReason.PLAYER_ERROR);
            }
            stateMachine.transitionState(PlayerState.ERROR, videoTime);
        }
    };

    private OnReadyListener onReadyListener = new OnReadyListener() {
        @Override
        public void onReady(ReadyEvent readyEvent) {
            playerIsReady = true;
            Log.d(TAG, "On Ready Listener");

            if (bitmovinPlayer.isPlaying()) {
                stateMachine.transitionState(PlayerState.PLAYING, getPosition());
                // autoplay
                isVideoAttemptedPlay = true;
                videoStartTimeout.start();
            } else {
                stateMachine.transitionState(PlayerState.PAUSE, getPosition());
            }

        }
    };

    private CountDownTimer videoStartTimeout = new CountDownTimer(Util.VIDEOSTART_TIMEOUT, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "VideoStartTimeout finish");
            stateMachine.setVideoStartFailedReason(VideoStartFailedReason.TIMEOUT);
            stateMachine.transitionState(PlayerState.EXITBEFOREVIDEOSTART, getPosition());
        }
    };
}
