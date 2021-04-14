package com.bitmovin.analytics.bitmovin.player;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import com.bitmovin.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.bitmovin.player.features.BitmovinFeatureFactory;
import com.bitmovin.analytics.config.SourceMetadata;
import com.bitmovin.analytics.data.DeviceInformationProvider;
import com.bitmovin.analytics.features.FeatureFactory;
import com.bitmovin.player.api.Player;
import com.bitmovin.player.api.source.Source;
import java.util.HashMap;
import java.util.Map;

public class BitmovinPlayerCollector extends BitmovinAnalytics {
    private HashMap<Source, SourceMetadata> sourceMetadataMap = new HashMap<>();

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     */
    public BitmovinPlayerCollector(
            BitmovinAnalyticsConfig bitmovinAnalyticsConfig, Context context) {
        super(bitmovinAnalyticsConfig, context);
    }

    @Deprecated
    public BitmovinPlayerCollector(BitmovinAnalyticsConfig bitmovinAnalyticsConfig) {
        this(bitmovinAnalyticsConfig, bitmovinAnalyticsConfig.getContext());
    }

    public void attachPlayer(Player player) {
        DeviceInformationProvider deviceInformationProvider =
                new DeviceInformationProvider(context, getUserAgent(context));
        FeatureFactory featureFactory = new BitmovinFeatureFactory(this, player, context);
        BitmovinSdkAdapter adapter =
                new BitmovinSdkAdapter(
                        player,
                        this.bitmovinAnalyticsConfig,
                        deviceInformationProvider,
                        this.playerStateMachine,
                        featureFactory,
                        (Map<Source, SourceMetadata>) sourceMetadataMap);

        this.attach(adapter);

        if (this.adAnalytics != null) {
            BitmovinSdkAdAdapter adAdapter = new BitmovinSdkAdAdapter(player, this.adAnalytics);
            this.attachAd(adAdapter);
        }
    }

    public void addSourceMetadata(Source playerSource, SourceMetadata sourceMetadata) {
        sourceMetadataMap.put(playerSource, sourceMetadata);
    }

    private String getUserAgent(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        String applicationName = "Unknown";
        if (stringId == 0 && applicationInfo.nonLocalizedLabel != null) {
            applicationInfo.nonLocalizedLabel.toString();
        }
        String versionName;
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException var5) {
            versionName = "?";
        }

        return applicationName
                + "/"
                + versionName
                + " (Linux;Android "
                + Build.VERSION.RELEASE
                + ") "
                + "BitmovinPlayer/"
                + BitmovinUtil.getPlayerVersion();
    }
}
