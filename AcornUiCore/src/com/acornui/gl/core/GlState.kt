/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.gl.core

import com.acornui.collection.ArrayList
import com.acornui.collection.arrayCopy
import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.graphics.*
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.Matrix4
import com.acornui.math.Matrix4Ro

/**
 * GlState stores OpenGl state information necessary for knowing whether basic operations can be batched.
 *
 * @author nbilyk
 */
class GlState(
		private val gl: Gl20
) : Disposable {

	var defaultShader: ShaderProgram = DefaultShaderProgram(gl)

	private val viewProjectionCache = MatrixCache(gl, ShaderProgram.U_PROJ_TRANS)
	private val modelCache = MatrixCache(gl, ShaderProgram.U_MODEL_TRANS)

	private var _activeTexture: Int = -1

	private val _boundTextures: Array<Texture?> = Array(30, { null })

	private var _batch: ShaderBatch = ShaderBatchImpl(gl, this)

	/**
	 * The shader batch, used by gl backend acorn components.
	 */
	var batch: ShaderBatch
		get() = _batch
		set(value) {
			if (_batch == value) return
			_batch.flush(true)
			_batch = value
		}

	private val defaultWhitePixel by lazy {
		rgbTexture(gl, this, rgbData(1, 1, hasAlpha = true) { setPixel(0, 0, Color.WHITE) }) {
			filterMin = TextureMinFilter.NEAREST
			filterMag = TextureMagFilter.NEAREST
			refInc()
		}
	}

	private var _whitePixel: Texture? = null

	/**
	 * To reduce batch flushing from switching back and forth from a texture to a single white pixel, as is the case
	 * in mesh drawings, a texture can declare that its 0,0 pixel is white.
	 * The acorn texture packer will optionally add this white pixel.
	 */
	val whitePixel: Texture
		get() = _whitePixel ?: defaultWhitePixel

	/**
	 * @see Gl20.activeTexture
	 */
	fun activeTexture(value: Int) {
		if (value < 0 || value > 30) throw IllegalArgumentException("Texture index must be between 0 and 30")
		if (_activeTexture == value) return
		_activeTexture = value
		gl.activeTexture(Gl20.TEXTURE0 + value)
	}

	fun getTexture(unit: Int): Texture? {
		return _boundTextures[unit]
	}

	fun setTexture(texture: Texture? = null, unit: Int = 0) {
		val previous: Texture? = _boundTextures[unit]
		if (previous == texture && _activeTexture == unit) return
		batch.flush(true)
		_whitePixel = if (unit == 0 && texture?.hasWhitePixel == true) texture else null
		activeTexture(unit)
		_boundTextures[unit] = texture
		if (texture == null) {
			if (previous != null) {
				gl.bindTexture(previous.target.value, null)
			}
		} else {
			if (texture.textureHandle == null) throw Exception("Texture is not initialized. Use texture.refInc() before binding.")
			gl.bindTexture(texture.target.value, texture.textureHandle!!)
		}
	}

	/**
	 * For any unit where this texture is bound, unbind it.
	 */
	fun unsetTexture(texture: Texture) {
		for (i in 0.._boundTextures.lastIndex) {
			if (_boundTextures[i] == texture) {
				setTexture(null, i)
			}
		}
	}

	/**
	 * The ShaderProgram uploads GLSL to the GPU.
	 */
	private var _shader: ShaderProgram? = null

	var shader: ShaderProgram?
		get() = _shader
		set(value) {
			if (_shader == value) return
			batch.flush(true)
			_shader?.unbind()
			_shader = value
			_shader?.bind()
		}

	//----------------------------------------------------
	// Blending
	//----------------------------------------------------

	private var _blendMode: BlendMode = BlendMode.NONE
	private var _premultipliedAlpha: Boolean = false

	private var _blendingEnabled = true

	/**
	 * A global override to disable blending. Useful if the shader doesn't support blending.
	 */
	var blendingEnabled: Boolean
		get() = _blendingEnabled
		set(value) {
			if (_blendingEnabled == value) return
			batch.flush(true)
			_blendingEnabled = value
			refreshBlendMode()
		}

	val blendMode: BlendMode
		get() = _blendMode

	val premultipliedAlpha: Boolean
		get() = _premultipliedAlpha

	fun blendMode(blendMode: BlendMode, premultipliedAlpha: Boolean) {
		if (_blendMode == blendMode && _premultipliedAlpha == premultipliedAlpha) return
		batch.flush(true)
		_blendMode = blendMode
		_premultipliedAlpha = premultipliedAlpha
		if (!_blendingEnabled) return
		refreshBlendMode()
	}

	private fun refreshBlendMode() {
		if (!_blendingEnabled || _blendMode == BlendMode.NONE) {
			gl.disable(Gl20.BLEND)
		} else {
			gl.enable(Gl20.BLEND)
			_blendMode.applyBlending(gl, _premultipliedAlpha)
		}
	}

	private val _mvp = Matrix4()

	fun camera(camera: CameraRo, model: Matrix4Ro = Matrix4.IDENTITY) {
		val hasModel = _shader!!.getUniformLocation(ShaderProgram.U_MODEL_TRANS) != null
		if (hasModel) {
			viewProjection(camera.combined.values)
			model(model.values)
		} else {
			_mvp.set(camera.combined)
			if (model !== Matrix4.IDENTITY) _mvp.mul(model)
			viewProjection(_mvp.values)
		}
	}

	fun projTrans(value: Matrix4Ro) = viewProjection(value.values)

	/**
	 * Applies the given matrix as the view-projection transformation.
	 */
	fun viewProjection(value: List<Float>) {
		viewProjectionCache.set(value, _shader!!, batch)
	}

	/**
	 * Applies the given matrix as the model transformation.
	 */
	fun model(value: List<Float>) {
		modelCache.set(value, _shader!!, batch)
	}

	init {
		shader = defaultShader
		blendMode(BlendMode.NORMAL, premultipliedAlpha = false)
	}

	override fun dispose() {
		defaultWhitePixel.refDec()
		_whitePixel = null
		shader = null
	}

	companion object : DKey<GlState>


}

private class MatrixCache(
		private val gl: Gl20,
		private val name: String) {

	private val values = ArrayList(16, { 0f })
	private var _shader: ShaderProgram? = null

	/**
	 * Applies the given matrix as the model transformation.
	 */
	fun set(value: List<Float>, shader: ShaderProgram, batch: ShaderBatch) {
		val uniform = shader.getUniformLocation(name) ?: return
		if (_shader != shader || value != values) {
			batch.flush(true)
			_shader = shader
			arrayCopy(value, 0, values)
			gl.uniformMatrix4fv(uniform, false, value)
		}
	}
}

private class ColorCache(
		private val gl: Gl20,
		private val name: String) {

	private val _value = Color()
	private var _shader: ShaderProgram? = null

	fun set(value: ColorRo, shader: ShaderProgram, batch: ShaderBatch) {
		val uniform = shader.getUniformLocation(name) ?: return
		if (_shader != shader || value != _value) {
			batch.flush(true)
			_shader = shader
			_value.set(value)
			gl.uniform4f(uniform, value)
		}
	}
}