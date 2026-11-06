# Migrating from Chucker to WormaCeptor V2

## Overview

Chucker is an HTTP traffic inspector -- and that's all it does. WormaCeptor V2 is a full-featured Android debug toolkit with 20+ tools built in:

- HTTP traffic inspection (OkHttp + Ktor)
- WebSocket monitoring
- WebView monitoring
- Performance monitoring (FPS, Memory, CPU)
- SQLite database browser with live query execution
- SharedPreferences editor
- Leak detection
- Crash reporting
- Push notification simulator
- Location mocking
- Crypto tools
- Feature toggles
- Deep link navigation to every tool

If you only need HTTP inspection, Chucker works fine. If you want a single library that replaces your entire debug drawer, migrate to WormaCeptor.

## 1. Swap Dependencies

### Before (Chucker)

```kotlin
// build.gradle.kts (app)
debugImplementation("com.github.chuckerteam.chucker:library:4.x.x")
releaseImplementation("com.github.chuckerteam.chucker:library-no-op:4.x.x")
```

### After (WormaCeptor)

<!-- x-release-please-start-version -->
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// build.gradle.kts (app)
implementation("com.azikar24.wormaceptor:wormaceptor-client:3.4.3")
debugImplementation("com.azikar24.wormaceptor:wormaceptor-persistence:3.4.3")
```
<!-- x-release-please-end -->

**No release no-op artifact needed.** The `wormaceptor-client` module auto-discovers the implementation at runtime. In release builds where `wormaceptor-persistence` is absent, it falls back to a built-in no-op automatically.

## 2. Swap the Interceptor

### Before (Chucker)

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(
        ChuckerInterceptor.Builder(context)
            .collector(ChuckerCollector(context))
            .maxContentLength(250_000L)
            .redactHeaders("Authorization")
            .alwaysReadResponseBody(true)
            .build()
    )
    .build()
```

### After (WormaCeptor)

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(
        WormaCeptorInterceptor()
            .showNotification(true)
            .maxContentLength(250_000L)
            .redactHeader("Authorization")
    )
    .build()
```

Key differences:
- No `Builder` pattern -- direct method chaining
- No `Collector` object -- handled internally
- No `context` parameter -- the interceptor is context-free
- `alwaysReadResponseBody` is the default behavior

## 3. Add Initialization

Chucker doesn't require explicit initialization. WormaCeptor does.

Add this to your `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WormaCeptorApi.init(this)
    }
}
```

This is a one-liner and is safe to call in all build types. If `wormaceptor-persistence` is not on the classpath (release builds), the call is a no-op.

## 4. Redaction API Mapping

### Headers

| Chucker | WormaCeptor |
|---------|-------------|
| `.redactHeaders("Authorization", "Cookie")` | `.redactHeader("Authorization").redactHeader("Cookie")` |

WormaCeptor uses individual calls per header instead of varargs.

### Body Redaction (WormaCeptor only)

WormaCeptor adds body-level redaction that Chucker does not support:

```kotlin
WormaCeptorInterceptor()
    .redactHeader("Authorization")
    .redactHeader("Cookie")
    .redactJsonValue("token")       // Redacts JSON fields named "token"
    .redactXmlValue("apiKey")       // Redacts XML elements named "apiKey"
    .redactBody("pattern")          // Redacts body content matching pattern
```

## 5. Remove Chucker Leftovers

After migrating, search your codebase for and remove:

```
import com.chuckerteam.chucker
ChuckerInterceptor
ChuckerCollector
```

## 6. What You Gain

| Feature | Chucker | WormaCeptor V2 |
|---------|---------|----------------|
| HTTP traffic inspection | Yes | Yes |
| Ktor native plugin | No | Yes |
| WebSocket monitoring | No | Yes |
| WebView monitoring | No | Yes |
| Performance monitoring (FPS, Memory, CPU) | No | Yes |
| SQLite browser with query execution | No | Yes |
| SharedPreferences editor | No | Yes |
| Leak detection | No | Yes |
| Crash reporting | No | Yes |
| Push notification simulator | No | Yes |
| Location mocking | No | Yes |
| Crypto tools | No | Yes |
| Deep links to every tool | No | Yes |
| Feature toggles | No | Yes |
| Release no-op without extra artifact | No | Yes |

## FAQ

### Do I need to change my OkHttp version?

No. WormaCeptor works with the same OkHttp versions Chucker supports.

### Can I use WormaCeptor alongside Chucker during migration?

Yes. Both add OkHttp interceptors and can coexist. Remove Chucker once you've confirmed WormaCeptor captures traffic correctly.

### Does WormaCeptor support Ktor?

Yes. WormaCeptor provides a native Ktor plugin in addition to the OkHttp interceptor. See the Ktor setup docs for details.

### What about ProGuard / R8?

The `wormaceptor-client` module ships with its own ProGuard rules. No manual configuration needed.

### Is there a size impact?

The `wormaceptor-client` module (included in release) is minimal -- it contains only interfaces and the no-op fallback. The `wormaceptor-persistence` module (debug only) adds the full implementation and UI, but it never ships to production.

### What if I was using `ChuckerCollector` to programmatically clear transactions?

WormaCeptor manages storage internally. Use `WormaCeptorApi` for any programmatic control over recorded data.

### I used `maxContentLength` to limit memory usage. Does WormaCeptor support this?

Yes. The API is identical: `.maxContentLength(250_000L)`. Payloads exceeding this limit are truncated.
