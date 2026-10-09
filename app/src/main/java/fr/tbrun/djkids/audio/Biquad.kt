package fr.tbrun.djkids.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Coefficients d'un filtre biquad (cookbook RBJ, Audio EQ Cookbook).
 * Partagés entre les canaux d'une même bande ; l'état (x1,x2,y1,y2) reste par canal.
 */
class BiquadCoeffs {
    var b0 = 1.0; var b1 = 0.0; var b2 = 0.0; var a1 = 0.0; var a2 = 0.0

    fun lowShelf(fs: Int, f0: Double, dbGain: Double, s: Double = 0.9) {
        val a = 10.0.pow(dbGain / 40.0)
        val w0 = 2 * PI * f0 / fs
        val cw = cos(w0); val sw = sin(w0)
        val alpha = sw / 2 * sqrt((a + 1 / a) * (1 / s - 1) + 2)
        val ap1 = a + 1; val am1 = a - 1
        val twoSqrtAalpha = 2 * sqrt(a) * alpha
        val b0n = a * (ap1 - am1 * cw + twoSqrtAalpha)
        val b1n = 2 * a * (am1 - ap1 * cw)
        val b2n = a * (ap1 - am1 * cw - twoSqrtAalpha)
        val a0 = ap1 + am1 * cw + twoSqrtAalpha
        val a1n = -2 * (am1 + ap1 * cw)
        val a2n = ap1 + am1 * cw - twoSqrtAalpha
        normalize(b0n, b1n, b2n, a0, a1n, a2n)
    }

    fun highShelf(fs: Int, f0: Double, dbGain: Double, s: Double = 0.9) {
        val a = 10.0.pow(dbGain / 40.0)
        val w0 = 2 * PI * f0 / fs
        val cw = cos(w0); val sw = sin(w0)
        val alpha = sw / 2 * sqrt((a + 1 / a) * (1 / s - 1) + 2)
        val ap1 = a + 1; val am1 = a - 1
        val twoSqrtAalpha = 2 * sqrt(a) * alpha
        val b0n = a * (ap1 + am1 * cw + twoSqrtAalpha)
        val b1n = -2 * a * (am1 + ap1 * cw)
        val b2n = a * (ap1 + am1 * cw - twoSqrtAalpha)
        val a0 = ap1 - am1 * cw + twoSqrtAalpha
        val a1n = 2 * (am1 - ap1 * cw)
        val a2n = ap1 - am1 * cw - twoSqrtAalpha
        normalize(b0n, b1n, b2n, a0, a1n, a2n)
    }

    fun peaking(fs: Int, f0: Double, dbGain: Double, q: Double = 0.9) {
        val a = 10.0.pow(dbGain / 40.0)
        val w0 = 2 * PI * f0 / fs
        val cw = cos(w0); val sw = sin(w0)
        val alpha = sw / (2 * q)
        val b0n = 1 + alpha * a
        val b1n = -2 * cw
        val b2n = 1 - alpha * a
        val a0 = 1 + alpha / a
        val a1n = -2 * cw
        val a2n = 1 - alpha / a
        normalize(b0n, b1n, b2n, a0, a1n, a2n)
    }

    private fun normalize(b0n: Double, b1n: Double, b2n: Double, a0: Double, a1n: Double, a2n: Double) {
        b0 = b0n / a0; b1 = b1n / a0; b2 = b2n / a0
        a1 = a1n / a0; a2 = a2n / a0
    }
}

/** État de filtrage d'un canal (Direct Form I). */
class BiquadState {
    private var x1 = 0f; private var x2 = 0f; private var y1 = 0f; private var y2 = 0f

    fun reset() { x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f }

    fun process(x: Float, c: BiquadCoeffs): Float {
        val y = (c.b0 * x + c.b1 * x1 + c.b2 * x2 - c.a1 * y1 - c.a2 * y2).toFloat()
        x2 = x1; x1 = x
        y2 = y1; y1 = y
        return y
    }
}
