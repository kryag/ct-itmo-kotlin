import kotlinx.coroutines.*

fun CoroutineScope.runApplication(
    runUI: suspend () -> Unit,
    runApi: suspend () -> Unit,
) {
    val apiJob = launch {
        while (true) {
            try {
                runApi()
                break
            } catch (_: Exception) {
                delay(1000)
            }
        }
    }

    launch {
        try {
            runUI()
        } catch (e: Exception) {
            apiJob.cancel()
            throw e
        }
    }
}
