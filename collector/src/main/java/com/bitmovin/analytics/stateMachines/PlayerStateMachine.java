package com.bitmovin.analytics.stateMachines;

import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;

import com.bitmovin.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.enums.AnalyticsErrorCodes;
import com.bitmovin.analytics.enums.VideoStartFailedReason;
import com.bitmovin.analytics.utils.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerStateMachine {
    private static final String TAG = "PlayerStateMachine";
    private final BitmovinAnalyticsConfig config;
    private List<StateMachineListener> listeners = new ArrayList<StateMachineListener>();
    private PlayerState currentState;
    private long elapsedTimeOnEnter = 0;
    private long startupTime = 0;
    private boolean startupFinished = false;
    private long elapsedTimeSeekStart = 0;
    private long videoTimeStart;
    private long videoTimeEnd;
    private ErrorCode errorCode;
    private String impressionId;
    private Handler heartbeatHandler = new Handler();
    private int currentRebufferingIntervalIndex = 0;
    private static List<Integer> rebufferingIntervals = Arrays.asList(3000, 5000, 10000, 30000, 59700);
    private int heartbeatDelay = 59700; // default to 60 seconds
    private final BitmovinAnalytics analytics;
    private VideoStartFailedReason videoStartFailedReason;
    private int qualityChangeCount = 0;
    protected boolean isQualityChangeTimerRunning = false;

    public PlayerStateMachine(BitmovinAnalyticsConfig config, BitmovinAnalytics analytics) {
        this.config = config;
        this.analytics = analytics;
        this.heartbeatDelay = this.config.getHeartbeatInterval();
        resetStateMachine();
    }

    public void enableHeartbeat() {
        heartbeatHandler.postDelayed(new Runnable() {
            public void run() {
                triggerHeartbeat();
                heartbeatHandler.postDelayed(this, heartbeatDelay);
            }
        }, heartbeatDelay);
    }

    public void disableHeartbeat() {
        heartbeatHandler.removeCallbacksAndMessages(null);
    }

    public void enableRebufferHeartbeat() {
        heartbeatHandler.postDelayed(new Runnable() {
            public void run() {
                triggerHeartbeat();
                currentRebufferingIntervalIndex = Math.min(currentRebufferingIntervalIndex + 1, rebufferingIntervals.size() - 1);
                heartbeatHandler.postDelayed(this, rebufferingIntervals.get(currentRebufferingIntervalIndex));
            }
        }, rebufferingIntervals.get(currentRebufferingIntervalIndex));
    }

    public void disableRebufferHeartbeat() {
        currentRebufferingIntervalIndex = 0;
        heartbeatHandler.removeCallbacksAndMessages(null);
    }

    private void triggerHeartbeat() {
        long elapsedTime = Util.getElapsedTime();
        videoTimeEnd = analytics.getPosition();
        for (StateMachineListener listener : getListeners()) {
            listener.onHeartbeat(elapsedTime - elapsedTimeOnEnter);
        }
        elapsedTimeOnEnter = elapsedTime;
        videoTimeStart = videoTimeEnd;
    }

    public void resetStateMachine() {
        disableHeartbeat();
        disableRebufferHeartbeat();
        this.impressionId = Util.getUUID();
        this.videoStartFailedReason = null;
        startupTime = 0;
        startupFinished = false;
        videoStartTimeout.cancel();
        qualityChangeResetTimeout.cancel();
        rebufferingTimeout.cancel();
        setCurrentState(PlayerState.READY);
        resetQualityChangeCount();
    }

    public synchronized void transitionState(PlayerState destinationPlayerState, long videoTime) {
        if (!this.isTransitionAllowed(currentState, destinationPlayerState)) {
            return;
        }

        long elapsedTime = Util.getElapsedTime();
        videoTimeEnd = videoTime;

        Log.d(TAG, "Transitioning from " + currentState.toString() + " to " + destinationPlayerState.toString());

        currentState.onExitState(this, elapsedTime, destinationPlayerState);
        this.elapsedTimeOnEnter = elapsedTime;
        videoTimeStart = videoTimeEnd;
        destinationPlayerState.onEnterState(this);
        setCurrentState(destinationPlayerState);
    }

    private boolean isTransitionAllowed(PlayerState currentState, PlayerState destination) {
        if (destination == this.currentState) {
            return false;
        }
        // no state transitions like PLAYING or PAUSE during AD
        else if (currentState == PlayerState.AD && (destination != PlayerState.ERROR && destination != PlayerState.ADFINISHED )) {
            return false;
        }
        else if(currentState == PlayerState.READY && (destination != PlayerState.ERROR && destination != PlayerState.EXITBEFOREVIDEOSTART &&
                destination != PlayerState.STARTUP && destination != PlayerState.AD)){
            return false;
        }

        return true;
    }

    public boolean isStartupFinished() {
        return startupFinished;
    }

    public void setStartupFinished(boolean startupFinished) {
        this.startupFinished = startupFinished;
    }

    public void addListener(StateMachineListener toAdd) {
        listeners.add(toAdd);
    }

    public void removeListener(StateMachineListener toRemove) {
        listeners.remove(toRemove);
    }

    List<StateMachineListener> getListeners() {
        return listeners;
    }

    public PlayerState getCurrentState() {
        return currentState;
    }

    private void setCurrentState(final PlayerState newPlayerState) {
        this.currentState = newPlayerState;
    }

    public long getStartupTime() {
        return startupTime;
    }

    public void addStartupTime(long elapsedTime) {
        this.startupTime += elapsedTime;
    }

    public String getImpressionId() {
        return impressionId;
    }

    public long getVideoTimeStart() {
        return videoTimeStart;
    }

    public long getVideoTimeEnd() {
        return videoTimeEnd;
    }

    public long getElapsedTimeOnEnter() {
        return elapsedTimeOnEnter;
    }

    public long getElapsedTimeSeekStart() {
        return elapsedTimeSeekStart;
    }

    public void setElapsedTimeSeekStart(long elapsedTimeSeekStart) {
        this.elapsedTimeSeekStart = elapsedTimeSeekStart;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public VideoStartFailedReason getVideoStartFailedReason() {
        return videoStartFailedReason;
    }

    public void setVideoStartFailedReason(VideoStartFailedReason videoStartFailedReason) {
        this.videoStartFailedReason = videoStartFailedReason;
    }

    protected CountDownTimer videoStartTimeout = new CountDownTimer(Util.VIDEOSTART_TIMEOUT, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "VideoStartTimeout finish");
            setVideoStartFailedReason(VideoStartFailedReason.TIMEOUT);
            transitionState(PlayerState.EXITBEFOREVIDEOSTART, 0);
        }
    };

    public void pause(long position) {
        if (isStartupFinished()) {
            transitionState(PlayerState.PAUSE, position);
        }
        else {
            transitionState(PlayerState.READY, position);
        }
    }

    public void startAd(long position){
        transitionState(PlayerState.AD, position);
        startupTime = 0;
    }

    public boolean isQualityChangeEventEnabled(){
        return this.qualityChangeCount <= Util.ANALYTICS_QUALITY_CHANGE_COUNT_THRESHOLD;
    }

    public void increaseQualityChangeCount(){
        this.qualityChangeCount++;
    }

    protected void resetQualityChangeCount(){
        this.qualityChangeCount = 0;
    }

    protected CountDownTimer qualityChangeResetTimeout = new CountDownTimer(Util.ANALYTICS_QUALITY_CHANGE_COUNT_RESET_INTERVAL, 1000) {

        @Override
        public void onTick(long millisUntilFinished) {
            isQualityChangeTimerRunning = true;
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "qualityChangeResetTimeout finish");
            resetQualityChangeCount();
            isQualityChangeTimerRunning = false;
        }
    };

    protected CountDownTimer rebufferingTimeout = new CountDownTimer(Util.REBUFFERING_TIMEOUT, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "rebufferingTimeout finish");
            setErrorCode(AnalyticsErrorCodes.ANALYTICS_BUFFERING_TIMEOUT_REACHED.getErrorCode());
            transitionState(PlayerState.ERROR, analytics.getPosition());
            disableRebufferHeartbeat();
            resetStateMachine();
        }

    };

}


