package com.azikar24.wormaceptor.mcp.server

import com.azikar24.wormaceptor.core.engine.McpHolder
import com.azikar24.wormaceptor.core.engine.McpServerHandle
import com.azikar24.wormaceptor.mcp.server.middleware.AuthPlugin
import com.azikar24.wormaceptor.mcp.server.middleware.LocalRequestGuard
import com.azikar24.wormaceptor.mcp.server.routes.actionRoutes
import com.azikar24.wormaceptor.mcp.server.routes.crashRoutes
import com.azikar24.wormaceptor.mcp.server.routes.diagnosticRoutes
import com.azikar24.wormaceptor.mcp.server.routes.healthRoutes
import com.azikar24.wormaceptor.mcp.server.routes.inspectionRoutes
import com.azikar24.wormaceptor.mcp.server.routes.logRoutes
import com.azikar24.wormaceptor.mcp.server.routes.mockRoutes
import com.azikar24.wormaceptor.mcp.server.routes.monitoringRoutes
import com.azikar24.wormaceptor.mcp.server.routes.networkRoutes
import com.azikar24.wormaceptor.mcp.server.routes.performanceRoutes
import com.azikar24.wormaceptor.mcp.server.routes.replayRoutes
import com.azikar24.wormaceptor.mcp.server.routes.storageRoutes
import com.azikar24.wormaceptor.mcp.server.routes.timelineRoutes
import com.azikar24.wormaceptor.mcp.server.routes.transactionRoutes
import com.azikar24.wormaceptor.mcp.server.routes.waitRoutes
import com.azikar24.wormaceptor.mcp.server.serialization.JsonConfig
import com.azikar24.wormaceptor.mcp.server.streaming.EngineCollector
import com.azikar24.wormaceptor.mcp.server.streaming.EventStreamManager
import com.azikar24.wormaceptor.mcp.server.streaming.streamRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

@kotlinx.serialization.Serializable
internal data class ErrorResponse(
    val success: Boolean = false,
    val error: String,
)

/**
 * Embedded Ktor server. Reads its [ServerConfig] on every [start], so a restart picks up
 * `WormaCeptorApi.configureMcpServer` changes.
 */
internal class WormaCeptorServer(
    private val packageName: String? = null,
    private val configProvider: () -> ServerConfig = { ServerConfig.from(McpHolder.config) },
) : McpServerHandle {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private var collectorScope: CoroutineScope? = null
    private val _isRunning = MutableStateFlow(false)

    override fun isRunning(): Boolean = _isRunning.value

    @Synchronized
    override fun start() {
        if (_isRunning.value) return
        val config = configProvider()
        if (config.enableAuth && config.authToken.isNullOrBlank()) {
            android.util.Log.e(TAG, "MCP auth is enabled without a token; refusing to start the server")
            return
        }
        val eventManager = EventStreamManager()
        try {
            server = embeddedServer(CIO, port = config.port, host = LOCALHOST) {
                configureServer(config, eventManager)
            }.start(wait = false)
            _isRunning.value = true
            if (ApiCategory.STREAMING in config.enabledCategories) {
                val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
                collectorScope = scope
                EngineCollector(eventManager, scope).startCollecting()
            }
            android.util.Log.i(TAG, "WormaCeptor MCP server started on port ${config.port}")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to start MCP server", e)
        }
    }

    @Synchronized
    override fun stop() {
        collectorScope?.cancel()
        collectorScope = null
        try {
            server?.stop(GRACE_PERIOD_MS, TIMEOUT_MS)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Error stopping server", e)
        }
        server = null
        _isRunning.value = false
    }

    private fun Application.configureServer(
        config: ServerConfig,
        eventManager: EventStreamManager,
    ) {
        install(ContentNegotiation) {
            json(JsonConfig.instance)
        }

        install(WebSockets) {
            pingPeriodMillis = WEBSOCKET_PING_SECONDS * 1000
            timeoutMillis = WEBSOCKET_TIMEOUT_SECONDS * 1000
            maxFrameSize = Long.MAX_VALUE
        }

        install(StatusPages) {
            exception<Throwable> { call, cause ->
                android.util.Log.w(TAG, "Unhandled route error", cause)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(error = cause.message ?: "Internal error"),
                )
            }
        }

        // No CORS: the only client is the MCP bridge over adb, never a browser.
        install(LocalRequestGuard)

        if (config.enableAuth) {
            install(AuthPlugin) {
                enabled = true
                token = config.authToken
            }
        }

        routing {
            healthRoutes(packageName)

            if (ApiCategory.NETWORK in config.enabledCategories) {
                transactionRoutes(maxBodySize = config.maxBodySize)
                networkRoutes()
                mockRoutes()
                replayRoutes()
            }

            if (ApiCategory.DIAGNOSTICS in config.enabledCategories) {
                crashRoutes()
                logRoutes()
                diagnosticRoutes()
                inspectionRoutes()
                timelineRoutes()
                waitRoutes()
            }

            if (ApiCategory.PERFORMANCE in config.enabledCategories) {
                performanceRoutes()
                monitoringRoutes()
            }

            if (ApiCategory.STORAGE in config.enabledCategories) {
                storageRoutes(redactSecrets = config.redactSecrets)
            }

            if (ApiCategory.ACTIONS in config.enabledCategories) {
                actionRoutes()
            }

            if (ApiCategory.STREAMING in config.enabledCategories) {
                streamRoutes(eventManager)
            }
        }
    }

    companion object {
        private const val TAG = "WormaCeptorServer"
        private const val LOCALHOST = "127.0.0.1"
        private const val GRACE_PERIOD_MS = 1000L
        private const val TIMEOUT_MS = 2000L
        private const val WEBSOCKET_PING_SECONDS = 15L
        private const val WEBSOCKET_TIMEOUT_SECONDS = 15L
    }
}
