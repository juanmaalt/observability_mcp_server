package tools

import core.NewRelicClient
import core.NrqlBuilder

object LogsTools {

    /**
     * Searches logs by keyword or pattern with optional filters.
     *
     * @param query The search query/pattern to look for in log messages
     * @param service Optional service name to filter logs
     * @param level Optional log level filter (e.g., "ERROR", "WARN", "INFO", "DEBUG")
     * @param duration The time window size (default: 10)
     * @param unit The time unit - "minutes", "hours", "days", "weeks" (default: "minutes")
     * @param limit Maximum number of log entries to return (default: 50, max: 200)
     * @return JSON string with matching log entries
     */
    suspend fun searchLogs(
        query: String,
        service: String? = null,
        level: String? = null,
        duration: Int? = 10,
        unit: String? = "minutes",
        limit: Int? = 50
    ): String {
        val nrqlQuery = NrqlBuilder.searchLogs(query, service, level, duration, unit, limit)
        return NewRelicClient.executeNrqlFormatted(nrqlQuery)
    }
}