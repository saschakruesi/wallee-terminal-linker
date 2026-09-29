package com.wallee.terminallinker.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.wallee.terminallinker.TerminalLinkerApp
import com.wallee.terminallinker.core.auth.Credentials
import com.wallee.terminallinker.di.AppContainer
import com.wallee.terminallinker.feature.spaces.SpaceRef
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

/**
 * In-process wallee stand-in for the UI tests: a MockWebServer on localhost plus a fresh [AppContainer]
 * pointed at it. Every test starts from wiped local data.
 */
class MockWallee {
    val server = MockWebServer()
    val app: TerminalLinkerApp = ApplicationProvider.getApplicationContext()
    lateinit var container: AppContainer
        private set

    /** Path prefix → response; later entries win. Unmatched paths yield 404. */
    private val routes = mutableListOf<Pair<(RecordedRequest) -> Boolean, (RecordedRequest) -> MockResponse>>()

    fun start(): MockWallee {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                routes.lastOrNull { (matches, _) -> matches(request) }?.second?.invoke(request)
                    ?: MockResponse().setResponseCode(404).setBody("""{"message":"no route for ${request.path}"}""")
        }
        server.start()
        container = AppContainer(app, walleeBaseUrl = server.url("/").toString().trimEnd('/'))
        app.container = container
        runBlocking { container.wipe() }
        return this
    }

    fun stop() {
        server.shutdown()
        runBlocking { container.wipe() }
    }

    fun route(method: String, pathPrefix: String, response: (RecordedRequest) -> MockResponse) {
        routes += Pair(
            { request: RecordedRequest ->
                request.method.equals(method, ignoreCase = true) &&
                    (request.path ?: "").startsWith("/api/v2.0$pathPrefix")
            },
            response,
        )
    }

    fun json(body: String, status: Int = 200): MockResponse = MockResponse().setResponseCode(status).setBody(body)

    /** Credentials in memory only plus one active manual space, as after a completed setup. */
    fun signIn(spaceId: Long = 67890, spaceName: String = "Test Space") {
        container.credentialStore.save(Credentials(12345, TEST_KEY), persist = false)
        runBlocking {
            container.spaceRepository.addManual(SpaceRef(spaceId, spaceName, manual = true))
            container.spaceRepository.setActive(spaceId)
        }
    }

    fun asset(name: String): String =
        InstrumentationRegistry.getInstrumentation().context.assets.open(name).bufferedReader().use { it.readText() }

    companion object {
        const val TEST_KEY = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA="

        fun terminal(id: Long, name: String, serial: String? = null, version: Int = 1): String =
            """{"id":$id,"name":"$name","identifier":"WT-$id","state":"ACTIVE","version":$version""" +
                (serial?.let { ""","deviceSerialNumber":"$it","deviceName":"PAX A77"""" } ?: "") +
                ""","type":{"id":5,"name":{"de":"PAX A77","en":"PAX A77"}}}"""
    }
}
