# ProGuard rules that are required for the Bitmovin Analytics SDKs to work properly

# Make sure that all DTOs that are relevant for the analytics backend are not minified/obfuscated
-keep class com.bitmovin.analytics.dtos.** { *; }