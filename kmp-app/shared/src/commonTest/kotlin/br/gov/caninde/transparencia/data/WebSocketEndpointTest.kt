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
        assertEquals("wss://example.com?token=abc", e.url)
    }

    @Test
    fun healthCheckUrlProducao() {
        val e = WebSocketEndpoint("wss", "transparencia-caninde.onrender.com", 443)
        assertEquals("https://transparencia-caninde.onrender.com/health", e.healthCheckUrl)
    }

    @Test
    fun mediaProxyUrlEncodaQuery() {
        val e = WebSocketEndpoint("wss", "transparencia-caninde.onrender.com", 443)
        val proxied = e.mediaProxyUrl("https://www.cmcaninde.ce.gov.br/foto.jpg") { "ENC" }
        assertEquals(
            "https://transparencia-caninde.onrender.com/media?url=ENC",
            proxied,
        )
    }
}
