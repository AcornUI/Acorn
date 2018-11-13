/*
 * Spine Runtimes Software License
 * Version 2.3
 *
 * Copyright (c) 2013-2015, Esoteric Software
 * All rights reserved.
 *
 * You are granted a perpetual, non-exclusive, non-sublicensable and
 * non-transferable license to use, install, execute and perform the Spine
 * Runtimes Software (the "Software") and derivative works solely for personal
 * or internal use. Without the written permission of Esoteric Software (see
 * Section 2 of the Spine Software License Agreement), you may not (a) modify,
 * translate, adapt or otherwise create derivative works, improvements of the
 * Software or develop new applications using the Software or (b) remove,
 * delete, alter or obscure any trademarks or any copyright, trademark, patent
 * or other intellectual property or proprietary rights notices on or in the
 * Software, including any copy thereof. Redistributions in binary or source
 * form must include this license and terms.
 *
 * THIS SOFTWARE IS PROVIDED BY ESOTERIC SOFTWARE "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL ESOTERIC SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.esotericsoftware.spine

import com.acornui.math.Vector2
import com.esotericsoftware.spine.data.TransformConstraintData

class TransformConstraint : Updatable {
	val data: TransformConstraintData
	val bone: Bone
	val target: Bone
	var translateMix: Float = 0f
	var x: Float = 0f
	var y: Float = 0f

	private val temp = Vector2()

	constructor(data: TransformConstraintData, skeleton: Skeleton) {
		this.data = data
		translateMix = data.translateMix
		x = data.x
		y = data.y

		bone = skeleton.findBone(data.boneName) ?: throw Exception("Could not find bone with name ${data.boneName}")
		target = skeleton.findBone(data.targetName) ?: throw Exception("Could not find target bone with name ${data.targetName}")
	}

	/** Copy constructor.  */
	constructor(constraint: TransformConstraint, skeleton: Skeleton) {
		data = constraint.data
		bone = skeleton.bones[constraint.bone.skeleton.bones.indexOf(constraint.bone)]
		target = skeleton.bones[constraint.target.skeleton.bones.indexOf(constraint.target)]
		translateMix = constraint.translateMix
		x = constraint.x
		y = constraint.y
	}

	override fun update() {
		val translateMix = this.translateMix
		if (translateMix > 0) {
			val bone = this.bone
			val temp = this.temp
			target.localToWorld(temp.set(x, y))
			bone.worldX += (temp.x - bone.worldX) * translateMix
			bone.worldY += (temp.y - bone.worldY) * translateMix
		}
	}

	override fun toString(): String {
		return data.name
	}
}