package com.bitmovin.analytics.data;

public enum ErrorCode {
    LICENSE_ERROR(1016, "A license error has occurred"),
    LICENSE_ERROR_INVALID_DOMAIN(1017, "License error invalid domain"),
    LICENSE_ERROR_INVALID_SERVER_URL(1018, "License error invalid server url"),
    DRM_REQUEST_HTTP_STATUS(3011, "DRM Request failed with HTTP Status: "),
    DRM_REQUEST_ERROR(3019, "DRM Request Error"),
    DRM_UNSUPPORTED(3021, "DRM Unsupported"),
    DRM_SESSION_ERROR(4000, "DRM Session Error"),
    UNKNOWN_ERROR(3000, "Unknown Error"),
    DATASOURCE_HTTP_FAILURE(3006, "Data Source request failed with HTTP status: "),
    DATASOURCE_INVALID_CONTENT_TYPE(1000001, "Invalid content type: "),
    DATASOURCE_UNABLE_TO_CONNECT(1000002, "Unable to connect: "),
    EXOPLAYER_RENDERER_ERROR(1000003, "ExoPlayer Renderer Error");

    private final int errorCode;
    private String description;

    private ErrorCode(int errorCode, String description) {
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
