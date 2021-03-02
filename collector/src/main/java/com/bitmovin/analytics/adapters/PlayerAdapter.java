package com.bitmovin.analytics.adapters;

import com.bitmovin.analytics.data.DRMInformation;
import com.bitmovin.analytics.data.DeviceInformationProvider;
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline;
import com.bitmovin.analytics.features.Feature;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;

public interface PlayerAdapter {
    Collection<Feature<?>> init();

    void release();

    void registerEventDataManipulators(EventDataManipulatorPipeline pipeline);

    long getPosition();

    @Nullable
    DRMInformation getDRMInformation();

    DeviceInformationProvider getDeviceInformationProvider();

    void clearValues();
}
