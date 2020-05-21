package com.example

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient

internal class GoogleCalendarClientTest {
    private val objectMapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
    private val internal = WebClient.builder()
        .baseUrl("https://www.googleapis.com")
        .codecs {
            it.defaultCodecs().jackson2JsonDecoder(
                Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON, MediaType.APPLICATION_STREAM_JSON)
            )
            it.defaultCodecs().jackson2JsonEncoder(
                Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON, MediaType.APPLICATION_STREAM_JSON)
            )
            it.defaultCodecs().maxInMemorySize(8 * 1024 * 1024)
        }
        .build()

    private val auth = GoogleAuthorization(System.getenv("GOOGLE_AUTH_TOKEN")?: System.getProperty("google.auth.token"))
    private val client = GoogleCalendarClient(internal)

    @Test
    fun primaryCalendarEvents() = runBlocking {

//        val reccurringCount = client.primaryCalendarEvents(auth, 200)
//                .flatMapConcat { it.items.asFlow() }
//                .filter { it.has("recurrence") }
//                .count()
//        println(reccurringCount)
        val count = client.primaryCalendarEvents(auth)
                .map { it.items.size }
                .reduce{ i1, i2 -> i1 + i2}
        assertTrue(count > 0)

        val splitPagesCount = client.primaryCalendarEvents(auth, 83)
                .map { it.items.size }
                .reduce{ i1, i2 -> i1 + i2}
        assertEquals(count, splitPagesCount)
    }
}