package com.azikar24.wormaceptor.mcp.bridge.config

import com.azikar24.wormaceptor.mcp.bridge.mcp.McpProtocol

internal data class BridgeConfig(
    val port: Int = 8999,
    val deviceSerial: String? = null,
    /** The `--adb` flag; null means search for adb. */
    val adbPath: String? = null,
    val verbose: Boolean = false,
    val authToken: String? = null,
    /** The `--package` flag: the app under debug, when adb finds several apps with the MCP server. */
    val packageName: String? = null,
) {
    companion object {
        fun fromArgs(args: Array<String>): BridgeConfig {
            var port = 8999
            var device: String? = null
            var adbPath: String? = null
            var verbose = false
            var authToken: String? = null
            var packageName: String? = null

            val iter = args.iterator()
            while (iter.hasNext()) {
                when (val arg = iter.next()) {
                    "--port" -> port = iter.next().toInt()
                    "--device", "-s" -> device = iter.next()
                    "--adb" -> adbPath = iter.next()
                    "--verbose", "-v" -> verbose = true
                    "--token" -> authToken = iter.next()
                    "--package" -> packageName = iter.next()
                    "--version" -> {
                        System.err.println("wormaceptor-bridge ${McpProtocol.SERVER_VERSION}")
                        kotlin.system.exitProcess(0)
                    }
                    "--help", "-h" -> {
                        printHelp()
                        kotlin.system.exitProcess(0)
                    }
                    else -> {
                        System.err.println("Unknown argument: $arg")
                        printHelp()
                        kotlin.system.exitProcess(1)
                    }
                }
            }
            return BridgeConfig(port, device, adbPath, verbose, authToken, packageName)
        }

        private fun printHelp() {
            System.err.println(
                """
                |Usage: wormaceptor-bridge [options]
                |
                |Options:
                |  --port <port>     Local port to forward (default: 8999)
                |  --device, -s <id> Target device serial (required if multiple devices)
                |  --adb <path>      Path to adb (default: searches ANDROID_HOME, ANDROID_SDK_ROOT,
                |                    PATH, then the default Android SDK dir)
                |  --token <token>   Bearer token for authentication
                |  --package <id>    App under debug (default: the only app with the MCP server)
                |  --verbose, -v     Enable verbose logging
                |  --version         Print version and exit
                |  --help, -h        Print this help message
                """.trimMargin(),
            )
        }
    }
}
