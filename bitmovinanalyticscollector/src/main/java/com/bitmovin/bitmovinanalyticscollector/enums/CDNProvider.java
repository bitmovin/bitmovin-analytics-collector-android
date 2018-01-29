package com.bitmovin.bitmovinanalyticscollector.enums;

public enum CDNProvider {
    BITMOVIN("bitmovin"),
    AKAMAI("akamai"),
    FASTLY("fastly"),
    MAXCDN("maxcdn"),
    CLOUDFRONT("cloudfront"),
    CHINACACHE("chinacache"),
    BITGRAVITY("bitgravity");

    private final String name;

    CDNProvider(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }
}
