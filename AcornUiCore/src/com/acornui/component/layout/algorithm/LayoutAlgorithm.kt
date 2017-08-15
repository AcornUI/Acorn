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

package com.acornui.component.layout.algorithm

import com.acornui.component.layout.BasicLayoutElement
import com.acornui.component.layout.LayoutData
import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.SizeConstraints
import com.acornui.math.Bounds
import com.acornui.signal.Signal0
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A LayoutAlgorithm implementation sizes and positions layout elements. This is typically paired with a
 * LayoutContainer implementation.
 */
interface LayoutAlgorithm<in S, out T : LayoutData> : LayoutDataProvider<T> {

	/**
	 * Calculates the minimum and maximum dimensions of this layout.
	 *
	 * @param elements The list of layout entry objects to use in calculating the size constraints.
	 * @param out This will be set to the  size constraints for the provided elements. This will describe the minimum,
	 * and maximum dimensions for the laid out elements.
	 */
	fun calculateSizeConstraints(elements: List<LayoutElement>, props: S, out: SizeConstraints)

	/**
	 * Sizes and positions the given layout elements.
	 *
	 * @param explicitWidth
	 * @param explicitHeight
	 * @param elements The list of objects to lay out.
	 * @param out This will be set to bounds that the layout elements take up.
	 */
	fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, props: S, out: Bounds)

}

interface LayoutDataProvider<out T : LayoutData> {

	/**
	 * A factory method to create a new layout data object for use in this layout.
	 */
	fun createLayoutData(): T

	/**
	 * Constructs a new layout data object and applies it to the receiver layout element.
	 */
	infix fun <R : LayoutElement> R.layout(init: T.() -> Unit): R {
		val layoutData = createLayoutData()
		layoutData.init()
		this.layoutData = layoutData
		return this
	}
}

fun <T> bindable(changed: Signal0, initialValue: T): ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
	override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
		changed.dispatch()
	}
}


interface BasicLayoutAlgorithm<in S, out T : LayoutData> : LayoutDataProvider<T> {

	/**
	 * Sizes and positions the given layout elements.
	 *
	 * @param explicitWidth
	 * @param explicitHeight
	 * @param elements The list of objects to lay out.
	 * @param out This will be set to bounds that the layout elements take up.
	 */
	fun basicLayout(explicitWidth: Float?, explicitHeight: Float?, elements: List<BasicLayoutElement>, props: S, out: Bounds)

}

/**
 * A sequenced layout is a layout where the the elements are laid out in a serial manner.
 */
interface SequencedLayout<in S, out T : LayoutData> : BasicLayoutAlgorithm<S, T> {

	/**
	 * Given the x, y position, returns the index of the nearest element.
	 * This assumes the elements were laid out via [basicLayout].
	 * If there are zero elements, this returns -1, otherwise,
	 * the return value will be an index in the range of 0 to elements.lastIndex
	 */
	fun getNearestElementIndex(x: Float, y: Float, elements: List<BasicLayoutElement>, props: S): Int

}