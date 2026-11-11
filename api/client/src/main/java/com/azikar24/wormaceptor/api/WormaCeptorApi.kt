package com.azikar24.wormaceptor.api

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import com.azikar24.wormaceptor.core.engine.CoreHolder
import com.azikar24.wormaceptor.core.engine.McpHolder
import com.azikar24.wormaceptor.domain.entities.McpConfig
import com.azikar24.wormaceptor.platform.android.ShakeDetector

private const val TAG = "WormaCeptorApi"

/** Main entry point for the WormaCeptor debugging toolkit API. */
@Suppress("unused")
object WormaCeptorApi {

    @Volatile
    internal var provider: ServiceProvider? = null
        private set

    /**
     * [provider] when it actually records traffic. Null when uninitialized or when only a no-op
     * provider is present (release builds), so capture paths can skip body copying entirely.
     */
    internal val capturingProvider: ServiceProvider?
        get() = provider?.takeIf { CoreHolder.captureEngine != null }

    @Volatile
    private var enabledFeatures: Set<Feature> = Feature.DEFAULT

    @Volatile
    private var shakeToOpenEnabled = false

    /** Configuration for redacting sensitive data from captured network traffic. */
    val redactionConfig = RedactionConfig()

    /**
     * Initializes WormaCeptor.
     * If an implementation module (persistence, imdb) is present in the classpath,
     * it will be automatically discovered and initialized.
     *
     * @param context Application context
     * @param logCrashes Whether to capture uncaught exceptions
     * @param features Set of features to enable (defaults to all features). The leak detector and the
     *   thread-violation monitor only run when [Feature.LEAK_DETECTION] / [Feature.THREAD_VIOLATIONS] are in it
     * @param leakNotifications Whether to show notifications when memory leaks are detected
     */
    fun init(
        context: Context,
        logCrashes: Boolean = true,
        features: Set<Feature> = Feature.DEFAULT,
        leakNotifications: Boolean = true,
    ) {
        if (provider != null) return

        enabledFeatures = features

        // Discovery via reflection to avoid compile-time dependency on implementation modules
        val implClass = try {
            Class.forName("com.azikar24.wormaceptor.api.internal.ServiceProviderImpl")
        } catch (_: Exception) {
            null
        }

        val instance = implClass?.getDeclaredConstructor()?.newInstance() as? ServiceProvider
        provider = instance ?: NoOpProvider()

        provider?.init(context, logCrashes, leakNotifications, features)
    }

    /**
     * Check if a specific feature is enabled.
     * @param feature The feature to check
     * @return true if the feature is enabled, false otherwise
     */
    fun isFeatureEnabled(feature: Feature): Boolean = feature in enabledFeatures

    /**
     * Get all currently enabled features.
     * @return A copy of the enabled features set
     */
    fun getEnabledFeatures(): Set<Feature> = enabledFeatures.toSet()

    /**
     * Registers a shake gesture detector that opens WormaCeptor when the device is shaken.
     * The detector is lifecycle-aware and automatically stops when the activity is destroyed.
     * Does nothing on shake when no implementation module is installed (release builds).
     *
     * @param activity The activity to attach the shake detector to
     */
    fun startActivityOnShake(activity: ComponentActivity) {
        // Platform classes are safe to refer if they are in a common layout or handled similarly
        // For simplicity, we'll delegate shake start to provider if needed,
        // or keep it in platform if platform is a shared dependency.
        try {
            ShakeDetector.start(activity) {
                val intent = getLaunchIntent(activity)
                // No component: no-op provider. Same class: WormaCeptor is already in front.
                val target = intent.component?.className
                if (target != null && target != activity.javaClass.name) activity.startActivity(intent)
            }
        } catch (_: Exception) {
            // Shake detector might not be present in No-Op
        }
    }

    /**
     * Opens WormaCeptor when the device is shaken in any activity of the app. Call once from
     * `Application.onCreate`; later calls are ignored. Covers every [ComponentActivity] created afterwards,
     * and the detector only listens while its activity is resumed, so it follows the foreground activity.
     *
     * @param application The host application
     */
    fun enableShakeToOpen(application: Application) {
        if (shakeToOpenEnabled) return
        shakeToOpenEnabled = true
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) {
                    if (activity is ComponentActivity) startActivityOnShake(activity)
                }

                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    /**
     * Returns an Intent that launches the WormaCeptor UI.
     *
     * @param context Application or Activity context
     * @return Intent to launch WormaCeptor, or an empty Intent if not initialized
     */
    fun getLaunchIntent(context: Context): Intent {
        return provider?.getLaunchIntent(context) ?: Intent()
    }

    // ========== Floating Button API ==========

    /**
     * Check if the app has permission to show floating button (overlay permission).
     * Returns true if permission is granted, false otherwise.
     * On API < 23, always returns true.
     */
    fun canShowFloatingButton(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Show the floating button overlay.
     * The button is draggable and snaps to screen edges when released.
     * Tapping the button opens the ViewerActivity.
     *
     * Note: Requires SYSTEM_ALERT_WINDOW permission.
     * Call [canShowFloatingButton] first to check if permission is granted.
     * If not granted, direct user to Settings.ACTION_MANAGE_OVERLAY_PERMISSION.
     *
     * @param context Application or Activity context
     * @return true if the floating button was started, false if permission not granted
     */
    fun showFloatingButton(context: Context): Boolean {
        if (!canShowFloatingButton(context)) {
            return false
        }

        // Use reflection to start FloatingButtonService to avoid hard dependency
        return try {
            val serviceClass = Class.forName(
                "com.azikar24.wormaceptor.platform.android.FloatingButtonService",
            )
            val companion = serviceClass.getDeclaredField("Companion").get(null)
            val companionClass = companion.javaClass
            val companionStartMethod = companionClass.getMethod("start", Context::class.java)
            companionStartMethod.invoke(companion, context)
            true
        } catch (e: ReflectiveOperationException) {
            Log.w(TAG, "Failed to show floating button", e)
            false
        }
    }

    /**
     * Hide the floating button overlay.
     *
     * @param context Application or Activity context
     */
    fun hideFloatingButton(context: Context) {
        // Use reflection to stop FloatingButtonService to avoid hard dependency
        try {
            val serviceClass = Class.forName(
                "com.azikar24.wormaceptor.platform.android.FloatingButtonService",
            )
            val companion = serviceClass.getDeclaredField("Companion").get(null)
            val companionClass = companion.javaClass
            val companionStopMethod = companionClass.getMethod("stop", Context::class.java)
            companionStopMethod.invoke(companion, context)
        } catch (e: ReflectiveOperationException) {
            Log.w(TAG, "Failed to hide floating button", e)
        }
    }

    /**
     * Get an Intent to open the system overlay permission settings.
     * Use this when [canShowFloatingButton] returns false.
     *
     * @param context Application context
     * @return Intent to open overlay settings, or null if not needed (API < 23)
     */
    fun getOverlayPermissionIntent(context: Context): Intent? {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri(),
        )
    }

    // ========== Performance Overlay API ==========

    /**
     * Show the performance overlay on top of the app.
     * The overlay displays real-time FPS, Memory, and CPU metrics.
     * It is draggable and can be expanded to show mini sparkline charts.
     * Tapping the overlay opens the detailed performance screens.
     *
     * Note: Requires SYSTEM_ALERT_WINDOW permission.
     * Call [canShowFloatingButton] first to check if permission is granted.
     *
     * @param activity The activity to attach the overlay to
     * @return true if the overlay was shown, false if permission not granted
     */
    fun showPerformanceOverlay(activity: ComponentActivity): Boolean {
        if (!canShowFloatingButton(activity)) {
            return false
        }

        return try {
            val engineClass = Class.forName(
                "com.azikar24.wormaceptor.core.engine.PerformanceOverlayEngine",
            )
            val koinClass = Class.forName("com.azikar24.wormaceptor.core.engine.di.WormaCeptorKoin")
            val getMethod = koinClass.getMethod("get", Class::class.java)
            val engine = getMethod.invoke(null, engineClass)
            val showMethod = engineClass.getMethod("show", Activity::class.java)
            showMethod.invoke(engine, activity)
            true
        } catch (e: ReflectiveOperationException) {
            Log.w(TAG, "Failed to show performance overlay", e)
            false
        }
    }

    /**
     * Hide the performance overlay.
     */
    fun hidePerformanceOverlay() {
        try {
            val engineClass = Class.forName(
                "com.azikar24.wormaceptor.core.engine.PerformanceOverlayEngine",
            )
            val koinClass = Class.forName("com.azikar24.wormaceptor.core.engine.di.WormaCeptorKoin")
            val getMethod = koinClass.getMethod("get", Class::class.java)
            val engine = getMethod.invoke(null, engineClass)
            val hideMethod = engineClass.getMethod("hide")
            hideMethod.invoke(engine)
        } catch (e: ReflectiveOperationException) {
            Log.w(TAG, "Failed to hide performance overlay", e)
        }
    }

    /**
     * Check if the performance overlay is currently visible.
     *
     * @return true if the overlay is visible, false otherwise
     */
    fun isPerformanceOverlayVisible(): Boolean {
        return try {
            val engineClass = Class.forName(
                "com.azikar24.wormaceptor.core.engine.PerformanceOverlayEngine",
            )
            val koinClass = Class.forName("com.azikar24.wormaceptor.core.engine.di.WormaCeptorKoin")
            val getMethod = koinClass.getMethod("get", Class::class.java)
            val engine = getMethod.invoke(null, engineClass)
            val isVisibleField = engineClass.getMethod("isVisible")
            val stateFlow = isVisibleField.invoke(engine)
            // Resolve on the public interface: the runtime class (ReadonlyStateFlow) isn't public, so invoking
            // its getValue reflectively throws IllegalAccessException
            val valueMethod = Class.forName("kotlinx.coroutines.flow.StateFlow").getMethod("getValue")
            valueMethod.invoke(stateFlow) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    // ========== MCP Server API ==========

    /**
     * Configures the MCP (Model Context Protocol) debug server.
     * Call from `Application.onCreate`. If the server already auto-started it restarts with
     * the new settings; `McpConfig(enabled = false)` stops it.
     *
     * The MCP server runs on the device and exposes debugging data
     * (network transactions, crashes, performance metrics, etc.) via REST API
     * for AI agents and development tools.
     *
     * @param config Server configuration (port, auth, body size limits)
     */
    fun configureMcpServer(config: McpConfig) {
        McpHolder.configure(config)
        if (McpHolder.isRunning()) {
            McpHolder.restart()
        }
    }

    /**
     * Check if the MCP debug server is currently running.
     * The server auto-starts when the `mcp-device-server` module is included
     * as a `debugImplementation` dependency.
     *
     * @return true if the server is running
     */
    fun isMcpServerRunning(): Boolean = McpHolder.isRunning()

    /**
     * Start the MCP debug server manually.
     * Only needed if the server was previously stopped or if auto-start is disabled
     * via `McpConfig(enabled = false)`.
     */
    fun startMcpServer() {
        McpHolder.start()
    }

    /**
     * Stop the MCP debug server.
     */
    fun stopMcpServer() {
        McpHolder.stop()
    }

    // ========== Extension Provider API ==========

    /**
     * Register a custom extension provider.
     * Extension providers extract custom metadata from network transactions.
     * The extracted data will be stored with the transaction and displayed in the UI.
     *
     * @param provider The extension provider to register
     */
    fun registerExtensionProvider(provider: ExtensionProvider) {
        CoreHolder.extensionRegistry?.register(provider)
    }

    /**
     * Unregister an extension provider by name.
     *
     * @param name The name of the provider to unregister
     * @return true if a provider was removed, false otherwise
     */
    fun unregisterExtensionProvider(name: String): Boolean {
        return CoreHolder.extensionRegistry?.unregister(name) ?: false
    }

    /**
     * Get the names of all registered extension providers.
     *
     * @return List of provider names, or empty list if not initialized
     */
    fun getRegisteredExtensionProviders(): List<String> {
        return CoreHolder.extensionRegistry?.getRegisteredProviders() ?: emptyList()
    }

    private class NoOpProvider : ServiceProvider {
        override fun init(
            context: Context,
            logCrashes: Boolean,
            leakNotifications: Boolean,
        ) {
        }

        override fun startTransaction(
            url: String,
            method: String,
            headers: Map<String, List<String>>,
            bodyStream: java.io.InputStream?,
            bodySize: Long,
        ) = null

        override fun completeTransaction(
            id: java.util.UUID,
            code: Int,
            message: String,
            headers: Map<String, List<String>>,
            bodyStream: java.io.InputStream?,
            bodySize: Long,
            protocol: String?,
            tlsVersion: String?,
            error: String?,
        ) {
        }

        override fun cleanup(threshold: Long) {}
        override fun getLaunchIntent(context: Context): Intent = Intent()
        override fun getAllTransactions(): List<Any> = emptyList()
        override fun getTransaction(id: String): Any? = null
        override fun getTransactionCount(): Int = 0
        override fun clearTransactions() {}
        override fun getTransactionDetail(id: String): TransactionDetailDto? = null
    }
}
