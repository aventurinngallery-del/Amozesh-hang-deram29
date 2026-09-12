package com.sharn.handpan

import com.sharn.handpan.audio.AudioResourceCoordinator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test
import com.sharn.handpan.audio.AudioAnalysisSession
import com.sharn.handpan.model.NotePitchConfig

class AudioResourceCoordinatorTest {
    @Test
    fun inactiveSubscriptionDoesNotClaimUnavailableMicrophone() {
        val blocker = AudioResourceCoordinator.tryAcquire("other-owner")
        val session = AudioAnalysisSession()

        val subscription = session.acquire(
            scaleConfig = NotePitchConfig.D_KURD_9,
            onStrike = {}
        )

        assertEquals(false, subscription.isActive)
        subscription.close()
        session.close()
        blocker?.close()
    }

    @Test
    fun ownershipIsExclusiveAndReleaseIsIdempotent() {
        val first = AudioResourceCoordinator.tryAcquire("analysis")
        assertNotNull(first)
        assertNull(AudioResourceCoordinator.tryAcquire("custom-sample"))

        first!!.close()
        first.close()

        val second = AudioResourceCoordinator.tryAcquire("custom-sample")
        assertNotNull(second)
        second!!.close()
    }

    @Test
    fun sameOwnerCannotCreateIndependentLease() {
        val first = AudioResourceCoordinator.tryAcquire("analysis")
        val second = AudioResourceCoordinator.tryAcquire("analysis")

        assertNotNull(first)
        assertNotNull(second)

        first!!.close()
        second!!.close()
    }
}
