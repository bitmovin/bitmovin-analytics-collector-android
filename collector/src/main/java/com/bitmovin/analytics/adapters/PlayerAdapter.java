package com.bitmovin.analytics.adapters;

import com.bitmovin.analytics.config.SourceMetadata;
import com.bitmovin.analytics.data.DeviceInformationProvider;
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline;
import com.bitmovin.analytics.features.Feature;
import java.util.Collection;

public interface PlayerAdapter {
    Collection<Feature<?>> init();

    void release();

    void resetSourceRelatedState();

    void registerEventDataManipulators(EventDataManipulatorPipeline pipeline);

    long getPosition();

    Long getDRMDownloadTime();

    DeviceInformationProvider getDeviceInformationProvider();

    void clearValues();

    SourceMetadata getCurrentSourceMetadata();

    void updateCurrentSourceMetadata(SourceMetadata sourceMetadata);
}
