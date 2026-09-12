package com.sharn.handpan.audio

import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

/** Monotonic deadline wait with clock re-check after every scheduler wake-up. */
class DeadlineScheduler(
    private val clock: PracticeClock,
    private val sleepMillis: suspend (Long) -> Unit = { delay(it) }
) {
    suspend fun await(deadlineNanos: Long) {
        while (true) {
            val remainingNanos = deadlineNanos - clock.nowNanos()
            if (remainingNanos <= 0L) return
            val millis = remainingNanos / 1_000_000L
            if (millis > 0L) sleepMillis(millis)
            else yield()
        }
    }
}
