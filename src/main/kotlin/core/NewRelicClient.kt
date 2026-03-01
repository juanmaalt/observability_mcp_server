package core

import config.Settings
import exception.NewRelicApiException
import exception.NrqlQueryException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import model.NrqlResponse
import model.resultsToJson

object NewRelicClient {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(jsonParser)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    suspend fun executeNrql(nrqlQuery: String): NrqlResponse {
        try {
            val payload = buildGraphQLPayload(nrqlQuery)
            val payloadEncoded = Json.encodeToString(payload)

            val response = client.post(Settings.apiUrl) {
                headers {
                    append("API-Key", Settings.apiKey)
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                setBody(payloadEncoded)
            }

            val responseBody: String = response.body()
            val nrqlResponse = jsonParser.decodeFromString(NrqlResponse.serializer(), responseBody)

            if (nrqlResponse.hasErrors()) {
                throw NrqlQueryException("NRQL query failed: ${nrqlResponse.getErrorMessage()}")
            }

            return nrqlResponse
        } catch (e: NrqlQueryException) {
            throw e
        } catch (e: Exception) {
            throw NewRelicApiException("Failed to execute NRQL query: ${e.message}", e)
        }
    }

    suspend fun executeNrqlFormatted(nrqlQuery: String): String {
        val response = executeNrql(nrqlQuery)
        return response.resultsToJson()
    }

    @Deprecated("Use executeNrql() instead", ReplaceWith("executeNrql(nrqlQuery)"))
    suspend fun <T> runQuery(nrqlQuery: String, serializer: KSerializer<T>): T {
        try {
            val payload = buildGraphQLPayload(nrqlQuery)
            val payloadEncoded = Json.encodeToString(payload)
            val response = client.post(Settings.apiUrl) {
                headers {
                    append("API-Key", Settings.apiKey)
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                setBody(payloadEncoded)
            }

            return jsonParser.decodeFromString(serializer, response.body())
        } catch (e: Exception) {
            throw NewRelicApiException("Failed to execute NRQL query: ${e.message}", e)
        }
    }

    private fun buildGraphQLPayload(nrqlQuery: String): JsonObject =
        buildJsonObject {
            put("query", """
                {
                  actor {
                    account(id: ${Settings.accountId}) {
                      nrql(query: "$nrqlQuery") {
                        results
                      }
                    }
                  }
                }
            """.trimIndent())
        }
}
