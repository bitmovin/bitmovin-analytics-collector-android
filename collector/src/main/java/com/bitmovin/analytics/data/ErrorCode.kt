package com.bitmovin.analytics.data

enum class ErrorCode(val errorCode: Int, var description: String, var errorData: ErrorData? = null) {
    LICENSE_ERROR(1016, "A license error has occurred"),
    LICENSE_ERROR_INVALID_DOMAIN(1017, "License error invalid domain"),
    LICENSE_ERROR_INVALID_SERVER_URL(1018, "License error invalid server url"),
    SOURCE_ERROR(1020, "Source Error"),

    UNKNOWN_ERROR(3000, "Unknown Error"),
    DRM_REQUEST_HTTP_STATUS(3011, "DRM Request failed with HTTP Status: "),
    DRM_REQUEST_ERROR(3019, "DRM Request Error"),
    DRM_UNSUPPORTED(3021, "DRM Unsupported"),

    DRM_SESSION_ERROR(4000, "DRM Session Error"),
    FILE_ACCESS(4001, "File Access Error"),
    LOCKED_FOLDER(4002, "Locked Folder Error"),
    DEAD_LOCK(4003, "Dead Lock Error"),
    DRM_KEY_EXPIRED(4004, "DRM Key Expired Error"),
    PLAYER_SETUP_ERROR(4005, "Player Setup Error"),

    DATASOURCE_ERROR(1000000, "Datasource Error"),
    DATASOURCE_HTTP_FAILURE(3006, "Data Source request failed"),
    DATASOURCE_INVALID_CONTENT_TYPE(1000001, "Invalid content type"),
    DATASOURCE_UNABLE_TO_CONNECT(1000002, "Unable to connect"),
    EXOPLAYER_RENDERER_ERROR(1000003, "ExoPlayer Renderer Error"),
    BEHIND_LIVE_WINDOW(1000004, "Behind Live Window Error"),
    REMOTE_ERROR(1000005, "Remote Component Error"),
    OUT_OF_MEMORY_ERROR(1000006, "Out of memory Error"),
    UNEXPECTED_ERROR(1000007, "Unexpected Errors");

    @Override
    override fun toString(): String {
        return errorCode.toString() + ": " + description
    }
}
