package com.silomath.calculation

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap

/** Exact reduced rational arithmetic. No MathContext, floating point or intermediate rounding. */
internal class Rational private constructor(val numerator: BigInteger, val denominator: BigInteger) : Comparable<Rational> {
    operator fun plus(other: Rational) = of(numerator * other.denominator + other.numerator * denominator, denominator * other.denominator)
    operator fun minus(other: Rational) = of(numerator * other.denominator - other.numerator * denominator, denominator * other.denominator)
    operator fun times(other: Rational) = of(numerator * other.numerator, denominator * other.denominator)
    operator fun div(other: Rational) = of(numerator * other.denominator, denominator * other.numerator)
    fun pow(n: Int) = of(numerator.pow(n), denominator.pow(n))
    override fun compareTo(other: Rational) = (numerator * other.denominator).compareTo(other.numerator * denominator)
    fun rounded(scale: Int) = BigDecimal(numerator).divide(BigDecimal(denominator), scale, RoundingMode.HALF_UP)
    companion object {
        val ZERO = of(BigInteger.ZERO, BigInteger.ONE)
        fun integer(n: Long) = of(BigInteger.valueOf(n), BigInteger.ONE)
        fun decimal(n: BigDecimal): Rational = if (n.scale() >= 0) of(n.unscaledValue(), BigInteger.TEN.pow(n.scale()))
            else of(n.unscaledValue() * BigInteger.TEN.pow(-n.scale()), BigInteger.ONE)
        fun of(n: BigInteger, d: BigInteger): Rational {
            require(d.signum() != 0)
            val sign = if (d.signum() < 0) BigInteger.valueOf(-1) else BigInteger.ONE
            val gcd = n.gcd(d)
            return Rational(n / gcd * sign, d / gcd * sign)
        }
    }
}
internal enum class ExactConstant { ONE, PI, SQRT_THREE }

/** A rational coefficient times an exact mathematical constant; rounding happens only on explicit persistence. */
class ExactValue internal constructor(internal val coefficient: Rational, internal val constant: ExactConstant = ExactConstant.ONE) {
    fun times(multiplier: BigDecimal) = ExactValue(coefficient * Rational.decimal(multiplier), constant)
    fun toPersistenceDecimal(): BigDecimal {
        if (constant == ExactConstant.ONE || coefficient == Rational.ZERO || coefficient.numerator.signum() == 0)
            return checked(coefficient.rounded(6))
        var digits = 32
        while (digits <= 16384) {
            val (lower, upper) = ConstantBounds.forConstant(constant, digits)
            val a = (coefficient * lower).rounded(6)
            val b = (coefficient * upper).rounded(6)
            if (a.compareTo(b) == 0) return checked(a)
            digits *= 2
        }
        throw ArithmeticException("Persistence rounding could not be certified")
    }
    private fun checked(value: BigDecimal): BigDecimal {
        require(value.precision() <= 20) { "NUMERIC(20,6) boundary exceeded" }
        return value
    }
    companion object {
        fun fromCanonicalDecimal(value: BigDecimal) = ExactValue(Rational.decimal(value))
    }
}

/** Rigorous rational enclosures, not finite rounded literals used as exact constants. */
private object ConstantBounds {
    private val cache = ConcurrentHashMap<Pair<ExactConstant, Int>, Pair<Rational, Rational>>()
    fun forConstant(constant: ExactConstant, digits: Int): Pair<Rational, Rational> = cache.getOrPut(constant to digits) {
        val scale = BigInteger.TEN.pow(digits)
        val bounds = when (constant) {
            ExactConstant.PI -> {
                // Machin identity: pi = 16 atan(1/5) - 4 atan(1/239).
                val a = atanBounds(5, scale); val b = atanBounds(239, scale)
                a.first * BigInteger.valueOf(16) - b.second * BigInteger.valueOf(4) to
                    a.second * BigInteger.valueOf(16) - b.first * BigInteger.valueOf(4)
            }
            ExactConstant.SQRT_THREE -> {
                val root = integerSqrt(BigInteger.valueOf(3) * scale * scale)
                root to root + BigInteger.ONE
            }
            ExactConstant.ONE -> scale to scale
        }
        Rational.of(bounds.first, scale) to Rational.of(bounds.second, scale)
    }
    private fun atanBounds(q: Int, scale: BigInteger): Pair<BigInteger, BigInteger> {
        val base = BigInteger.valueOf(q.toLong())
        val square = base * base
        var power = base; var n = 0
        var lower = BigInteger.ZERO; var upper = BigInteger.ZERO
        while (true) {
            val floor = scale / (power * BigInteger.valueOf(2L * n + 1))
            if (floor.signum() == 0) {
                // Alternating-series remainder has the next term's sign and magnitude < one scale unit.
                if (n % 2 == 0) upper += BigInteger.ONE else lower -= BigInteger.ONE
                return lower to upper
            }
            if (n % 2 == 0) { lower += floor; upper += floor + BigInteger.ONE }
            else { lower -= floor + BigInteger.ONE; upper -= floor }
            power *= square; n++
        }
    }
    private fun integerSqrt(n: BigInteger): BigInteger {
        // Integer Newton iteration, also available on Android API26 without BigInteger.sqrt().
        var x = BigInteger.ONE.shiftLeft((n.bitLength() + 1) / 2)
        while (true) {
            val next = (x + n / x).shiftRight(1)
            if (next >= x) return x
            x = next
        }
    }
}
