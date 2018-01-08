package com.bitmovin.bitmovinanalyticscollector.enums;

/**
 * Created by zachmanc on 12/15/17.
 */

public enum PlayerType {
    BITMOVIN("bitmovin"),
    EXOPLAYER("exoplayer");

    private final String name;

    PlayerType(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }
}
