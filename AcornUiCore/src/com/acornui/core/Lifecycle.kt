/*
 * Copyright (c) 2014.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 * @author nbilyk
 */

package com.acornui.core

import com.acornui.signal.Signal
import com.acornui.signal.Signal1

/**
 * An object with a lifecycle. Create, Dispose, Activate, Deactivate
 *
 * @author nbilyk
 */
interface LifecycleRo {

	/**
	 * Dispatched then this object has been activated.
	 */
	val activated: Signal<(LifecycleRo) -> Unit>

	/**
	 * Dispatched then this object has been deactivated.
	 */
	val deactivated: Signal<(LifecycleRo) -> Unit>

	/**
	 * Dispatched then this object has been disposed.
	 */
	val disposed: Signal<(LifecycleRo) -> Unit>

	/**
	 * Returns true if this object is currently active.
	 */
	val isActive: Boolean

	/**
	 * Returns true if this object has been disposed.
	 */
	val isDisposed: Boolean
}

interface Lifecycle : LifecycleRo, Disposable {

	/**
	 * Invoke when this object is to become active.
	 */
	fun activate()

	/**
	 * Invoke when this object is no longer active.
	 */
	fun deactivate()
}

abstract class LifecycleBase : Lifecycle {

	protected val _activated = Signal1<Lifecycle>()
	override val activated: Signal<(Lifecycle) -> Unit>
		get() = _activated
	protected val _deactivated = Signal1<Lifecycle>()
	override val deactivated: Signal<(Lifecycle) -> Unit>
		get() = _deactivated
	protected val _disposed = Signal1<Lifecycle>()
	override val disposed: Signal<(Lifecycle)->Unit>
		get() = _disposed

	protected var _isDisposed: Boolean = false
	protected var _isDisposing: Boolean = false
	protected var _isActive: Boolean = false

	override val isActive: Boolean
		get() = _isActive

	override val isDisposed: Boolean
		get() = _isDisposed

	final override fun activate() {
		if (_isDisposed)
			throw IllegalStateException("Disposed")
		if (_isActive)
			throw IllegalStateException("Already active")
		_isActive = true
		onActivated()
		_activated.dispatch(this)
	}

	protected open fun onActivated() {}

	final override fun deactivate() {
		if (_isDisposed) throw IllegalStateException("Disposed")
		if (!_isActive) throw IllegalStateException("Not active")
		_isActive = false
		onDeactivated()
		_deactivated.dispatch(this)
	}

	protected open fun onDeactivated() {
	}

	override fun dispose() {
		if (_isDisposed)
			throw IllegalStateException("Already disposed")
		if (_isDisposing) return
		_isDisposing = true
		if (isActive) {
			deactivate()
		}
		_disposed.dispatch(this)
		_isDisposed = true
		_disposed.dispose()
		_activated.dispose()
		_deactivated.dispose()
		_isDisposing = false
	}
}

/**
 * A common interface for objects with a lifecycle that can be updated.
 */
interface Drivable : Updatable, Lifecycle {
}

interface Updatable : Disposable {

	/**
	 * Updates this object.
	 */
	fun update(stepTime: Float)
}

interface DrivableChild : Drivable, ChildRo {

	override var parent: Parent<DrivableChild>?

	fun remove() {
		parent?.removeChild(this)
	}
}

abstract class DrivableChildBase : LifecycleBase(), DrivableChild {

	override var parent: Parent<DrivableChild>? = null

	override fun dispose() {
		super.dispose()
		remove()
	}
}