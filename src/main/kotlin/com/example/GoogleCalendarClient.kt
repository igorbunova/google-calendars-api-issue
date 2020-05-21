package com.example

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange

const val GOOGLE_CLIENT_ERROR_OCCURRED = "google.client.error.occurred"
class GoogleClientException(messageId: String, val httpCode: Int, val responseBody: String?=null)
    : RuntimeException(messageId)

class GoogleCalendarClient(private val client: WebClient) {

    suspend fun calendarEvents(calendarId: String, auth: GoogleAuthorization, pageSize: Int=2500): Flow<EventsPage> = flow {
        val eventsPage = firstEventsPage(calendarId, auth, pageSize)
        emit(eventsPage)
        var token = eventsPage.nextPageToken
        while (token != null) {
            val nextPage = nextEventsPage(calendarId, auth, pageSize, token)
            emit(nextPage)
            token = nextPage.nextPageToken
        }
    }

    suspend fun primaryCalendarEvents(auth: GoogleAuthorization, pageSize: Int = 2500) =
        calendarEvents("primary", auth, pageSize)

    private suspend fun firstEventsPage(calendarId: String,
                                        auth: GoogleAuthorization,
                                        pageSize: Int) : EventsPage {
        val response = client.get()
            .uri { it.path("/calendar/v3/calendars/{0}/events")
                .queryParam("maxResults", pageSize)
                .build(calendarId) }
            .header(HttpHeaders.AUTHORIZATION, auth.toString())
            .awaitExchange()
        return awaitBody(response)
    }

    private suspend fun nextEventsPage(calendarId: String,
                                       auth: GoogleAuthorization,
                                       pageSize: Int,
                                       nextPageToken: String) : EventsPage {
        val response = client.get()
            .uri { it.path("/calendar/v3/calendars/{0}/events")
                .queryParam("maxResults", pageSize)
                .queryParam("pageToken", nextPageToken)
                .build(calendarId) }
            .header(HttpHeaders.AUTHORIZATION, auth.toString())
            .awaitExchange()
        return awaitBody(response)
    }

    private suspend inline fun <reified T: Any> awaitBody(response: ClientResponse) : T {
        checkResponse(response)
        val entity = response.awaitEntity<T>()
        if (!entity.hasBody()) {
            throw GoogleClientException(GOOGLE_CLIENT_ERROR_OCCURRED, 412, "Unexpected response format")
        }
        return entity.body!!
    }

    private suspend fun checkResponse(response: ClientResponse) {
        if (response.statusCode().isError) {
            val entity = response.awaitEntity<String>()
            throw GoogleClientException(GOOGLE_CLIENT_ERROR_OCCURRED, response.rawStatusCode(), entity.body)
        }
    }
}