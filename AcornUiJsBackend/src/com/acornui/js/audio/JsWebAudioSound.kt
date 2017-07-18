package com.acornui.js.audio

import com.acornui.core.audio.MutableAudioManager
import com.acornui.core.audio.Sound
import com.acornui.core.time.time
import com.acornui.math.MathUtils
import org.khronos.webgl.ArrayBuffer

class JsWebAudioSound(
		private val audioManager: MutableAudioManager,
		private val context: AudioContext,
		private val decodedData: ArrayBuffer,
		override val priority: Float) : Sound {

	override var onCompleted: (() -> Unit)? = null

	private var gain: GainNode
	private val panner: PannerNode

	private val audioBufferSourceNode: AudioBufferSourceNode

	private var _isPlaying: Boolean = false
	override val isPlaying: Boolean
		get() = _isPlaying

	private var _startTime: Double = 0.0
	private var _stopTime: Double = 0.0

	init {
		// create a sound source
		audioBufferSourceNode = context.createBufferSource()
		audioBufferSourceNode.addEventListener("ended", {
			complete()
		})

		// Add the buffered data to our object
		audioBufferSourceNode.buffer = decodedData

		// Panning
		panner = context.createPanner()
		panner.panningModel = PanningModel.EQUAL_POWER.value

		// Volume
		gain = context.createGain()
		gain.gain.value = audioManager.soundVolume

		// Wire them together.
		audioBufferSourceNode.connect(panner)
		panner.connect(gain)
		panner.setPosition(0f, 0f, 1f)
		gain.connect(context.destination)

		audioManager.registerSound(this)
	}

	private fun complete() {
		_stopTime = time.nowS()
		_isPlaying = false
		onCompleted?.invoke()
		onCompleted = null
		audioManager.unregisterSound(this)
	}

	override var loop: Boolean
		get() = audioBufferSourceNode.loop
		set(value) {
			audioBufferSourceNode.loop = value
		}

	private var _volume: Float = 1f

	override var volume: Float
		get() = _volume
		set(value) {
			_volume = value
			gain.gain.value = MathUtils.clamp(value * audioManager.soundVolume, 0f, 1f)
		}

	override fun setPosition(x: Float, y: Float, z: Float) {
		panner.setPosition(x, y, z)
	}

	override fun start() {
		audioBufferSourceNode.start(context.currentTime)
		_startTime = time.nowS()
	}

	override fun stop() {
		audioBufferSourceNode.stop(0f)
	}

	override val currentTime: Float
		get() {
			if (!_isPlaying)
				return (_stopTime - _startTime).toFloat()
			else
				return (time.nowS() - _startTime).toFloat()
		}

	override fun update() {
	}

	override fun dispose() {
		stop()
	}

}