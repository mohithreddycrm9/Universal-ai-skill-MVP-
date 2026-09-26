package com.skillmcp.mentor.sync

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncEnvelope(
    val type: String,
    val deviceId: String,
    val cipher: String,
)
