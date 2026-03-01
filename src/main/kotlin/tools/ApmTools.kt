package tools

import core.NewRelicClient
import core.NrqlBuilder

object ApmTools {

    /**
     * Lists all monitored applications in the New Relic account.
     *
     * @return JSON string with unique application names
     */
    suspend fun listApplications(): String {
        val query = NrqlBuilder.listApplications()
        return NewRelicClient.executeNrqlFormatted(query)
    }

    /**
     * Gets health metrics for a specific service including response time,
     * throughput, and error rate.
     *
     * @param service The name of the service/application to query
     * @param duration The time window size (default: 10)
     * @param unit The time unit - "minutes", "hours", "days", "weeks" (default: "minutes")
     * @return JSON string with health metrics
     */
    suspend fun serviceHealth(
        service: String,
        duration: Int? = 10,
        unit: String? = "minutes"
    ): String {
        val query = NrqlBuilder.serviceHealth(service, duration, unit)
        return NewRelicClient.executeNrqlFormatted(query)
    }

    /**
     * Gets the count of errors for a service, grouped by error message.
     *
     * @param service The name of the service/application to query
     * @param duration The time window size (default: 10)
     * @param unit The time unit - "minutes", "hours", "days", "weeks" (default: "minutes")
     * @return JSON string with error counts by message
     */
    suspend fun errorsCount(
        service: String,
        duration: Int? = 10,
        unit: String? = "minutes"
    ): String {
        val query = NrqlBuilder.errorsCount(service, duration, unit)
        return NewRelicClient.executeNrqlFormatted(query)
    }

    /**
     * Gets recent errors with details for a specific service.
     *
     * @param service The name of the service/application to query
     * @param duration The time window size (default: 10)
     * @param unit The time unit - "minutes", "hours", "days", "weeks" (default: "minutes")
     * @param limit Maximum number of errors to return (default: 10, max: 100)
     * @return JSON string with error details
     */
    suspend fun recentErrors(
        service: String,
        duration: Int? = 10,
        unit: String? = "minutes",
        limit: Int? = 10
    ): String {
        val query = NrqlBuilder.recentErrors(service, duration, unit, limit)
        return NewRelicClient.executeNrqlFormatted(query)
    }

    /**
     * Gets the Apdex score and transaction breakdown for a service.
     * Apdex is a measure of user satisfaction based on response times:
     * - Satisfied: response time <= 0.5s
     * - Tolerating: response time between 0.5s and 2.0s
     * - Frustrated: response time > 2.0s
     *
     * @param service The name of the service/application to query
     * @param duration The time window size (default: 10)
     * @param unit The time unit - "minutes", "hours", "days", "weeks" (default: "minutes")
     * @return JSON string with Apdex score and breakdown
     */
    suspend fun apdexScore(
        service: String,
        duration: Int? = 10,
        unit: String? = "minutes"
    ): String {
        val query = NrqlBuilder.apdexScore(service, duration, unit)
        return NewRelicClient.executeNrqlFormatted(query)
    }
}