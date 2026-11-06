# Migrating from Flipper to WormaCeptor V2

## Overview

Flipper was archived by Meta in 2024. Even before that, it had well-known pain points:

- **Desktop app required** -- inspection only works when tethered to the Electron-based Flipper Desktop
- **Electron bloat** -- the desktop app consumes significant RAM and disk
- **SoLoader crashes** -- `SoLoader.init()` is a frequent source of native crashes, especially on older devices and custom ROMs
- **Build time impact** -- Flipper pulls in native dependencies, annotation processors, and multiple plugin artifacts that slow builds
- **Manual release gating** -- forgetting to strip Flipper from release builds leaks debug infrastructure to production

WormaCeptor V2 replaces Flipper with a single library that runs entirely on-device. No desktop app, no SoLoader, no native dependencies.

## 1. Remove Flipper

### Remove Dependencies

Delete all Flipper-related lines from your `build.gradle.kts`:

```kotlin
// DELETE all of these
debugImplementation("com.facebook.flipper:flipper:0.x.x")
debugImplementation("com.facebook.flipper:flipper-network-plugin:0.x.x")
debugImplementation("com.facebook.flipper:flipper-fresco-plugin:0.x.x")
releaseImplementation("com.facebook.flipper:flipper-noop:0.x.x")
implementation("com.facebook.soloader:soloader:0.x.x")
```

### Remove Initialization Code

Find and delete your Flipper setup. It typically lives in your `Application` class or a debug-only initializer:

```kotlin
// DELETE this block
SoLoader.init(this, false)

if (BuildConfig.DEBUG && FlipperUtils.shouldEnableFlipper(this)) {
    val client = AndroidFlipperClient.getInstance(this)
    client.addPlugin(InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()))
    client.addPlugin(NetworkFlipperPlugin())
    client.addPlugin(DatabasesFlipperPlugin(this))
    client.addPlugin(SharedPreferencesFlipperPlugin(this))
    client.addPlugin(LeakCanaryFlipperPlugin())
    client.addPlugin(CrashReporterPlugin.getInstance())
    client.start()
}
```

### Remove SoLoader

If SoLoader was only used by Flipper (common), remove it entirely:

```kotlin
// DELETE
SoLoader.init(this, false)
```

### Clean Up Build Types

If you had debug/release source sets specifically for Flipper (e.g., `src/debug/java/.../FlipperInit.kt` and `src/release/java/.../FlipperInit.kt`), delete those files.

### Sync and Verify

Run a Gradle sync and build. Fix any remaining import errors -- they indicate leftover Flipper references.

## 2. Add WormaCeptor

### Add the Repository

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

### Add Dependencies

<!-- x-release-please-start-version -->
```kotlin
// build.gradle.kts (app)
implementation("com.azikar24.wormaceptor:wormaceptor-client:3.4.3")
debugImplementation("com.azikar24.wormaceptor:wormaceptor-persistence:3.4.3")
```
<!-- x-release-please-end -->

No release no-op artifact needed. The `wormaceptor-client` module auto-discovers the implementation at runtime and falls back to no-op when `wormaceptor-persistence` is absent.

### Initialize

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WormaCeptorApi.init(this)
    }
}
```

### Add the OkHttp Interceptor

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(
        WormaCeptorInterceptor()
            .showNotification(true)
            .maxContentLength(250_000L)
    )
    .build()
```

That's it. No plugin registration, no client objects, no SoLoader.

## 3. Plugin-to-Feature Mapping

| Flipper Plugin | WormaCeptor Equivalent | Setup Required |
|---------------|----------------------|----------------|
| `NetworkFlipperPlugin` | `WormaCeptorInterceptor` | Add to OkHttp client |
| `DatabasesFlipperPlugin` | Built-in SQLite browser | None (enabled by default) |
| `SharedPreferencesFlipperPlugin` | Built-in SharedPreferences inspector | None (enabled by default) |
| `LeakCanaryFlipperPlugin` | Built-in leak detection | None (enabled by default) |
| `CrashReporterPlugin` | Built-in crash reporting | None (enabled by default) |
| Layout Inspector | Performance overlay (FPS, Memory, CPU) | None (enabled by default) |

All WormaCeptor features are available on-device through the WormaCeptor UI. No desktop app connection needed.

## 4. Key Differences

### No Desktop App

Flipper required the Flipper Desktop app running on your computer, connected via ADB. WormaCeptor runs entirely on the device. Open the WormaCeptor notification or launch it programmatically.

### No SoLoader

WormaCeptor has zero native dependencies. No `SoLoader.init()`, no `.so` files, no NDK crashes.

### Release Safety by Default

Flipper relied on developers correctly using `debugImplementation` and `releaseImplementation("flipper-noop")`. Forgetting this shipped debug tools to production.

WormaCeptor uses `debugImplementation` for the implementation module. The `wormaceptor-client` (included via `implementation`) contains only interfaces and automatically falls back to no-op. There is no separate no-op artifact to forget.

### Faster Builds

Flipper pulls in:
- Native `.so` libraries (SoLoader)
- Multiple plugin artifacts
- Annotation processors (for some plugins)

WormaCeptor is a pure Kotlin/Android library. No native compilation, no annotation processing. Build time impact is minimal.

### More Features

WormaCeptor includes tools that Flipper never had:

| Feature | Flipper | WormaCeptor V2 |
|---------|---------|----------------|
| WebSocket monitoring | No | Yes |
| WebView monitoring | No | Yes |
| Ktor native plugin | No | Yes |
| Push notification simulator | No | Yes |
| Location mocking | No | Yes |
| Crypto tools | No | Yes |
| Deep links to every tool | No | Yes |
| Feature toggles | No | Yes |
| Performance monitoring (FPS, Memory, CPU) | Via Layout Inspector | Built-in overlay |

## FAQ

### Flipper was archived -- will it stop working?

It already works less reliably with each AGP and Android version update. No further patches will be released. Migrating now avoids being forced to migrate later during an urgent upgrade.

### Can I keep LeakCanary alongside WormaCeptor?

Yes. WormaCeptor's built-in leak detection is independent. If you prefer LeakCanary's reporting, keep it -- they don't conflict. You can also use WormaCeptor's leak detection as a replacement and remove LeakCanary entirely.

### I used Flipper's network plugin to mock responses. Does WormaCeptor support this?

WormaCeptor focuses on inspection and monitoring rather than response mocking. For mock responses, continue using tools like MockWebServer or WireMock.

### What about custom Flipper plugins?

If you wrote custom Flipper plugins, they have no direct equivalent. Evaluate whether WormaCeptor's built-in features cover the use case. For truly custom needs, WormaCeptor's extensible architecture allows integration.

### Does WormaCeptor work with Compose?

Yes. WormaCeptor's UI is independent of your app's UI framework. It works with Views, Compose, or any mix.

### Will removing Flipper break my Gradle build?

It can if other dependencies transitively relied on SoLoader or Flipper artifacts. After removing Flipper dependencies, run a clean build and resolve any missing class errors. Most projects have no transitive dependency on Flipper internals.

### How do I access WormaCeptor at runtime?

Pull down the notification shade and tap the WormaCeptor notification (if `.showNotification(true)` is set), or launch it programmatically via `WormaCeptorApi.launch(context)`.
