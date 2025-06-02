package core

import config.Settings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*

object NewRelicClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun <T> runQuery(nrqlQuery: String, serializer: KSerializer<T>): T {
        val payload = buildGraphQLPayload(nrqlQuery)
        val payloadEncoded = Json.encodeToString(payload)
        val response = client.post(Settings.apiUrl) {
            headers {
                append("API-Key", Settings.apiKey)
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }

            setBody(payloadEncoded)
        }

        return Json.decodeFromString(serializer, response.body())
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
