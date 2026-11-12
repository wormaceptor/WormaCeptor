package com.azikar24.wormaceptor.mcp.protocol

/**
 * Abstract Unix socket the device server listens on. The bridge reaches it with
 * `adb forward tcp:<port> localabstract:<name>`.
 */
internal fun mcpSocketName(packageName: String): String = "wormaceptor_${packageName}_mcp"
