package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class NrqlResponse(
    val data: Data? = null,
    val errors: List<NrqlError>? = null
) {
    @Serializable
    data class Data(
        val actor: Actor
    )

    @Serializable
    data class Actor(
        val account: Account
    )

    @Serializable
    data class Account(
        val nrql: Nrql
    )

    @Serializable
    data class Nrql(
        val results: List<JsonElement>
    )

    @Serializable
    data class NrqlError(
        val message: String,
        val locations: List<ErrorLocation>? = null
    )

    @Serializable
    data class ErrorLocation(
        val line: Int,
        val column: Int
    )

    fun getResults(): List<JsonElement> {
        return data?.actor?.account?.nrql?.results ?: emptyList()
    }

    fun hasErrors(): Boolean = !errors.isNullOrEmpty()

    fun getErrorMessage(): String? {
        return errors?.joinToString("; ") { it.message }
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }

        fun formatResults(results: List<JsonElement>): String {
            return json.encodeToString(kotlinx.serialization.builtins.ListSerializer(JsonElement.serializer()), results)
        }
    }
}

fun NrqlResponse.toJsonString(): String {
    val json = Json { prettyPrint = true }
    return json.encodeToString(NrqlResponse.serializer(), this)
}

fun NrqlResponse.resultsToJson(): String {
    return NrqlResponse.formatResults(getResults())
}