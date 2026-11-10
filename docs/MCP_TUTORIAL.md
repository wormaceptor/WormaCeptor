# Using WormaCeptor MCP with AI Agents

A hands-on guide to connecting your Android app's runtime data to AI tools like Claude Code, Cursor, and other MCP-compatible clients.

## What You'll Get

Once set up, you can ask your AI agent things like:

- "Show me the last 5 network requests"
- "Why did the app crash?"
- "What's in the user's SharedPreferences?"
- "Is there a memory leak?"
- "What's the current CPU and memory usage?"

The AI reads live data from your running app — no copy-pasting logs or screenshots.

---

## Part 1: Setup

### Step 1: Add the dependency

In your app's `build.gradle.kts`:

<!-- x-release-please-start-version -->
```kotlin
dependencies {
    // Your existing WormaCeptor dependencies
    implementation("com.azikar24.wormaceptor:wormaceptor-client:3.4.3")
    debugImplementation("com.azikar24.wormaceptor:wormaceptor-persistence:3.4.3")

    // Add the MCP server (debug only, zero code in release)
    debugImplementation("com.azikar24.wormaceptor:wormaceptor-mcp-server:3.4.3")
}
```
<!-- x-release-please-end -->

That's it. The server auto-starts when your debug app launches. No code changes.

### Step 2: Build the bridge

The bridge is a small CLI that sits between your AI tool and the device. Build it once:

```bash
cd /path/to/WormaCeptor
./gradlew :mcp:bridge:jar
```

This creates `mcp/bridge/build/libs/bridge.jar`.

### Step 3: Test the connection

Make sure your emulator or device is running the app:

```bash
# Check device is connected
adb devices
# Should show something like:
# emulator-5554    device

# Run the bridge manually to verify
java -jar mcp/bridge/build/libs/bridge.jar --verbose
```

You should see:

<!-- x-release-please-start-version -->
```
WormaCeptor MCP Bridge v3.4.3
Config: port=8999, device=auto
Found device: emulator-5554 (sdk_gphone64_arm64)
Port forwarded: localhost:8999 -> device:8999
Connected to WormaCeptor server
MCP server ready. Listening for requests on stdin...
```
<!-- x-release-please-end -->

If the app isn't running yet you'll see a `Warning: Could not connect...` line instead of `Connected`; the bridge keeps running and connects on the first tool call.

Press `Ctrl+C` to stop. The bridge works.

### Step 4: Configure your AI tool

#### Claude Code

```bash
claude mcp add --transport stdio wormaceptor -- java -jar /absolute/path/to/bridge.jar
```

To share the setup with your team, commit a `.mcp.json` at the project root instead:

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

Run `claude mcp get wormaceptor` to check that it connected.

#### Cursor

Add to `.cursor/mcp.json`:

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

#### Any MCP-compatible client

The bridge speaks standard MCP (JSON-RPC 2.0 on stdin/stdout). Point your client at:

```
command: java -jar /path/to/bridge.jar
```

---

## Part 2: Using It

### Debugging Network Issues

**"What requests is the app making?"**

```
Show me the recent network transactions
```

The AI calls `list_transactions` and returns a table of HTTP requests with method, URL, status code, and duration.

**"Why is this endpoint slow?"**

```
Find the slowest request and show its headers, request body, and response body
```

The AI calls `get_transaction`, `get_request_body`, and `get_response_body` to show you headers, timing, and payloads.

**"Show me all the failed requests"**

```
List transactions that returned a 500
```

The AI passes `query: "500"` to `list_transactions`, which matches URL, method, or status code.

**"What's going over the wire?"**

```
Show me the request body for the last POST request to /api/login
```

**"Are there any WebSocket connections?"**

```
List active WebSocket connections and their recent messages
```

### Investigating Crashes

**"Did the app crash?"**

```
List all crashes
```

**"What caused it?"**

```
Get the full stack trace for crash #1
```

**"Fix the crash"**

```
Get crash #1, find the source file that caused it, and suggest a fix
```

The AI reads the crash, finds `MainActivityViewModel.kt:108` in your codebase, reads the file, and proposes a fix — all in one flow.

### Monitoring Performance

**"Is the app laggy?"**

```
Turn on FPS monitoring, then get the FPS stats. Are there any dropped frames?
```

FPS only samples while its monitor runs; the AI starts it with `set_monitoring`.

**"How's memory?"**

```
Show me CPU, memory, and FPS as a performance snapshot
```

**"Is it getting worse?"**

```
Get memory stats with history. Is there a trend of increasing memory use?
```

### Inspecting Storage

**"What's stored locally?"**

```
List all SharedPreferences files and show me what's in them
```

**"What databases exist?"**

```
List databases, then show me the tables in the main database
```

**"Run a query"**

```
Query the transactions database: SELECT * FROM transactions ORDER BY date DESC LIMIT 10
```

**"What's in the app's files?"**

```
Browse the app's files directory and show me the contents of config.json
```

### Finding Memory Leaks

**"Any leaks?"**

```
List detected memory leaks
```

**"Any thread violations?"**

```
Show me StrictMode violations — are there any disk reads on the main thread?
```

### Taking Actions

**"Clean up test data"**

```
Clear all transactions and crash logs
```

**"Test location features"**

```
Set the device location to the Eiffel Tower (48.8584, 2.2945)
```

**"Test push notifications"**

```
Send a test push notification with title "Order Update" and body "Your order has shipped!"
```

### Watching a Flow Live

**"What happened when I tapped Login?"**

```
I just tapped Login and nothing happened. Show me the timeline of the last minute.
```

The AI calls `get_timeline` and gets requests, logs, crashes, StrictMode violations and WebSocket messages in one time-ordered list, each with the id to drill into.

**"Catch the request as it happens"**

```
Wait for the next POST to /api/login, then show me its response body. I'll tap the button now.
```

The AI calls `wait_for_transaction` with `url_contains: "/api/login"` and `method: "POST"`; it returns as soon as a new matching request finishes (30 s by default). `wait_for_crash` does the same for crashes.

### Mocking Responses

**"Force an error state"**

```
Make GET https://api.example.com/users return a 500 with {"error":"down"}, then I'll check the error screen.
```

The AI calls `create_mock_rule`. Mocks apply to OkHttp and Ktor clients that use the WormaCeptor interceptor or plugin; `list_mock_rules`, `set_mock_rule_enabled` and `delete_mock_rule` manage them afterwards.

**"Replay a real response with tweaks"**

```
Take the last /api/feed response, mock it with an empty items array, and slow it down by 3 seconds.
```

The AI calls `mock_from_transaction` with an edited `body` (or `create_mock_rule` with `delay_ms`).

### Reproducing Outside the App

```
Give me a curl command for the failed checkout request, then replay it against https://staging.example.com and compare.
```

The AI calls `export_curl` (sensitive headers stay `[REDACTED]`) and `replay_transaction` with a `url` override. The replay is sent from the device straight to the server, skipping mock rules and throttling, and shows up as a new transaction.

### Combining It All

The real power is combining tools in a single conversation:

```
The app crashed after making a network request. Find the crash, show me
the most recent transactions around the same time, check if there's a
memory issue, and suggest what went wrong.
```

The AI will:
1. Call `list_crashes` to find the crash
2. Call `get_crash` to read the stack trace
3. Call `list_transactions` to see recent network activity
4. Call `get_memory_stats` to check memory pressure
5. Cross-reference the timestamps and give you a diagnosis

---

## Part 3: Configuration Scenarios

### Default (zero config)

Nothing extra needed. The server starts on port 8999 with no auth.

```kotlin
// Application.kt
WormaCeptorApi.init(context = this)
```

Bridge:
```bash
java -jar bridge.jar
```

### Custom Port

Maybe port 8999 conflicts with something else on your machine.

**App side:**

```kotlin
WormaCeptorApi.configureMcpServer(McpConfig(port = 9090))
WormaCeptorApi.init(context = this)
```

**Bridge side:**

```bash
java -jar bridge.jar --port 9090
```

**MCP config:**

```json
{
  "mcpServers": {
    "wormaceptor": {
      "command": "java",
      "args": ["-jar", "/path/to/bridge.jar", "--port", "9090"]
    }
  }
}
```

### With Authentication

Prevent other apps on the device, and other local processes while `adb forward` is active, from reading the debug server.

**App side:**

```kotlin
WormaCeptorApi.configureMcpServer(
    McpConfig(
        enableAuth = true,
        authToken = "my-secret-debug-token"
    )
)
WormaCeptorApi.init(context = this)
```

**Bridge side:**

```bash
java -jar bridge.jar --token my-secret-debug-token
```

**MCP config:**

```json
{
  "mcpServers": {
    "wormaceptor": {
      "command": "java",
      "args": [
        "-jar", "/path/to/bridge.jar",
        "--token", "my-secret-debug-token"
      ]
    }
  }
}
```

### With Auth + Custom Port

```kotlin
WormaCeptorApi.configureMcpServer(
    McpConfig(
        port = 9090,
        enableAuth = true,
        authToken = "my-secret-debug-token"
    )
)
```

```json
{
  "mcpServers": {
    "wormaceptor": {
      "command": "java",
      "args": [
        "-jar", "/path/to/bridge.jar",
        "--port", "9090",
        "--token", "my-secret-debug-token"
      ]
    }
  }
}
```

### Larger Response Bodies

By default, transaction bodies are capped at 1 MB. For APIs with large payloads:

```kotlin
WormaCeptorApi.configureMcpServer(
    McpConfig(maxBodySize = 5_242_880L) // 5 MB
)
```

### Disable Auto-Start

If you only want the MCP server running during specific debug sessions:

```kotlin
WormaCeptorApi.configureMcpServer(McpConfig(enabled = false))
WormaCeptorApi.init(context = this)
```

Then start it from code when needed (the AI agent can't start it, since the bridge has nothing to talk to until it runs):

```kotlin
// From a debug menu button, for example:
WormaCeptorApi.startMcpServer()
```

### Multiple Devices

If you have multiple devices/emulators connected:

```bash
# List devices
adb devices
# emulator-5554    device
# emulator-5556    device

# Target a specific one
java -jar bridge.jar --device emulator-5556
```

**MCP config for a specific device:**

```json
{
  "mcpServers": {
    "wormaceptor": {
      "command": "java",
      "args": [
        "-jar", "/path/to/bridge.jar",
        "-s", "emulator-5556"
      ]
    }
  }
}
```

### Verbose Mode (Debugging the Bridge)

If things aren't working, run with verbose to see every request and response:

```bash
java -jar bridge.jar --verbose
```

This logs all MCP traffic to stderr:

```
<-- {"method":"tools/call","params":{"name":"list_crashes"}}
--> {"result":{"content":[{"type":"text","text":"1 crash(es):\n..."}]}}
```

**MCP config with verbose:**

```json
{
  "mcpServers": {
    "wormaceptor": {
      "command": "java",
      "args": ["-jar", "/path/to/bridge.jar", "--verbose"]
    }
  }
}
```

---

## Part 4: Tips

### Keep the app process alive

The MCP server runs inside the app process. When another app takes the foreground, Android may freeze or kill yours and every tool call times out. The bridge checks with adb and answers "The app is frozen in the background" or "The app isn't running"; ask the AI to call `bring_app_to_front` (or tap the app), and the next tool call reconnects.

### Bridge reconnects on demand

If you restart the app, the next tool call that fails to connect makes the bridge re-create the port forward and retry (4 attempts with backoff), then repeat the call. You don't need to restart the bridge.

### Sensitive data is redacted

Headers like `Authorization`, `Cookie`, and `X-Api-Key` are automatically replaced with `[REDACTED]` in API responses, on top of any `RedactionConfig` rules applied at capture time. `browse_secure_storage` returns key names only, never values. Preference values and file contents under secret-looking keys (`token`, `password`, `api_key`, `auth`, `session`, ...) are masked too (`McpConfig(redactSecrets = false)` turns that off). Bodies, databases, and everything else are sent as-is, so treat the conversation as containing app data.

### Auth is off by default

Any app on the device, and anything that can use `adb` on your machine, can read the server while auth is off. Fine on your own emulator; turn it on for shared devices (see "With Authentication" above).

### SQL queries are read-only

The `query_database` tool only allows `SELECT` statements. `DROP`, `DELETE`, `INSERT`, `UPDATE`, and `ALTER` are blocked at both the bridge and server level.

### File access is scoped

File browsing and reading is limited to the app's sandbox. Path traversal (`..`) is rejected.

### Release builds are safe

The `mcp-device-server` module is included as `debugImplementation`. It physically does not exist in your release APK. There is nothing to disable or guard.

---

## Quick Reference

### All Bridge CLI Flags

| Flag | Short | Default | Description |
|------|-------|---------|-------------|
| `--port` | | 8999 | Device server port |
| `--device` | `-s` | auto | Target device serial |
| `--adb` | | found automatically | Path to adb. Without it the bridge checks `$ANDROID_HOME`, `$ANDROID_SDK_ROOT`, PATH, then the default SDK dir |
| `--token` | | none | Bearer auth token |
| `--verbose` | `-v` | off | Log all requests/responses |
| `--version` | | | Print version |
| `--help` | `-h` | | Print help |

### All McpConfig Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `port` | Int | 8999 | Server port on device |
| `enableAuth` | Boolean | false | Require bearer token |
| `authToken` | String? | null | The token to require |
| `maxBodySize` | Long | 1,048,576 | Max body characters in responses |
| `enabled` | Boolean | true | Auto-start on app launch; `false` stops a running server |
| `redactSecrets` | Boolean | true | Mask preference and file values under secret-looking keys |

### All 44 MCP Tools

| Category | Tools |
|----------|-------|
| **Network** | `list_transactions`, `get_transaction`, `get_request_body`, `get_response_body`, `list_websocket_connections`, `list_websocket_messages`, `get_rate_limit`, `set_rate_limit` |
| **Diagnostics** | `list_crashes`, `get_crash`, `tail_logs`, `list_leaks`, `list_violations`, `get_device_info` |
| **Performance** | `get_cpu_stats`, `get_memory_stats`, `get_fps_stats`, `get_performance_snapshot`, `set_monitoring` |
| **Storage** | `list_preferences`, `list_databases`, `query_database`, `list_files`, `read_file`, `browse_secure_storage`, `list_dependencies`, `list_loaded_libraries` |
| **Actions** | `clear_transactions`, `clear_crashes`, `clear_logs`, `simulate_location`, `stop_location_simulation`, `send_push_notification`, `bring_app_to_front` |
| **Actions** | `clear_transactions`, `clear_crashes`, `clear_logs`, `simulate_location`, `stop_location_simulation`, `send_push_notification` |
| **Timeline and events** | `get_timeline`, `wait_for_transaction`, `wait_for_crash` |
| **Mock rules** | `list_mock_rules`, `create_mock_rule`, `mock_from_transaction`, `set_mock_rule_enabled`, `delete_mock_rule` |
| **Reproduce** | `export_curl`, `replay_transaction` |
