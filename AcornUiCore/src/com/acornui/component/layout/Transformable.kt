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

package com.acornui.component.layout

import com.acornui.core.round
import com.acornui.math.*

interface TransformableRo : PositionableRo {

	/**
	 * This component's transformation matrix.
	 * Responsible for 3d positioning, scaling, rotation, etc.
	 *
	 * Do not modify this matrix directly, but instead use the exposed transformation methods:
	 * [x], [y], [z], [scaleX], [scaleY], [scaleZ], [rotationX], [rotationY], [rotation]
	 */
	val transform: Matrix4Ro

	/**
	 * If this is not null, this custom transformation matrix will be used. Note that if this is set, all properties
	 * that would otherwise generate the transformation matrix are no longer applicable.
	 * [scaleX], [scaleY], [scaleZ]
	 * [rotationX], [rotationY], [rotation],
	 * [originX], [originY], [originZ]
	 */
	val customTransform: Matrix4Ro?

	val rotationX: Float

	val rotationY: Float

	/**
	 * Rotation around the Z axis
	 */
	val rotation: Float

	val scaleX: Float

	val scaleY: Float

	val scaleZ: Float

	val originX: Float

	val originY: Float

	val originZ: Float

	/**
	 * Converts a coordinate from local coordinate space to global coordinate space.
	 * This will modify the provided coord parameter.
	 * @param localCoord The coordinate local to this Transformable. This will be mutated to become a global coordinate.
	 * @return Returns the coord
	 */
	fun localToGlobal(localCoord: Vector3): Vector3 {
		concatenatedTransform.prj(localCoord)
		return localCoord
	}

	/**
	 * Converts a coordinate from global coordinate space to local coordinate space.
	 * This will modify the provided coord parameter.
	 * @param globalCoord The coordinate in global space. This will be mutated to become a local coordinate.
	 * @return Returns the coord
	 */
	fun globalToLocal(globalCoord: Vector3): Vector3 {
		concatenatedTransformInv.prj(globalCoord)
		return globalCoord
	}

	/**
	 * Converts a ray from local coordinate space to global coordinate space.
	 * This will modify the provided ray parameter.
	 * @param ray The ray local to this Transformable. This will be mutated to become a global ray.
	 * @return Returns the ray
	 */
	fun localToGlobal(ray: Ray): Ray {
		ray.mul(concatenatedTransform)
		return ray
	}

	/**
	 * Converts a ray from global coordinate space to local coordinate space.
	 * This will modify the provided ray parameter.
	 *
	 * Note: This is a heavy operation as it performs a Matrix4 inversion.
	 *
	 * @param ray The ray in global space. This will be mutated to become a local coordinate.
	 * @return Returns the ray
	 */
	fun globalToLocal(ray: Ray): Ray {
		ray.mul(concatenatedTransformInv)
		return ray
	}

	/**
	 * Calculates the intersection coordinates of the provided Ray (in local coordinate space) and this layout
	 * element's plane.
	 * @return Returns true if the provided Ray intersects with this plane, or false if the Ray is parallel.
	 */
	fun rayToPlane(ray: RayRo, out: Vector2): Boolean {
		if (ray.direction.z == 0f) return false
		val m = -ray.origin.z * ray.directionInv.z
		out.x = ray.origin.x + m * ray.direction.x
		out.y = ray.origin.y + m * ray.direction.y
		return true
	}

	/**
	 * Converts a coordinate from this Transformable's coordinate space to the target coordinate space.
	 */
	fun convertCoord(coord: Vector3, targetCoordSpace: TransformableRo): Vector3 {
		return targetCoordSpace.globalToLocal(localToGlobal(coord))
	}

	/**
	 * The global transform of this component, of all ancestor transforms multiplied together.
	 * Do not modify this matrix directly, it will be overwritten on a TRANSFORM validation.
	 * @see transform
	 */
	val concatenatedTransform: Matrix4Ro

	/**
	 * Returns the inverse concatenated transformation matrix.
	 * Note that this is a heavy operation and should be used judiciously. It is not part of the normal validation
	 * cycle.
	 * @see concatenatedTransform
	 * @see transform
	 */
	val concatenatedTransformInv: Matrix4Ro

}

/**
 * The API for reading and modifying a component's 3d transformation.
 * @author nbilyk
 */
interface Transformable : TransformableRo, Positionable {

	override var customTransform: Matrix4Ro?

	override var rotationX: Float

	override var rotationY: Float

	/**
	 * Rotation around the Z axis
	 */
	override var rotation: Float

	fun setRotation(x: Float = 0f, y: Float = 0f, z: Float = 0f)

	//---------------------------------------------------------------------------------------
	// Transformation and translation methods
	//---------------------------------------------------------------------------------------

	override var scaleX: Float

	override var scaleY: Float

	override var scaleZ: Float

	fun setScaling(x: Float = 1f, y: Float = 1f, z: Float = 1f)

	override var originX: Float

	override var originY: Float

	override var originZ: Float

	fun setOrigin(x: Float, y: Float, z: Float = 0f)

}

interface PositionableRo {
	val x: Float
	val y: Float
	val z: Float

	val position: Vector3Ro
}

interface Positionable : PositionableRo {

	override var x: Float
	override var y: Float
	override var z: Float

	override val position: Vector3Ro

	fun moveTo(value: Vector3Ro) {
		moveTo(value.x, value.y, value.z)
	}

	/**
	 * Sets the position of this component, rounding the x and y coordinates.
	 */
	fun moveTo(x: Float = 0f, y: Float = 0f, z: Float = 0f) {
		// Round after a small, but obscure offset, to avoid flip-flopping around the common case of 0.5f
		setPosition((x - 0.0136f).round(), (y - 0.0136f).round(), z)
	}

	/**
	 * Sets the position of this component. (Without rounding)
	 */
	fun setPosition(x: Float = 0f, y: Float = 0f, z: Float = 0f)

}