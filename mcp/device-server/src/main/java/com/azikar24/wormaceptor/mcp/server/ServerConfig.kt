package com.azikar24.wormaceptor.mcp.server

import com.azikar24.wormaceptor.domain.entities.McpConfig

internal data class ServerConfig(
    val port: Int = DEFAULT_PORT,
    val enableAuth: Boolean = false,
    val authToken: String? = null,
    val maxBodySize: Long = DEFAULT_MAX_BODY_SIZE,
    val enabledCategories: Set<ApiCategory> = ApiCategory.entries.toSet(),
    val redactSecrets: Boolean = true,
    val allowTcp: Boolean = false,
) {
    companion object {
        const val DEFAULT_PORT = 8999
        const val DEFAULT_MAX_BODY_SIZE = 1_048_576L

        fun from(config: McpConfig) = ServerConfig(
            port = config.port,
            enableAuth = config.enableAuth,
            authToken = config.authToken,
            maxBodySize = config.maxBodySize,
            redactSecrets = config.redactSecrets,
            allowTcp = config.allowTcp,
        )
    }
}

internal enum class ApiCategory {
    NETWORK,
    DIAGNOSTICS,
    PERFORMANCE,
    STORAGE,
    ACTIONS,
    STREAMING,
}
