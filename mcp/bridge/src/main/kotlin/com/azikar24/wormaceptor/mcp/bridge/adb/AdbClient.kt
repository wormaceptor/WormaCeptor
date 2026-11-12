package com.azikar24.wormaceptor.mcp.bridge.adb

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

internal data class CommandResult(
    val exitCode: Int,
    val output: String,
    val error: String,
)

internal data class DeviceInfo(
    val serial: String,
    val state: String,
    val model: String?,
    val product: String?,
)

/** Parses `adb devices -l` output. */
internal fun parseDevices(output: String): List<DeviceInfo> = output.lines()
    .drop(1)
    .filter { it.isNotBlank() && !it.startsWith("*") }
    .mapNotNull { line ->
        val parts = line.trim().split("\\s+".toRegex())
        if (parts.size < 2) return@mapNotNull null
        val model = parts.find { it.startsWith("model:") }?.removePrefix("model:")
        val product = parts.find { it.startsWith("product:") }?.removePrefix("product:")
        DeviceInfo(parts[0], parts[1], model, product)
    }

private const val McpProviderClass = "com.azikar24.wormaceptor.mcp.server.WormaCeptorServerInitializer"
private val McpProviderLine = Regex("""^\s+([\w.]+)/""" + Regex.escape(McpProviderClass) + ":")

/** Parses `dumpsys package providers` for apps that include the MCP device server. */
internal fun parseMcpPackages(output: String): List<String> =
    output.lineSequence().mapNotNull { McpProviderLine.find(it)?.groupValues?.get(1) }.distinct().toList()

internal class AdbClient(private val adbPath: String = "adb") {

    fun listDevices(): List<DeviceInfo> {
        val result = runCommand(listOf(adbPath, "devices", "-l"))
        if (result.exitCode != 0) return emptyList()
        return parseDevices(result.output)
    }

    /** `adb forward tcp:<localPort> <remote>`, where [remote] is e.g. `tcp:8999` or `localabstract:<name>`. */
    fun forward(
        serial: String?,
        localPort: Int,
        remote: String,
    ): Boolean = run(serial, "forward", "tcp:$localPort", remote).exitCode == 0

    fun removeForward(localPort: Int): Boolean {
        return runCommand(listOf(adbPath, "forward", "--remove", "tcp:$localPort")).exitCode == 0
    }

    /** Runs `adb [-s serial] <args>`. */
    fun run(
        serial: String?,
        vararg args: String,
    ): CommandResult = runCommand(listOfNotNull(adbPath, serial?.let { "-s" }, serial) + args)

    /** The app process's whole logcat buffer in threadtime format; null when it isn't running or adb failed. */
    /** Packages installed on the device that register the MCP server's ContentProvider. */
    fun mcpPackages(serial: String?): List<String> {
        val result = run(serial, "shell", "dumpsys", "package", "providers")
        return if (result.exitCode == 0) parseMcpPackages(result.output) else emptyList()
    }

    fun appLogcat(
        serial: String?,
        packageName: String,
    ): String? {
        val pid = parsePid(run(serial, "shell", "pidof", packageName).output) ?: return null
        val result = run(serial, "logcat", "-d", "-v", "threadtime", "--pid=$pid")
        return result.output.takeIf { result.exitCode == 0 }
    }

    /** Null when adb itself failed, so callers don't mistake a broken adb for a dead app. */
    fun processState(
        serial: String?,
        packageName: String,
    ): AppProcessState? {
        val pidof = run(serial, "shell", "pidof", packageName)
        // pidof exits 1 with empty output when nothing matches; anything on stderr is an adb problem.
        if (pidof.exitCode != 0 && pidof.error.isNotBlank()) return null
        val pid = parsePid(pidof.output) ?: return AppProcessState.NotRunning
        val dumpsys = run(serial, "shell", "dumpsys", "activity", "processes", packageName)
        if (dumpsys.exitCode != 0) return null
        return appProcessState(pid, isMainProcessFrozen(dumpsys.output, packageName))
    }

    private fun runCommand(
        args: List<String>,
        timeoutSeconds: Long = COMMAND_TIMEOUT_SECONDS,
    ): CommandResult {
        var process: Process? = null
        return try {
            process = ProcessBuilder(args)
                .redirectErrorStream(false)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val error = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

            if (!completed) {
                CommandResult(-1, "", "Command timed out")
            } else {
                CommandResult(process.exitValue(), output, error)
            }
        } catch (e: Exception) {
            CommandResult(-1, "", e.message ?: "Unknown error")
        } finally {
            process?.destroyForcibly()
        }
    }

    companion object {
        internal const val COMMAND_TIMEOUT_SECONDS = 10L
    }
}
