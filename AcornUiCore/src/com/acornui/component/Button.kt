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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.acornui.component

import com.acornui.component.layout.SizeConstraints
import com.acornui.component.style.*
import com.acornui.core.UserInfo
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.cursor.cursor
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.focus.Focusable
import com.acornui.core.input.*
import com.acornui.core.input.interaction.*
import com.acornui.factory.LazyInstance
import com.acornui.factory.disposeInstance
import com.acornui.math.Bounds
import com.acornui.signal.Signal1


/**
 * A skinnable button with up, over, down, and disabled states.
 */
open class Button(
		owner: Owned
) : ElementContainerImpl(owner), Labelable, Toggleable, Focusable {

	val style = bind(ButtonStyle())

	/**
	 * Dispatched when the toggled flag has changed.
	 */
	val toggledChanged = own(Signal1<Button>())

	/**
	 * If true, when this button is pressed, the selected state will be toggled.
	 */
	var toggleOnClick = false

	protected var _mouseIsOver = false
	protected var _mouseIsDown = false
	protected var _disabled = false
	protected var _selected = false

	protected var _label: String = ""

	private var _currentState = ButtonState.UP
	private var _currentSkinPart: UiComponent? = null
	private val _stateSkinMap = HashMap<ButtonState, LazyInstance<Owned, UiComponent>>()

	override var focusEnabled = true
	override var focusOrder = 0f
	override var highlight: UiComponent? by createSlot()

	private val rollOverHandler = {
		event: MouseInteraction ->
		_mouseIsOver = true
		refreshState()
	}

	private val rollOutHandler = {
		event: MouseInteraction ->
		_mouseIsOver = false
		refreshState()
	}

	private val mouseDownHandler = {
		event: MouseInteraction ->
		if (!_mouseIsDown && event.button == WhichButton.LEFT) {
			_mouseIsDown = true
			stage.mouseUp().add(stageMouseUpHandler, true)
			refreshState()
		}
	}

	private val touchStartHandler = {
		event: TouchInteraction ->
		if (!_mouseIsDown) {
			_mouseIsDown = true
			stage.touchEnd().add(stageTouchEndHandler, true)
			refreshState()
		}
	}

	private val stageMouseUpHandler = {
		event: MouseInteraction ->
		if (event.button == WhichButton.LEFT) {
			_mouseIsDown = false
			refreshState()
		}
	}

	private val stageTouchEndHandler = {
		event: TouchInteraction ->
		_mouseIsDown = false
		refreshState()
	}

	private val clickHandler = {
		event: ClickInteraction ->
		if (toggleOnClick)
			toggled = !toggled
	}

	init {
		styleTags.add(Button)

		// Mouse over / out handlers cause problems on mobile.
		if (!UserInfo.isTouchDevice) {
			rollOver().add(rollOverHandler)
			rollOut().add(rollOutHandler)
		}
		mouseDown().add(mouseDownHandler)
		touchStart().add(touchStartHandler)
		click().add(clickHandler)
		cursor(StandardCursors.HAND)

		val oldInstances = ArrayList<LazyInstance<Owned, UiComponent>>()
		watch(style) {
			oldInstances.addAll(_stateSkinMap.values)
			_stateSkinMap[ButtonState.UP] = LazyInstance(this, it.upState)
			_stateSkinMap[ButtonState.OVER] = LazyInstance(this, it.overState)
			_stateSkinMap[ButtonState.DOWN] = LazyInstance(this, it.downState)
			_stateSkinMap[ButtonState.SELECTED_UP] = LazyInstance(this, it.selectedUpState)
			_stateSkinMap[ButtonState.SELECTED_OVER] = LazyInstance(this, it.selectedOverState)
			_stateSkinMap[ButtonState.SELECTED_DOWN] = LazyInstance(this, it.selectedDownState)
			_stateSkinMap[ButtonState.DISABLED] = LazyInstance(this, it.disabledState)
			refreshState()
			// Dispose the old state instances after we refresh state so that onCurrentStateChanged overrides have a
			// chance to transfer content children if necessary.
			for (i in 0..oldInstances.lastIndex) {
				oldInstances[i].disposeInstance()
			}
			oldInstances.clear()
		}
	}

	open var disabled: Boolean
		get() {
			return _disabled
		}
		set(value) {
			if (_disabled == value) return
			_disabled = value
			interactivityMode = if (value) InteractivityMode.NONE else InteractivityMode.ALL
			refreshState()
		}

	override var toggled: Boolean
		get() {
			return _selected
		}
		set(value) {
			if (_selected == value) return
			_selected = value
			refreshState()
			toggledChanged.dispatch(this)
		}

	protected open fun refreshState() {
		currentState(calculateButtonState())
	}

	protected open fun calculateButtonState(): ButtonState {
		val newState: ButtonState
		if (_disabled) {
			newState = ButtonState.DISABLED
		} else {
			if (_selected) {
				if (_mouseIsDown) {
					newState = ButtonState.SELECTED_DOWN
				} else if (_mouseIsOver) {
					newState = ButtonState.SELECTED_OVER
				} else {
					newState = ButtonState.SELECTED_UP
				}
			} else {
				if (_mouseIsDown) {
					newState = ButtonState.DOWN
				} else if (_mouseIsOver) {
					newState = ButtonState.OVER
				} else {
					newState = ButtonState.UP
				}
			}
		}
		return newState
	}

	protected val currentSkinPart: UiComponent?
		get() = _currentSkinPart

	val currentState: ButtonState
		get() {
			return _currentState
		}

	protected open fun currentState(newState: ButtonState) {
		if (isDisposed) return
		val previousState = _currentState
		_currentState = newState
		val newSkinPart = _stateSkinMap[newState]?.instance
		val previousSkinPart = _currentSkinPart
		if (previousSkinPart == newSkinPart) return
		newSkinPart?.interactivityMode = InteractivityMode.NONE
		_currentSkinPart = newSkinPart
		if (newSkinPart is Labelable) {
			newSkinPart.label = _label
		}
		onCurrentStateChanged(previousState, newState, previousSkinPart, newSkinPart)
		if (newSkinPart != null) addChild(newSkinPart)
		removeChild(previousSkinPart)
	}

	protected open fun onCurrentStateChanged(previousState: ButtonState, newState: ButtonState, previousSkinPart: UiComponent?, newSkinPart: UiComponent?) {
	}

	/**
	 * Sets the label of this button. It is up to the skin to implement [Labelable] and use this label.
	 */
	override var label: String
		get() = _label
		set(value) {
			_label = value
			(_currentSkinPart as? Labelable)?.label = (value)
			invalidate(ValidationFlags.SIZE_CONSTRAINTS)
		}

	override fun updateSizeConstraints(out: SizeConstraints) {
		if (_currentSkinPart == null) return
		out.set(_currentSkinPart!!.sizeConstraints)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		if (_currentSkinPart != null) {
			_currentSkinPart!!.setSize(explicitWidth, explicitHeight)
			out.set(_currentSkinPart!!.bounds)
		}
		highlight?.setSize(out.width, out.height)
	}

	override fun dispose() {
		super.dispose()
		stage.mouseUp().remove(stageMouseUpHandler)
		stage.touchEnd().remove(stageTouchEndHandler)
	}

	companion object : StyleTag

}

enum class ButtonState(val selected: Boolean) {
	UP(false),
	OVER(false),
	DOWN(false),
	SELECTED_UP(true),
	SELECTED_OVER(true),
	SELECTED_DOWN(true),
	DISABLED(false);
}

open class ButtonStyle : StyleBase() {

	override val type: StyleType<ButtonStyle> = ButtonStyle

	var upState by prop(noSkin)
	var overState by prop(noSkin)
	var downState by prop(noSkin)
	var selectedUpState by prop(noSkin)
	var selectedOverState by prop(noSkin)
	var selectedDownState by prop(noSkin)
	var disabledState by prop(noSkin)

	companion object : StyleType<ButtonStyle>
}

interface LabelableRo : UiComponentRo {
	val label: String
}

/**
 * An interface for a skin part that can have a label assigned to it.
 */
interface Labelable : LabelableRo, UiComponent {
	override var label: String
}

interface ToggleableRo : UiComponentRo {
	val toggled: Boolean
}

interface Toggleable : ToggleableRo, UiComponent {
	override var toggled: Boolean
}

fun Owned.button(init: ComponentInit<Button> = {}): Button {
	val b = Button(this)
	b.init()
	return b
}

fun Owned.button(label: String, init: ComponentInit<Button> = {}): Button {
	val b = Button(this)
	b.label = label
	b.init()
	return b
}