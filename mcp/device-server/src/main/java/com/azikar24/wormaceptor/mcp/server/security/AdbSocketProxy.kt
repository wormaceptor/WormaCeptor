package com.azikar24.wormaceptor.mcp.server.security

import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.Process
import android.util.Log
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Accepts connections on an abstract Unix socket and pipes them to the Ktor server on loopback.
 *
 * Loopback TCP is open to every app on the device, a Unix socket isn't: the kernel reports the peer's UID,
 * so only adbd (`shell`), `root` and this app get through, the same rule WebView DevTools uses. The Ktor
 * side then only answers TCP peers whose source port is one of [proxyPorts]: another app can't bind a
 * port this process holds.
 */
internal class AdbSocketProxy(
    private val socketName: String,
    private val targetPort: Int,
) : Closeable {
    private val serverSocket = LocalServerSocket(socketName)
    private val open = ConcurrentHashMap.newKeySet<Closeable>()
    private val ports = ConcurrentHashMap.newKeySet<Int>()

    @Volatile
    private var closed = false

    /** Local ports of the proxy's loopback connections to Ktor. */
    val proxyPorts: Set<Int> get() = ports

    fun start() {
        thread(name = "wormaceptor-mcp-accept", isDaemon = true) { acceptLoop() }
    }

    private fun acceptLoop() {
        while (!closed) {
            val client = try {
                serverSocket.accept()
            } catch (e: IOException) {
                if (!closed) Log.w(TAG, "MCP socket accept failed", e)
                return
            }
            if (closed) client.close() else admit(client)
        }
    }

    private fun admit(client: LocalSocket) {
        val uid = try {
            client.peerCredentials.uid
        } catch (e: IOException) {
            Log.w(TAG, "Couldn't read MCP socket peer", e)
            null
        }
        if (uid != null && uid in allowedUids()) {
            thread(name = "wormaceptor-mcp-conn", isDaemon = true) { pipe(client) }
        } else {
            if (uid != null) Log.w(TAG, "Rejected MCP connection from uid $uid")
            closeQuietly(client)
        }
    }

    private fun pipe(client: LocalSocket) {
        val upstream = try {
            Socket(LOOPBACK, targetPort)
        } catch (e: IOException) {
            Log.w(TAG, "MCP server not reachable on port $targetPort", e)
            client.close()
            return
        }
        val port = upstream.localPort
        ports.add(port)
        open.add(client)
        open.add(upstream)
        val closeBoth = {
            // Drop the port first: once the socket closes, the port is free for anyone to bind.
            ports.remove(port)
            closeQuietly(client)
            closeQuietly(upstream)
            open.remove(client)
            open.remove(upstream)
        }
        thread(name = "wormaceptor-mcp-up", isDaemon = true) {
            copy(client.inputStream, upstream.getOutputStream())
            closeBoth()
        }
        copy(upstream.getInputStream(), client.outputStream)
        closeBoth()
    }

    override fun close() {
        if (closed) return
        closed = true
        // LocalServerSocket.close() doesn't wake a blocked accept(); a dummy connection does.
        try {
            LocalSocket().use { it.connect(LocalSocketAddress(socketName)) }
        } catch (e: IOException) {
            Log.d(TAG, "MCP socket wake-up connect failed", e)
        }
        closeQuietly(Closeable(serverSocket::close))
        open.toList().forEach(::closeQuietly)
        open.clear()
        ports.clear()
    }

    private fun copy(
        input: InputStream,
        output: OutputStream,
    ) {
        val buffer = ByteArray(BUFFER_SIZE)
        try {
            while (true) {
                val read = input.read(buffer)
                if (read < 0) return
                output.write(buffer, 0, read)
                output.flush()
            }
        } catch (_: IOException) {
            // Either side hung up; the caller closes both.
            return
        }
    }

    private fun closeQuietly(closeable: Closeable) {
        try {
            closeable.close()
        } catch (e: IOException) {
            Log.d(TAG, "Close failed", e)
        }
    }

    private companion object {
        const val TAG = "AdbSocketProxy"
        const val BUFFER_SIZE = 16 * 1024

        // Ktor binds 127.0.0.1; InetAddress.getLoopbackAddress() is ::1 on Android
        const val LOOPBACK = "127.0.0.1"

        // android.os.Process.ROOT_UID / SHELL_UID are hidden; these values are fixed in the platform.
        const val ROOT_UID = 0
        const val SHELL_UID = 2000

        fun allowedUids() = setOf(ROOT_UID, SHELL_UID, Process.myUid())
    }
}
