import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*

class ParallelEvaluator {
    suspend fun run(task: Task, n: Int, context: CoroutineContext) {
        val deferredList = mutableListOf<Deferred<Unit>>()

        coroutineScope {
            for (i in 0 until n) {
                val deferred = async(context) {
                    try {
                        task.run(i)
                    } catch (e: Exception) {
                        deferredList.forEach { it.cancel() }
                        throw TaskEvaluationException(e)
                    }
                }
                deferredList.add(deferred)
            }
        }

        deferredList.awaitAll()
    }
}
