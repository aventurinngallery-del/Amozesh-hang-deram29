package com.sharn.handpan.audio

/** Serializes microphone ownership between analysis and custom-sample recording. */
object AudioResourceCoordinator {
    private var activeOwner: String? = null
    private var leaseCount = 0

    @Synchronized
    fun tryAcquire(owner: String): Lease? {
        if (activeOwner != null && activeOwner != owner) return null
        activeOwner = owner
        leaseCount++
        return Lease(owner)
    }

    @Synchronized
    private fun release(owner: String) {
        if (activeOwner == owner) {
            leaseCount--
            if (leaseCount <= 0) {
                activeOwner = null
                leaseCount = 0
            }
        }
    }

    class Lease internal constructor(private val owner: String) : AutoCloseable {
        private var closed = false

        @Synchronized
        override fun close() {
            if (!closed) {
                closed = true
                release(owner)
            }
        }
    }
}
