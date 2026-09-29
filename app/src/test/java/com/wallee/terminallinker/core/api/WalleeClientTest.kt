package com.wallee.terminallinker.core.api

import com.wallee.terminallinker.core.api.dto.ListResponse
import com.wallee.terminallinker.core.api.dto.Space
import com.wallee.terminallinker.core.auth.Credentials
import java.util.Base64
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WalleeClientTest {
    private val server = MockWebServer()
    private val key = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA="
    private lateinit var client: WalleeClient

    @BeforeEach
    fun setUp() {
        server.start()
        client = WalleeClient(
            credentialsProvider = { Credentials(12345, key) },
            baseUrl = server.url("/"),
            clock = { 1_700_000_000_000 },
        )
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `signs exactly the sent path and query, repeats expand, sets space header`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":[],"hasMore":false}"""))
        client.get<ListResponse<Space>>(
            "/payment/terminals/search",
            query = listOf("limit" to "100", "query" to "name:~\"kasse 1\""),
            expand = listOf("locationVersion.location", "type"),
            spaceId = 67890,
        )
        val recorded = server.takeRequest()
        // OkHttp percent-encodes '~' in queries; what is sent is what is signed.
        val expectedPath = "/api/v2.0/payment/terminals/search?limit=100&query=name%3A%7E%22kasse%201%22" +
            "&expand=locationVersion.location&expand=type"
        assertEquals(expectedPath, recorded.path)
        assertEquals("67890", recorded.getHeader("space"))
        assertEquals("application/json", recorded.getHeader("Accept"))
        val token = recorded.getHeader("Authorization")!!.removePrefix("Bearer ")
        val payload = Json.parseToJsonElement(String(Base64.getUrlDecoder().decode(token.split('.')[1]))).jsonObject
        assertEquals(expectedPath, payload["requestPath"]!!.jsonPrimitive.content)
        assertEquals("GET", payload["requestMethod"]!!.jsonPrimitive.content)
        assertEquals("12345", payload["sub"]!!.jsonPrimitive.content)
        assertEquals("1700000000", payload["iat"]!!.jsonPrimitive.content)
    }

    @Test
    fun `serial number is url encoded in path and signed identically`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        client.postNoContent("/payment/terminals/42/link", query = listOf("serialNumber" to "SN 12+3/4"), spaceId = 1)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v2.0/payment/terminals/42/link?serialNumber=SN%2012%2B3%2F4", recorded.path)
        assertNull(recorded.getHeader("Content-Type"))
    }

    @Test
    fun `decodes list responses and ignores unknown fields`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":[{"id":1,"name":"Hotel","state":"ACTIVE","primaryCurrency":"CHF","account":{"id":9,"name":"Acc","state":"ACTIVE"},"unknown":{"x":1}}],"hasMore":true,"limit":100}""",
            ),
        )
        val page: ListResponse<Space> = client.get("/spaces", query = listOf("limit" to "100"))
        assertEquals(1, page.data.size)
        assertEquals("Hotel", page.data[0].name)
        assertTrue(page.data[0].isActive)
        assertEquals("Acc", page.data[0].account?.name)
        assertTrue(page.hasMore)
    }

    @Test
    fun `maps error bodies to WalleeApiException`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(422).setBody(
                """{"code":"TERMINAL_DEVICE_UNKNOWN","message":"The serial number is not known.","id":"abc","errors":{"serialNumber":"unknown"}}""",
            ),
        )
        val e = assertThrows(WalleeApiException::class.java) {
            runBlocking {
                client.postNoContent("/payment/terminals/42/link", query = listOf("serialNumber" to "X"), spaceId = 1)
            }
        }
        assertEquals(422, e.status)
        assertEquals("TERMINAL_DEVICE_UNKNOWN", e.code)
        assertEquals("The serial number is not known.", e.message)
        assertEquals("unknown", e.errors["serialNumber"])
    }

    @Test
    fun `non-json error bodies still yield the status`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
        val e = assertThrows(WalleeApiException::class.java) { runBlocking { client.get<Space>("/spaces/1") } }
        assertEquals(401, e.status)
        assertTrue(e.isUnauthorized)
        assertEquals("HTTP 401", e.message)
    }

    @Test
    fun `transport failures become WalleeNetworkException`() {
        server.shutdown()
        assertThrows(WalleeNetworkException::class.java) { runBlocking { client.get<Space>("/spaces/1") } }
    }

    @Test
    fun `missing credentials fail before any request`() {
        val noCreds = WalleeClient(credentialsProvider = { null }, baseUrl = server.url("/"))
        assertThrows(MissingCredentialsException::class.java) { runBlocking { noCreds.get<Space>("/spaces/1") } }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `429 on GET is retried with backoff and succeeds`() = runBlocking {
        val delays = mutableListOf<Long>()
        val retrying = WalleeClient(
            credentialsProvider = { Credentials(12345, key) },
            baseUrl = server.url("/"),
            retryDelaysMillis = listOf(2_000, 4_000, 8_000),
            sleep = { delays += it },
        )
        repeat(2) { server.enqueue(MockResponse().setResponseCode(429).setBody("""{"message":"slow down"}""")) }
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":[],"hasMore":false}"""))
        retrying.get<ListResponse<Space>>("/spaces")
        assertEquals(3, server.requestCount)
        assertEquals(listOf(2_000L, 4_000L), delays)
    }

    @Test
    fun `429 gives up after the last backoff and is never retried for POST`() = runBlocking {
        val delays = mutableListOf<Long>()
        val retrying = WalleeClient(
            credentialsProvider = { Credentials(12345, key) },
            baseUrl = server.url("/"),
            retryDelaysMillis = listOf(2_000, 4_000, 8_000),
            sleep = { delays += it },
        )
        repeat(4) { server.enqueue(MockResponse().setResponseCode(429)) }
        val e = assertThrows(WalleeApiException::class.java) {
            runBlocking { retrying.get<ListResponse<Space>>("/spaces") }
        }
        assertEquals(429, e.status)
        assertEquals(4, server.requestCount)
        assertEquals(listOf(2_000L, 4_000L, 8_000L), delays)

        server.enqueue(MockResponse().setResponseCode(429))
        assertThrows(WalleeApiException::class.java) {
            runBlocking { retrying.postNoContent("/payment/terminals/1/link", listOf("serialNumber" to "X")) }
        }
        assertEquals(5, server.requestCount)
        assertEquals(3, delays.size)
    }
}
