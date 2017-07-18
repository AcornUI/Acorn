/*
 * Copyright 2016 Nicholas Bilyk
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

package com.acornui.component.layout.algorithm.virtual

import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.LayoutData
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.signal.Signal0
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A Virtualized layout algorithm is a type of layout that is only given a single layout element to size and position
 * at a time.
 */
interface VirtualLayoutAlgorithm<out T : LayoutData> : LayoutDataProvider<T> {

	val changed: Signal0

	val direction: VirtualDirection

	/**
	 * Given a renderer, returns the position offset of that item.
	 *
	 * @param width The explicit width.
	 * @param height The explicit height.
	 * @param element The element for which the offset is calculated.
	 * @param index The index of the element.
	 * @param lastIndex The index of the last item.
	 * @param isReversed
	 * @return
	 */
	fun getOffset(width: Float, height: Float, element: LayoutElement, index: Int, lastIndex: Int, isReversed: Boolean): Float

	/**
	 * Sizes and positions the given layout element.
	 *
	 * @param explicitWidth The explicit width.
	 * @param explicitHeight The explicit height.
	 * @param element The layout element to size and position.
	 * @param currentIndex The index of the entry to lay out.
	 * @param startIndex The index of the first virtualized item.
	 * @param lastIndex The index of the last item.
	 * @param previousElement The layout element previously updated. May be null if this is the first item.
	 * @param isReversed If true, the layout is iterating in reverse order.
	 *
	 * @return out Returns x,y coordinates representing the measured width and height.
	 */
	fun updateLayoutEntry(explicitWidth: Float?, explicitHeight: Float?, element: LayoutElement, currentIndex: Int, startIndex: Float, lastIndex: Int, previousElement: LayoutElement?, isReversed: Boolean)

	/**
	 * Given a list of the elements laid out via [updateLayoutEntry] this calculates the measured dimensions considering
	 * whitespace such as padding.
	 */
	fun measure(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, out: Bounds) {
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val r = element.right
			if (r > out.width)
				out.width = r
			val b = element.bottom
			if (b > out.height)
				out.height = b
		}
	}

	/**
	 * Returns true if the layout element is in bounds.
	 */
	fun shouldShowRenderer(explicitWidth: Float?, explicitHeight: Float?, element: LayoutElement): Boolean

}

interface ItemRendererOwner<out T : LayoutData> : Owned, LayoutDataProvider<T> {}

enum class VirtualDirection {
	VERTICAL,
	HORIZONTAL
}

fun <T> VirtualLayoutAlgorithm<*>.bindable(initialValue: T): ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
	override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
		changed.dispatch()
	}
}