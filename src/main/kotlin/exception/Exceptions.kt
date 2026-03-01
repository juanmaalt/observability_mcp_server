package exception

class NewRelicApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

class NrqlQueryException(message: String) : Exception(message)
