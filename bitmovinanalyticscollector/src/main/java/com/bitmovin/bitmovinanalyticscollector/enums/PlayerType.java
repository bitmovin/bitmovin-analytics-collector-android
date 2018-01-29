package com.bitmovin.bitmovinanalyticscollector.enums;

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
