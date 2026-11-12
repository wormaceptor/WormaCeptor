package com.azikar24.wormaceptor.mcp.server.middleware

import com.azikar24.wormaceptor.mcp.server.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.response.respond

/**
 * Answers only TCP peers whose source port is in [allowedPorts] (the adb socket proxy's connections), so other
 * apps on the device can't use the loopback port.
 */
internal fun proxyOnlyGuard(allowedPorts: () -> Set<Int>) = createApplicationPlugin("WormaCeptorProxyOnlyGuard") {
    onCall { call ->
        if (call.request.local.remotePort !in allowedPorts()) {
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(error = "Request not allowed"))
        }
    }
}
