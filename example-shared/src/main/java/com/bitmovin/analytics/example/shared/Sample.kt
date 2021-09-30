package com.bitmovin.analytics.example.shared

import android.net.Uri

data class Sample(val name: String, val uri: Uri, val drmScheme: String? = null, val drmLicenseUri: Uri? = null) {
    constructor(name: String, uri: String, drmScheme: String? = null, drmLicenseUri: String? = null) :
            this(name, Uri.parse(uri), drmScheme, drmLicenseUri?.let { Uri.parse(it) })
}
