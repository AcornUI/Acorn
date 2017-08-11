package com.acornui.core.selection

import com.acornui.collection.Clearable
import com.acornui.component.UiComponent
import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.di.Injector
import com.acornui.core.di.inject
import com.acornui.signal.Signal
import com.acornui.signal.Signal2

interface SelectionManager : Disposable, Clearable {

	/**
	 * Dispatched when the [selection] value changes.
	 * The handler should be (oldSelection: List<SelectionRange>, newSelection: List<SelectionRange>)
	 */
	val selectionChanged: Signal<(List<SelectionRange>, List<SelectionRange>) -> Unit>

	var selection: List<SelectionRange>

	fun contains(target: Selectable, index: Int): Boolean {
		for (i in 0..selection.lastIndex) {
			val s = selection[i]
			if (s.target == target && s.contains(index)) return true
		}
		return false
	}

	override fun clear() {
		if (selection.isEmpty()) return
		selection = listOf()
	}

	companion object : DKey<SelectionManager> {
		override fun factory(injector: Injector): SelectionManager? {
			return SelectionManagerImpl()
		}
	}
}

class SelectionManagerImpl : SelectionManager {

	private val _selectionChanged = Signal2<List<SelectionRange>, List<SelectionRange>>()
	override val selectionChanged: Signal2<List<SelectionRange>, List<SelectionRange>>
		get() = _selectionChanged

	private var _selection: List<SelectionRange> = ArrayList()
	override var selection: List<SelectionRange>
		get() = _selection
		set(value) {
			val old = _selection
			_selection = value
			selectionChanged.dispatch(old, value)
		}

	override fun dispose() {
		selectionChanged.dispose()
	}
}

data class SelectionRange(val target: Selectable, val startIndex: Int, val endIndex: Int) {

	fun contains(index: Int): Boolean {
		return index >= startIndex && index < endIndex
	}
}

/**
 * A marker interface indicating that a component can be selected.
 */
interface Selectable
interface SelectableComponent : UiComponent, Selectable

/**
 * Sets the current selection to this component with the range 0 to MAX_VALUE
 */
fun SelectableComponent.selectAll() {
	inject(SelectionManager).selection = listOf(SelectionRange(this, 0, Int.MAX_VALUE))
}