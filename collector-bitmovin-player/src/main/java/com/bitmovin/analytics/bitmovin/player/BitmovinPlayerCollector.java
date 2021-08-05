package com.bitmovin.analytics.bitmovin.player;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import com.bitmovin.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.Collector;
import com.bitmovin.analytics.DefaultCollector;
import com.bitmovin.analytics.adapters.AdAdapter;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.bitmovin.player.features.BitmovinFeatureFactory;
import com.bitmovin.analytics.config.SourceMetadata;
import com.bitmovin.analytics.data.DeviceInformationProvider;
import com.bitmovin.analytics.features.FeatureFactory;
import com.bitmovin.player.api.Player;
import com.bitmovin.player.api.source.Source;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;

public class BitmovinPlayerCollector extends DefaultCollector<Player> implements Collector<Player> {
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

    @NotNull
    @Override
    protected PlayerAdapter createAdapter(
            Player player,
            @NotNull BitmovinAnalytics analytics,
            @NotNull DeviceInformationProvider deviceInformationProvider) {
        FeatureFactory featureFactory = new BitmovinFeatureFactory(analytics, player);
        return new BitmovinSdkAdapter(
                player,
                analytics.getConfig(),
                deviceInformationProvider,
                analytics.getPlayerStateMachine(),
                featureFactory,
                sourceMetadataMap);
    }

    @NotNull
    @Override
    protected AdAdapter createAdAdapter(Player player) {
        return new BitmovinSdkAdAdapter(player);
    }

    public void addSourceMetadata(Source playerSource, SourceMetadata sourceMetadata) {
        sourceMetadataMap.put(playerSource, sourceMetadata);
    }

    @NotNull
    @Override
    protected String getUserAgent(Context context) {
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
