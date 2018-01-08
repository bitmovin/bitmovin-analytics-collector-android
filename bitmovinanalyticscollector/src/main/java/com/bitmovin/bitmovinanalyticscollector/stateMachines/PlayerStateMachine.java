package com.bitmovin.bitmovinanalyticscollector.stateMachines;

import android.os.Handler;

import com.bitmovin.bitmovinanalyticscollector.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.bitmovinanalyticscollector.utils.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zachmanc on 12/15/17.
 */

public class PlayerStateMachine {
    private static final String TAG = "PlayerStateMachine";
    private List<StateMachineListener> listeners = new ArrayList<StateMachineListener>();
    private PlayerState currentState;
    private long initialTimestamp = 0;
    private long firstReadyTimestamp = 0;
    private long onEnterStateTimeStamp= 0;
    private long seekTimeStamp = 0;
    private String impressionId;
    private final BitmovinAnalyticsConfig config;

    private Handler heartbeatHandler = new Handler();
    private int heartbeatDelay = 59700; //milliseconds

    public PlayerStateMachine(BitmovinAnalyticsConfig config){
        this.config = config;
        resetStateMachine();
    }

    void enableHeartbeat(){
        heartbeatHandler.postDelayed(new Runnable(){
            public void run(){
                long currentTimestamp = Util.getTimeStamp();
                long enterTimestamp = getOnEnterStateTimeStamp();

                for (StateMachineListener listener : getListeners()){
                    listener.onHeartbeat(currentTimestamp - onEnterStateTimeStamp);
                }
                onEnterStateTimeStamp = currentTimestamp;
                heartbeatHandler.postDelayed(this, heartbeatDelay);
            }
        }, heartbeatDelay);
    }

    void disableHeartbeat(){
        heartbeatHandler.removeCallbacksAndMessages(null);
    }

    private void setCurrentState(final PlayerState newPlayerState){
        this.currentState = newPlayerState;
    }

    public void resetStateMachine(){
        disableHeartbeat();
        this.impressionId = Util.getUUID();
        this.initialTimestamp = Util.getTimeStamp();
        setCurrentState(PlayerState.SETUP);
    }

    public synchronized void transitionState(PlayerState destinationPlayerState){
        long timeStamp = Util.getTimeStamp();
        currentState.onExitState(this,timeStamp,destinationPlayerState);
        this.onEnterStateTimeStamp = timeStamp;
        destinationPlayerState.onEnterState(this);
        setCurrentState(destinationPlayerState);
    }

    public long getFirstReadyTimestamp() {
        return firstReadyTimestamp;
    }

    public void setFirstReadyTimestamp(long firstReadyTimestamp) {
        this.firstReadyTimestamp = firstReadyTimestamp;
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

    public void addListener(StateMachineListener toAdd) {
        listeners.add(toAdd);
    }

    public void removeListener(StateMachineListener toRemove){
        listeners.remove(toRemove);
    }

    public PlayerState getCurrentState() { return currentState; }

    List<StateMachineListener> getListeners() {
        return listeners;
    }

    public long getStartupTime(){
        return firstReadyTimestamp-initialTimestamp;
    }

    public String getImpressionId() {
        return impressionId;
    }

}


