package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ErrorsCount(
    val data: Data
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
        val results: List<Result>
    )

    @Serializable
    data class Result(
        val facet: String? = null,
        val Error: String? = null,
        val count: Int? = null
    )
}

fun ErrorsCount.toJsonString() = Json.encodeToString(this)
