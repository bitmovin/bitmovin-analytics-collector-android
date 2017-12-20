package com.bitmovin.bitmovinanalyticscollector.stateMachines;

import android.util.Log;

import com.bitmovin.bitmovinanalyticscollector.utils.BitmovinAnalyticsConfig;
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
    private final String impressionId;
    private final String userId;
    private final BitmovinAnalyticsConfig config;

    public PlayerStateMachine(BitmovinAnalyticsConfig config){
        this.userId = Util.getUUID();
        this.impressionId = Util.getUUID();
        this.initialTimestamp = Util.getTimeStamp();
        this.config = config;
        setCurrentState(PlayerState.SETUP);
        this.onEnterStateTimeStamp = Util.getTimeStamp();
    }

    private void setCurrentState(final PlayerState newPlayerState){
        this.currentState = newPlayerState;
    }

    public synchronized void transitionState(PlayerState destinationPlayerState){
        currentState.onExitState(this);
        long timeStamp = Util.getTimeStamp();
        Log.d(TAG,String.format("Transitioning from %s to %s - Duration: %d", currentState, destinationPlayerState,timeStamp - this.onEnterStateTimeStamp));
        this.onEnterStateTimeStamp = timeStamp;
        destinationPlayerState.onEnterState(this);
        setCurrentState(destinationPlayerState);
    }

    public long getFirstReadyTimestamp() {
        return firstReadyTimestamp;
    }

    public void setFirstReadyTimestamp(long firstReadyTimestamp) {
        this.firstReadyTimestamp = firstReadyTimestamp;
        Log.d(TAG,String.format("Startup Time %d",firstReadyTimestamp-initialTimestamp));
    }

    public long getOnEnterStateTimeStamp() {
        return onEnterStateTimeStamp;
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




}


