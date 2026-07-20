package com.kitsugi.animelist.core.update

data class AppRelease(
    val versionCode: Int,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSizeBytes: Long = 0L
)
