package com.azikar24.wormaceptor.api

import android.content.Context
import android.content.Intent
import java.io.InputStream
import java.util.UUID

/**
 * Data transfer object containing complete transaction details for IDE plugin communication.
 * Serialized as JSON when returned via the content provider's openFile() endpoint.
 *
 * @property id Unique transaction identifier (UUID as string)
 * @property method HTTP method (GET, POST, PUT, DELETE, etc.)
 * @property url Complete request URL including query parameters
 * @property host The host portion of the URL
 * @property path The path portion of the URL
 * @property code HTTP response status code, or null if request failed
 * @property duration Request duration in milliseconds, or null if incomplete
 * @property status Transaction status (REQUESTED, COMPLETED, FAILED)
 * @property timestamp Unix timestamp when the request was initiated
 * @property requestHeaders Map of request header names to their values
 * @property requestBody Request body content as string, or null if no body
 * @property requestSize Size of the request body in bytes
 * @property responseHeaders Map of response header names to their values
 * @property responseBody Response body content as string, or null if no body
 * @property responseSize Size of the response body in bytes
 * @property responseMessage HTTP status message (e.g., "OK", "Not Found")
 * @property protocol HTTP protocol version (e.g., "HTTP/1.1", "h2")
 * @property tlsVersion TLS version used (e.g., "TLSv1.3"), or null for HTTP
 * @property error Error message if the request failed, or null on success
 * @property contentType Response Content-Type header value
 * @property extensions Custom metadata from registered extension providers
 */
data class TransactionDetailDto(
    val id: String,
    val method: String,
    val url: String,
    val host: String,
    val path: String,
    val code: Int?,
    val duration: Long?,
    val status: String,
    val timestamp: Long,
    val requestHeaders: Map<String, List<String>>,
    val requestBody: String?,
    val requestSize: Long,
    val responseHeaders: Map<String, List<String>>,
    val responseBody: String?,
    val responseSize: Long,
    val responseMessage: String?,
    val protocol: String?,
    val tlsVersion: String?,
    val error: String?,
    val contentType: String?,
    val extensions: Map<String, String> = emptyMap(),
)

/**
 * Interface for WormaCeptor implementation modules.
 *
 * **Note:** This interface is for WormaCeptor implementation modules only (e.g., persistence, in-memory).
 * Host applications should not implement this interface directly. It is discovered at runtime via
 * reflection by [WormaCeptorApi].
 *
 * Implementation modules must provide a class named `ServiceProviderImpl` in the package
 * `com.azikar24.wormaceptor.api.internal` with a no-argument constructor.
 */
interface ServiceProvider {

    /**
     * Initializes the service provider with the application context.
     *
     * @param context Application context for accessing resources and services
     * @param logCrashes Whether to install an uncaught exception handler for crash logging
     * @param leakNotifications Whether to show notifications when memory leaks are detected
     */
    fun init(
        context: Context,
        logCrashes: Boolean,
        leakNotifications: Boolean = true,
    )

    /**
     * Same as the overload without [features], with the features the host enabled, so monitors whose
     * feature is off don't start. The default implementation ignores [features].
     *
     * @param context Application context for accessing resources and services
     * @param logCrashes Whether to install an uncaught exception handler for crash logging
     * @param leakNotifications Whether to show notifications when memory leaks are detected
     * @param features Features the host passed to [WormaCeptorApi.init]
     */
    fun init(
        context: Context,
        logCrashes: Boolean,
        leakNotifications: Boolean,
        features: Set<Feature>,
    ) = init(context, logCrashes, leakNotifications)

    /**
     * Records the start of an HTTP transaction (request phase).
     *
     * @param url The complete request URL
     * @param method The HTTP method (GET, POST, etc.)
     * @param headers Request headers as a map of name to values
     * @param bodyStream Input stream for reading the request body, or null if no body
     * @param bodySize Size of the request body in bytes
     * @return A unique transaction ID for correlating with the response, or null on failure
     */
    fun startTransaction(
        url: String,
        method: String,
        headers: Map<String, List<String>>,
        bodyStream: InputStream?,
        bodySize: Long,
    ): UUID?

    /**
     * Same as the overload without [startedAtMillis], recording the caller's own start time instead of
     * the time the provider stores the row. The default implementation ignores [startedAtMillis].
     *
     * @param url The complete request URL
     * @param method The HTTP method (GET, POST, etc.)
     * @param headers Request headers as a map of name to values
     * @param bodyStream Input stream for reading the request body, or null if no body
     * @param bodySize Size of the request body in bytes
     * @param startedAtMillis Epoch millis when the request started
     * @return A unique transaction ID for correlating with the response, or null on failure
     */
    @Suppress("LongParameterList")
    fun startTransaction(
        url: String,
        method: String,
        headers: Map<String, List<String>>,
        bodyStream: InputStream?,
        bodySize: Long,
        startedAtMillis: Long,
    ): UUID? = startTransaction(url, method, headers, bodyStream, bodySize)

    /**
     * Records the completion of an HTTP transaction (response phase).
     *
     * @param id The transaction ID returned from [startTransaction]
     * @param code HTTP response status code
     * @param message HTTP status message
     * @param headers Response headers as a map of name to values
     * @param bodyStream Input stream for reading the response body, or null if no body
     * @param bodySize Size of the response body in bytes
     * @param protocol HTTP protocol version (e.g., "HTTP/1.1", "h2")
     * @param tlsVersion TLS version used for HTTPS connections, or null for HTTP
     * @param error Error message if the request failed, or null on success
     */
    fun completeTransaction(
        id: UUID,
        code: Int,
        message: String,
        headers: Map<String, List<String>>,
        bodyStream: InputStream?,
        bodySize: Long,
        protocol: String?,
        tlsVersion: String?,
        error: String?,
    )

    /**
     * Same as the overload without [showNotification], letting the caller suppress the capture
     * notification for this transaction and supply the measured duration. The default implementation
     * ignores [showNotification] and [durationMs].
     *
     * @param id The transaction ID returned from [startTransaction]
     * @param code HTTP response status code
     * @param message HTTP status message
     * @param headers Response headers as a map of name to values
     * @param bodyStream Input stream for reading the response body, or null if no body
     * @param bodySize Size of the response body in bytes
     * @param protocol HTTP protocol version (e.g., "HTTP/1.1", "h2")
     * @param tlsVersion TLS version used for HTTPS connections, or null for HTTP
     * @param error Error message if the request failed, or null on success
     * @param showNotification Whether a capture notification may be shown for this transaction
     * @param durationMs Time from request start until the response headers arrived, or null to let the
     *   provider derive it from the start timestamp
     */
    @Suppress("LongParameterList")
    fun completeTransaction(
        id: UUID,
        code: Int,
        message: String,
        headers: Map<String, List<String>>,
        bodyStream: InputStream?,
        bodySize: Long,
        protocol: String?,
        tlsVersion: String?,
        error: String?,
        showNotification: Boolean,
        durationMs: Long? = null,
    ) {
        completeTransaction(id, code, message, headers, bodyStream, bodySize, protocol, tlsVersion, error)
    }

    /**
     * Deletes transactions older than the specified threshold.
     *
     * @param threshold Unix timestamp in milliseconds; transactions before this time are deleted
     */
    fun cleanup(threshold: Long)

    /**
     * Returns an Intent to launch the WormaCeptor viewer activity.
     *
     * @param context Context for creating the Intent
     * @return Intent configured to launch the viewer
     */
    fun getLaunchIntent(context: Context): Intent

    /**
     * Returns all stored transactions for content provider queries.
     *
     * @return List of transaction objects (actual type depends on implementation)
     */
    fun getAllTransactions(): List<Any>

    /**
     * Returns a single transaction by its ID.
     *
     * @param id The transaction ID as a string
     * @return The transaction object, or null if not found
     */
    fun getTransaction(id: String): Any?

    /**
     * Returns the total number of stored transactions.
     *
     * @return Count of transactions in storage
     */
    fun getTransactionCount(): Int

    /**
     * Deletes all stored transactions.
     */
    fun clearTransactions()

    /**
     * Returns complete transaction details including request/response bodies.
     * Used by the IDE plugin for detailed inspection.
     *
     * @param id The transaction ID as a string
     * @return Full transaction details, or null if not found
     */
    fun getTransactionDetail(id: String): TransactionDetailDto?
}
