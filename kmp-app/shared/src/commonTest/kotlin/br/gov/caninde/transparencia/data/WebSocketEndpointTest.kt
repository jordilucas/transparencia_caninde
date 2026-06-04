package br.gov.caninde.transparencia.data

import kotlin.test.Test
import kotlin.test.assertEquals

class WebSocketEndpointTest {

    @Test
    fun urlSemToken() {
        val e = WebSocketEndpoint("ws", "10.0.2.2", 8080)
        assertEquals("ws://10.0.2.2:8080", e.url)
    }

    @Test
    fun urlComToken() {
        val e = WebSocketEndpoint("wss", "example.com", 443, authToken = "abc")
        assertEquals("wss://example.com:443?token=abc", e.url)
    }
}
