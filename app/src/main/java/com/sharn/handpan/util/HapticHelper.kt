package com.sharn.handpan.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Production-safe Haptic and Vibration helper with hardware capability caching and circuit breaker.
 * Detects emulator/virtualized/container environments and prevents continuous HAL/AIDL component interface query errors.
 */
class HapticHelper(context: Context) {

    private val isVirtualOrMissing: Boolean by lazy {
        try {
            // First check if the package manager declares vibrator hardware feature
            val pm = context.packageManager
            val hasFeature = pm.hasSystemFeature("android.hardware.vibrator") ||
                    pm.hasSystemFeature("android.hardware.vibrator.autocal")
            if (!hasFeature) return@lazy true

            val brand = Build.BRAND.lowercase()
            val device = Build.DEVICE.lowercase()
            val fingerprint = Build.FINGERPRINT.lowercase()
            val hardware = Build.HARDWARE.lowercase()
            val model = Build.MODEL.lowercase()
            val manufacturer = Build.MANUFACTURER.lowercase()
            val product = Build.PRODUCT.lowercase()
            val board = Build.BOARD.lowercase()
            val host = Build.HOST.lowercase()

            brand.startsWith("generic")
                || device.startsWith("generic")
                || device.contains("vsoc")
                || device.contains("cuttlefish")
                || fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || hardware.contains("vsoc")
                || hardware.contains("cuttlefish")
                || hardware.contains("qemu")
                || board.contains("goldfish")
                || board.contains("ranchu")
                || board.contains("vsoc")
                || board.contains("cuttlefish")
                || host.contains("android-build")
                || model.contains("google_sdk")
                || model.contains("emulator")
                || model.contains("android sdk")
                || model.contains("cuttlefish")
                || manufacturer.contains("genymotion")
                || product.contains("sdk")
                || product.contains("vbox")
                || product.contains("emulator")
                || product.contains("simulator")
                || product.contains("cuttlefish")
                || product.contains("vsoc")
        } catch (_: Throwable) {
            true
        }
    }

    @Volatile
    private var isHapticAvailable: Boolean = false

    private val vibrator: Vibrator? by lazy {
        if (isVirtualOrMissing) {
            isHapticAvailable = false
            null
        } else {
            try {
                val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    @Suppress("DEPRECATION")
                    manager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                val hasHw = vib?.hasVibrator() == true
                isHapticAvailable = hasHw
                if (hasHw) vib else null
            } catch (_: Throwable) {
                isHapticAvailable = false
                null
            }
        }
    }

    private var hasAmplitudeSupport: Boolean? = null

    init {
        if (!isVirtualOrMissing) {
            try {
                val vib = vibrator
                isHapticAvailable = (vib != null && vib.hasVibrator())
            } catch (_: Throwable) {
                isHapticAvailable = false
            }
        } else {
            isHapticAvailable = false
        }
    }

    val hasVibrator: Boolean
        get() = isHapticAvailable

    fun vibrate(durationMs: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        if (!isHapticAvailable || isVirtualOrMissing) return
        val vib = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (hasAmplitudeSupport == null) {
                    hasAmplitudeSupport = try {
                        vib.hasAmplitudeControl()
                    } catch (_: Throwable) {
                        false
                    }
                }

                val finalAmp = if (hasAmplitudeSupport == true && amplitude in 1..255) {
                    amplitude
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }

                vib.vibrate(VibrationEffect.createOneShot(durationMs.coerceAtLeast(1L), finalAmp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs.coerceAtLeast(1L))
            }
        } catch (_: Throwable) {
            // Disable if HAL or permission fails to prevent spamming AIDL query errors
            isHapticAvailable = false
        }
    }

    fun performClick(isAccent: Boolean = false) {
        if (!isHapticAvailable || isVirtualOrMissing) return
        try {
            val duration = if (isAccent) 25L else 12L
            val amplitude = if (isAccent) 180 else 90
            vibrate(duration, amplitude)
        } catch (_: Throwable) {
            isHapticAvailable = false
        }
    }
}
