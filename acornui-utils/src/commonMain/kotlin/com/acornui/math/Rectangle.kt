/*
 * Copyright 2019 Poly Forest, LLC
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

@file:Suppress("unused")

package com.acornui.math

import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import com.acornui.serialization.*

/**
 * A read-only interface to [Rectangle]
 */
interface RectangleRo {
	val x: Float
	val y: Float
	val width: Float
	val height: Float
	val left: Float
	val top: Float
	val right: Float
	val bottom: Float
	fun isEmpty(): Boolean
	fun isNotEmpty(): Boolean
	
	/**
	 * return the Vector2 with coordinates of this rectangle
	 * @param out The Vector2
	 */
	fun getPosition(out: Vector2): Vector2

	/**
	 * @return the Vector2 with size of this rectangle
	 * @param out The Vector2
	 */
	fun getSize(out: Vector2): Vector2

	/**
	 * @param x point x coordinate
	 * @param y point y coordinate
	 * @return whether the point is contained in the rectangle
	 */
	fun intersects(x: Float, y: Float): Boolean

	/**
	 * @param point The coordinates vector
	 * @return whether the point is contained in the rectangle
	 */
	fun intersects(point: Vector2Ro): Boolean

	/**
	 * Does an intersection test with a Ray (in the same coordinate space)
	 * @param r The ray to check against.
	 * @param out If provided, will be set to the intersection position if there was one.
	 * @return Returns true if the ray intersects this Rectangle.
	 */
	fun intersects(r: RayRo, out: Vector3? = null): Boolean

	/**
	 * @param rectangle the other {@link Rectangle}.
	 * @return whether the other rectangle is contained in this rectangle.
	 */
	fun contains(rectangle: RectangleRo): Boolean

	/**
	 * @param r the other {@link Rectangle}
	 * @return whether this rectangle overlaps the other rectangle.
	 */
	fun intersects(r: RectangleRo): Boolean

	fun intersects(xVal: Float, yVal: Float, widthVal: Float, heightVal: Float): Boolean
	/**
	 * Calculates the aspect ratio ( width / height ) of this rectangle
	 * @return the aspect ratio of this rectangle. Returns 0 if height is 0 to avoid NaN
	 */
	fun getAspectRatio(): Float

	/**
	 * Calculates the center of the rectangle. Results are located in the given Vector2
	 * @param out the Vector2 to use
	 * @return the given out with results stored inside
	 */
	fun getCenter(out: Vector2): Vector2

	/**
	 * Returns true if this rectangle's bounds can contain the given dimensions
	 * Note: x, y coordinates are not considered.
	 */
	fun canContain(width: Float, height: Float): Boolean

	fun area(): Float
	fun perimeter(): Float

	fun copy(x: Float = this.x, y: Float = this.y, width: Float = this.width, height: Float = this.height): Rectangle {
		return Rectangle(x, y, width, height)
	}

	fun reduce(padding: PadRo): Rectangle = reduce(padding.left, padding.top, padding.right, padding.bottom)

	/**
	 * Clips the sides of this rectangle by the given amounts, returning a new rectangle.
	 */
	fun reduce(left: Float, top: Float, right: Float, bottom: Float): Rectangle {
		return Rectangle(x + left, y + left, width - left - right, height - top - bottom)
	}

	companion object {
		val EMPTY = Rectangle()
	}
}

class Rectangle(
		override var x: Float = 0f,
		override var y: Float = 0f,
		override var width: Float = 0f,
		override var height: Float = 0f
) : Clearable, RectangleRo {

	override var left: Float
		get() = x
		set(value) {
			x = value
		}

	override var top: Float
		get() = y
		set(value) {
			y = value
		}

	/**
	 * The x + width value.
	 * If set, this will change the width to width = right - x
	 */
	override var right: Float
		get() = x + width
		set(value) {
			width = value - x
		}

	/**
	 * The y + height value.
	 * If set, this will change the height to height = bottom - y
	 */
	override var bottom: Float
		get() = y + height
		set(value) {
			height = value - y
		}

	override fun isEmpty(): Boolean {
		return width == 0f || height == 0f
	}

	override fun isNotEmpty(): Boolean = !isEmpty()

	/**
	 * @param x bottom-left x coordinate
	 * @param y bottom-left y coordinate
	 * @param width width
	 * @param height height
	 * @return this rectangle for chaining
	 */
	fun set(x: Float, y: Float, width: Float, height: Float): Rectangle {
		this.x = x
		this.y = y
		this.width = width
		this.height = height

		return this
	}

	fun set(x: Int, y: Int, width: Int, height: Int): Rectangle {
		return set(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
	}

	/**
	 * Sets this rectangle to match the given [minMax] region.
	 */
	fun set(minMax: MinMaxRo): Rectangle {
		return set(minMax.xMin, minMax.yMin, minMax.width, minMax.height)
	}

	/**
	 * Sets this Rectangle to 0,0,0,0
	 */
	override fun clear() {
		x = 0f
		y = 0f
		width = 0f
		height = 0f
	}

	/**
	 * @return the Vector2 with coordinates of this rectangle
	 * @param out The Vector2
	 */
	override fun getPosition(out: Vector2): Vector2 {
		return out.set(x, y)
	}

	/**
	 * Sets the x and y-coordinates of the bottom left corner from vector
	 * @param position The position vector
	 * @return this rectangle for chaining
	 */
	fun setPosition(position: Vector2Ro): Rectangle {
		this.x = position.x
		this.y = position.y

		return this
	}

	/**
	 * Sets the x and y-coordinates of the bottom left corner
	 * @param x The x-coordinate
	 * @param y The y-coordinate
	 * @return this rectangle for chaining
	 */
	fun setPosition(x: Float, y: Float): Rectangle {
		this.x = x
		this.y = y

		return this
	}

	/**
	 * Sets the width and height of this rectangle
	 * @param width The width
	 * @param height The height
	 * @return this rectangle for chaining
	 */
	fun setSize(width: Float, height: Float): Rectangle {
		this.width = width
		this.height = height

		return this
	}

	/**
	 * @return the Vector2 with size of this rectangle
	 * @param out The Vector2
	 */
	override fun getSize(out: Vector2): Vector2 {
		return out.set(width, height)
	}

	/**
	 * @param x point x coordinate
	 * @param y point y coordinate
	 * @return whether the point is contained in the rectangle
	 */
	override fun intersects(x: Float, y: Float): Boolean {
		return this.x <= x && right >= x && this.y <= y && bottom >= y
	}

	/**
	 * @param point The coordinates vector
	 * @return whether the point is contained in the rectangle
	 */
	override fun intersects(point: Vector2Ro): Boolean {
		return intersects(point.x, point.y)
	}

	/**
	 * Does an intersection test with a Ray (in the same coordinate space)
	 * @param r The ray to check against.
	 * @param out If provided, will be set to the intersection position if there was one.
	 * @return Returns true if the ray intersects this Rectangle.
	 */
	override fun intersects(r: RayRo, out: Vector3?): Boolean {
		if (r.direction.z == 0f) return false
		val m = -r.origin.z * r.directionInv.z
		if (m < 0) return false // Intersection (if there is one) is behind the ray.
		val x2 = r.origin.x + m * r.direction.x
		val y2 = r.origin.y + m * r.direction.y

		val intersects = x2 >= x && x2 <= x + width && y2 >= y && y2 <= y + height
		if (out != null && intersects) {
			r.getEndPoint(m, out)
		}
		return intersects
	}

	/**
	 * @param rectangle the other {@link Rectangle}.
	 * @return whether the other rectangle is contained in this rectangle.
	 */
	override fun contains(rectangle: RectangleRo): Boolean {
		val xMin = rectangle.x
		val xMax = xMin + rectangle.width

		val yMin = rectangle.y
		val yMax = yMin + rectangle.height

		return ((xMin > x && xMin < x + width) && (xMax > x && xMax < x + width)) && ((yMin > y && yMin < y + height) && (yMax > y && yMax < y + height))
	}

	/**
	 * @param r the other {@link Rectangle}
	 * @return whether this rectangle overlaps the other rectangle.
	 */
	override fun intersects(r: RectangleRo): Boolean {
		return intersects(r.x, r.y, r.width, r.height)
	}

	override fun intersects(xVal: Float, yVal: Float, widthVal: Float, heightVal: Float): Boolean {
		return x < xVal + widthVal && right > xVal && y < yVal + heightVal && bottom > yVal
	}

	/**
	 * Sets this rectangle to the intersection of this rectangle and [other].
	 */
	fun intersection(other: RectangleRo): Rectangle {
		x = maxOf(x, other.x)
		y = maxOf(y, other.y)
		right = minOf(right, other.right)
		bottom = minOf(bottom, other.bottom)
		return this
	}

	/**
	 * Sets the values of the given rectangle to this rectangle.
	 * @param rect the other rectangle
	 * @return this rectangle for chaining
	 */
	fun set(rect: RectangleRo): Rectangle {
		this.x = rect.x
		this.y = rect.y
		this.width = rect.width
		this.height = rect.height

		return this
	}

	override fun getAspectRatio(): Float {
		return if (height == 0f) 0f else width / height
	}

	override fun getCenter(out: Vector2): Vector2 {
		out.x = x + width * 0.5f
		out.y = y + height * 0.5f
		return out
	}

	/**
	 * Moves this rectangle so that its center point is located at a given position
	 * @param x the position's x
	 * @param y the position's y
	 * @return this for chaining
	 */
	fun setCenter(x: Float, y: Float): Rectangle {
		setPosition(x - width * 0.5f, y - height * 0.5f)
		return this
	}

	/**
	 * Moves this rectangle so that its center point is located at a given position
	 * @param position the position
	 * @return this for chaining
	 */
	fun setCenter(position: Vector2Ro): Rectangle {
		setPosition(position.x - width * 0.5f, position.y - height * 0.5f)
		return this
	}

	/**
	 * Fits this rectangle around another rectangle while maintaining aspect ratio. This scales and centers the
	 * rectangle to the other rectangle (e.g. Having a camera translate and scale to show a given area)
	 * @param rect the other rectangle to fit this rectangle around
	 * @return this rectangle for chaining
	 */
	fun fitOutside(rect: RectangleRo): Rectangle {
		val ratio = getAspectRatio()

		if (ratio > rect.getAspectRatio()) {
			// Wider than tall
			setSize(rect.height * ratio, rect.height)
		} else {
			// Taller than wide
			setSize(rect.width, rect.width / ratio)
		}

		setPosition((rect.x + rect.width * 0.5f) - width * 0.5f, (rect.y + rect.height * 0.5f) - height * 0.5f)
		return this
	}

	/**
	 * Fits this rectangle into another rectangle while maintaining aspect ratio. This scales and centers the rectangle
	 * to the other rectangle (e.g. Scaling a texture within a arbitrary cell without squeezing)
	 * @param rect the other rectangle to fit this rectangle inside
	 * @return this rectangle for chaining
	 */
	fun fitInside(rect: RectangleRo): Rectangle {
		val ratio = getAspectRatio()

		if (ratio < rect.getAspectRatio()) {
			// Taller than wide
			setSize(rect.height * ratio, rect.height)
		} else {
			// Wider than tall
			setSize(rect.width, rect.width / ratio)
		}

		setPosition((rect.x + rect.width / 2) - width / 2, (rect.y + rect.height / 2) - height / 2)
		return this
	}

	/**
	 * Returns true if this rectangle's bounds can contain the given dimensions
	 * Note: x, y coordinates are not considered.
	 */
	override fun canContain(width: Float, height: Float): Boolean {
		return this.width >= width && this.height >= height
	}

	override fun area(): Float {
		return this.width * this.height
	}

	override fun perimeter(): Float {
		return 2 * (this.width + this.height)
	}

	fun inflate(left: Float, top: Float, right: Float, bottom: Float) {
		x -= left
		width += left + right
		y -= top
		height += top + bottom
	}

	fun inflate(pad: PadRo) = inflate(pad.left, pad.top, pad.right, pad.bottom)

	/**
	 * Extends this rectangle to include the given coordinates.
	 */
	fun ext(x2: Float, y2: Float) {
		if (x2 > x + width) width = x2 - x
		if (x2 < x) x = x2
		if (y2 > y + height) height = y2 - y
		if (y2 < y) y = y2
	}

	/**
	 * Extends this rectangle by the other rectangle. The rectangle should not have negative width or negative height.
	 * @param rect the other rectangle
	 * @return this rectangle for chaining
	 */
	fun ext(rect: RectangleRo): Rectangle {
		val minX = minOf(x, rect.x)
		val maxX = maxOf(x + width, rect.x + rect.width)
		x = minX
		width = maxX - minX

		val minY = minOf(y, rect.y)
		val maxY = maxOf(y + height, rect.y + rect.height)
		y = minY
		height = maxY - minY

		return this
	}

	fun scl(scalar: Float) {
		x *= scalar
		y *= scalar
		width *= scalar
		height *= scalar
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		other as RectangleRo

		if (x != other.x) return false
		if (y != other.y) return false
		if (width != other.width) return false
		if (height != other.height) return false

		return true
	}

	override fun hashCode(): Int {
		var result = x.hashCode()
		result = 31 * result + y.hashCode()
		result = 31 * result + width.hashCode()
		result = 31 * result + height.hashCode()
		return result
	}

	override fun toString(): String {
		return "Rectangle(x=$x, y=$y, width=$width, height=$height)"
	}

	companion object {
		private val pool = ClearableObjectPool { Rectangle() }
		fun obtain(): Rectangle = pool.obtain()
		fun free(obj: Rectangle) = pool.free(obj)
	}
}

object RectangleSerializer : To<RectangleRo>, From<Rectangle> {

	override fun RectangleRo.write(writer: Writer) {
		writer.float("x", x)
		writer.float("y", y)
		writer.float("width", width)
		writer.float("height", height)
	}

	override fun read(reader: Reader): Rectangle {
		return Rectangle(
				x = reader.float("x")!!,
				y = reader.float("y")!!,
				width = reader.float("width")!!,
				height = reader.float("height")!!
		)
	}
}
