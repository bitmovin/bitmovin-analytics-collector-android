package com.bitmovin.analytics.adapters;

import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline;
import com.bitmovin.analytics.data.DRMInformation;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.features.Feature;
import com.bitmovin.analytics.data.DeviceInformationProvider;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface PlayerAdapter {
    EventData createEventData();

    void init();

    void release();

    void registerEventDataManipulators(EventDataManipulatorPipeline pipeline);

    long getPosition();

    @Nullable
    DRMInformation getDRMInformation();

    DeviceInformationProvider getDeviceInformationProvider();

    void clearValues();

    List<Feature<?>> getFeatures();
}
