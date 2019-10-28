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

package com.acornui.gl.core

import com.acornui.Disposable
import com.acornui.di.DKey
import com.acornui.graphic.*
import com.acornui.math.*
import com.acornui.reflect.observable
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The gl state contains a [ShaderBatch] and a set of gl state properties. When properties are changed, the batch is
 * flushed.
 *
 */
interface GlState {

	/**
	 * The shader batch, used by gl backend acorn components.
	 */
	var batch: ShaderBatch

	/**
	 * Sets the shader program.  If this is being changed, it should be set before [setCamera].
	 */
	var shader: ShaderProgram?

	/**
	 * A global override to disable blending. Useful if the shader doesn't support blending.
	 */
	var blendingEnabled: Boolean

	val blendMode: BlendMode

	val premultipliedAlpha: Boolean

	/**
	 * Sets the color transformation matrix and offset uniforms.
	 */
	var colorTransformation: ColorTransformationRo?

	/**
	 * Returns whether scissoring is currently enabled.
	 * @see useScissor
	 */
	var scissorEnabled: Boolean

	/**
	 * To reduce batch flushing from switching back and forth from a texture to a single white pixel, as is the case
	 * in mesh drawings, a texture can declare that its 0,0 pixel is white.
	 * The acorn texture packer will optionally add this white pixel.
	 */
	val whitePixel: TextureRo

	/**
	 * @see Gl20.activeTexture
	 */
	fun activeTexture(value: Int)

	fun getTexture(unit: Int): TextureRo?
	fun setTexture(texture: TextureRo? = null, unit: Int = 0)

	/**
	 * For any unit where this texture is bound, unbind it.
	 */
	fun unsetTexture(texture: TextureRo)

	fun blendMode(blendMode: BlendMode, premultipliedAlpha: Boolean)

	/**
	 * The current scissor rectangle.
	 */
	val scissor: IntRectangleRo

	@Deprecated("use out.set(scissor)", ReplaceWith("out.set(scissor)"))
	fun getScissor(out: IntRectangle): IntRectangle = out.set(scissor)

	/**
	 * @see Gl20.scissor
	 */
	fun setScissor(x: Int, y: Int, width: Int, height: Int)

	/**
	 * Sets the model, view, and projection matrices.
	 * This will set the gl uniforms `u_modelTrans` (if exists), `u_viewTrans` (if exists), and `u_projTrans`
	 * The shader should have the following uniforms:
	 * `u_projTrans` - Either MVP or VP if u_modelTrans is present.
	 * `u_modelTrans` (optional) - M
	 * `u_viewTrans` (optional) - V
	 */
	fun setCamera(viewProjection: Matrix4Ro, viewTransform: Matrix4Ro, modelTransform: Matrix4Ro = Matrix4.IDENTITY) {
		shader!!.uniforms.setCamera(viewProjection, viewTransform, modelTransform)
	}

	/**
	 * The current viewport rectangle, in gl window coordinates.
	 * (0,0 is bottom left, width, height includes dpi scaling)
	 */
	val viewport: IntRectangleRo

	/**
	 * Gets the current viewport in gl window coordinates. (0,0 is bottom left, width, height includes dpi scaling)
	 * This is not to be confused with UiComponent.viewport, which is in canvas coordinates.
	 *
	 * @param out Sets this rectangle to the current viewport.
	 * @return Returns the [out] parameter.
	 */
	@Deprecated("use out.set(viewport)", ReplaceWith("out.set(viewport)"))
	fun getViewport(out: IntRectangle): IntRectangle = out.set(viewport)

	/**
	 * @see Gl20.viewport
	 */
	fun setViewport(x: Int, y: Int, width: Int, height: Int)

	/**
	 * Gets the current framebuffer, populating the [out] parameter.
	 */
	fun getFramebuffer(out: FramebufferInfo)

	/**
	 * Sets the current framebuffer and some information about it.
	 */
	fun setFramebuffer(framebuffer: GlFramebufferRef?,
					   width: Int,
					   height: Int,
					   scaleX: Float,
					   scaleY: Float)

	companion object : DKey<GlState>
}

/**
 * GlState stores OpenGl state information necessary for knowing whether basic draw calls can be batched.
 *
 * @author nbilyk
 */
class GlStateImpl(
		private val gl: Gl20,
		window: Window
) : GlState, Disposable {

	/**
	 * The default shader for this application.
	 */
	private val defaultShader: ShaderProgram = DefaultShaderProgram(gl)

	private var _activeTexture: Int = -1

	private val _boundTextures: Array<TextureRo?> = Array(Gl20.MAX_COMBINED_TEXTURE_IMAGE_UNITS) { null }

	private var _batch: ShaderBatch = ShaderBatchImpl(gl, this, uiVertexAttributes)

	override var batch: ShaderBatch
		get() = _batch
		set(value) {
			if (_batch == value) return
			_batch.flush()
			_batch = value
		}

	private val defaultWhitePixel by lazy {
		rgbTexture(gl, this, rgbData(1, 1, hasAlpha = true) { setPixel(0, 0, Color.WHITE) }) {
			filterMin = TextureMinFilter.NEAREST
			filterMag = TextureMagFilter.NEAREST
			refInc()
		}
	}

	private var _whitePixel: TextureRo? = null

	override val whitePixel: TextureRo
		get() = _whitePixel ?: defaultWhitePixel

	override fun activeTexture(value: Int) {
		if (value < 0 || value >= Gl20.MAX_COMBINED_TEXTURE_IMAGE_UNITS) throw IllegalArgumentException("Texture index must be between 0 and ${Gl20.MAX_COMBINED_TEXTURE_IMAGE_UNITS - 1}")
		if (_activeTexture == value) return
		batch.flush()
		_activeTexture = value
		gl.activeTexture(Gl20.TEXTURE0 + value)
	}

	override fun getTexture(unit: Int): TextureRo? {
		return _boundTextures[unit]
	}

	override fun setTexture(texture: TextureRo?, unit: Int) {
		val previous = _boundTextures[unit]
		if (previous == texture && _activeTexture == unit) return
		batch.flush()
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

	override fun unsetTexture(texture: TextureRo) {
		for (i in 0.._boundTextures.lastIndex) {
			if (_boundTextures[i] == texture) {
				setTexture(null, i)
			}
		}
	}

	private var _shader: ShaderProgram? = null

	override var shader: ShaderProgram?
		get() = _shader
		set(value) {
			if (_shader == value) return
			_shader?.uniforms?.changing?.remove(::uniformsChangingHandler)
			batch.flush()
			_shader?.unbind()
			_shader = value
			_shader?.bind()
			_shader?.uniforms?.changing?.add(::uniformsChangingHandler)
		}

	private fun uniformsChangingHandler() {
		batch.flush()
	}

	//----------------------------------------------------
	// Blending
	//----------------------------------------------------

	private var _blendMode: BlendMode = BlendMode.NONE
	private var _premultipliedAlpha: Boolean = false

	private var _blendingEnabled = true

	override var blendingEnabled: Boolean
		get() = _blendingEnabled
		set(value) {
			if (_blendingEnabled == value) return
			batch.flush()
			_blendingEnabled = value
			refreshBlendMode()
		}

	override val blendMode: BlendMode
		get() = _blendMode

	override val premultipliedAlpha: Boolean
		get() = _premultipliedAlpha

	override fun blendMode(blendMode: BlendMode, premultipliedAlpha: Boolean) {
		if (_blendMode == blendMode && _premultipliedAlpha == premultipliedAlpha) return
		batch.flush()
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

	//----------------------------------------------------

	private var _viewport = IntRectangle()

	override val viewport: IntRectangleRo = _viewport

	override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
		if (_viewport.x == x && _viewport.y == y && _viewport.width == width && _viewport.height == height) return
		_batch.flush()
		_viewport.set(x, y, width, height)
		gl.viewport(x, y, width, height)
	}

	override var scissorEnabled: Boolean by observable(false) {
		batch.flush()
		if (it)
			gl.enable(Gl20.SCISSOR_TEST)
		else
			gl.disable(Gl20.SCISSOR_TEST)
	}

	private val _framebuffer = FramebufferInfo(null, window.framebufferWidth, window.framebufferHeight, window.scaleX, window.scaleY)

	override fun getFramebuffer(out: FramebufferInfo) = out.set(_framebuffer)

	override fun setFramebuffer(framebuffer: GlFramebufferRef?,
								width: Int,
								height: Int,
								scaleX: Float,
								scaleY: Float) {
		if (_framebuffer.equals(framebuffer, width, height, scaleX, scaleY)) return
		batch.flush()
		_framebuffer.set(framebuffer, width, height, scaleX, scaleY)
		gl.bindFramebuffer(Gl20.FRAMEBUFFER, framebuffer)
	}

	private val _scissor = IntRectangle()
	override val scissor: IntRectangleRo = _scissor

	override fun setScissor(x: Int, y: Int, width: Int, height: Int) {
		if (_scissor.x != x || _scissor.y != y || _scissor.width != width || _scissor.height != height) {
			batch.flush()
			_scissor.set(x, y, width, height)
			gl.scissor(x, y, width, height)
		}
	}

	private var _colorTransformationIsSet = false
	private var _colorTransformation = ColorTransformation()

	override var colorTransformation: ColorTransformationRo?
		get() = if (_colorTransformationIsSet) _colorTransformation else null
		set(value) {
			batch.flush()
			val uniforms = _shader!!.uniforms
			val useColorTransU = uniforms.getUniformLocation(CommonShaderUniforms.U_USE_COLOR_TRANS)
			if (useColorTransU != null) {
				if (value == null) {
					_colorTransformationIsSet = false
					uniforms.put(useColorTransU, 0)
				} else {
					_colorTransformationIsSet = true
					_colorTransformation.set(value)
					uniforms.put(useColorTransU, 1)

					val colorTransU = uniforms.getRequiredUniformLocation(CommonShaderUniforms.U_COLOR_TRANS)
					uniforms.put(colorTransU, value.matrix)

					val colorOffsetU = uniforms.getRequiredUniformLocation(CommonShaderUniforms.U_COLOR_OFFSET)
					uniforms.putRgba(colorOffsetU, value.offset)
				}
			}
		}

	init {
		shader = defaultShader
		blendMode(BlendMode.NORMAL, premultipliedAlpha = false)
	}

	override fun dispose() {
		defaultWhitePixel.refDec()
		_whitePixel = null
		shader = null
		defaultShader.dispose()
	}

}

/**
 * Temporarily uses a scissor rectangle, resetting to the old scissor rectangle after [inner].
 * @see Gl20.setScissor
 */
inline fun GlState.useScissor(x: Int, y: Int, width: Int, height: Int, inner: () -> Unit) {
	val oldScissor = IntRectangle.obtain().set(scissor)
	val oldEnabled = scissorEnabled
	scissorEnabled = true
	setScissor(x, y, width, height)
	inner()
	scissorEnabled = oldEnabled
	setScissor(oldScissor)
	IntRectangle.free(oldScissor)
}


/**
 * Temporarily uses a viewport, resetting to the old viewport after [inner].
 * @see Gl20.viewport
 */
inline fun GlState.useViewport(x: Int, y: Int, width: Int, height: Int, inner: () -> Unit) {
	val oldViewport = IntRectangle.obtain().set(viewport)
	setViewport(x, y, width, height)
	inner()
	setViewport(oldViewport)
	IntRectangle.free(oldViewport)
}

private val framebufferInfo = FramebufferInfo()

fun GlState.useViewportFromCanvasTransform(canvasTransform: RectangleRo, inner: () -> Unit) {
	getFramebuffer(framebufferInfo)
	useViewport(
			floor(canvasTransform.x * framebufferInfo.scaleX).toInt(),
			floor((framebufferInfo.height - canvasTransform.bottom * framebufferInfo.scaleY)).toInt(),
			ceil(canvasTransform.width * framebufferInfo.scaleX).toInt(),
			ceil(canvasTransform.height * framebufferInfo.scaleY).toInt(),
			inner
	)
}

/**
 * Temporarily uses a shader, resetting to the old shader after [inner].
 */
inline fun GlState.useShader(s: ShaderProgram, inner: () -> Unit) {
	val previousShader = shader
	shader = s
	inner()
	shader = previousShader
}

/**
 * Temporarily uses a batch, resetting to the old batch after [inner].
 */
inline fun GlState.useBatch(b: ShaderBatch, inner: () -> Unit) {
	val previousBatch = this.batch
	batch = b
	inner()
	batch = previousBatch
}

/**
 * @see Gl20.viewport
 */
fun GlState.setViewport(value: IntRectangleRo) = setViewport(value.x, value.y, maxOf(0, value.width), maxOf(0, value.height))

fun GlState.setScissor(value: IntRectangleRo) = setScissor(value.x, value.y, value.width, value.height)

fun GlState.setFramebuffer(value: FramebufferInfoRo) = setFramebuffer(value.framebuffer, value.width, value.height, value.scaleX, value.scaleY)

private val combined = ColorTransformation()

/**
 * Adds a color transformation to the current stack, using that color transformation within [inner].
 */
fun GlState.useColorTransformation(cT: ColorTransformationRo, inner: () -> Unit) {
	val wasSet = colorTransformation != null
	val previous = if (wasSet) ColorTransformation.obtain().set(colorTransformation!!) else null
	colorTransformation = combined.combine(previous, cT)
	inner()
	if (wasSet) {
		colorTransformation = previous
		ColorTransformation.free(previous!!)
	} else {
		colorTransformation = null
	}
}

fun ColorTransformation.combine(previous: ColorTransformationRo?, cT: ColorTransformationRo): ColorTransformation {
	return if (previous != null) set(previous).mul(cT) else set(cT)
}

private val mvp = Matrix4()
private val tmpMat = Matrix3()

fun Uniforms.getCamera(viewProjectionOut: Matrix4, viewTransformOut: Matrix4, modelTransformOut: Matrix4) {
	getUniformLocation(CommonShaderUniforms.U_PROJ_TRANS)?.let {
		get(it, viewProjectionOut)
	}
	getUniformLocation(CommonShaderUniforms.U_VIEW_TRANS)?.let {
		get(it, viewTransformOut)
	}
	getUniformLocation(CommonShaderUniforms.U_MODEL_TRANS)?.let {
		get(it, modelTransformOut)
	}
}

fun Uniforms.setCamera(viewProjection: Matrix4Ro, viewTransform: Matrix4Ro, modelTransform: Matrix4Ro) {
	val hasModel = getUniformLocation(CommonShaderUniforms.U_MODEL_TRANS) != null
	if (hasModel) {
		putOptional(CommonShaderUniforms.U_PROJ_TRANS, viewProjection)
		putOptional(CommonShaderUniforms.U_VIEW_TRANS, viewTransform)
		putOptional(CommonShaderUniforms.U_MODEL_TRANS, modelTransform)
		getUniformLocation(CommonShaderUniforms.U_NORMAL_TRANS)?.let {
			tmpMat.set(modelTransform).setTranslation(0f, 0f).inv().tra()
			put(it, tmpMat)
		}
	} else {
		val v = if (modelTransform.isIdentity) viewProjection else mvp.set(viewProjection).mul(modelTransform)
		putOptional(CommonShaderUniforms.U_PROJ_TRANS, v)
	}
}

fun Uniforms.setCamera(camera: CameraRo, model: Matrix4Ro = Matrix4.IDENTITY) = setCamera(camera.combined, camera.view, model)

/**
 * Temporarily uses a camera, resetting the uniforms when [inner] has completed.
 */
fun Uniforms.useCamera(viewProjection: Matrix4Ro, viewTransform: Matrix4Ro, modelTransform: Matrix4Ro, inner: ()->Unit) {
	val previousViewProjection = Matrix4.obtain()
	val previousViewTransform = Matrix4.obtain()
	val previousModelTransform = Matrix4.obtain()
	getCamera(previousViewProjection, previousViewTransform, previousModelTransform)
	setCamera(viewProjection, viewTransform, modelTransform)
	inner()
	setCamera(previousViewProjection, previousViewTransform, previousModelTransform)
	Matrix4.free(previousViewProjection)
	Matrix4.free(previousViewTransform)
	Matrix4.free(previousModelTransform)
}