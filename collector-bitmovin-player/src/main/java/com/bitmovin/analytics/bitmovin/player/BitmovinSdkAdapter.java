package com.bitmovin.analytics.bitmovin.player;

import android.util.Log;

import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.enums.PlayerType;
import com.bitmovin.analytics.stateMachines.PlayerState;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
import com.bitmovin.analytics.utils.Util;
import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.api.event.data.ErrorEvent;
import com.bitmovin.player.api.event.data.PausedEvent;
import com.bitmovin.player.api.event.data.PlayEvent;
import com.bitmovin.player.api.event.data.PlaybackFinishedEvent;
import com.bitmovin.player.api.event.data.ReadyEvent;
import com.bitmovin.player.api.event.data.SeekEvent;
import com.bitmovin.player.api.event.data.SeekedEvent;
import com.bitmovin.player.api.event.data.SourceLoadedEvent;
import com.bitmovin.player.api.event.data.SourceUnloadedEvent;
import com.bitmovin.player.api.event.data.StallEndedEvent;
import com.bitmovin.player.api.event.data.StallStartedEvent;
import com.bitmovin.player.api.event.data.VideoPlaybackQualityChangedEvent;
import com.bitmovin.player.api.event.listener.OnErrorListener;
import com.bitmovin.player.api.event.listener.OnPausedListener;
import com.bitmovin.player.api.event.listener.OnPlayListener;
import com.bitmovin.player.api.event.listener.OnPlaybackFinishedListener;
import com.bitmovin.player.api.event.listener.OnReadyListener;
import com.bitmovin.player.api.event.listener.OnSeekListener;
import com.bitmovin.player.api.event.listener.OnSeekedListener;
import com.bitmovin.player.api.event.listener.OnSourceLoadedListener;
import com.bitmovin.player.api.event.listener.OnSourceUnloadedListener;
import com.bitmovin.player.api.event.listener.OnStallEndedListener;
import com.bitmovin.player.api.event.listener.OnStallStartedListener;
import com.bitmovin.player.api.event.listener.OnVideoPlaybackQualityChangedListener;
import com.bitmovin.player.config.quality.VideoQuality;

public class BitmovinSdkAdapter implements PlayerAdapter {
    private static final String TAG = "BitmovinPlayerAdapter";
    private final BitmovinAnalyticsConfig config;
    private final BitmovinPlayer bitmovinPlayer;
    private PlayerStateMachine stateMachine;

    public BitmovinSdkAdapter(BitmovinPlayer bitmovinPlayer, BitmovinAnalyticsConfig config, PlayerStateMachine stateMachine) {
        this.config = config;
        this.stateMachine = stateMachine;
        this.bitmovinPlayer = bitmovinPlayer;

        addPlayerListeners();
    }

    @Override
    public void release() {
        if (bitmovinPlayer != null) {
            removePlayerListener();
        }
    }

    private void addPlayerListeners() {
        Log.d(TAG, "Adding Player Listeners");
        this.bitmovinPlayer.addEventListener(onSourceLoadedListener);
        this.bitmovinPlayer.addEventListener(onSourceUnloadedListener);

        this.bitmovinPlayer.addEventListener(onPlayListener);
        this.bitmovinPlayer.addEventListener(onPausedListener);
        this.bitmovinPlayer.addEventListener(onStallEndedListener);
        this.bitmovinPlayer.addEventListener(onSeekedListener);
        this.bitmovinPlayer.addEventListener(onSeekListener);
        this.bitmovinPlayer.addEventListener(onStallStartedListener);
        this.bitmovinPlayer.addEventListener(onPlaybackFinishedListener);
        this.bitmovinPlayer.addEventListener(onReadyListener);
        this.bitmovinPlayer.addEventListener(onVideoPlaybackQualityChangedListener);
        this.bitmovinPlayer.addEventListener(onErrorListener);
    }

    private void removePlayerListener() {
        Log.d(TAG, "Removing Player Listeners");
        this.bitmovinPlayer.removeEventListener(onSourceLoadedListener);
        this.bitmovinPlayer.removeEventListener(onSourceUnloadedListener);

        this.bitmovinPlayer.removeEventListener(onPlayListener);
        this.bitmovinPlayer.removeEventListener(onPausedListener);
        this.bitmovinPlayer.removeEventListener(onStallEndedListener);
        this.bitmovinPlayer.removeEventListener(onSeekedListener);
        this.bitmovinPlayer.removeEventListener(onStallStartedListener);
        this.bitmovinPlayer.removeEventListener(onSeekListener);
        this.bitmovinPlayer.removeEventListener(onPlaybackFinishedListener);
        this.bitmovinPlayer.removeEventListener(onReadyListener);
        this.bitmovinPlayer.removeEventListener(onVideoPlaybackQualityChangedListener);
        this.bitmovinPlayer.removeEventListener(onErrorListener);
    }

    @Override
    public EventData createEventData() {

        EventData data = new EventData(config, stateMachine.getImpressionId());
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
        data.setLive(bitmovinPlayer.isLive());

        //version
        data.setVersion(com.bitmovin.player.BuildConfig.VERSION_NAME);

        //isCasting
        data.setCasting(bitmovinPlayer.isCasting());

        //streamFormat, mpdUrl, and m3u8Url
        if (bitmovinPlayer.getConfig() != null && bitmovinPlayer.getConfig().getSourceItem() != null && bitmovinPlayer.getConfig().getSourceItem().getDashSource() != null) {
            data.setMpdUrl(bitmovinPlayer.getConfig().getSourceItem().getDashSource().getUrl());
            data.setStreamFormat(Util.DASH_STREAM_FORMAT);
        } else if (bitmovinPlayer.getConfig() != null && bitmovinPlayer.getConfig().getSourceItem() != null && bitmovinPlayer.getConfig().getSourceItem().getHlsSource() != null) {
            data.setM3u8Url(bitmovinPlayer.getConfig().getSourceItem().getHlsSource().getUrl());
            data.setStreamFormat(Util.HLS_STREAM_FORMAT);
        }

        //video quality
        VideoQuality videoQuality = bitmovinPlayer.getVideoQuality();
        if (videoQuality != null) {
            data.setVideoBitrate(videoQuality.getBitrate());
            data.setVideoPlaybackHeight(videoQuality.getHeight());
            data.setVideoPlaybackWidth(videoQuality.getWidth());
        }


        return data;
    }

    private long getPosition() {
        return (long) bitmovinPlayer.getCurrentTime() * Util.MILLISECONDS_IN_SECONDS;
    }

    /**
     * Player Listeners
     */

    private OnSourceLoadedListener onSourceLoadedListener = new OnSourceLoadedListener() {
        @Override
        public void onSourceLoaded(SourceLoadedEvent sourceLoadedEvent) {
            Log.d(TAG, "On Source Loaded");
        }
    };

    private OnSourceUnloadedListener onSourceUnloadedListener = new OnSourceUnloadedListener() {
        @Override
        public void onSourceUnloaded(SourceUnloadedEvent sourceUnloadedEvent) {
            Log.d(TAG, "On Source Unloaded");
            stateMachine.resetStateMachine();
        }
    };

    private OnPlaybackFinishedListener onPlaybackFinishedListener = new OnPlaybackFinishedListener() {
        @Override
        public void onPlaybackFinished(PlaybackFinishedEvent playbackFinishedEvent) {
            Log.d(TAG, "On Playback Finished Listener");
            stateMachine.transitionState(PlayerState.PAUSE, getPosition());
            stateMachine.disableHeartbeat();

        }
    };

    private OnPausedListener onPausedListener = new OnPausedListener() {
        @Override
        public void onPaused(PausedEvent pausedEvent) {
            Log.d(TAG, "On Pause Listener");
            //Do not transition to a paused state unless a firstReadyTimestamp has been set. This will be set by the onReadyListener and prevents the player from showing inaccurate startup times
            if (stateMachine.getFirstReadyTimestamp() != 0) {
                stateMachine.transitionState(PlayerState.PLAYING, getPosition());
            }
        }
    };

    private OnPlayListener onPlayListener = new OnPlayListener() {
        @Override
        public void onPlay(PlayEvent playEvent) {
            Log.d(TAG, "On Play Listener");
            //Do not transition to a playing state unless a firstReadyTimestamp has been set. This will be set by the onReadyListener and prevents the player from showing inaccurate startup times when autopplay is enabled
            if (stateMachine.getFirstReadyTimestamp() != 0) {
                stateMachine.transitionState(PlayerState.PLAYING, getPosition());
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
            if (stateMachine.getCurrentState() != PlayerState.SEEKING) {
                stateMachine.transitionState(PlayerState.SEEKING, getPosition());
            }
        }
    };

    private OnStallEndedListener onStallEndedListener = new OnStallEndedListener() {
        @Override
        public void onStallEnded(StallEndedEvent stallEndedEvent) {
            Log.d(TAG, "On Stall Ended: " + String.valueOf(bitmovinPlayer.isPlaying()));
            if (bitmovinPlayer.isPlaying() && stateMachine.getCurrentState() != PlayerState.PLAYING) {
                stateMachine.transitionState(PlayerState.PLAYING, getPosition());
            } else if (bitmovinPlayer.isPaused() && stateMachine.getCurrentState() != PlayerState.PAUSE) {
                stateMachine.transitionState(PlayerState.PAUSE, getPosition());
            }
        }
    };

    private OnStallStartedListener onStallStartedListener = new OnStallStartedListener() {
        @Override
        public void onStallStarted(StallStartedEvent stallStartedEvent) {
            Log.d(TAG, "On Stall Started Listener");
            if (stateMachine.getCurrentState() != PlayerState.SEEKING && stateMachine.getFirstReadyTimestamp() != 0) {
                stateMachine.transitionState(PlayerState.BUFFERING, getPosition());
            }
        }
    };

    private OnVideoPlaybackQualityChangedListener onVideoPlaybackQualityChangedListener = new OnVideoPlaybackQualityChangedListener() {
        @Override
        public void onVideoPlaybackQualityChanged(VideoPlaybackQualityChangedEvent videoPlaybackQualityChangedEvent) {
            Log.d(TAG, "On Video Quality Changed");
            if ((stateMachine.getCurrentState() == PlayerState.PLAYING) || (stateMachine.getCurrentState() == PlayerState.PAUSE)) {
                PlayerState originalState = stateMachine.getCurrentState();
                stateMachine.transitionState(PlayerState.QUALITYCHANGE, getPosition());
                stateMachine.transitionState(originalState, getPosition());
            }
        }
    };


    private OnErrorListener onErrorListener = new OnErrorListener() {
        @Override
        public void onError(ErrorEvent errorEvent) {
            ErrorCode errorCode;

            switch (errorEvent.getCode()) {
                case 1016:
                    errorCode = ErrorCode.LICENSE_ERROR;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 1017:
                    errorCode = ErrorCode.LICENSE_ERROR_INVALID_DOMAIN;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 1018:
                    errorCode = ErrorCode.LICENSE_ERROR_INVALID_SERVER_URL;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 1020:
                    errorCode = ErrorCode.SOURCE_ERROR;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 3011:
                    errorCode = ErrorCode.DRM_REQUEST_HTTP_STATUS;
                    errorCode.setDescription(errorEvent.getMessage());
                case 3019:
                    errorCode = ErrorCode.DRM_REQUEST_ERROR;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 3021:
                    errorCode = ErrorCode.DRM_UNSUPPORTED;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 4000:
                    errorCode = ErrorCode.DRM_SESSION_ERROR;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 4001:
                    errorCode = ErrorCode.FILE_ACCESS;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 4002:
                    errorCode = ErrorCode.LOCKED_FOLDER;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 4003:
                    errorCode = ErrorCode.DEAD_LOCK;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 4004:
                    errorCode = ErrorCode.DRM_KEY_EXPIRED;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 4005:
                    errorCode = ErrorCode.PLAYER_SETUP_ERROR;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 3006:
                    errorCode = ErrorCode.DATASOURCE_HTTP_FAILURE;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 1000001:
                    errorCode = ErrorCode.DATASOURCE_INVALID_CONTENT_TYPE;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 1000002:
                    errorCode = ErrorCode.DATASOURCE_UNABLE_TO_CONNECT;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                case 1000003:
                    errorCode = ErrorCode.EXOPLAYER_RENDERER_ERROR;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
                default:
                    errorCode = ErrorCode.UNKNOWN_ERROR;
                    errorCode.setDescription(errorEvent.getMessage());
                    break;
            }
            stateMachine.setErrorCode(errorCode);
            stateMachine.transitionState(PlayerState.ERROR, getPosition());

        }
    };

    private OnReadyListener onReadyListener = new OnReadyListener() {
        @Override
        public void onReady(ReadyEvent readyEvent) {
            Log.d(TAG, "On Ready Listener");

            if (bitmovinPlayer.isPlaying()) {
                stateMachine.transitionState(PlayerState.PLAYING, getPosition());
            } else {
                stateMachine.transitionState(PlayerState.PAUSE, getPosition());
            }

        }
    };
}
