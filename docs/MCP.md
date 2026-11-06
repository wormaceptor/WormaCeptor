# WormaCeptor MCP Integration

WormaCeptor exposes a [Model Context Protocol](https://modelcontextprotocol.io/) (MCP) server that lets AI agents and development tools inspect a running Android app in real time. The system has two parts: a **device server** embedded in your debug build and a **bridge CLI** that translates MCP requests into HTTP calls over ADB.

```
┌─────────────────────┐    stdin/stdout     ┌─────────────────────┐
│   AI Agent / IDE    │◄──── MCP JSON-RPC ──►│   Bridge CLI (JVM)  │
└─────────────────────┘                      └──────────┬──────────┘
                                                        │ HTTP via ADB
                                                        │ port forward
                                             ┌──────────▼──────────┐
                                             │  Device Server      │
                                             │  (Ktor, port 8999)  │
                                             │  inside debug app   │
                                             └─────────────────────┘
```

## Setup for AI agents

These steps are written so a coding agent can do the whole setup itself. Use the WormaCeptor version the project already depends on (the version of `wormaceptor-client`) everywhere `VERSION` appears; the server, bridge and launcher are released together.

1. Add the device server to the app module, next to the existing WormaCeptor dependencies:
   ```kotlin
   debugImplementation("com.azikar24.wormaceptor:wormaceptor-mcp-server:VERSION")
   ```
2. Register the bridge with the MCP client. With Node.js 18+ (the launcher downloads the matching bridge jar from the GitHub release, checks its SHA-256 and caches it):
   ```bash
   claude mcp add --transport stdio wormaceptor -- npx -y wormaceptor-mcp@VERSION
   ```
   Without Node, download the jar and its checksum, verify, and register it directly:
   ```bash
   curl -fLO https://github.com/wormaceptor/WormaCeptor/releases/download/vVERSION/wormaceptor-mcp-bridge.jar
   curl -fLO https://github.com/wormaceptor/WormaCeptor/releases/download/vVERSION/wormaceptor-mcp-bridge.jar.sha256
   shasum -a 256 -c wormaceptor-mcp-bridge.jar.sha256
   claude mcp add --transport stdio wormaceptor -- java -jar /absolute/path/to/wormaceptor-mcp-bridge.jar
   ```
3. The bridge needs Java 17+ and `adb`. If `java` isn't on PATH, use Android Studio's bundled JDK (macOS: `/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java`); the npx launcher finds it automatically. The bridge looks for `adb` in `$ANDROID_HOME/platform-tools`, `$ANDROID_SDK_ROOT/platform-tools`, on PATH, then in the default SDK dir (macOS `~/Library/Android/sdk`, Linux `~/Android/Sdk`, Windows `%LOCALAPPDATA%\Android\Sdk`); anywhere else, pass `--adb <path>`.
4. With more than one device attached, append `--device <serial>` (from `adb devices`) to the command.
5. Build and run the debug app, keep it in the foreground, then check with `claude mcp get wormaceptor` and call `get_device_info`.

## Quick Start

### 1. Add the dependency

The device server is already included in the demo app. For your own app, add it next to your existing WormaCeptor dependencies:

<!-- x-release-please-start-version -->
```kotlin
// app/build.gradle.kts
debugImplementation("com.azikar24.wormaceptor:wormaceptor-mcp-server:3.4.3")
```
<!-- x-release-please-end -->

The server auto-starts via a ContentProvider, no code changes needed. It answers as soon as the process starts; tools return data once `WormaCeptorApi.init()` has run.

### 1b. Configure via API (optional)

The server works out of the box with defaults. To customize, call `configureMcpServer` in `Application.onCreate`. If the server already auto-started, it restarts with the new settings:

```kotlin
// In Application.onCreate()
WormaCeptorApi.configureMcpServer(
    McpConfig(
        port = 9000,                  // default: 8999
        enableAuth = true,            // default: false
        authToken = "my-debug-token", // required when auth enabled
        maxBodySize = 2_097_152L,     // default: 1 MB
    )
)
WormaCeptorApi.init(context = this)
```

`McpConfig(enabled = false)` stops a running server and keeps it from auto-starting. To control it manually:

```kotlin
WormaCeptorApi.configureMcpServer(McpConfig(enabled = false))
WormaCeptorApi.init(context = this)

// Later, when ready:
WormaCeptorApi.startMcpServer()

// Check status:
WormaCeptorApi.isMcpServerRunning() // true

// Stop:
WormaCeptorApi.stopMcpServer()
```

### 2. Build the bridge

```bash
./gradlew :mcp:bridge:jar
```

This produces `mcp/bridge/build/libs/bridge.jar`.

### 3. Connect

```bash
# Make sure your device/emulator is connected
adb devices

# Run the bridge (it handles port forwarding automatically)
java -jar mcp/bridge/build/libs/bridge.jar
```

The bridge discovers the device, forwards port 8999, verifies the server is reachable, and starts listening for MCP requests on stdin.

### 4. Configure in Claude Code

```bash
claude mcp add --transport stdio wormaceptor -- java -jar /absolute/path/to/bridge.jar
```

Or share it with your team by committing a `.mcp.json` at the project root:

```json
{
  "mcpServers": {
    "wormaceptor": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/bridge.jar"]
    }
  }
}
```

With more than one device attached, add `"--device", "<serial>"` to `args`.

## Architecture

### Device Server (`mcp/device-server`)

An embedded Ktor/Netty HTTP server that runs inside the debug build on port 8999. It exposes REST endpoints backed by WormaCeptor's core engines.

- **Auto-starts** via `WormaCeptorServerInitializer`, a ContentProvider that registers the server with `McpHolder` and starts it off the main thread
- **Debug-only**: included as `debugImplementation`, zero code in release builds
- **Localhost-only**: binds `127.0.0.1` and rejects requests with an `Origin` header or a non-local `Host` (blocks browser and DNS-rebinding access)
- **Optional auth**: bearer token validation when `McpConfig.enableAuth` is true
- **Secret redaction**: preference values and file contents under secret-looking keys are masked (`McpConfig.redactSecrets`)
- **Header redaction** — sensitive headers (`Authorization`, `Cookie`, `X-Api-Key`, etc.) are replaced with `[REDACTED]` in API responses
- **Secure storage keys only**: `browse_secure_storage` never returns stored values
- **WebSocket streaming**: real-time engine data at `/api/stream`

### Bridge CLI (`mcp/bridge`)

A standalone JVM application that translates MCP protocol (JSON-RPC 2.0 on stdin/stdout) into HTTP calls to the device server.

- **Auto-discovers** connected devices via ADB
- **Port forwarding** — sets up `tcp:8999 → tcp:8999` automatically
- **Reconnection**: at startup it waits ~10s for the server, then starts anyway; a tool call that fails with a connection error re-creates the port forward, retries 4 times with backoff (1s, 2s, 4s), then repeats the call once
- **Frozen or dead app**: requests time out after 10 s (health checks after 2 s). On a timeout or connection error the bridge asks adb whether the app's process is alive (`pidof`) or frozen (`dumpsys activity processes`, `isFrozen=true`) and says so instead of retrying; the package name comes from `/api/health`, cached after the first successful check
- **Input validation** — path traversal protection, SQL injection prevention, parameter range checks
- **Output size**: each result is capped at 20,000 characters. Lists keep what fits (logs keep the newest entries) and end with `… truncated, N more entries; use limit/offset to page`; log messages and list field values are clipped at 500 characters
- **Tool errors**: invalid arguments, device-server errors and connection failures come back as MCP `isError: true` results whose text starts with `Error: `
- **Verbose mode** — `--verbose` logs all MCP requests and responses to stderr

## Bridge CLI Reference

```
Usage: java -jar bridge.jar [options]

Options:
  --port <port>      Device server port (default: 8999)
  --device, -s <id>  Target device serial (required if multiple devices)
  --adb <path>       Path to adb (default: searches ANDROID_HOME, ANDROID_SDK_ROOT,
                     PATH, then the default Android SDK dir)
  --token <token>    Bearer token for authentication
  --package <id>     App under debug (default: the only installed app with the MCP server)
  --verbose, -v      Enable verbose request/response logging
  --version          Print version and exit
  --help, -h         Print help
```

## MCP Tools (44 total)

### Network (8 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `list_transactions` | `query?`, `limit?`, `offset?` | List captured HTTP transactions, paginated (default 20). `query` is a case-insensitive substring match on URL, method, or status code |
| `get_transaction` | `id` | Get full details of a specific transaction |
| `get_request_body` | `id` | Get the request body of a transaction |
| `get_response_body` | `id` | Get the response body of a transaction |
| `list_websocket_connections` | — | List active WebSocket connections |
| `list_websocket_messages` | `connection_id?`, `limit?` | List WebSocket messages, optionally filtered by connection |
| `get_rate_limit` | — | Get current rate limiting configuration |
| `set_rate_limit` | `enabled?`, `preset?`, `download_kbps?`, `upload_kbps?`, `latency_ms?`, `packet_loss?` | Configure network throttling. Presets: `WIFI`, `GOOD_3G`, `REGULAR_3G`, `SLOW_3G`, `GOOD_2G`, `SLOW_2G`, `EDGE`, `OFFLINE`. `enabled: false` turns it off; `packet_loss` is a percentage (0-100) |

### Diagnostics (6 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `list_crashes` | `limit?`, `offset?` | List captured crash reports, paginated (default 20) |
| `get_crash` | `id` | Get full crash details with stack trace |
| `tail_logs` | `level?`, `tag?`, `limit?` | Retrieve the most recent log entries (default 30). `level` matches exactly (`VERBOSE`...`ASSERT`); `tag` is a case-insensitive substring. The bridge reads the app process's logcat over adb (`logcat -d -v threadtime --pid=<pid>`): the whole buffer, no in-app capture and no Android 13+ log access prompt. If adb can't read it, it falls back to `/api/logs`, which only has entries while WormaCeptor's in-app capture runs (Logs screen) |
| `list_leaks` | — | List detected memory leaks (LeakCanary integration) |
| `list_violations` | — | List StrictMode and thread policy violations |
| `get_device_info` | — | Get device model, Android version, app info, and system properties |

### Performance (5 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `get_cpu_stats` | `include_history?` | Get CPU usage with optional history. When the monitor is off, returns a one-off sample and no history |
| `get_memory_stats` | `include_history?` | Get memory usage with optional history. When the monitor is off, returns a one-off sample and no history |
| `get_fps_stats` | `include_history?` | Get frame rate stats with optional history. Returns an error while FPS monitoring is off (start it with `set_monitoring`, the FPS tool or the performance overlay) |
| `get_performance_snapshot` | — | Get a combined snapshot of CPU, memory, and FPS |
| `set_monitoring` | `target`, `enabled` | Start or stop the `cpu`, `memory` or `fps` monitor. Stopping keeps a monitor running while the performance overlay shows it |

### Storage (8 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `list_preferences` | — | List all SharedPreferences files and entries |
| `list_databases` | — | List all SQLite databases |
| `query_database` | `database`, `query` | Execute a SELECT query (read-only, no DROP/DELETE/INSERT/UPDATE/ALTER) |
| `list_files` | `path?` | Browse app file system directory |
| `read_file` | `path` | Read a text file (JSON/XML pretty-printed). Binary files, images, and PDFs return a one-line summary |
| `browse_secure_storage` | `type?` | List secure storage keys and metadata (`ENCRYPTED_SHARED_PREFS`, `KEYSTORE`, `DATASTORE`). Values are never returned |
| `list_dependencies` | `category?` | List detected app dependencies, optionally by category (`NETWORKING`, `UI_FRAMEWORK`, ...) |
| `list_loaded_libraries` | `type?`, `system?` | List loaded libraries. `type`: `NATIVE_SO`, `DEX`, `JAR`, `AAR_RESOURCE`; `system`: true/false |

### Actions (7 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `clear_transactions` | — | Clear all captured network transactions |
| `clear_crashes` | — | Clear all crash reports |
| `clear_logs` | — | Empty the in-app log buffer; `tail_logs` then hides entries logged before the call (by host clock; logcat itself isn't cleared) |
| `simulate_location` | `latitude`, `longitude`, `altitude?`, `name?` | Set a mock GPS location (lat: -90..90, lng: -180..180). The app must be the mock location app: Developer options, or `adb shell appops set <package> android:mock_location allow` |
| `stop_location_simulation` | — | Stop mock location |
| `send_push_notification` | `title`, `body`, `channel_id?`, `priority?` | Send a simulated push notification |
| `bring_app_to_front` | `package?` | Launch the app or bring it back to the foreground via adb (`monkey` with the launcher intent). Bridge-only; defaults to the package the bridge last reached |

### Timeline and events (3 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `get_timeline` | `since_ms?`, `until_ms?`, `limit?` | Transactions, crashes, logs, StrictMode violations, leaks and WebSocket messages merged in time order, newest last. Window defaults to the last 5 minutes (device clock, epoch ms), `limit` to 50 (max 500, newest kept). Logs are included only while log capture is running |
| `wait_for_transaction` | `url_contains?`, `method?`, `status?`, `timeout_s?` | Block until a NEW request matching the filters finishes (started after the call), then return it. `url_contains` is a case-insensitive URL substring, `status` an exact code. `timeout_s` defaults to 30, max 120 |
| `wait_for_crash` | `timeout_s?` | Block until a NEW crash is recorded, then return it with the stack trace. A fatal crash kills the process, so the call can end with "connection dropped": relaunch the app and use `list_crashes` |

### Mock rules (5 tools)

Mocks apply to OkHttp and Ktor clients that use the WormaCeptor interceptor or plugin. Rules are the same ones the in-app Mock Rules screen edits, and only apply while mocking is on globally (`list_mock_rules` shows it).

| Tool | Parameters | Description |
|------|-----------|-------------|
| `list_mock_rules` | — | List mock rules (pattern, match type, method, status, content type, body preview, delay, enabled) and whether mocking is on |
| `create_mock_rule` | `url_pattern`, `status`, `match_type?`, `method?`, `body?`, `content_type?`, `delay_ms?`, `enabled?` | Create a rule that returns a canned response. `match_type`: `PREFIX` (default) or `EXACT` compare against the full URL including scheme and query; `REGEX` searches anywhere in it. `method` omitted matches any; `content_type` defaults to `application/json`; `status` 100-599 |
| `mock_from_transaction` | `id`, `status?`, `body?` | Copy a captured transaction into a rule, like the in-app "Add to Mock": exact URL and method, captured status, content type and text body. `status` / `body` override them; binary or oversized bodies are left out |
| `set_mock_rule_enabled` | `id`, `enabled` | Turn a rule on or off without deleting it |
| `delete_mock_rule` | `id` | Delete a rule permanently |

### Reproduce (2 tools)

| Tool | Parameters | Description |
|------|-----------|-------------|
| `export_curl` | `id` | Build a shell-quoted cURL command (method, URL, headers, text body) for a captured request. Sensitive headers stay `[REDACTED]` and are listed; binary bodies are left out |
| `replay_transaction` | `id`, `url?`, `headers?`, `body?` | Send a captured request again from the device and capture it as a new transaction; returns its id and status. `headers` is an object whose entries replace captured headers of the same name. Goes straight to the server: mock rules, throttling and the app's own interceptors don't apply |

## REST API Endpoints

All endpoints are prefixed with `/api`. Responses follow a standard envelope:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": 1774224835921,
  "meta": {
    "total": 42,
    "limit": 50,
    "offset": 0,
    "truncated": false
  }
}
```

### Health

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/health` | No | Server health check: `{success, server, version, timestamp, packageName}` |

### Network

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/transactions` | List transactions (`?query=&limit=&offset=`) |
| GET | `/api/transactions/{id}` | Transaction details |
| GET | `/api/transactions/{id}/request-body` | Request body: `{body, contentType, truncated, totalSize}`, `body` cut at `maxBodySize` |
| GET | `/api/transactions/{id}/response-body` | Response body, same shape |
| GET | `/api/websockets/connections` | WebSocket connections |
| GET | `/api/websockets/messages` | WebSocket messages (`?connection_id=`) |
| GET | `/api/rate-limit` | Current rate limit config |
| POST | `/api/rate-limit` | Update rate limit config |
| GET | `/api/mock-rules` | Mock rules and the global `mockingEnabled` flag |
| POST | `/api/mock-rules` | Create a mock rule: `{urlPattern, status, method?, matchType?, body?, contentType?, delayMs?, enabled?}` |
| POST | `/api/mock-rules/{id}/enabled` | Enable or disable a rule: `{enabled}` |
| DELETE | `/api/mock-rules/{id}` | Delete a rule |
| POST | `/api/transactions/{id}/mock` | Create a rule from a captured transaction: `{status?, body?}` |
| GET | `/api/transactions/{id}/curl` | cURL command: `{command, redactedHeaders, bodyOmitted}` |
| POST | `/api/transactions/{id}/replay` | Re-send a request: `{url?, headers?, body?}`, returns `{transactionId, method, url, code, message, durationMs, error}` |

### Diagnostics

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/crashes` | List crashes (`?limit=&offset=`) |
| GET | `/api/crashes/{id}` | Crash details with stack trace |
| GET | `/api/logs` | Entries from the in-app capture (`?level=&tag=&limit=&offset=`); empty unless capture was started in the app |
| GET | `/api/leaks` | Memory leaks (`?limit=&offset=`) |
| GET | `/api/violations` | StrictMode violations (`?limit=&offset=`) |
| GET | `/api/device-info` | Device and app information |
| GET | `/api/timeline` | Merged event timeline (`?since_ms=&until_ms=&limit=`) |
| GET | `/api/wait/transaction` | Long-poll for the next new finished transaction (`?url_contains=&method=&status=&timeout_s=`); `data.transaction` is null on timeout |
| GET | `/api/wait/crash` | Long-poll for the next new crash (`?timeout_s=`); `data.crash` is null on timeout |

### Performance

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/cpu` | CPU stats (`?include_history=true`) |
| GET | `/api/memory` | Memory stats (`?include_history=true`) |
| GET | `/api/fps` | FPS stats (`?include_history=true`) |
| GET | `/api/performance` | Combined performance snapshot |
| POST | `/api/monitoring` | Start/stop a monitor: `{target: cpu|memory|fps, enabled}` returns `{target, running, note}` |

### Storage

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/preferences` | SharedPreferences files and entries |
| GET | `/api/databases` | SQLite database list |
| POST | `/api/databases/{name}/query` | Execute SELECT query |
| GET | `/api/files/browse` | Browse files (`?path=`) |
| GET | `/api/files/read` | Read file (`?path=`) |
| GET | `/api/secure-storage` | Secure storage keys and metadata (`?type=&limit=&offset=`) |

### Inspection

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dependencies` | App dependencies (`?category=`) |
| GET | `/api/loaded-libraries` | Loaded libraries (`?type=&system=`) |

### Actions

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/clear/transactions` | Clear transactions |
| POST | `/api/clear/crashes` | Clear crashes |
| POST | `/api/clear/logs` | Clear logs |
| POST | `/api/location` | Set mock location |
| DELETE | `/api/location` | Stop mock location |
| POST | `/api/push` | Send simulated notification |

### Streaming

| Protocol | Endpoint | Description |
|----------|----------|-------------|
| WebSocket | `/api/stream` | Real-time events: `transactions`, `crashes`, `logs`, `cpu`, `memory`, `fps` |

Subscribe by sending (send `"type": "unsubscribe"` to stop a channel):
```json
{ "type": "subscribe", "channels": ["transactions", "crashes", "logs", "cpu", "memory", "fps"] }
```

`transactions` and `crashes` emit `"event": "new"` once per new item; `logs` emits `"new"` per entry while the in-app log capture runs (open the Logs screen; the server doesn't start it); `cpu`, `memory` and `fps` emit `"update"` whenever the monitor publishes a reading (the monitors only sample while they are running: `set_monitoring`, the performance overlay or the tool's screen). The bridge doesn't use the stream; it is for scripts and custom clients.

Events arrive as:
```json
{ "channel": "cpu", "event": "update", "timestamp": 1774224835921, "data": { ... } }
```

## Security

The MCP server is designed for **debug builds only**. It should never be included in release builds.

### Authentication

**Auth is off by default.** With it off, anything that can reach port 8999 on the device can read every captured request and response body, crash, log line, SharedPreferences value, database row and app file, and can run the action tools. The server binds to `127.0.0.1` and rejects browser requests, so in practice that means:

- other apps on the same device (any app can connect to `127.0.0.1:8999`)
- anyone with `adb` access to the device, and any local process on your computer while `adb forward` is active

That is acceptable on a personal emulator or test device. Turn auth on for shared devices, device farms, or apps that handle real user data:

```kotlin
WormaCeptorApi.configureMcpServer(
    McpConfig(enableAuth = true, authToken = "your-secret-token"),
)
```

Then pass `--token your-secret-token` to the bridge CLI. If `enableAuth` is true but `authToken` is blank, the server refuses to start rather than run unprotected.

Without a token the server answers `401`, with a wrong token `403`. The health endpoint (`/api/health`) always bypasses authentication so the bridge can probe it.

### Header Redaction

Sensitive HTTP headers are automatically redacted in API responses. The following headers have their values replaced with `[REDACTED]`:

- `Authorization`
- `Cookie`
- `Set-Cookie`
- `Proxy-Authorization`
- `X-Api-Key`
- `X-Auth-Token`

### Secret Redaction

With `McpConfig.redactSecrets` (on by default) the server replaces values with `[REDACTED]` when their key contains `token`, `secret`, `password`, `passwd`, `passphrase`, `apikey`, `api_key`, `auth`, `credential`, `private_key` or `session` (case-insensitive):

- `list_preferences`: by preference key; JSON stored in a preference value is scanned too
- `read_file`: `"key": "value"` pairs in any text file, and SharedPreferences XML entries (`<string name="key">`, `name="key" value="..."`)

It is best effort: a secret under an unrelated key, or in a format other than these, is sent as-is. Turn it off with `McpConfig(redactSecrets = false)` when you need the raw values.

### Input Validation

The bridge validates tool inputs before forwarding to the device, and the server checks again:

- **Path traversal**: file paths and database names containing `..` are rejected by the bridge; the server only resolves paths inside the app's own storage directories
- **SQL injection** — only `SELECT` queries are allowed; `DROP`, `DELETE`, `INSERT`, `UPDATE`, `ALTER` are blocked
- **Parameter ranges** — latitude (-90..90), longitude (-180..180) are bounds-checked
- **Required fields** — IDs, titles, and bodies are checked for presence and non-emptiness

### Network Exposure

The device server binds to `127.0.0.1:8999` on the Android device. It is not exposed on the device's network interfaces; from your computer it is reachable only through ADB port forwarding.

## Configuration

`McpConfig` (package `com.azikar24.wormaceptor.domain.entities`), passed to `WormaCeptorApi.configureMcpServer`:

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `port` | Int | 8999 | HTTP server port |
| `enableAuth` | Boolean | false | Enable bearer token authentication |
| `authToken` | String? | null | Required token when auth is enabled |
| `maxBodySize` | Long | 1,048,576 (1 MB) | Max characters of a body returned by the body endpoints |
| `enabled` | Boolean | true | Auto-start the server; `false` stops a running one |
| `redactSecrets` | Boolean | true | Mask values under secret-looking keys in `list_preferences` and `read_file` (see Secret Redaction) |

## Module Structure

```
mcp/
├── bridge/                          # Host-side JVM CLI
│   ├── build.gradle.kts
│   └── src/main/kotlin/.../bridge/
│       ├── Main.kt                  # Entry point
│       ├── config/
│       │   └── BridgeConfig.kt      # CLI argument parsing
│       ├── adb/
│       │   ├── AdbClient.kt         # ADB command runner
│       │   └── DeviceDiscovery.kt   # Device selection
│       ├── device/
│       │   ├── DeviceConnection.kt  # Connection + reconnection
│       │   ├── DeviceApiClient.kt   # HTTP client to device
│       │   └── ConnectionState.kt   # Connection lifecycle
│       ├── mcp/
│       │   ├── McpServer.kt         # JSON-RPC dispatcher
│       │   ├── McpProtocol.kt       # Protocol constants
│       │   └── McpCapabilities.kt   # Initialize response
│       ├── mcp/tools/
│       │   ├── McpTool.kt           # Abstract tool base
│       │   ├── ToolRegistry.kt      # Tool registration
│       │   ├── NetworkTools.kt      # 8 network tools
│       │   ├── DiagnosticTools.kt   # 6 diagnostic tools
│       │   ├── PerformanceTools.kt  # 4 performance tools
│       │   ├── StorageTools.kt      # 8 storage tools
│       │   ├── ActionTools.kt       # 6 action tools
│       │   ├── GetTimelineTool.kt   # Event timeline
│       │   ├── MockTools.kt         # 5 mock rule tools
│       │   ├── ReplayTools.kt       # export_curl, replay_transaction
│       │   └── WaitTools.kt         # wait_for_transaction, wait_for_crash
│       └── util/
│           ├── TextFormatter.kt     # Human-readable output
│           └── JsonRpc.kt           # JSON-RPC data classes
│
└── device-server/                   # Android-side embedded server
    ├── build.gradle.kts
    ├── src/main/AndroidManifest.xml
    └── src/main/java/.../server/
        ├── WormaCeptorServer.kt         # Ktor server setup
        ├── WormaCeptorServerInitializer.kt  # ContentProvider auto-start
        ├── ServerConfig.kt              # Internal config built from McpConfig
        ├── middleware/
        │   ├── AuthPlugin.kt            # Bearer token auth
        │   ├── LocalRequestGuard.kt     # Rejects browser / non-local requests
        │   └── RedactionPlugin.kt       # Header redaction helper
        ├── security/
        │   └── FilePathGuard.kt         # Keeps file paths inside app storage
        ├── routes/
        │   ├── HealthRoutes.kt
        │   ├── TransactionRoutes.kt
        │   ├── CrashRoutes.kt
        │   ├── LogRoutes.kt
        │   ├── PerformanceRoutes.kt
        │   ├── DiagnosticRoutes.kt
        │   ├── StorageRoutes.kt
        │   ├── NetworkRoutes.kt
        │   ├── InspectionRoutes.kt
        │   ├── ActionRoutes.kt
        │   ├── MockRoutes.kt
        │   ├── ReplayRoutes.kt
        │   ├── TimelineRoutes.kt
        │   ├── WaitRoutes.kt
        │   └── DeviceInfoCollector.kt
        ├── streaming/
        │   ├── StreamEvent.kt
        │   ├── EventStreamManager.kt
        │   ├── EngineCollector.kt
        │   └── StreamRoutes.kt
        └── serialization/
            ├── JsonConfig.kt
            ├── EntityMappers.kt
            └── dto/                     # 12 DTO files
```

## Troubleshooting

**Bridge can't find device**
- Run `adb devices` and confirm your device shows as `device` (not `offline` or `unauthorized`)
- If multiple devices are connected, use `--device <serial>` or `-s <serial>`

**Connection refused / timeout**
- Make sure the app is running in the foreground
- Check logcat: `adb logcat -s WormaCeptorMCP WormaCeptorServer`
- Verify port forwarding: `adb forward tcp:8999 tcp:8999 && curl http://localhost:8999/api/health`

**Server not starting**
- Check that `wormaceptor-mcp-server` is included as `debugImplementation`
- Look for `WormaCeptorServer` in logcat: a port clash or `enableAuth` without a token is logged there
- Tools answering "engine not available" mean `WormaCeptorApi.init()` hasn't run yet

**`simulate_location` says mock locations aren't enabled**
- Select the app under Developer options > Select mock location app, or run `adb shell appops set <package> android:mock_location allow`

**Auth failures**
- Ensure `McpConfig(enableAuth = true, authToken = ...)` is set on the device side
- Pass the same token with `--token` on the bridge side
- The health endpoint (`/api/health`) always bypasses auth

**"The app is frozen in the background" / "The app isn't running"**
- Android froze or killed the app after another app took the foreground; every request then times out
- Bring it back with the `bring_app_to_front` tool (or tap the app), then retry the call
- The bridge learns the package from the app's first successful health check; before that, pass `package` to `bring_app_to_front`

**Bridge reconnection exhausted**
- A failing tool call retries the port forward 4 times, then returns the connection error
- The bridge keeps running; the next tool call tries again, so reconnecting the device or relaunching the app is enough
