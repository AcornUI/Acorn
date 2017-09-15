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

package com.acornui.gl.component.drawing

import com.acornui._assert
import com.acornui.assertionsEnabled
import com.acornui.collection.Clearable
import com.acornui.collection.arrayListObtain
import com.acornui.collection.arrayListPool
import com.acornui.component.ComponentInit
import com.acornui.component.UiComponentImpl
import com.acornui.component.ValidationFlags
import com.acornui.core.TreeWalk
import com.acornui.core.childWalkPreOrder
import com.acornui.core.childWalkPreOrderReversed
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.graphics.BlendMode
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.gl.core.Vertex
import com.acornui.math.*


/**
 * A UiComponent for drawing [MeshData].
 * This component should be for dynamic meshes with few vertices. Use [StaticMesh] when displaying static
 * 3d meshes with many vertices.
 *
 * @author nbilyk
 */
open class DynamicMeshComponent(
		owner: Owned
) : UiComponentImpl(owner), Clearable {

	private val data: MeshData = MeshData()

	var intersectionType = MeshIntersectionType.BOUNDING_BOX

	protected val boundingBox = Box()
	private val globalBoundingBox = Box()

	private val glState = inject(GlState)

	private var globalPrimitivesAreValid = false
	private val _globalPrimitives = ArrayList<MutableList<Vertex>>()
	private val globalPrimitives: List<List<Vertex>>
		get() {
			if (!globalPrimitivesAreValid) {
				globalPrimitivesAreValid = true
				clearGlobalPrimitives()
				data.childWalkPreOrder {
					primitive ->
					val globalPrimitive = arrayListObtain<Vertex>()
					for (j in 0..primitive.vertices.lastIndex) {
						val localVertex = primitive.vertices[j]
						val globalVertex = Vertex.obtain()
						globalVertex.u = localVertex.u
						globalVertex.v = localVertex.v
						globalPrimitive.add(globalVertex)
					}
					_globalPrimitives.add(globalPrimitive)
					TreeWalk.CONTINUE
				}
			}
			return _globalPrimitives

		}

	private fun clearGlobalPrimitives() {
		for (i in 0.._globalPrimitives.lastIndex) {
			for (j in 0.._globalPrimitives[i].lastIndex) {
				_globalPrimitives[i][j].free()
			}
			arrayListPool.free(_globalPrimitives[i])
		}
		_globalPrimitives.clear()
	}

	init {
		validation.addNode(VERTEX_TRANSFORM, ValidationFlags.CONCATENATED_TRANSFORM, { validateGlobalTransform() })
		validation.addNode(VERTEX_COLOR_TRANSFORM, ValidationFlags.CONCATENATED_COLOR_TRANSFORM, { validateGlobalColor() })
		validation.addNode(GLOBAL_BOUNDING_BOX, ValidationFlags.LAYOUT or ValidationFlags.CONCATENATED_TRANSFORM, { globalBoundingBox.set(boundingBox).mul(concatenatedTransform) })
	}

	fun buildMesh(inner: MeshData.() -> Unit) {
		fillStyle.clear()
		lineStyle.clear()
		data.clear()
		data.inner()
		invalidate(VERTEX_TRANSFORM or VERTEX_COLOR_TRANSFORM or ValidationFlags.LAYOUT)
		globalPrimitivesAreValid = false
	}

	override fun clear() {
		data.clear()
		invalidate(VERTEX_TRANSFORM or VERTEX_COLOR_TRANSFORM or ValidationFlags.LAYOUT)
		globalPrimitivesAreValid = false
	}

	private fun validateGlobalTransform() {
		var index = 0
		data.childWalkPreOrder {
			primitive ->
			val cT = concatenatedTransform
			val globalPrimitive = globalPrimitives[index++]
			for (j in 0..primitive.vertices.lastIndex) {
				val localVertex = primitive.vertices[j]
				val globalVertex = globalPrimitive[j]
				cT.prj(globalVertex.position.set(localVertex.position))
				cT.rot(globalVertex.normal.set(localVertex.normal))
			}
			TreeWalk.CONTINUE
		}
	}

	private fun validateGlobalColor() {
		var index = 0
		data.childWalkPreOrder {
			primitive ->
			val cCt = concatenatedColorTint
			val globalPrimitive = globalPrimitives[index++]
			for (j in 0..primitive.vertices.lastIndex) {
				val localVertex = primitive.vertices[j]
				val globalVertex = globalPrimitive[j]
				globalVertex.colorTint.set(localVertex.colorTint).mul(cCt)
			}
			TreeWalk.CONTINUE
		}
	}

	/**
	 * Overrides intersectsGlobalRay to check against each drawn triangle.
	 */
	override fun intersectsGlobalRay(globalRay: RayRo, intersection: Vector3): Boolean {
		validate(VERTEX_TRANSFORM or GLOBAL_BOUNDING_BOX)

		if (!globalBoundingBox.intersects(globalRay, intersection)) {
			return false
		}
		if (intersectionType == MeshIntersectionType.EXACT) {
			if (data.children.isEmpty() && data.vertices.isEmpty()) return false
			var index = 0
			data.childWalkPreOrderReversed {
				primitive ->
				val indices = primitive.indices
				val globalPrimitive = globalPrimitives[index++]
				// TODO: Implement intersections for TRIANGLE_STRIP and TRIANGLE_FAN
				if (primitive.drawMode == Gl20.TRIANGLES) {
					for (j in 0..indices.lastIndex step 3) {
						val p1 = globalPrimitive[indices[j]].position
						val p2 = globalPrimitive[indices[j + 1]].position
						val p3 = globalPrimitive[indices[j + 2]].position
						if (globalRay.intersects(p1, p2, p3, intersection)) {
							return true
						}
					}
				}
				TreeWalk.CONTINUE
			}
			return false
		} else {
			return true
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		out.clear()
		boundingBox.inf()
		if (data.children.isEmpty() && data.vertices.isEmpty()) return

		data.childWalkPreOrder {
			primitive ->
			for (j in 0..primitive.vertices.lastIndex) {
				val p = primitive.vertices[j].position
				boundingBox.ext(p, false)
			}
			TreeWalk.CONTINUE
		}
		boundingBox.update()
		out.ext(boundingBox.max.x, boundingBox.max.y)
	}

	private var globalPrimitiveIndex = 0

	override fun draw() {
		glState.camera(camera)
		globalPrimitiveIndex = 0
		renderMeshData(data)
	}

	private fun renderMeshData(meshData: MeshData) {
		val batch = glState.batch
		val indices = meshData.indices
		val vertices = meshData.vertices
		val globalPrimitive = globalPrimitives[globalPrimitiveIndex++]

		if (meshData.flushBatch)
			batch.flush(true)

		if (indices.isNotEmpty()) {
			if (assertionsEnabled) {
				if (meshData.drawMode == Gl20.LINES) {
					_assert(indices.size % 2 == 0, { "indices size ${indices.size} not evenly divisible by 2" })
				} else if (meshData.drawMode == Gl20.TRIANGLES) {
					_assert(indices.size % 3 == 0, { "indices size ${indices.size} not evenly divisible by 3" })
				}
				_assert(vertices.isNotEmpty(), "Indices pushed with no vertices.")
				//_assert(vertices.lastIndex == meshData.highestIndex, "Vertices were added without reference via indices.")
			}

			glState.setTexture(meshData.texture ?: glState.whitePixel)
			batch.begin(meshData.drawMode)
			glState.blendMode(meshData.blendMode, premultipliedAlpha = false)
			for (j in 0..globalPrimitive.lastIndex) {
				val v = globalPrimitive[j]
				batch.putVertex(v.position, v.normal, v.colorTint, v.u, v.v)
			}
			val n = batch.highestIndex + 1
			for (j in 0..indices.lastIndex) {
				batch.putIndex(n + indices[j])
			}
		}
		meshData.iterateChildren {
			renderMeshData(it)
			true
		}
		if (meshData.flushBatch)
			batch.flush(true)
	}


	override fun dispose() {
		super.dispose()
		clearGlobalPrimitives()
	}

	companion object {
		const val VERTEX_TRANSFORM = 1 shl 16
		const val VERTEX_COLOR_TRANSFORM = 1 shl 17
		private const val GLOBAL_BOUNDING_BOX = 1 shl 18
	}
}

fun Owned.dynamicMeshC(init: ComponentInit<DynamicMeshComponent> = {}): DynamicMeshComponent {
	val s = DynamicMeshComponent(this)
	s.init()
	return s
}