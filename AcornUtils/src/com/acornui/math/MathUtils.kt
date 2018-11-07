/*
 * Derived from LibGDX by Nicholas Bilyk
 * https://github.com/libgdx
 * Copyright 2011 See https://github.com/libgdx/libgdx/blob/master/AUTHORS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE")

package com.acornui.math

import kotlin.math.floor
import kotlin.math.pow

const val PI: Float = 3.1415927f
const val PI2: Float = PI * 2f
const val E: Float = 2.7182818f
const val TO_DEG = 180f / PI
const val TO_RAD = PI / 180f

/**
 * Utility and fast math functions.
 *
 * Thanks to Riven on JavaGaming.org for the basis of sin/cos/atan2/floor/ceil.
 * @author Nathan Sweet
 */
@Suppress("NOTHING_TO_INLINE")
object MathUtils {

	const val nanoToSec: Float = 1f / 1000000000f

	// ---
	const val FLOAT_ROUNDING_ERROR: Float = 0.000001f // 32 bits

	/**
	 * multiply by this to convert from radians to degrees
	 */
	const val radDeg: Float = 180f / PI

	/**
	 * multiply by this to convert from degrees to radians
	 */
	const val degRad: Float = PI / 180f

	/**
	 * Returns the sine in radians from a lookup table.
	 */
	@Deprecated("Use native math", ReplaceWith("kotlin.math.sin(radians)"), DeprecationLevel.ERROR)
	fun sin(radians: Float): Float = throw Exception()

	/**
	 * Returns the cosine in radians from a lookup table.
	 */
	@Deprecated("Use native math", ReplaceWith("kotlin.math.cos(radians)"), DeprecationLevel.ERROR)
	fun cos(radians: Float): Float = throw Exception()

	/**
	 * Returns the tan in radians from a lookup table.
	 * Throws DivideByZero exception when cos(radians) == 0
	 */
	@Deprecated("Use native math", ReplaceWith("kotlin.math.tan(radians)"), DeprecationLevel.ERROR)
	fun tan(radians: Float): Float = throw Exception()

	// ---

	@Deprecated("Use native math", ReplaceWith("kotlin.math.atan2(y, x)"), DeprecationLevel.ERROR)
	fun atan2(y: Float, x: Float): Float = throw Exception()

	// ---

	val rng: Random = Random()

	/**
	 * Returns a random number between 0 (inclusive) and the specified value (inclusive).
	 */
	fun random(range: Int): Int = rng.random(range)

	/**
	 * Returns a random number between start (inclusive) and end (inclusive).
	 */
	fun random(start: Int, end: Int): Int = rng.random(start, end)

	/**
	 * Returns a random number between 0 (inclusive) and the specified value (inclusive).
	 */
	fun random(range: Long): Long = rng.random(range)

	/**
	 * Returns a random number between start (inclusive) and end (inclusive).
	 */
	fun random(start: Long, end: Long): Long = rng.random(start, end)

	/**
	 * Returns a random boolean value.
	 */
	fun randomBoolean(): Boolean = rng.nextBoolean()

	/**
	 * Returns true if a random value between 0 and 1 is less than the specified value.
	 */
	fun randomBoolean(chance: Float): Boolean = rng.randomBoolean(chance)

	/**
	 * Returns random number between 0.0 (inclusive) and 1.0 (exclusive).
	 */
	fun random(): Float = rng.nextFloat()

	/**
	 * Returns a random number between 0 (inclusive) and the specified value (exclusive).
	 */
	fun random(range: Float): Float = rng.random(range)

	/**
	 * Returns a random number between start (inclusive) and end (exclusive).
	 */
	fun random(start: Float, end: Float): Float = rng.random(start, end)

	/**
	 * Returns -1 or 1, randomly.
	 */
	fun randomSign(): Int = rng.randomSign()

	/**
	 * Returns a triangularly distributed random number between -1.0 (exclusive) and 1.0 (exclusive), where values around zero are
	 * more likely.
	 * This is an optimized version of {@link #randomTriangular(float, float, float) randomTriangular(-1, 1, 0)}
	 */
	fun randomTriangular(): Float = rng.randomTriangular()

	/**
	 * Returns a triangularly distributed random number between {@code -max} (exclusive) and {@code max} (exclusive), where values
	 * around zero are more likely.
	 * This is an optimized version of {@link #randomTriangular(float, float, float) randomTriangular(-max, max, 0)}
	 * @param max the upper limit
	 */
	fun randomTriangular(max: Float): Float = rng.randomTriangular(max)

	/**
	 * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where the
	 * `mode` argument defaults to the midpoint between the bounds, giving a symmetric distribution.
	 *
	 * This method is equivalent of [randomTriangular(min, max, (max - min) * .5f)]
	 * @param min the lower limit
	 * @param max the upper limit
	 */
	fun randomTriangular(min: Float, max: Float): Float = rng.randomTriangular(min, max)

	/**
	 * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where values
	 * around {@code mode} are more likely.
	 * @param min the lower limit
	 * @param max the upper limit
	 * @param mode the point around which the values are more likely
	 */
	fun randomTriangular(min: Float, max: Float, mode: Float): Float = rng.randomTriangular(min, max, mode)

	// ---

	/**
	 * Returns the next power of two. Returns the specified value if the value is already a power of two.
	 */
	fun nextPowerOfTwo(value: Int): Int {
		var v = value
		if (v == 0) return 1
		v--
		v = v or (v shr 1)
		v = v or (v shr 2)
		v = v or (v shr 4)
		v = v or (v shr 8)
		v = v or (v shr 16)
		return v + 1
	}

	fun isPowerOfTwo(value: Int): Boolean {
		return value != 0 && (value and value - 1) == 0
	}

	// ---

	/**
	 * Linearly interpolates between fromValue to toValue on progress position.
	 */
	fun lerp(fromValue: Float, toValue: Float, progress: Float): Float {
		return fromValue + (toValue - fromValue) * progress
	}

	// ---

	@Deprecated("Use native math", ReplaceWith("kotlin.math.abs(value)"), DeprecationLevel.ERROR)
	inline fun abs(value: Float): Float {
		return if (value < 0f) -value else value
	}

	@Deprecated("Use native math", ReplaceWith("kotlin.math.abs(value)"), DeprecationLevel.ERROR)
	inline fun abs(value: Double): Double {
		return if (value < 0f) -value else value
	}

	@Deprecated("Use native math", ReplaceWith("kotlin.math.abs(value)"), DeprecationLevel.ERROR)
	inline fun abs(value: Int): Int {
		return if (value < 0f) -value else value
	}

	@Deprecated("Use native math", ReplaceWith("kotlin.math.abs(value)"), DeprecationLevel.ERROR)
	inline fun abs(value: Long): Long {
		return if (value < 0f) -value else value
	}

	/**
	 * Returns true if the value is zero
	 * @param tolerance represent an upper bound below which the value is considered zero.
	 */
	fun isZero(value: Float, tolerance: Float = FLOAT_ROUNDING_ERROR): Boolean {
		return kotlin.math.abs(value.toDouble()) <= tolerance
	}

	/**
	 * Returns true if the value is zero
	 * @param tolerance represent an upper bound below which the value is considered zero.
	 */
	fun isZero(value: Double, tolerance: Float = FLOAT_ROUNDING_ERROR): Boolean {
		return kotlin.math.abs(value) <= tolerance
	}

	/**
	 * Returns true if a is nearly equal to b. The function uses the default floating error tolerance.
	 * @param a the first value.
	 * @param b the second value.
	 */
	fun isEqual(a: Float, b: Float): Boolean {
		return kotlin.math.abs(a - b) <= FLOAT_ROUNDING_ERROR
	}

	/**
	 * Returns true if a is nearly equal to b.
	 * @param a the first value.
	 * @param b the second value.
	 * @param tolerance represent an upper bound below which the two values are considered equal.
	 */
	fun isEqual(a: Float, b: Float, tolerance: Float): Boolean {
		return kotlin.math.abs(a - b) <= tolerance
	}

	/**
	 * @return the logarithm of x with base a
	 */
	fun log(x: Float, base: Float): Float {
		return (Math.log(x.toDouble()) / Math.log(base.toDouble())).toFloat()
	}

	/**
	 * @return the logarithm of x with base 2
	 */
	fun log2(x: Float): Float {
		return log(x, 2f)
	}

	inline fun <T : Comparable<T>> clamp(value: T, min: T, max: T): T {
		if (value <= min) return min
		if (value >= max) return max
		return value
	}

	@Deprecated("Use minOf", ReplaceWith("minOf(x, y)"))
	inline fun <T : Comparable<T>> min(x: T, y: T): T {
		return minOf(x, y)
	}

	@Deprecated("Use minOf", ReplaceWith("minOf(x, y, z)"))
	inline fun <T : Comparable<T>> min(x: T, y: T, z: T): T {
		return minOf(x, y, z)
	}

	@Deprecated("Use minOf", ReplaceWith("minOf(w, x, minOf(y, z))"))
	inline fun <T : Comparable<T>> min(w: T, x: T, y: T, z: T): T {
		return minOf(w, x, minOf(y, z))
	}

	@Deprecated("Use maxOf", ReplaceWith("maxOf(x, y)"))
	inline fun <T : Comparable<T>> max(x: T, y: T): T {
		return maxOf(x, y)
	}

	@Deprecated("Use maxOf", ReplaceWith("maxOf(x, y, z)"))
	inline fun <T : Comparable<T>> max(x: T, y: T, z: T): T {
		return maxOf(x, y, z)
	}

	@Deprecated("Use maxOf", ReplaceWith("maxOf(w, x, maxOf(y, z))"))
	inline fun <T : Comparable<T>> max(w: T, x: T, y: T, z: T): T {
		return maxOf(w, x, maxOf(y, z))
	}

	// TODO: deprecate what's now in kotlin native math

	@Deprecated("Use native math", ReplaceWith("kotlin.math.ceil(v).toInt()"), DeprecationLevel.ERROR)
	inline fun ceil(v: Float): Int {
		return Math.ceil(v.toDouble()).toInt()
	}

	@Deprecated("Use native math", ReplaceWith("kotlin.math.floor(v).toInt()"), DeprecationLevel.ERROR)
	inline fun floor(v: Float): Int {
		return Math.floor(v.toDouble()).toInt()
	}

	@Deprecated("Use native math", ReplaceWith("kotlin.math.round(v).toInt()"), DeprecationLevel.ERROR)
	inline fun round(v: Float): Int {
		return Math.round(v.toDouble()).toInt()
	}

	@Deprecated("Use native math", ReplaceWith("kotlin.math.sqrt(v)"), DeprecationLevel.ERROR)
	inline fun sqrt(v: Float): Float {
		return Math.sqrt(v.toDouble()).toFloat()
	}

	@Deprecated("Use native math", ReplaceWith("a.pow(b)", "kotlin.math.pow"), DeprecationLevel.ERROR)
	inline fun pow(a: Float, b: Float): Float {
		return Math.pow(a.toDouble(), b.toDouble()).toFloat()
	}

	@Deprecated("Use native math", ReplaceWith("kotlin.math.acos(v)"), DeprecationLevel.ERROR)
	inline fun acos(v: Float): Float {
		return Math.acos(v.toDouble()).toFloat()
	}

	@Deprecated("Use native math", ReplaceWith("kotlin.math.asin(v)"), DeprecationLevel.ERROR)
	inline fun asin(v: Float): Float {
		return Math.asin(v.toDouble()).toFloat()
	}

	/**
	 * Returns the signum function of the argument; zero if the argument
	 * is zero, 1.0f if the argument is greater than zero, -1.0f if the
	 * argument is less than zero.
	 *
	 * <p>Special Cases:
	 * <ul>
	 * <li> If the argument is NaN, then the result is NaN.
	 * <li> If the argument is positive zero or negative zero, then the
	 *      result is the same as the argument.
	 * </ul>
	 *
	 * @param f the floating-point value whose signum is to be returned
	 * @return the signum function of the argument
	 * @author Joseph D. Darcy
	 * @since 1.5
	 */
	fun signum(v: Float): Float {
		if (v > 0) return 1f
		if (v < 0) return -1f
		if (v.isNaN()) return Float.NaN
		return 0f
	}

	/**
	 * n must be positive.
	 * mod( 5f, 3f) produces 2f
	 * mod(-5f, 3f) produces 1f
	 */
	fun mod(a: Float, n: Float): Float {
		return if (a < 0f) (a % n + n) % n else a % n
	}

	/**
	 * n must be positive.
	 * mod( 5, 3) produces 2
	 * mod(-5, 3) produces 1
	 */
	fun mod(a: Int, n: Int): Int {
		return if (a < 0) (a % n + n) % n else a % n
	}

	/**
	 * Finds the difference between two angles.
	 * The returned difference will always be >= -PI and < PI
	 */
	fun angleDiff(a: Float, b: Float): Float {
		var diff = b - a
		if (diff < -PI) diff = PI2 - diff
		if (diff > PI2) diff %= PI2
		if (diff >= PI) diff -= PI2
		return diff
	}

	/**
	 * Given a quadratic equation of the form: y = ax^2 + bx + c, returns the solutions to x where y == 0f
	 * Uses the quadratic formula: x = (-b += sqrt(b^2 - 4ac)) / 2a
	 * @param a The a coefficient.
	 * @param b The b coefficient.
	 * @param c The c coefficient.
	 * @param out The list to populate with the solutions.
	 * @return Returns the x values where y == 0f. This may have 0, 1, or 2 values.
	 */
	fun getQuadraticRoots(a: Float, b: Float, c: Float, out: MutableList<Float>) {
		out.clear()
		if (a == 0f) {
			// Not a quadratic equation.
			if (b == 0f) return
			out.add(-c / b)
		}

		val q = b * b - 4f * a * c
		val signQ = if (q > 0f) 1 else if (q < 0f) -1 else 0

		if (signQ < 0) {
			// No solution
		} else if (signQ == 0) {
			out.add(-b / (2f * a))
		} else {
			val aa = -b / (2f * a)
			val tmp = kotlin.math.sqrt(q) / (2f * a)
			out.add(aa - tmp)
			out.add(aa + tmp)
		}
	}

	/**
	 * Given a cubic equation of the form: y = ax^3 + bx^2 + cx + d, returns the solutions to x where y == 0f
	 * @param a The a coefficient.
	 * @param b The b coefficient.
	 * @param c The c coefficient.
	 * @param c The d coefficient.
	 * @param out The list to populate with the solutions.
	 * @return Returns the x values where y == 0f. This may have 0, 1, 2 or 3 values.
	 */
	fun getCubicRoots(a: Float = 0f, b: Float = 0f, c: Float = 0f, d: Float = 0f, out: MutableList<Float>) {
		if (a == 0f) {
			// Not a cubic equation
			return getQuadraticRoots(b, c, d, out)
		}
		out.clear()

		var b = b
		var c = c
		var d = d
		// normalize the coefficients so the cubed term is 1 and we can ignore it hereafter
		if (a != 1f) {
			b /= a
			c /= a
			d /= a
		}

		val q = (b * b - 3f * c) / 9f
		val q3 = q * q * q
		val r = (2f * b * b * b - 9f * b * c + 27f * d) / 54f
		val diff: Float = q3 - r * r
		if (diff >= 0) {
			if (q == 0f) {
				// avoid division by zero
				out.add(0f)
			} else {
				// three real roots
				val theta: Float = kotlin.math.acos(r / kotlin.math.sqrt(q3))
				val qSqrt: Float = kotlin.math.sqrt(q)

				out.add(-2f * qSqrt * kotlin.math.cos(theta / 3f) - b / 3f)
				out.add(-2f * qSqrt * kotlin.math.cos((theta + 2f * PI) / 3f) - b / 3f)
				out.add(-2f * qSqrt * kotlin.math.cos((theta + 4f * PI) / 3f) - b / 3f)
			}
		} else {
			// one real root
			val tmp: Float = (kotlin.math.sqrt(-diff) + kotlin.math.abs(r)).pow(1f / 3f)
			val rSign = if (r > 0f) 1f else if (r < 0f) -1f else 0f
			out.add(-rSign * (tmp + q / tmp) - b / 3f)
		}
	}

	// TODO: Document

	/**
	 * Snaps a value to the nearest interval.
	 */
	fun roundToNearest(value: Float, snap: Float, offset: Float = 0f): Float {
		if (snap <= 0) return value
		var v = value - offset
		v /= snap
		v = kotlin.math.round(v)
		v *= snap
		return v + offset
	}

	fun floorToNearest(value: Float, snap: Float, offset: Float = 0f): Float {
		if (snap <= 0) return value
		var v = value - offset
		v /= snap
		v = kotlin.math.floor(v)
		v *= snap
		return v + offset
	}

	fun ceilToNearest(value: Float, snap: Float, offset: Float = 0f): Float {
		if (snap <= 0) return value
		var v = value - offset
		v /= snap
		v = kotlin.math.ceil(v)
		v *= snap
		return v + offset
	}

	inline fun offsetRound(x: Float, offset: Float = 0.0136f): Float {
		return kotlin.math.round(x + offset)
	}
}

inline fun Float.ceil(): Int {
	return kotlin.math.ceil(this).toInt()
}

/**
 * Returns the fraction of this float.
 */
inline fun Float.fpart(): Float {
	return this - floor(this)
}

/**
 * Gets the number of fraction digits for this value.
 * E.g.  if this value is 1f, returns 0, if this value is 3.1f, returns 1.
 * The max return value is 10.
 */
val Float.fractionDigits: Int
	get() {
		var m = 1f
		for (i in 0..10) {
			if ((this * m).fpart() == 0f) {
				return i
			}
			m *= 10f
		}
		return 10
	}


inline fun <T : Comparable<T>> maxOf4(a: T, b: T, c: T, d: T): T {
	return maxOf(maxOf(a, b), maxOf(c, d))
}

inline fun <T : Comparable<T>> minOf4(a: T, b: T, c: T, d: T): T {
	return minOf(minOf(a, b), minOf(c, d))
}