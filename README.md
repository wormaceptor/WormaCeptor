# WormaCeptor

[![GitHub Stars](https://img.shields.io/github/stars/wormaceptor/WormaCeptor?style=social)](https://github.com/wormaceptor/WormaCeptor)
[![Maven Central](https://img.shields.io/maven-central/v/com.azikar24.wormaceptor/wormaceptor-client.svg)](https://central.sonatype.com/artifact/com.azikar24.wormaceptor/wormaceptor-client)
[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg)](https://developer.android.com/about/versions/marshmallow)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Your AI agent can read your code. Now it can read your running Android app.**
WormaCeptor puts network traffic, crashes, logs, databases and preferences one tool call away for Claude Code, Cursor and other MCP clients, and on screen for you. No proxy, no certificates. Ships only in debug builds through Gradle's `debugImplementation`, so nothing reaches production. [Set up the agent in two lines](#ai-agents-mcp).

## Demo

An agent finds and fixes a failing checkout by reading the app's network traffic through WormaCeptor:

<p align="center">
  <img src="media/mcp_agent_demo.webp" width="100%" alt="An AI agent calls WormaCeptor, reads the 422 response body, and fixes the currency field" />
</p>

Network inspection, system tools, and performance monitoring side by side:

<p>
  <img src="media/transaction_list.webp" width="24%" />
  <img src="media/tools_overview.webp" width="24%" />
  <img src="media/shared_preferences.webp" width="24%" />
  <img src="media/fps_monitor.webp" width="24%" />
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.azikar24.wormaceptorapp"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="60"></a>
  <br>
  <a href="https://youtube.com/shorts/iSEifbkq7NI">Watch the demo video</a>
</p>

---

## Quick Start

Make sure `mavenCentral()` is declared in your `settings.gradle.kts` (it's in the default Android template):

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

Add the dependencies in your app's `build.gradle.kts`:

<!-- x-release-please-start-version -->
```kotlin
dependencies {
    implementation("com.azikar24.wormaceptor:wormaceptor-client:3.4.3")
    debugImplementation("com.azikar24.wormaceptor:wormaceptor-persistence:3.4.3")
}
```
<!-- x-release-please-end -->

Initialize in your `Application` class:

```kotlin
import com.azikar24.wormaceptor.api.WormaCeptorApi

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        WormaCeptorApi.init(this)
    }
}
```

Add the interceptor to your OkHttp client:

```kotlin
import com.azikar24.wormaceptor.api.WormaCeptorInterceptor

val client = OkHttpClient.Builder()
    .addInterceptor(WormaCeptorInterceptor())
    .build()
```

Open WormaCeptor from anywhere:

```kotlin
startActivity(WormaCeptorApi.getLaunchIntent(context))
```

You can also enable the shake gesture with `WormaCeptorApi.startActivityOnShake(activity)`. Using Ktor? See the [Ktor guide](https://wormaceptor.com/docs/ktor).

---

## Features

**Network**: Intercept HTTP/HTTPS via OkHttp or Ktor, monitor WebSocket connections and WebView requests, copy URLs or export as cURL, JSON and HAR, throttle with the rate limiter, and mock responses with Mock Rules (turn any captured request into a mock with Add to Mock)

**Performance**: Track FPS, memory, and CPU in real time with a live overlay, catch memory leaks and StrictMode thread violations, spot excessive Compose recompositions

**System**: Browse SQLite databases, edit SharedPreferences, inspect DataStore and encrypted storage, explore files, view loaded native libraries and Gradle dependencies, read device info and logcat, capture crash reports

**Testing**: Fire test push notifications, manage FCM tokens, mock GPS locations, encrypt/decrypt and hash with the crypto tool

**AI agents**: An optional MCP server lets Claude Code, Cursor, and other MCP clients query the running debug app: 44 tools for transactions, crashes, logs, performance, storage, mock rules, request replay, and actions. See [AI Agents (MCP)](#ai-agents-mcp)

Every feature is its own module. Enable exactly what you need at init time, disable the rest. See [Feature Toggles](https://wormaceptor.com/docs/feature-toggles).

---

## How It Works

Your release APK never sees debug code. Here's why:

1. `wormaceptor-client` ships in all build types. It contains only interfaces and a reflection-based lookup, no debug logic.
2. `wormaceptor-persistence` (or `wormaceptor-imdb`) ships only in debug via `debugImplementation`. It registers itself through classpath discovery.
3. In release builds, no implementation exists on the classpath. `wormaceptor-client` falls back to a no-op automatically. No ProGuard rules, no runtime checks, no dead code.

More details in [No-Op Behavior](https://wormaceptor.com/docs/no-op-behavior).

---

## Redacting Sensitive Data

Strip credentials and PII before they hit the inspector.

```kotlin
val interceptor = WormaCeptorInterceptor()
    .redactHeader("Authorization")
    .redactJsonValue("password")
    .redactXmlValue("apiKey")
    .redactBody("ssn=\\d{3}-\\d{2}-\\d{4}")
```

For global redaction that applies across all interceptors, use `WormaCeptorApi.redactionConfig`. See the [Data Redaction](https://wormaceptor.com/docs/data-redaction) guide.

---

## AI Agents (MCP)

Add the MCP device server to your debug build and your AI coding agent can read live app data over [MCP](https://modelcontextprotocol.io/) instead of you pasting logs.

<!-- x-release-please-start-version -->
```kotlin
debugImplementation("com.azikar24.wormaceptor:wormaceptor-mcp-server:3.4.3")
```
<!-- x-release-please-end -->

Register the bridge with your client (needs Node.js 18+, Java 17+ and `adb`; the launcher downloads the matching bridge jar and checks its SHA-256):

<!-- x-release-please-start-version -->
```bash
claude mcp add --transport stdio wormaceptor -- npx -y wormaceptor-mcp@3.4.3
```
<!-- x-release-please-end -->

No Node? Download `wormaceptor-mcp-bridge.jar` from the [latest release](https://github.com/wormaceptor/WormaCeptor/releases/latest) and run it with `java -jar`.

The server binds to `127.0.0.1` on the device and the bridge reaches it through `adb forward`. Bearer-token auth is available and off by default; read [Security](docs/MCP.md#security) before using it on a shared device. Setup, all 44 tools, and configuration: [docs/MCP.md](docs/MCP.md). Walkthrough: [docs/MCP_TUTORIAL.md](docs/MCP_TUTORIAL.md).

---

## Documentation

Full docs at **[wormaceptor.com](https://wormaceptor.com)**: [Getting Started](https://wormaceptor.com/docs)

- [Installation & Requirements](https://wormaceptor.com/docs/installation)
- [Ktor Client Plugin](https://wormaceptor.com/docs/ktor)
- [WebSocket Monitoring](https://wormaceptor.com/docs/websocket)
- [WebView Monitoring](https://wormaceptor.com/docs/webview)
- [Launching the UI](https://wormaceptor.com/docs/launching-ui)
- [Performance Overlay](https://wormaceptor.com/docs/performance-overlay)
- [Extension Providers](https://wormaceptor.com/docs/extension-providers)
- [Migrating from Chucker](https://wormaceptor.com/docs/migration/from-chucker)
- [Troubleshooting](https://wormaceptor.com/docs/troubleshooting)

---

## Contributing

PRs welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for setup, architecture rules and the PR process. To get started:

```bash
./gradlew build              # Build everything
./gradlew spotlessApply      # Format code
./gradlew :app:installDebug  # Run the demo app
```

---

## License

MIT, see [LICENSE](LICENSE).
