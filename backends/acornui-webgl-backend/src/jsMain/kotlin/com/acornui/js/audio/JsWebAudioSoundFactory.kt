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

package com.acornui.js.audio

import com.acornui.core.audio.AudioManager
import com.acornui.core.audio.Sound
import com.acornui.core.audio.SoundFactory
import org.khronos.webgl.ArrayBuffer

/**
 * A sound source for audio buffer source nodes using an AudioContext.
 * Does not work in IE.
 */
class JsWebAudioSoundFactory(
		private val audioManager: AudioManager,
		private val context: AudioContext,
		private val decodedData: ArrayBuffer
) : SoundFactory {

	override var defaultPriority: Float = 0f

	override val duration: Float
		get() = decodedData.duration.toFloat()

	init {
		audioManager.registerSoundSource(this)
	}

	override fun createInstance(priority: Float): Sound? {
		if (!audioManager.canPlaySound(priority))
			return null
		return JsWebAudioSound(audioManager, context, decodedData, priority)
	}

	override fun dispose() {
		audioManager.unregisterSoundSource(this)
	}

}

private val ArrayBuffer.duration: Double
	get() = asDynamic().duration
