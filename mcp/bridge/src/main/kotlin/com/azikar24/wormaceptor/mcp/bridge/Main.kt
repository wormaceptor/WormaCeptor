package com.azikar24.wormaceptor.mcp.bridge

import com.azikar24.wormaceptor.mcp.bridge.adb.AdbClient
import com.azikar24.wormaceptor.mcp.bridge.adb.DeviceDiscovery
import com.azikar24.wormaceptor.mcp.bridge.adb.resolveAdbPath
import com.azikar24.wormaceptor.mcp.bridge.config.BridgeConfig
import com.azikar24.wormaceptor.mcp.bridge.device.DeviceConnection
import com.azikar24.wormaceptor.mcp.bridge.mcp.McpProtocol
import com.azikar24.wormaceptor.mcp.bridge.mcp.McpServer
import com.azikar24.wormaceptor.mcp.bridge.mcp.tools.ToolRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val config = BridgeConfig.fromArgs(args)

    System.err.println("WormaCeptor MCP Bridge v${McpProtocol.SERVER_VERSION}")
    if (config.verbose) {
        System.err.println("Config: port=${config.port}, device=${config.deviceSerial ?: "auto"}")
    }

    runBlocking {
        val adbPath = resolveAdbPath(config.adbPath)
        if (config.verbose) System.err.println("adb: $adbPath")
        val adbClient = AdbClient(adbPath)

        val device = try {
            DeviceDiscovery(adbClient).findDevice(config.deviceSerial)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            System.err.println("Error: ${e.message}")
            kotlin.system.exitProcess(1)
        }
        System.err.println("Found device: ${device.serial} (${device.model ?: "unknown"})")

        // Pin the discovered serial: adb diagnostics and logcat need it even when --device wasn't given.
        val connection = DeviceConnection(config.copy(deviceSerial = device.serial), adbClient)
        if (!connection.forward()) {
            System.err.println("Error: Failed to set up ADB port forwarding")
            kotlin.system.exitProcess(1)
        }
        System.err.println(
            "Port forwarded: localhost:${config.port} -> ${connection.packageName ?: "device:${config.port}"}",
        )
        // Connect in the background: MCP clients time out the initialize handshake (Claude Code: 30 s), and
        // a frozen or not-yet-started app can keep connect() busy longer than that. Tool calls reconnect on demand.
        launch(Dispatchers.IO) {
            try {
                connection.connect()
                System.err.println("Connected to WormaCeptor server")
            } catch (e: IllegalStateException) {
                System.err.println("Warning: ${e.message}")
            }
        }

        Runtime.getRuntime().addShutdownHook(
            Thread {
                System.err.println("Shutting down...")
                connection.close()
            },
        )

        System.err.println("MCP server ready. Listening for requests on stdin...")
        val mcpServer = McpServer(connection, ToolRegistry.allTools(), config.verbose)
        mcpServer.run()
    }
}
