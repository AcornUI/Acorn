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

package com.acornui.math

import com.acornui.recycle.Clearable
import kotlinx.serialization.*
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.FloatSerializer
import kotlinx.serialization.internal.StringDescriptor
import kotlin.math.ceil

/**
 * A read-only interface to [Pad]
 */
@Serializable(with = PadSerializer::class)
interface PadRo {
	val top: Float
	val right: Float
	val bottom: Float
	val left: Float

	fun isEmpty(): Boolean = top == 0f && right == 0f && bottom == 0f && left == 0f
	fun isNotEmpty(): Boolean = !isEmpty()

	fun reduceWidth(width: Float?): Float? {
		if (width == null) return null
		return width - left - right
	}

	fun reduceHeight(height: Float?): Float? {
		if (height == null) return null
		return height - top - bottom
	}

	fun reduceWidth(width: Float): Float {
		return width - left - right
	}

	fun reduceHeight(height: Float): Float {
		return height - top - bottom
	}

	fun expandWidth(width: Float?): Float? {
		if (width == null) return null
		return width + left + right
	}

	fun expandHeight(height: Float?): Float? {
		if (height == null) return null
		return height + top + bottom
	}

	fun expandWidth(width: Float): Float {
		return width + left + right
	}

	fun expandHeight(height: Float): Float {
		return height + top + bottom
	}

	fun toCssString(): String {
		return "${top}px ${right}px ${bottom}px ${left}px"
	}

	fun copy(top: Float = this.top, right: Float = this.right, bottom: Float = this.bottom, left: Float = this.left): Pad {
		return Pad(top, right, bottom, left)
	}
}

/**
 * A representation of margins or padding.
 *
 * @author nbilyk
 */
@Serializable(with = PadSerializer::class)
class Pad(
		override var top: Float,
		override var right: Float,
		override var bottom: Float,
		override var left: Float) : PadRo, Clearable {

	constructor() : this(0f, 0f, 0f, 0f)

	constructor(all: Float) : this(all, all, all, all)

	constructor(all: Array<Float>) : this(all[0], all[1], all[2], all[3])

	fun set(all: Float): Pad {
		top = all
		bottom = all
		right = all
		left = all
		return this
	}

	fun set(other: PadRo): Pad {
		top = other.top
		bottom = other.bottom
		right = other.right
		left = other.left
		return this
	}

	fun set(top: Float = 0f, right: Float = 0f, bottom: Float = 0f, left: Float = 0f): Pad {
		this.top = top
		this.right = right
		this.bottom = bottom
		this.left = left
		return this
	}

	/**
	 * Inflates the padding by the given amount.
	 */
	fun inflate(padding: PadRo): Pad {
		left += padding.left
		top += padding.top
		right += padding.right
		bottom += padding.bottom
		return this
	}

	/**
	 * Scales all padding values by the given scalar.
	 */
	fun scl(scalar: Float) {
		left *= scalar
		top *= scalar
		right *= scalar
		bottom *= scalar
	}

	/**
	 * Ceils each value of this padding object. E.g. `top = ceil(top)`
	 */
	fun ceil(): Pad {
		top = ceil(top)
		right = ceil(right)
		bottom = ceil(bottom)
		left = ceil(left)
		return this
	}

	override fun clear() {
		top = 0f
		right = 0f
		bottom = 0f
		left = 0f
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is PadRo) return false

		if (top != other.top) return false
		if (right != other.right) return false
		if (bottom != other.bottom) return false
		if (left != other.left) return false

		return true
	}

	override fun hashCode(): Int {
		var result = top.hashCode()
		result = 31 * result + right.hashCode()
		result = 31 * result + bottom.hashCode()
		result = 31 * result + left.hashCode()
		return result
	}

	override fun toString(): String {
		return "Pad(top=$top, right=$right, bottom=$bottom, left=$left)"
	}


	companion object {
		val EMPTY_PAD: PadRo = Pad()
	}
}

@Serializer(forClass = Pad::class)
object PadSerializer : KSerializer<Pad> {

	override val descriptor: SerialDescriptor =
			StringDescriptor.withName("Pad")

	override fun serialize(encoder: Encoder, obj: Pad) {
		encoder.encodeSerializableValue(ArrayListSerializer(FloatSerializer), listOf(obj.top, obj.right, obj.bottom, obj.left))
	}

	override fun deserialize(decoder: Decoder): Pad {
		val values = decoder.decodeSerializableValue(ArrayListSerializer(FloatSerializer))
		return Pad(
				top = values[0],
				right = values[1],
				bottom = values[2],
				left = values[3]
		)
	}
}