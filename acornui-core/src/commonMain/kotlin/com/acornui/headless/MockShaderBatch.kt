package com.acornui.headless

import com.acornui.gl.core.DrawElementsCall
import com.acornui.gl.core.ShaderBatch
import com.acornui.gl.core.VertexAttributes
import com.acornui.graphic.BlendMode
import com.acornui.graphic.TextureRo
import com.acornui.io.ReadWriteBuffer

object MockShaderBatch : ShaderBatch {

	override val isDynamic: Boolean = false

	override val whitePixel: TextureRo = MockTexture

	override fun begin(texture: TextureRo, blendMode: BlendMode, premultipliedAlpha: Boolean, drawMode: Int) {
	}

	override fun flush() {
	}

	override fun putVertex(positionX: Float, positionY: Float, positionZ: Float, normalX: Float, normalY: Float, normalZ: Float, colorR: Float, colorG: Float, colorB: Float, colorA: Float, u: Float, v: Float) {
	}

	override val vertexComponentsCount: Int = 0
	override val vertexAttributes: VertexAttributes = VertexAttributes(emptyList())

	override fun putVertexComponent(value: Float) {
	}

	override val indicesCount: Int = 0
	override val highestIndex: Short = 0

	override fun putIndex(index: Short) {
	}

	override val indices: ReadWriteBuffer<Short> = MockReadWriteBuffer()
	override val vertexComponents: ReadWriteBuffer<Float> = MockReadWriteBuffer()
	override val drawCalls: MutableList<DrawElementsCall> = ArrayList()

	override fun delete() {
	}

	override fun upload() {
	}

	override fun render() {
	}

	override fun clear() {
	}
}
