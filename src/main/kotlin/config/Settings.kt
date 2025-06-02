package config

import io.github.cdimascio.dotenv.dotenv

object Settings {
    private val dotenv = dotenv()
    val apiKey = dotenv["NEW_RELIC_API_KEY"] ?: error("Missing API key")
    val accountId = dotenv["NEW_RELIC_ACCOUNT_ID"] ?: error("Missing account ID")
    val apiUrl = dotenv["NEW_RELIC_API_URL"] ?: "https://api.newrelic.com/graphql"
}
