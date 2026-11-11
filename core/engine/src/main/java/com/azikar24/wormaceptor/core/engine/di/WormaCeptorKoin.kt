package com.azikar24.wormaceptor.core.engine.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

/**
 * Owns WormaCeptor's private Koin container.
 *
 * Never touches the global Koin context: a host that calls `startKoin {}` after
 * `WormaCeptorApi.init()` would otherwise crash with KoinApplicationAlreadyStartedException,
 * and a host Koin started without `androidContext()` would break engine resolution.
 *
 * All WormaCeptor resolution must go through [getKoin] / [get], and Compose roots must be
 * wrapped in `KoinIsolatedContext(WormaCeptorKoin.application)` so `koinInject()` works.
 */
object WormaCeptorKoin {
    @Volatile
    private var koinApp: KoinApplication? = null

    /** Whether [init] has run, for callers that start before the host initializes WormaCeptor. */
    val isInitialized: Boolean
        get() = koinApp != null

    /** The isolated application, for `KoinIsolatedContext`. Throws if [init] hasn't run. */
    val application: KoinApplication
        get() = checkNotNull(koinApp) { "WormaCeptor Koin not initialized. Call WormaCeptor.init() first" }

    /**
     * Creates the isolated container. Safe to call multiple times - only the first call has effect.
     *
     * @param context Any context; the application context is retained
     */
    @Synchronized
    fun init(context: Context) {
        if (koinApp != null) return
        start(context, listOf(engineModule))
    }

    @Synchronized
    internal fun start(
        context: Context,
        modules: List<Module>,
    ) {
        if (koinApp != null) return
        koinApp = koinApplication {
            androidContext(context.applicationContext)
            modules(modules)
        }
    }

    /** The isolated container. Throws if [init] hasn't run. */
    fun getKoin(): Koin = application.koin

    /**
     * Resolves [clazz] from the isolated container.
     *
     * Static so `api:client` can call it by reflection without depending on Koin.
     */
    @JvmStatic
    fun <T : Any> get(clazz: Class<T>): T = getKoin().get(clazz.kotlin)

    /** Closes the isolated container. Call only when completely done with WormaCeptor. */
    @Synchronized
    fun cleanup() {
        koinApp?.close()
        koinApp = null
    }
}
