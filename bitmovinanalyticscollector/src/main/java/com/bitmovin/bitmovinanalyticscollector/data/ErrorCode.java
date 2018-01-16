package com.bitmovin.bitmovinanalyticscollector.data;

/**
 * Created by zachmanc on 1/16/18.
 */

public enum ErrorCode {
    LICENSE_ERROR(1016, "A license error has occurred"),
    LICENSE_ERROR_INVALID_DOMAIN(1017, "License error invalid domain"),
    LICENSE_ERROR_INVALID_SERVER_URL(1018, "License error invalid server url"),

    UNKNOWN_ERROR(3000, "License error invalid server url"),
    MANIFEST_HTTP_FAILURE(3006, "License request failed with HTTP status: " ),
    DRM_REQUEST_HTTP_STATUS(3011, "License error invalid server url"),
    DRM_REQUEST_ERROR(3019, "License error invalid server url"),
    DRM_UNSUPPORTED(3021, "License error invalid server url"),

    DRM_SESSION_ERROR(4000, "License error invalid server url");



    private final int errorCode;
    private String description;

    ErrorCode(int errorCode, String description) {
        this.errorCode = errorCode;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return errorCode + ": " + description;
    }
}
