import io.ktor.server.plugins.*
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.*
import model.toJsonString
import tools.errorsCount

fun main() = runBlocking {
    val server = Server(
        Implementation(
            name = "observability",
            version = "1.0.0"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
        )
    )

    server.addTool(
        name = "errors_count",
        description = "Returns the number of errors for a given service in the specified time window.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("service") {  put("type", "string") }
                putJsonObject("duration") {  put("type", "number") }
                putJsonObject("unit") { put("type", "string") }
            },
            required = listOf("service")
        ),
        handler = { req ->
            val service = req.arguments["service"]?.jsonPrimitive?.content
                ?: throw BadRequestException("Wrong service input")
            val duration = req.arguments["duration"]?.jsonPrimitive?.intOrNull
            val unit = req.arguments["unit"]?.jsonPrimitive?.content
            val result = errorsCount(service, duration, unit)

            CallToolResult(content = listOf(TextContent(result.toJsonString())))
        }
    )

    val transport = StdioServerTransport(
        System.`in`.asInput(),
        System.out.asSink().buffered()
    )

    val done = Job()
    server.onClose { done.complete() }
    server.connect(transport)
    done.join()
}