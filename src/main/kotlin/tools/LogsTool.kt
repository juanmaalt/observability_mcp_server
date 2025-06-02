package tools

import core.NewRelicClient
import kotlinx.coroutines.runBlocking
import model.ErrorsCount

fun errorsCount(service: String, duration: Int? = 10, unit: String? = "minutes"): ErrorsCount = runBlocking {
    /**
     * Count the number of error logs for a given service over a specified time window.
     *
     * @param service The name of the service to query (e.g., "auth").
     * @param duration The time window size (e.g., 7 for "7 days"). Default value 10
     * @param unit The time unit ("minutes", "hours", "days", "weeks"). Default value "minutes"
     * @return A human-readable summary with the error count.
     *
     * Example: "There were 13 errors in 'auth' in the last 7 days."
     */

    val query = """SELECT count(*) FROM TransactionError WHERE appName = '$service' FACET error.message as 'Error' SINCE $duration $unit ago""".trim()

    return@runBlocking NewRelicClient.runQuery(query, ErrorsCount.serializer())
}