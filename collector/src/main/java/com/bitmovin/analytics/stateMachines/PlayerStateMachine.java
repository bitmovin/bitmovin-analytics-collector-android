package com.bitmovin.analytics.stateMachines;

import android.os.Handler;
import android.util.Log;

import com.bitmovin.analytics.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.utils.Util;

import java.util.ArrayList;
import java.util.List;

public class PlayerStateMachine {
    private static final String TAG = "PlayerStateMachine";
    private final BitmovinAnalyticsConfig config;
    private List<StateMachineListener> listeners = new ArrayList<StateMachineListener>();
    private PlayerState currentState;
    private long initialTimestamp = 0;
    private long firstReadyTimestamp = 0;
    private long onEnterStateTimeStamp = 0;
    private long seekTimeStamp = 0;
    private long videoTimeStart;
    private long videoTimeEnd;
    private ErrorCode errorCode;
    private String impressionId;
    private Handler heartbeatHandler = new Handler();
    private int heartbeatDelay = 59700; // default to 60 seconds

    public PlayerStateMachine(BitmovinAnalyticsConfig config) {
        this.config = config;
        this.heartbeatDelay = this.config.getHeartbeatInterval();
        resetStateMachine();
    }

    public void enableHeartbeat() {
        heartbeatHandler.postDelayed(new Runnable() {
            public void run() {
                long currentTimestamp = Util.getTimeStamp();
                long enterTimestamp = getOnEnterStateTimeStamp();

                for (StateMachineListener listener : getListeners()) {
                    listener.onHeartbeat(currentTimestamp - onEnterStateTimeStamp);
                }
                onEnterStateTimeStamp = currentTimestamp;
                heartbeatHandler.postDelayed(this, heartbeatDelay);
            }
        }, heartbeatDelay);
    }

    public void disableHeartbeat() {
        heartbeatHandler.removeCallbacksAndMessages(null);
    }

    public void resetStateMachine() {
        disableHeartbeat();
        this.impressionId = Util.getUUID();
        this.initialTimestamp = Util.getTimeStamp();
        this.firstReadyTimestamp = 0;
        setCurrentState(PlayerState.SETUP);
    }

    public synchronized void transitionState(PlayerState destinationPlayerState, long videoTime) {
        long timeStamp = Util.getTimeStamp();
        videoTimeEnd = videoTime;

        Log.d(TAG, "Transitioning from " + currentState.toString() + " to " + destinationPlayerState.toString());

        currentState.onExitState(this, timeStamp, destinationPlayerState);
        this.onEnterStateTimeStamp = timeStamp;
        videoTimeStart = videoTimeEnd;
        destinationPlayerState.onEnterState(this);
        setCurrentState(destinationPlayerState);
    }

    public long getFirstReadyTimestamp() {
        return firstReadyTimestamp;
    }

    public void setFirstReadyTimestamp(long firstReadyTimestamp) {
        this.firstReadyTimestamp = firstReadyTimestamp;
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
        return firstReadyTimestamp - initialTimestamp;
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

    public long getOnEnterStateTimeStamp() {
        return onEnterStateTimeStamp;
    }

    public long getSeekTimeStamp() {
        return seekTimeStamp;
    }

    public void setSeekTimeStamp(long seekTimeStamp) {
        this.seekTimeStamp = seekTimeStamp;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

}


