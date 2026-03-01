package core


object NrqlBuilder {

    private val validTimeUnits = setOf("minutes", "hours", "days", "weeks")

    fun sanitize(input: String): String {
        return input
            .replace("'", "\\'")
            .replace("\\", "\\\\")
            .replace("\n", " ")
            .replace("\r", "")
    }

    fun validateTimeUnit(unit: String?): String {
        val normalized = unit?.lowercase() ?: "minutes"
        require(normalized in validTimeUnits) {
            "Invalid time unit: '$unit'. Valid units are: ${validTimeUnits.joinToString()}"
        }
        return normalized
    }

    fun validateDuration(duration: Int?): Int {
        val value = duration ?: 10
        require(value > 0) { "Duration must be positive, got: $value" }
        return value
    }

    fun timeWindow(duration: Int?, unit: String?): String {
        val validDuration = validateDuration(duration)
        val validUnit = validateTimeUnit(unit)
        return "SINCE $validDuration $validUnit ago"
    }

    fun listApplications(): String = "SELECT uniques(appName) FROM Transaction SINCE 1 day ago"

    fun errorsCount(service: String, duration: Int?, unit: String?): String {
        val sanitizedService = sanitize(service)
        return """
            SELECT count(*) FROM TransactionError
            WHERE appName = '$sanitizedService'
            FACET error.message as 'Error'
            ${timeWindow(duration, unit)}
        """.trimIndent().replace("\n", " ")
    }

    fun serviceHealth(service: String, duration: Int?, unit: String?): String {
        val sanitizedService = sanitize(service)
        return """
            SELECT average(duration) as 'avgResponseTime',
                   percentile(duration, 95) as 'p95ResponseTime',
                   count(*) as 'throughput',
                   percentage(count(*), WHERE error IS true) as 'errorRate'
            FROM Transaction
            WHERE appName = '$sanitizedService'
            ${timeWindow(duration, unit)}
        """.trimIndent().replace("\n", " ")
    }

    fun recentErrors(service: String, duration: Int?, unit: String?, limit: Int?): String {
        val sanitizedService = sanitize(service)
        val validLimit = (limit ?: 10).coerceIn(1, 100)
        return """
            SELECT timestamp, error.message, error.class, transactionName
            FROM TransactionError
            WHERE appName = '$sanitizedService'
            ${timeWindow(duration, unit)}
            LIMIT $validLimit
        """.trimIndent().replace("\n", " ")
    }

    fun apdexScore(service: String, duration: Int?, unit: String?): String {
        val sanitizedService = sanitize(service)
        return """
            SELECT apdex(duration, t: 0.5) as 'apdex',
                   count(*) as 'totalTransactions',
                   filter(count(*), WHERE duration <= 0.5) as 'satisfied',
                   filter(count(*), WHERE duration > 0.5 AND duration <= 2.0) as 'tolerating',
                   filter(count(*), WHERE duration > 2.0) as 'frustrated'
            FROM Transaction
            WHERE appName = '$sanitizedService'
            ${timeWindow(duration, unit)}
        """.trimIndent().replace("\n", " ")
    }

    fun searchLogs(
        query: String,
        service: String?,
        level: String?,
        duration: Int?,
        unit: String?,
        limit: Int?
    ): String {
        val sanitizedQuery = sanitize(query)
        val validLimit = (limit ?: 50).coerceIn(1, 200)

        val conditions = mutableListOf("message LIKE '%$sanitizedQuery%'")

        service?.let { conditions.add("service = '${sanitize(it)}'") }
        level?.let { conditions.add("level = '${sanitize(it.uppercase())}'") }

        val whereClause = conditions.joinToString(" AND ")

        return """
            SELECT timestamp, message, level, service
            FROM Log
            WHERE $whereClause
            ${timeWindow(duration, unit)}
            LIMIT $validLimit
        """.trimIndent().replace("\n", " ")
    }
}