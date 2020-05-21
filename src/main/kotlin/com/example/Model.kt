package com.example

import com.fasterxml.jackson.databind.JsonNode
import java.time.OffsetDateTime

data class GoogleAuthorization(val token: String) {
    override fun toString() : String {
        return "Bearer $token"
    }
}

data class EventsPage(
    val updated: OffsetDateTime,
    val accessRole: String,
    val nextSyncToken: String?,
    val nextPageToken: String?,
    val items: Set<JsonNode>)