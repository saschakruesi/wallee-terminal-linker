package com.wallee.terminallinker.feature.terminals

import com.wallee.terminallinker.core.api.WalleeApiException
import com.wallee.terminallinker.core.api.WalleeClient
import com.wallee.terminallinker.core.auth.Credentials
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TerminalRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repo: TerminalRepository

    @BeforeEach
    fun setUp() {
        server.start()
        val client = WalleeClient(
            credentialsProvider = { Credentials(1, "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=") },
            baseUrl = server.url("/"),
        )
        repo = TerminalRepository(client, clock = { 1_700_000_000_000 })
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun terminal(id: Long, name: String = "T$id", serial: String? = null, version: Int = 3) =
        """{"id":$id,"name":"$name","identifier":"WT-$id","state":"ACTIVE","version":$version""" +
            (serial?.let { ""","deviceSerialNumber":"$it"""" } ?: "") +
            ""","locationVersion":{"id":9,"location":{"id":8,"name":"Filiale"}},"type":{"id":5,"name":{"de":"PAX A77","en":"PAX A77"}}}"""

    private fun page(vararg items: String, hasMore: Boolean, offset: Int) = MockResponse().setResponseCode(
        200,
    ).setBody("""{"data":[${items.joinToString(",")}],"hasMore":$hasMore,"limit":100,"offset":$offset}""")

    @Test
    fun `refreshAll pages with offset, expands nested names and stores the list`() = runBlocking {
        server.enqueue(page(terminal(1), terminal(2, serial = "S2"), hasMore = true, offset = 0))
        server.enqueue(page(terminal(3), hasMore = false, offset = 2))
        repo.refreshAll(spaceId = 42)
        val first = server.takeRequest()
        assertEquals(
            "/api/v2.0/payment/terminals/search?limit=100&offset=0&expand=locationVersion.location&expand=configurationVersion.configuration",
            first.path,
        )
        assertEquals("42", first.getHeader("space"))
        assertEquals(
            "/api/v2.0/payment/terminals/search?limit=100&offset=2&expand=locationVersion.location&expand=configurationVersion.configuration",
            server.takeRequest().path,
        )
        val state = repo.state(42).value
        assertEquals(listOf(1L, 2L, 3L), state.terminals.map { it.id })
        assertEquals("Filiale", state.terminals[0].locationName)
        assertEquals("PAX A77", state.terminals[0].type?.localizedName("de"))
        assertTrue(state.terminals[1].linked)
        assertFalse(state.loading)
        assertFalse(state.capped)
        assertEquals(1_700_000_000_000, state.loadedAtMillis)
    }

    @Test
    fun `a failing refresh keeps the previous list and exposes the error`() = runBlocking {
        server.enqueue(page(terminal(1), hasMore = false, offset = 0))
        repo.refreshAll(7)
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"no"}"""))
        assertThrows(WalleeApiException::class.java) { runBlocking { repo.refreshAll(7) } }
        val state = repo.state(7).value
        assertEquals(1, state.terminals.size)
        assertNotNull(state.error)
        assertFalse(state.loading)
    }

    @Test
    fun `rename patches name and version and replaces the cached terminal`() = runBlocking {
        server.enqueue(page(terminal(1, name = "Old"), hasMore = false, offset = 0))
        repo.refreshAll(7)
        server.enqueue(MockResponse().setResponseCode(200).setBody(terminal(1, name = "New", version = 4)))
        val updated = repo.rename(7, repo.cached(7, 1)!!, "New")
        server.takeRequest() // the list load
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        assertEquals("""{"name":"New","version":3}""", patch.body.readUtf8())
        assertEquals("New", updated.name)
        assertEquals("New", repo.cached(7, 1)?.name)
        assertEquals(4, repo.cached(7, 1)?.version)
    }

    @Test
    fun `refreshDevice posts and replaces the cached entry, triggerConfiguration posts`() = runBlocking {
        server.enqueue(page(terminal(1), hasMore = false, offset = 0))
        repo.refreshAll(7)
        server.enqueue(MockResponse().setResponseCode(200).setBody(terminal(1, serial = "NEW")))
        server.enqueue(MockResponse().setResponseCode(200))
        val refreshed = repo.refreshDevice(7, 1)
        repo.triggerConfiguration(7, 1)
        server.takeRequest()
        assertEquals(
            "/api/v2.0/payment/terminals/1/refresh?expand=locationVersion.location&expand=configurationVersion.configuration",
            server.takeRequest().path,
        )
        assertEquals("/api/v2.0/payment/terminals/1/trigger-configuration", server.takeRequest().path)
        assertEquals("NEW", refreshed.deviceSerialNumber)
        assertTrue(repo.cached(7, 1)!!.linked)
    }

    @Test
    fun `unsupported expand falls back to plain requests once`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"message":"unknown expand"}"""))
        server.enqueue(page(terminal(1), hasMore = false, offset = 0))
        repo.refreshAll(7)
        server.takeRequest()
        assertEquals("/api/v2.0/payment/terminals/search?limit=100&offset=0", server.takeRequest().path)
        assertEquals(1, repo.state(7).value.terminals.size)
    }

    @Test
    fun `link and unlink post to the right paths with the space header`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))
        repo.link(7, 1, "2290012345")
        repo.unlink(7, 1)
        val link = server.takeRequest()
        assertEquals("POST", link.method)
        assertEquals("/api/v2.0/payment/terminals/1/link?serialNumber=2290012345", link.path)
        assertEquals("7", link.getHeader("space"))
        assertEquals("/api/v2.0/payment/terminals/1/unlink", server.takeRequest().path)
    }
}
