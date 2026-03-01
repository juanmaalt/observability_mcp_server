# New Relic Observability MCP Server

A Kotlin-based MCP server that exposes New Relic APM and Logs data as tools callable by any MCP-compatible client.

Pass a service name and time window, get back structured observability data — error counts, health metrics, Apdex scores, recent errors, and log search results — in JSON.

---

## What is MCP?

The [Model Context Protocol](https://modelcontextprotocol.io) is an open standard that lets AI assistants call external tools. This server exposes New Relic observability data as MCP tools, so you can ask Claude things like *"show me the error count for my-service in the last hour"* or *"what's the Apdex score for checkout-service this week?"* and get back real computed results.

---

## Features

### APM Tools

- **list_applications** — Lists all monitored applications in your New Relic account
- **errors_count** — Returns error counts grouped by error message
- **service_health** — Gets health metrics: response time, p95, throughput, error rate
- **recent_errors** — Retrieves recent error details with timestamps and stack traces
- **apdex_score** — Gets the Application Performance Index score with breakdown

### Logs Tools

- **search_logs** — Searches logs by keyword with optional service/level filters

---

## Prerequisites

- Java 17 or higher
- Gradle 8.x
- New Relic account with API access

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/observability_mcp_server.git
cd observability_mcp_server
```

### 2. Configure environment variables

Create a `.env` file in the project root:

```env
NEW_RELIC_API_KEY=your-api-key-here
NEW_RELIC_ACCOUNT_ID=your-account-id
```

### 3. Build the project

```bash
./gradlew build
```

This creates a fat JAR at `build/libs/observability_mcp_server-all.jar`

---

## Connecting to an MCP Client

### Claude Desktop

Add the following to your `claude_desktop_config.json`
(usually at `~/Library/Application Support/Claude/claude_desktop_config.json` on macOS):

```json
{
  "mcpServers": {
    "observability": {
      "command": "java",
      "args": ["-jar", "/path/to/observability_mcp_server-all.jar"]
    }
  }
}
```

### Cursor / VS Code (MCP extension)

```json
{
  "mcp": {
    "servers": {
      "observability": {
        "type": "stdio",
        "command": "java",
        "args": ["-jar", "/path/to/observability_mcp_server-all.jar"]
      }
    }
  }
}
```

Replace `/path/to/observability_mcp_server-all.jar` with the absolute path to your built JAR. Restart the client after saving the configuration.

---

## Available Tools

### `list_applications`

Lists all monitored applications in your New Relic account. No parameters required.

**Example output:**

```json
[
  { "appName": "checkout-service" },
  { "appName": "payment-api" },
  { "appName": "user-service" }
]
```

---

### `service_health`

Gets health metrics for a specific service over a given time window: average response time, p95 response time, throughput, and error rate.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `service` | `String` | required | Name of the service/application to query |
| `duration` | `Int` | `10` | Time window size |
| `unit` | `String` | `"minutes"` | Time unit: `"minutes"`, `"hours"`, `"days"`, `"weeks"` |

**Example output:**

```json
[
  {
    "avgResponseTime": 142.3,
    "p95ResponseTime": 530.1,
    "throughput": 87.4,
    "errorRate": 0.012
  }
]
```

---

### `errors_count`

Returns error counts grouped by error message for a given service and time window. Useful for identifying the most frequent failure modes.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `service` | `String` | required | Name of the service/application to query |
| `duration` | `Int` | `10` | Time window size |
| `unit` | `String` | `"minutes"` | Time unit: `"minutes"`, `"hours"`, `"days"`, `"weeks"` |

**Example output:**

```json
[
  { "error.message": "Connection timeout", "count": 45 },
  { "error.message": "NullPointerException at PaymentService.kt:82", "count": 12 },
  { "error.message": "HTTP 503 from upstream", "count": 7 }
]
```

---

### `recent_errors`

Retrieves individual error events with timestamps, messages, and stack traces. Useful for debugging a specific incident.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `service` | `String` | required | Name of the service/application to query |
| `duration` | `Int` | `10` | Time window size |
| `unit` | `String` | `"minutes"` | Time unit: `"minutes"`, `"hours"`, `"days"`, `"weeks"` |
| `limit` | `Int` | `10` | Maximum number of errors to return (max: 100) |

**Example output:**

```json
[
  {
    "timestamp": 1718200800000,
    "error.message": "Connection timeout",
    "error.class": "java.net.SocketTimeoutException",
    "error.stack": "at com.example.PaymentService.call(PaymentService.kt:82)\n  at ..."
  }
]
```

---

### `apdex_score`

Gets the Apdex (Application Performance Index) score for a service, along with a breakdown of satisfied, tolerating, and frustrated transactions.

Apdex thresholds used:
- **Satisfied**: response time ≤ 0.5s
- **Tolerating**: response time between 0.5s and 2.0s
- **Frustrated**: response time > 2.0s

| Parameter | Type | Default | Description |
|---|---|---|---|
| `service` | `String` | required | Name of the service/application to query |
| `duration` | `Int` | `10` | Time window size |
| `unit` | `String` | `"minutes"` | Time unit: `"minutes"`, `"hours"`, `"days"`, `"weeks"` |

**Example output:**

```json
[
  {
    "apdex": 0.87,
    "satisfied": 1240,
    "tolerating": 180,
    "frustrated": 32
  }
]
```

---

### `search_logs`

Searches log entries by keyword or pattern, with optional filters for service name and log level.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `query` | `String` | required | Keyword or pattern to search for in log messages |
| `service` | `String` | `null` | Optional service name to filter logs |
| `level` | `String` | `null` | Optional log level: `"ERROR"`, `"WARN"`, `"INFO"`, `"DEBUG"` |
| `duration` | `Int` | `10` | Time window size |
| `unit` | `String` | `"minutes"` | Time unit: `"minutes"`, `"hours"`, `"days"`, `"weeks"` |
| `limit` | `Int` | `50` | Maximum number of log entries to return (max: 200) |

**Example output:**

```json
[
  {
    "timestamp": 1718200800000,
    "message": "Connection timeout after 30s calling payment gateway",
    "level": "ERROR",
    "service.name": "checkout-service"
  },
  {
    "timestamp": 1718200750000,
    "message": "Retrying timeout request (attempt 2/3)",
    "level": "WARN",
    "service.name": "checkout-service"
  }
]
```

---

## Composing Tools

Tools are designed to be used together. Some useful patterns:

```
# Discover services, then inspect one
list_applications → service_health (service="checkout-service")

# Find the most common errors, then drill into details
errors_count → recent_errors (same service, narrower window)

# Correlate a health degradation with logs
service_health → search_logs (query="timeout", level="ERROR")

# Investigate an incident end-to-end
service_health → errors_count → recent_errors → search_logs
```

---

## Example Prompts

Once configured, you can ask Claude:

- "List all my monitored applications in New Relic"
- "Show me the error count for my-service in the last hour"
- "What's the health of my-api service over the past day?"
- "Find recent errors in payment-service"
- "What's the Apdex score for checkout-service this week?"
- "Search logs for 'timeout' errors in the last 30 minutes"

---

## Project Structure

```
src/main/kotlin/
├── App.kt                      # MCP server entry point
├── config/
│   └── Settings.kt             # Configuration management
├── core/
│   ├── NewRelicClient.kt       # HTTP client for New Relic GraphQL API
│   └── NrqlBuilder.kt          # NRQL query builder with sanitization
├── model/
│   └── NrqlResponse.kt         # Generic response model
└── tools/
    ├── ApmTools.kt             # APM-related tool implementations
    └── LogsTools.kt            # Logs-related tool implementations
```

---

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Claude/AI      │────▶│  MCP Server     │────▶│  New Relic      │
│  Assistant      │◀────│  (this project) │◀────│  GraphQL API    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │
        │   MCP Protocol        │   NRQL Queries
        │   (stdio)             │   (HTTPS)
```

---

## Tech Stack

- **Kotlin** 2.1.x
- **Ktor** 3.0.1 — HTTP client
- **MCP Kotlin SDK** 0.5.0 — Model Context Protocol implementation
- **Kotlinx Serialization** — JSON parsing
- **Gradle** with Shadow plugin — Build and packaging

---

## Security

- Input sanitization prevents NRQL injection attacks
- API keys should be stored in environment variables
- Never commit `.env` files to version control
