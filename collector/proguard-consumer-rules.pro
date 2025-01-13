# ProGuard rules that are required for the Bitmovin Analytics SDKs to work properly

# Make sure that all DTOs are not minified/obfuscated
-keep class com.bitmovin.analytics.data.** { *; }
-keep class com.bitmovin.analytics.features.** { *; }
-keep class com.bitmovin.analytics.license.** { *; }