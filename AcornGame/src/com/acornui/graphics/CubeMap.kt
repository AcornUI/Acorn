package com.acornui.graphics


import com.acornui.core.graphics.Texture
import com.acornui.gl.core.*

/**
 * Wraps a standard OpenGL ES Cube map. Must be disposed when it is no longer used.
 * @author Xoppa
 * @author nbilyk
 */
class CubeMap(
		positiveX: Texture,
		negativeX: Texture,
		positiveY: Texture,
		negativeY: Texture,
		positiveZ: Texture,
		negativeZ: Texture,

		gl: Gl20,
		glState: GlState
) : GlTextureBase(gl, glState) {

	private val sides = arrayOf(positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ)

	private val _width: Int
	private val _height: Int
	private val _depth: Int

	private val firstSideOrdinal = TextureTarget.TEXTURE_CUBE_MAP_POSITIVE_X.ordinal

	init {
		target = TextureTarget.TEXTURE_CUBE_MAP
		filterMin = TextureMinFilter.LINEAR
		filterMag = TextureMagFilter.LINEAR
		wrapS = TextureWrapMode.CLAMP_TO_EDGE
		wrapT = TextureWrapMode.CLAMP_TO_EDGE

		var w = 0
		if (positiveZ.width > w) w = positiveZ.width
		if (negativeZ.width > w) w = negativeZ.width
		if (positiveY.width > w) w = positiveY.width
		if (negativeY.width > w) w = negativeY.width
		this._width = w

		var h = 0
		if (positiveZ.height > h) h = positiveZ.height
		if (negativeZ.height > h) h = negativeZ.height
		if (positiveX.height > h) h = positiveX.height
		if (negativeX.height > h) h = negativeX.height
		this._height = h

		var d = 0
		if (positiveX.width > d) d = positiveX.width
		if (negativeX.width > d) d = negativeX.width
		if (positiveY.height > d) d = positiveY.height
		if (negativeY.height > d) d = negativeY.height
		this._depth = d
	}

	override fun uploadTexture() {
		for (i in 0..sides.lastIndex) {
			val side = sides[i]
			side.target = TextureTarget.VALUES[i + firstSideOrdinal]

			// TODO: The cubemap shouldn't force the creation like this:
			side.textureHandle = gl.createTexture()
			gl.texImage2Db(side.target.value, 0, pixelFormat.value, side.width, side.height, 0, pixelFormat.value, pixelType.value, null)
		}
//		gl.texImage2Db(target.value, 0, pixelFormat.value, _width, _height, 0, pixelFormat.value, pixelType.value, null)
	}

	override fun delete() {
		super.delete()
		for (i in 0..sides.lastIndex) {
			gl.deleteTexture(sides[i].textureHandle!!)
			sides[i].textureHandle = null
		}
	}

	/**
	 * @return The [Texture] for the specified side, can be null if the cube map is incomplete.
	 */
	fun getSide(target: TextureTarget): Texture {
		return sides[target.ordinal - firstSideOrdinal]
	}

	override val width: Int
		get() = _width

	override val height: Int
		get() = _height

	val depth: Int
		get() = _depth

}
