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

package com.acornui.audio

import com.acornui.*
import com.acornui.collection.*
import com.acornui.di.Context
import com.acornui.time.FrameDriverRo


interface AudioManagerRo {

	/**
	 * The global sound gain. 0-1
	 */
	var soundVolume: Float

	/**
	 * The global music gain. 0-1
	 */
	var musicVolume: Float

	/**
	 * The current active sounds being played.
	 */
	val activeSounds: List<Sound>

	/**
	 * The current active musics being played.
	 */
	val activeMusics: List<Music>

	companion object : Context.Key<AudioManagerRo>

}

interface AudioManager : AudioManagerRo, Updatable {

	val simultaneousSounds: Int

	/**
	 * A quick check to ensure this sound can be played given the priority of the current active sounds.
	 */
	fun canPlaySound(priority: Float): Boolean {
		if (activeSounds.size < simultaneousSounds) return true
		return priority >= activeSounds.last().priority
	}

	fun registerSoundSource(soundSource: SoundFactory)
	fun unregisterSoundSource(soundSource: SoundFactory)

	fun registerSound(sound: Sound)
	fun unregisterSound(sound: Sound)

	fun registerMusic(music: Music)
	fun unregisterMusic(music: Music)

	companion object : Context.Key<AudioManager> {

		override val extends: Context.Key<AudioManagerRo>? = AudioManagerRo
	}
}

open class AudioManagerImpl(
		override val frameDriver: FrameDriverRo,
		final override val simultaneousSounds: Int = 8
) : AudioManager, Disposable {

	override val activeSounds = SnapshotListImpl<Sound>(simultaneousSounds)
	override val activeMusics = SnapshotListImpl<Music>()
	private val soundSources = SnapshotListImpl<SoundFactory>()

	private val soundPriorityComparator = {
		o1: Sound?, o2: Sound? ->
		if (o1 == null && o2 == null) 0
		else if (o1 == null) 1
		else if (o2 == null) -1
		else {
			// A lower priority value should be first in the list (and therefore first to be removed).
			o1.priority.compareTo(o2.priority)
		}
	}

	override fun registerSoundSource(soundSource: SoundFactory) {
		soundSources.add(soundSource)
	}

	override fun unregisterSoundSource(soundSource: SoundFactory) {
		soundSources.remove(soundSource)
	}

	override fun registerSound(sound: Sound) {
		val index = activeSounds.sortedInsertionIndex(sound, comparator = soundPriorityComparator)
		activeSounds.add(index, sound)
		if (activeSounds.size > simultaneousSounds) {
			// Exceeded simultaneous sounds.
			activeSounds.poll().stop()
		}
	}

	override fun unregisterSound(sound: Sound) {
		activeSounds.remove(sound)
	}

	override fun registerMusic(music: Music) {
		activeMusics.add(music)
	}

	override fun unregisterMusic(music: Music) {
		activeMusics.remove(music)
	}

	private var _soundVolume: Float = 1f

	override var soundVolume: Float
		get() = _soundVolume
		set(value) {
			if (_soundVolume == value) return
			_soundVolume = value
			for (i in 0..activeSounds.lastIndex) {
				activeSounds[i].volume = activeSounds[i].volume
			}
		}

	private var _musicVolume: Float = 1f

	override var musicVolume: Float
		get() = _musicVolume
		set(value) {
			if (_musicVolume == value) return
			_musicVolume = value
			for (i in 0..activeMusics.lastIndex) {
				activeMusics[i].volume = activeMusics[i].volume
			}
		}

	override fun update(dT: Float) {
		activeMusics.forEach {
			it.update()
		}
		activeSounds.forEach {
			it.update()
		}
	}

	override fun dispose() {
		stop()
		while (activeSounds.isNotEmpty()) {
			activeSounds.poll().dispose()
		}
		while (activeMusics.isNotEmpty()) {
			activeMusics.poll().dispose()
		}
		while (soundSources.isNotEmpty()) {
			soundSources.pop().dispose()
		}
	}
}
