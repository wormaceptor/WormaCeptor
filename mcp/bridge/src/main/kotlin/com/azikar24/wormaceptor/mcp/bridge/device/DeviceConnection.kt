package com.azikar24.wormaceptor.mcp.bridge.device

import com.azikar24.wormaceptor.mcp.bridge.adb.AdbClient
import com.azikar24.wormaceptor.mcp.bridge.adb.isNoise
import com.azikar24.wormaceptor.mcp.bridge.adb.launchFailure
import com.azikar24.wormaceptor.mcp.bridge.adb.parseThreadtime
import com.azikar24.wormaceptor.mcp.bridge.config.BridgeConfig
import com.azikar24.wormaceptor.mcp.protocol.LogEntryDto
import com.azikar24.wormaceptor.mcp.protocol.mcpSocketName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal class DeviceConnection(
    private val config: BridgeConfig,
    private val adbClient: AdbClient,
) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    val apiClient = DeviceApiClient(
        baseUrl = "http://localhost:${config.port}",
        authToken = config.authToken,
    )

    /** The app's package: `--package`, then the health check, then [resolvePackage]'s adb lookup. */
    @Volatile
    var packageName: String? = config.packageName
        private set

    /** Host clock at the last clear_logs; one app's logcat can't be cleared, so older entries are hidden. */
    @Volatile
    private var logsClearedAtMs = 0L

    /**
     * The app under debug. Before any health check succeeds (e.g. the app was already frozen when the bridge
     * started) it asks adb which installed app registers the MCP server, and uses it when there is exactly one.
     */
    suspend fun resolvePackage(): String? = packageName ?: withContext(Dispatchers.IO) {
        adbClient.mcpPackages(config.deviceSerial).singleOrNull()
    }?.also { packageName = it }

    /**
     * Forward targets, best first: the app's adb-only socket, then plain TCP for apps on 3.4.3 or older and
     * apps that set `McpConfig(allowTcp = true)`.
     */
    private suspend fun forwardTargets(): List<String> = listOfNotNull(
        resolvePackage()?.let { "localabstract:${mcpSocketName(it)}" },
        "tcp:${config.port}",
    )

    /** Points the local port at the preferred target, before the app has answered anything. */
    suspend fun forward(): Boolean = withContext(Dispatchers.IO) {
        adbClient.forward(config.deviceSerial, config.port, forwardTargets().first())
    }

    /** Tries each forward target until the server answers through one. */
    private suspend fun forwardAndCheck(): Boolean = forwardTargets().any { target ->
        withContext(Dispatchers.IO) { adbClient.forward(config.deviceSerial, config.port, target) } && healthy()
    }

    private suspend fun healthy(): Boolean {
        val health = apiClient.healthCheck() ?: return false
        health.packageName?.let { packageName = it }
        return true
    }

    suspend fun connect() {
        _state.value = ConnectionState.Connecting

        var attempts = 0
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                if (forwardAndCheck()) {
                    _state.value = ConnectionState.Connected
                    return
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
            attempts++
            delay(HEALTH_CHECK_TIMEOUT_MS)
        }

        val errorMsg = "Could not connect to WormaCeptor server on port ${config.port}. " +
            "Ensure the app is running with mcp-device-server dependency" +
            if (packageName == null) ", and pass --package if several apps have it." else "."
        _state.value = ConnectionState.Error(errorMsg)
        throw IllegalStateException(errorMsg)
    }

    /** Re-creates the port forward and health-checks with exponential backoff. Returns whether it reconnected. */
    suspend fun reconnect(maxRetries: Int = RECONNECT_ATTEMPTS): Boolean {
        var retryDelay = INITIAL_RETRY_DELAY_MS
        for (attempt in 1..maxRetries) {
            _state.value = ConnectionState.Reconnecting(attempt, retryDelay)
            System.err.println("Reconnecting (attempt $attempt/$maxRetries)...")
            if (forwardAndCheck()) {
                _state.value = ConnectionState.Connected
                System.err.println("Reconnected to WormaCeptor server")
                return true
            }
            delay(retryDelay)
            retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
        }

        val errorMsg = "Reconnection failed after $maxRetries attempts"
        System.err.println(errorMsg)
        _state.value = ConnectionState.Error(errorMsg)
        return false
    }

    /** Why the app stopped answering, when adb can tell (frozen or not running); null otherwise. */
    suspend fun unreachableAppHint(): String? {
        val pkg = resolvePackage() ?: return null
        return withContext(Dispatchers.IO) { adbClient.processState(config.deviceSerial, pkg)?.hint }
    }

    fun markLogsCleared() {
        logsClearedAtMs = System.currentTimeMillis()
    }

    /**
     * The app's own logcat read through adb (full history, no in-app capture or permission prompt),
     * minus entries before the last [markLogsCleared]. Null when adb can't read it.
     */
    suspend fun readAppLogs(): List<LogEntryDto>? {
        val pkg = resolvePackage() ?: return null
        val output = withContext(Dispatchers.IO) { adbClient.appLogcat(config.deviceSerial, pkg) } ?: return null
        return parseThreadtime(output).filter { it.timestamp > logsClearedAtMs && !it.isNoise() }
    }

    /** Starts or resumes the app's launcher activity; returns an error message, or null on success. */
    suspend fun bringAppToFront(pkg: String): String? = withContext(Dispatchers.IO) {
        val result = adbClient.run(
            config.deviceSerial,
            "shell",
            "monkey",
            "-p",
            pkg,
            "-c",
            "android.intent.category.LAUNCHER",
            "1",
        )
        launchFailure(result.exitCode, result.output + result.error)
    }

    fun close() {
        apiClient.close()
        adbClient.removeForward(config.port)
        _state.value = ConnectionState.Disconnected
    }

    companion object {
        internal const val INITIAL_RETRY_DELAY_MS = 1_000L
        internal const val MAX_RETRY_DELAY_MS = 30_000L
        internal const val MAX_RETRY_ATTEMPTS = 20
        internal const val RECONNECT_ATTEMPTS = 4
        internal const val HEALTH_CHECK_TIMEOUT_MS = 500L
    }
}
