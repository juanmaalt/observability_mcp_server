import exception.NewRelicApiException
import exception.NrqlQueryException
import io.ktor.utils.io.streams.asInput
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import tools.ApmTools
import tools.LogsTools

fun main(): Unit = runBlocking {
    val server = Server(
        Implementation(
            name = "observability",
            version = "1.0.0"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
        )
    )

    registerApmTools(server)
    registerLogsTools(server)

    val transport = StdioServerTransport(
        System.`in`.asInput(),
        System.out.asSink().buffered()
    )

    val done = Job()
    server.onClose { done.complete() }
    server.connect(transport)
    done.join()
}


private fun registerApmTools(server: Server) {
    server.addTool(
        name = "list_applications",
        description = "Lists all monitored applications in the New Relic account. " +
                "Returns unique application names from recent transaction data.",
        inputSchema = Tool.Input(
            properties = buildJsonObject { },
            required = emptyList()
        ),
        handler = { _ ->
            executeToolSafely {
                ApmTools.listApplications()
            }
        }
    )

    server.addTool(
        name = "errors_count",
        description = "Returns the count of errors for a given service, grouped by error message. " +
                "Useful for identifying the most common errors in your application.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("service") {
                    put("type", "string")
                    put("description", "The name of the service/application to query")
                }
                putJsonObject("duration") {
                    put("type", "number")
                    put("description", "Time window size (default: 10)")
                }
                putJsonObject("unit") {
                    put("type", "string")
                    put("description", "Time unit: 'minutes', 'hours', 'days', or 'weeks' (default: 'minutes')")
                    put("enum", buildJsonArray {
                        add("minutes")
                        add("hours")
                        add("days")
                        add("weeks")
                    })
                }
            },
            required = listOf("service")
        ),
        handler = { req ->
            executeToolSafely {
                val service = req.arguments["service"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Missing required parameter: service")
                val duration = req.arguments["duration"]?.jsonPrimitive?.intOrNull
                val unit = req.arguments["unit"]?.jsonPrimitive?.content
                ApmTools.errorsCount(service, duration, unit)
            }
        }
    )

    server.addTool(
        name = "service_health",
        description = "Gets health metrics for a service including average response time, " +
                "95th percentile response time, throughput (requests count), and error rate percentage.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("service") {
                    put("type", "string")
                    put("description", "The name of the service/application to query")
                }
                putJsonObject("duration") {
                    put("type", "number")
                    put("description", "Time window size (default: 10)")
                }
                putJsonObject("unit") {
                    put("type", "string")
                    put("description", "Time unit: 'minutes', 'hours', 'days', or 'weeks' (default: 'minutes')")
                    put("enum", buildJsonArray {
                        add("minutes")
                        add("hours")
                        add("days")
                        add("weeks")
                    })
                }
            },
            required = listOf("service")
        ),
        handler = { req ->
            executeToolSafely {
                val service = req.arguments["service"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Missing required parameter: service")
                val duration = req.arguments["duration"]?.jsonPrimitive?.intOrNull
                val unit = req.arguments["unit"]?.jsonPrimitive?.content
                ApmTools.serviceHealth(service, duration, unit)
            }
        }
    )

    server.addTool(
        name = "recent_errors",
        description = "Gets recent error details for a service including timestamp, error message, " +
                "error class, and the transaction where the error occurred.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("service") {
                    put("type", "string")
                    put("description", "The name of the service/application to query")
                }
                putJsonObject("duration") {
                    put("type", "number")
                    put("description", "Time window size (default: 10)")
                }
                putJsonObject("unit") {
                    put("type", "string")
                    put("description", "Time unit: 'minutes', 'hours', 'days', or 'weeks' (default: 'minutes')")
                    put("enum", buildJsonArray {
                        add("minutes")
                        add("hours")
                        add("days")
                        add("weeks")
                    })
                }
                putJsonObject("limit") {
                    put("type", "number")
                    put("description", "Maximum number of errors to return (default: 10, max: 100)")
                }
            },
            required = listOf("service")
        ),
        handler = { req ->
            executeToolSafely {
                val service = req.arguments["service"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Missing required parameter: service")
                val duration = req.arguments["duration"]?.jsonPrimitive?.intOrNull
                val unit = req.arguments["unit"]?.jsonPrimitive?.content
                val limit = req.arguments["limit"]?.jsonPrimitive?.intOrNull
                ApmTools.recentErrors(service, duration, unit, limit)
            }
        }
    )

    server.addTool(
        name = "apdex_score",
        description = "Gets the Apdex (Application Performance Index) score for a service. " +
                "Apdex measures user satisfaction based on response times: " +
                "Satisfied (<=0.5s), Tolerating (0.5-2.0s), Frustrated (>2.0s). " +
                "Returns apdex score (0-1) and transaction breakdown.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("service") {
                    put("type", "string")
                    put("description", "The name of the service/application to query")
                }
                putJsonObject("duration") {
                    put("type", "number")
                    put("description", "Time window size (default: 10)")
                }
                putJsonObject("unit") {
                    put("type", "string")
                    put("description", "Time unit: 'minutes', 'hours', 'days', or 'weeks' (default: 'minutes')")
                    put("enum", buildJsonArray {
                        add("minutes")
                        add("hours")
                        add("days")
                        add("weeks")
                    })
                }
            },
            required = listOf("service")
        ),
        handler = { req ->
            executeToolSafely {
                val service = req.arguments["service"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Missing required parameter: service")
                val duration = req.arguments["duration"]?.jsonPrimitive?.intOrNull
                val unit = req.arguments["unit"]?.jsonPrimitive?.content
                ApmTools.apdexScore(service, duration, unit)
            }
        }
    )
}

private fun registerLogsTools(server: Server) {
    server.addTool(
        name = "search_logs",
        description = "Searches logs by keyword or pattern. " +
                "Can filter by service name and log level (ERROR, WARN, INFO, DEBUG). " +
                "Returns matching log entries with timestamp, message, level, and service.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("query") {
                    put("type", "string")
                    put("description", "The search query/pattern to look for in log messages")
                }
                putJsonObject("service") {
                    put("type", "string")
                    put("description", "Optional: Filter logs by service name")
                }
                putJsonObject("level") {
                    put("type", "string")
                    put("description", "Optional: Filter by log level")
                    put("enum", buildJsonArray {
                        add("ERROR")
                        add("WARN")
                        add("INFO")
                        add("DEBUG")
                    })
                }
                putJsonObject("duration") {
                    put("type", "number")
                    put("description", "Time window size (default: 10)")
                }
                putJsonObject("unit") {
                    put("type", "string")
                    put("description", "Time unit: 'minutes', 'hours', 'days', or 'weeks' (default: 'minutes')")
                    put("enum", buildJsonArray {
                        add("minutes")
                        add("hours")
                        add("days")
                        add("weeks")
                    })
                }
                putJsonObject("limit") {
                    put("type", "number")
                    put("description", "Maximum number of log entries to return (default: 50, max: 200)")
                }
            },
            required = listOf("query")
        ),
        handler = { req ->
            executeToolSafely {
                val query = req.arguments["query"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Missing required parameter: query")
                val service = req.arguments["service"]?.jsonPrimitive?.content
                val level = req.arguments["level"]?.jsonPrimitive?.content
                val duration = req.arguments["duration"]?.jsonPrimitive?.intOrNull
                val unit = req.arguments["unit"]?.jsonPrimitive?.content
                val limit = req.arguments["limit"]?.jsonPrimitive?.intOrNull
                LogsTools.searchLogs(query, service, level, duration, unit, limit)
            }
        }
    )
}

private suspend fun executeToolSafely(block: suspend () -> String): CallToolResult {
    return try {
        val result = block()
        CallToolResult(content = listOf(TextContent(result)))
    } catch (e: IllegalArgumentException) {
        CallToolResult(
            content = listOf(TextContent("Invalid input: ${e.message}")),
            isError = true
        )
    } catch (e: NrqlQueryException) {
        CallToolResult(
            content = listOf(TextContent("Query error: ${e.message}")),
            isError = true
        )
    } catch (e: NewRelicApiException) {
        CallToolResult(
            content = listOf(TextContent("API error: ${e.message}")),
            isError = true
        )
    } catch (e: Exception) {
        CallToolResult(
            content = listOf(TextContent("Unexpected error: ${e.message}")),
            isError = true
        )
    }
}