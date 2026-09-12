package com.sharn.handpan.audio

/**
 * Unified monotonic clock for precise audio and musical practice synchronization.
 * Uses strictly System.nanoTime() as the monotonic reference.
 * Prevents clock-drift, wall-clock jumps, daylight savings or NTP shifts from corrupting musical timing.
 */
interface PracticeClock {
    /**
     * Current monotonic time in nanoseconds.
     */
    fun nowNanos(): Long

    /**
     * Current monotonic time in milliseconds.
     */
    fun nowMillis(): Long = nowNanos() / 1_000_000L

    companion object {
        val Default: PracticeClock = object : PracticeClock {
            override fun nowNanos(): Long = System.nanoTime()
        }
    }
}
