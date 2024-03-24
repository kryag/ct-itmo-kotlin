interface Value<T> {
    var value: T
    fun observe(observer: (T) -> Unit): Cancellation
}

class MutableValue<T>(initial: T) : Value<T> {
    private var currentValue: T = initial
    private val observers = mutableSetOf<(T) -> Unit>()

    override var value: T
        get() = currentValue
        set(value) {
            currentValue = value
            observers.forEach {
                try {
                    it(value)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    override fun observe(observer: (T) -> Unit): Cancellation {
        observers.add(observer)
        try {
            observer(currentValue)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Cancellation { observers.remove(observer) }
    }
}

fun interface Cancellation {
    fun cancel()
}
