package com.bitmovin.analytics.adapters;

import com.bitmovin.analytics.data.DeviceInformationProvider;
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline;
import com.bitmovin.analytics.features.Feature;
import java.util.Collection;

public interface PlayerAdapter {
    Collection<Feature<?>> init();

    void release();

    void reset();

    void registerEventDataManipulators(EventDataManipulatorPipeline pipeline);

    long getPosition();

    Long getDRMDownloadTime();

    DeviceInformationProvider getDeviceInformationProvider();

    void clearValues();
}
