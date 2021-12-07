package com.bitmovin.analytics.adapters;

import com.bitmovin.analytics.config.SourceMetadata;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline;
import com.bitmovin.analytics.features.Feature;
import com.bitmovin.analytics.license.FeatureConfigContainer;
import java.util.Collection;

public interface PlayerAdapter {
    Collection<Feature<FeatureConfigContainer, ?>> init();

    void release();

    void resetSourceRelatedState();

    void registerEventDataManipulators(EventDataManipulatorPipeline pipeline);

    long getPosition();

    Long getDRMDownloadTime();

    void clearValues();

    SourceMetadata getCurrentSourceMetadata();

    EventData createEventData();

    default AdAdapter createAdAdapter() {
        return null;
    }
}
