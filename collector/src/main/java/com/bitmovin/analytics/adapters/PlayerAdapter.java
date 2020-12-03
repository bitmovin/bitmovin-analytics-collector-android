package com.bitmovin.analytics.adapters;

import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline;
import com.bitmovin.analytics.data.DRMInformation;
import com.bitmovin.analytics.data.DeviceInformationProvider;

import org.jetbrains.annotations.Nullable;


public interface PlayerAdapter {

    void init();

    void release();

    void registerEventDataManipulators(EventDataManipulatorPipeline pipeline);

    long getPosition();

    @Nullable
    DRMInformation getDRMInformation();

    DeviceInformationProvider getDeviceInformationProvider();

    void clearValues();
}
