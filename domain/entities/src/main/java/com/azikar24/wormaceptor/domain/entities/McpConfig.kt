package com.azikar24.wormaceptor.domain.entities

/**
 * Settings for the debug-only MCP device server.
 *
 * @property port Port the server binds on localhost when [allowTcp] is true
 * @property enableAuth Whether requests must carry a bearer token
 * @property authToken Bearer token required when [enableAuth] is true
 * @property maxBodySize Maximum transaction body size, in bytes, returned by the server
 * @property enabled Whether the server starts automatically
 * @property redactSecrets Whether preference values and file contents under secret-looking keys
 *   (token, password, api_key, auth, session, ...) are replaced with `[REDACTED]`
 * @property allowTcp Whether the server also answers plain TCP on `127.0.0.1:`[port]. Off by default: every app on
 *   the device can reach loopback, so the server only listens on an abstract Unix socket that admits adb (`shell`),
 *   `root` and the app itself. Turn on only for tooling that runs `adb forward tcp:8999 tcp:8999` itself
 */
data class McpConfig(
    val port: Int = DEFAULT_PORT,
    val enableAuth: Boolean = false,
    val authToken: String? = null,
    val maxBodySize: Long = DEFAULT_MAX_BODY_SIZE,
    val enabled: Boolean = true,
    val redactSecrets: Boolean = true,
    val allowTcp: Boolean = false,
) {
    /** Default values for [McpConfig]. */
    companion object {
        /** Default server port. */
        const val DEFAULT_PORT = 8999

        /** Default maximum body size: 1 MiB. */
        const val DEFAULT_MAX_BODY_SIZE = 1_048_576L
    }
}
