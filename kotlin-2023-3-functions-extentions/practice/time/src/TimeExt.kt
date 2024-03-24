import kotlin.math.abs

private const val SEC_IN_HOUR = 3600
private const val SEC_IN_MIN = 60
private const val MS_IN_SEC = 1000

private fun getCorrectTime(sec: Long, ms: Long) : Time {
    var diff = ms / MS_IN_SEC
    if (ms < 0) {
        diff *= -1
        if (-ms % MS_IN_SEC != 0L) {
            diff--
        }
    }
    val newSec = sec + diff
    val newMs = abs(ms - diff * MS_IN_SEC)
    if (newSec < 0) throw IllegalArgumentException("Negative result")
    return Time(newSec, newMs.toInt())
}

val Int.milliseconds: Time
    get() = getCorrectTime(0, this.toLong())

val Long.milliseconds: Time
    get() = getCorrectTime(0, this)

val Int.seconds: Time
    get() = getCorrectTime(this.toLong(), 0)

val Long.seconds: Time
    get() = getCorrectTime(this, 0)

val Int.minutes: Time
    get() = getCorrectTime(this.toLong() * SEC_IN_MIN, 0)

val Long.minutes: Time
    get() = getCorrectTime(this * SEC_IN_MIN, 0)

val Int.hours: Time
    get() = getCorrectTime(this.toLong() * SEC_IN_HOUR, 0)

val Long.hours: Time
    get() = getCorrectTime(this * SEC_IN_HOUR, 0)

operator fun Time.plus(other: Time) = getCorrectTime(this.seconds + other.seconds,
    this.milliseconds.toLong() + other.milliseconds)

operator fun Time.minus(other: Time) = getCorrectTime(this.seconds - other.seconds,
    this.milliseconds.toLong() - other.milliseconds)

operator fun Time.times(factor: Int) = getCorrectTime(this.seconds * factor, this.milliseconds.toLong() * factor)
