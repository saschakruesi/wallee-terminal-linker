package com.wallee.terminallinker.feature.spaces

import com.wallee.terminallinker.core.api.IatUnit
import com.wallee.terminallinker.core.api.WalleeApiException
import com.wallee.terminallinker.core.api.WalleeClient
import com.wallee.terminallinker.core.auth.Credentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class FakeStorage : SpaceStorage {
    override val activeSpaceId = MutableStateFlow<Long?>(null)
    override val recentSpaceIds = MutableStateFlow<List<Long>>(emptyList())
    override val discoveredSpaces = MutableStateFlow<List<SpaceRef>>(emptyList())
    override val manualSpaces = MutableStateFlow<List<SpaceRef>>(emptyList())
    override val spaceMode = MutableStateFlow(SpaceMode.AUTO)
    override val discoveryTruncated = MutableStateFlow(false)
    override val hiddenSpaceIds = MutableStateFlow<Set<Long>>(emptySet())

    override suspend fun setActiveSpaceId(id: Long?) {
        activeSpaceId.value = id
        if (id != null) recentSpaceIds.value = (listOf(id) + recentSpaceIds.value).distinct().take(3)
    }

    override suspend fun setDiscoveredSpaces(spaces: List<SpaceRef>) {
        discoveredSpaces.value = spaces
    }

    override suspend fun setManualSpaces(spaces: List<SpaceRef>) {
        manualSpaces.value = spaces
    }

    override suspend fun setSpaceMode(mode: SpaceMode) {
        spaceMode.value = mode
    }

    override suspend fun setDiscoveryTruncated(truncated: Boolean) {
        discoveryTruncated.value = truncated
    }

    override suspend fun setHiddenSpaceIds(ids: Set<Long>) {
        hiddenSpaceIds.value = ids
    }
}

class SpaceRepositoryTest {
    private val server = MockWebServer()
    private val storage = FakeStorage()
    private val iatChanges = mutableListOf<IatUnit>()
    private lateinit var client: WalleeClient
    private lateinit var repo: SpaceRepository

    @BeforeEach
    fun setUp() {
        server.start()
        client =
            WalleeClient(credentialsProvider = {
                Credentials(1, "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=")
            }, baseUrl = server.url("/"))
        repo = SpaceRepository(client, storage) { iatChanges += it }
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun page(vararg spaces: String, hasMore: Boolean) = MockResponse().setResponseCode(
        200,
    ).setBody("""{"data":[${spaces.joinToString(",")}],"hasMore":$hasMore,"limit":100}""")

    private fun space(id: Long, name: String, state: String = "ACTIVE") =
        """{"id":$id,"name":"$name","state":"$state"}"""

    @Test
    fun `discover makes a single request and stores sorted spaces`() = runBlocking {
        server.enqueue(page(space(20, "Zoo"), space(10, "Bar"), space(30, "Alpha", "INACTIVE"), hasMore = true))
        val result = repo.discover()
        assertTrue(result is DiscoveryResult.Found)
        assertEquals("/api/v2.0/spaces?limit=10", server.takeRequest().path)
        assertEquals(1, server.requestCount)
        assertTrue((result as DiscoveryResult.Found).hasMore)
        assertTrue(storage.discoveryTruncated.value)
        assertEquals(listOf("Alpha", "Bar", "Zoo"), repo.spaces.first().map { it.name })
        assertEquals(SpaceMode.AUTO, storage.spaceMode.value)
        assertEquals(listOf(false, true, true), repo.spaces.first().map { it.active })
    }

    @Test
    fun `empty list switches to manual mode`() = runBlocking {
        server.enqueue(page(hasMore = false))
        val result = repo.discover()
        assertEquals(DiscoveryResult.None(forbidden = false), result)
        assertEquals(SpaceMode.MANUAL, storage.spaceMode.value)
    }

    @Test
    fun `403 switches to manual mode without throwing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"forbidden"}"""))
        assertEquals(DiscoveryResult.None(forbidden = true), repo.discover())
        assertEquals(SpaceMode.MANUAL, storage.spaceMode.value)
    }

    @Test
    fun `401 is retried once with millis and the unit is remembered on success`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        server.enqueue(page(space(1, "Hotel"), hasMore = false))
        val result = repo.discover()
        assertTrue(result is DiscoveryResult.Found)
        assertEquals(IatUnit.MILLIS, client.iatUnit)
        assertEquals(listOf(IatUnit.MILLIS), iatChanges)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `401 twice surfaces the error and resets to seconds`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        val e = assertThrows(WalleeApiException::class.java) { runBlocking { repo.discover() } }
        assertEquals(401, e.status)
        assertEquals(IatUnit.SECONDS, client.iatUnit)
        assertTrue(iatChanges.isEmpty())
    }

    @Test
    fun `manual spaces merge with discovered ones and chooseActive prefers the requested id`() = runBlocking {
        storage.setDiscoveredSpaces(listOf(SpaceRef(1, "Hotel"), SpaceRef(2, "Inactive", active = false)))
        server.enqueue(MockResponse().setResponseCode(200).setBody(space(3, "Manual")))
        val verified = repo.verify(3)
        assertEquals("/api/v2.0/spaces/3", server.takeRequest().path)
        repo.addManual(verified)
        assertEquals(listOf(1L, 2L, 3L), repo.spaces.first().map { it.id })
        assertEquals(3L, repo.chooseActive(preferredId = 3)?.id)
        assertEquals(3L, repo.chooseActive(preferredId = 2)?.id) // inactive preferred → keeps previous active
        repo.removeManual(3)
        assertEquals(1L, repo.chooseActive()?.id)
        assertEquals(listOf(1L, 3L), storage.recentSpaceIds.value)
    }

    @Test
    fun `remove hides a discovered space and clears it as active, restore brings it back`() = runBlocking {
        storage.setDiscoveredSpaces(listOf(SpaceRef(1, "Hotel"), SpaceRef(2, "Bar")))
        repo.setActive(1)
        repo.remove(1)
        assertEquals(listOf(2L), repo.spaces.first().map { it.id })
        assertEquals(listOf(1L), repo.hiddenSpaces.first().map { it.id })
        assertEquals(null, repo.activeSpace.first())
        repo.restore(1)
        assertEquals(listOf(2L, 1L), repo.spaces.first().map { it.id })
        assertTrue(repo.hiddenSpaces.first().isEmpty())
    }

    @Test
    fun `remove drops a manual space entirely and discover keeps hidden spaces hidden`() = runBlocking {
        storage.setManualSpaces(listOf(SpaceRef(3, "Manual", manual = true)))
        repo.remove(3)
        assertTrue(storage.manualSpaces.value.isEmpty())
        assertTrue(repo.hiddenSpaces.first().isEmpty())

        storage.setHiddenSpaceIds(setOf(1))
        server.enqueue(page(space(1, "Hotel"), space(2, "Bar"), hasMore = false))
        repo.discover()
        assertEquals(listOf(2L), repo.spaces.first().map { it.id })
        assertEquals(listOf(1L), repo.hiddenSpaces.first().map { it.id })
        // Hidden ids that no longer exist are dropped when a hidden space is restored.
        storage.setHiddenSpaceIds(setOf(1, 99))
        repo.restore(1)
        assertEquals(emptySet<Long>(), storage.hiddenSpaceIds.value)
    }
}
