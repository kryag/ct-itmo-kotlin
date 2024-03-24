import kotlin.Exception

sealed interface IntResult {
    data class Ok(val value: Int) : IntResult
    data class Error(val reason: String) : IntResult

    fun getOrDefault(value: Int): Int {
        return when (this) {
            is Ok -> this.value
            else -> value
        }
    }

    fun getOrNull(): Int? {
        return when (this) {
            is Ok -> this.value
            else -> null
        }
    }

    fun getStrict(): Int {
        return when (this) {
            is Ok -> this.value
            is Error -> throw NoResultProvided(this.reason)
        }
    }
}

class NoResultProvided(message: String) : NoSuchElementException(message)

fun safeRun(unsafeCall: () -> Int): IntResult {
    return try {
        IntResult.Ok(unsafeCall())
    } catch (e: Exception) {
        IntResult.Error(e.message ?: "Error!")
    }
}
