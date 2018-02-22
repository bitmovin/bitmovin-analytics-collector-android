package com.bitmovin.analytics.adapters;

import com.bitmovin.analytics.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
import com.bitmovin.player.BitmovinPlayer;

public class BitmovinSdkAdapter implements PlayerAdapter {
    private static final String TAG = "ExoPlayerAdapter";
    private final BitmovinAnalyticsConfig config;
    private final BitmovinPlayer bitmovinPlayer;
    private PlayerStateMachine stateMachine;

    public BitmovinSdkAdapter(BitmovinPlayer bitmovinPlayer, BitmovinAnalyticsConfig config, PlayerStateMachine stateMachine) {
        this.config = config;
        this.stateMachine = stateMachine;
        this.bitmovinPlayer = bitmovinPlayer;
    }
        @Override
    public EventData createEventData() {
        return null;
    }

    @Override
    public void release() {

    }
}
