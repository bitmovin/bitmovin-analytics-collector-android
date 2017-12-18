package com.bitmovin.bitmovinanalyticscollector.enums;

/**
 * Created by zachmanc on 12/15/17.
 */

public enum CDNProvider {
    BITMOVIN ("bitmovin"),
    AKAMAI ("akamai"),
    FASTLY ("fastly"),
    MAXCDN ("maxcdn"),
    CLOUDFRONT ("cloudfront"),
    CHINACACHE ("chinacache"),
    BITGRAVITY ("bitgravity");

    private final String name;

    CDNProvider(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }
}
