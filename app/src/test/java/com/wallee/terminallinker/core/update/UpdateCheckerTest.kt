package com.wallee.terminallinker.core.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class FakeUpdateStorage : UpdateStorage {
    override val lastUpdateCheckMillis = MutableStateFlow<Long?>(null)
    override val availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    override val dismissedUpdateVersion = MutableStateFlow<String?>(null)

    override suspend fun setLastUpdateCheck(millis: Long, update: UpdateInfo?) {
        lastUpdateCheckMillis.value = millis
        availableUpdate.value = update
    }

    override suspend fun setDismissedUpdateVersion(version: String?) {
        dismissedUpdateVersion.value = version
    }
}

class UpdateCheckerTest {
    private val server = MockWebServer()
    private val storage = FakeUpdateStorage()
    private var now = 1_700_000_000_000L
    private lateinit var checker: UpdateChecker

    @BeforeEach
    fun setUp() {
        server.start()
        checker = UpdateChecker(
            storage = storage,
            currentVersion = "1.0.0-debug",
            latestReleaseUrl = server.url("/repos/x/y/releases/latest"),
            // No transparent OkHttp retry, so a dropped connection is a network error in the test.
            httpClient = OkHttpClient.Builder().retryOnConnectionFailure(false).build(),
            clock = { now },
        )
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun release(tag: String) = MockResponse().setResponseCode(200)
        .setBody("""{"tag_name":"$tag","html_url":"https://github.com/x/y/releases/tag/$tag","draft":false}""")

    @Test
    fun `reports a newer release, stores it and asks at most once a day`() = runBlocking {
        server.enqueue(release("v1.1.0"))
        assertEquals(UpdateInfo("1.1.0", "https://github.com/x/y/releases/tag/v1.1.0"), checker.check())
        assertEquals("application/vnd.github+json", server.takeRequest().getHeader("Accept"))
        assertEquals(now, storage.lastUpdateCheckMillis.value)

        now += 60 * 60 * 1000
        assertEquals("1.1.0", checker.check()?.version)
        assertEquals(1, server.requestCount)

        now += 24 * 60 * 60 * 1000
        server.enqueue(release("v1.0.0"))
        assertNull(checker.check())
        assertEquals(2, server.requestCount)
        assertNull(storage.availableUpdate.value)
    }

    @Test
    fun `force ignores the interval, garbled and failed responses keep the last result`() = runBlocking {
        server.enqueue(release("v1.2.0"))
        checker.check()
        server.enqueue(release("v1.3.0"))
        assertEquals("1.3.0", checker.check(force = true)?.version)

        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))
        assertEquals("1.3.0", checker.check(force = true)?.version)

        server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))
        assertNull(checker.check(force = true))
        assertNull(storage.availableUpdate.value)

        server.shutdown()
        assertNull(checker.check(force = true))
    }
}
