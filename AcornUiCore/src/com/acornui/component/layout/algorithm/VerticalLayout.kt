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

import com.acornui.component.ComponentInit
import com.acornui.component.layout.*
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.MathUtils
import com.acornui.math.Pad
import com.acornui.math.PadRo

object VerticalLayout : LayoutAlgorithm<VerticalLayoutStyle, VerticalLayoutData>, BasicLayoutAlgorithm<VerticalLayoutStyle, VerticalLayoutData> {

	override fun calculateSizeConstraints(elements: List<LayoutElement>, props: VerticalLayoutStyle, out: SizeConstraints) {
		val padding = props.padding
		val gap = props.gap

		var minWidth: Float = 0f
		var minHeight: Float = 0f
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val sC = element.sizeConstraints
			val iMinWidth = sC.width.min
			if (iMinWidth != null) minWidth = maxOf(iMinWidth, minWidth)
			val iMinHeight = sC.height.min
			if (iMinHeight != null) minHeight += iMinHeight
		}
		minWidth += padding.left + padding.right
		minHeight += gap * elements.lastIndex + padding.top + padding.bottom
		out.width.min = minWidth
		out.height.min = minHeight
	}

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, props: VerticalLayoutStyle, out: Bounds) = basicLayout(explicitWidth, explicitHeight, elements, props, out)

	override fun basicLayout(explicitWidth: Float?, explicitHeight: Float?, elements: List<BasicLayoutElement>, props: VerticalLayoutStyle, out: Bounds) {
		val padding = props.padding
		val gap = props.gap

		val childAvailableWidth = padding.reduceWidth(explicitWidth)
		val childAvailableHeight = padding.reduceHeight(explicitHeight)

		// Size inflexible elements first.
		var maxWidth = childAvailableWidth ?: 0f
		var inflexibleHeight = 0f
		var flexibleHeight = 0f
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val layoutData = element.layoutData as VerticalLayoutData?
			if (childAvailableHeight == null || layoutData?.heightPercent == null) {
				val w = layoutData?.getPreferredWidth(childAvailableWidth)
				val h = layoutData?.getPreferredHeight(childAvailableHeight)
				element.setSize(w, h)
				inflexibleHeight += element.height
				if (element.width > maxWidth) maxWidth = element.width
			} else {
				flexibleHeight += layoutData.heightPercent!! * childAvailableHeight
			}
			inflexibleHeight += gap
		}
		inflexibleHeight -= gap

		// Size flexible elements within the remaining space.
		if (childAvailableHeight != null) {
			val scale = if (flexibleHeight > 0) MathUtils.clamp((childAvailableHeight - inflexibleHeight) / flexibleHeight, 0f, 1f) else 1f
			for (i in 0..elements.lastIndex) {
				val element = elements[i]
				val layoutData = element.layoutData as VerticalLayoutData?
				if (layoutData?.heightPercent != null) {
					val w = layoutData.getPreferredWidth(childAvailableWidth)
					val h = scale * layoutData.heightPercent!! * childAvailableHeight
					element.setSize(w, h)
					if (element.width > maxWidth) maxWidth = element.width
				}
			}
		}

		// Position
		var y = padding.top
		if (childAvailableHeight != null && props.verticalAlign != VAlign.TOP) {
			val d = childAvailableHeight - (inflexibleHeight + flexibleHeight)
			if (d > 0f) {
				if (props.verticalAlign == VAlign.BOTTOM) {
					y += d
				} else if (props.verticalAlign == VAlign.MIDDLE) {
					y += MathUtils.floor(d * 0.5f)
				}
			}
		}
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val layoutData = element.layoutData as VerticalLayoutData?
			val x = when (layoutData?.horizontalAlign ?: props.horizontalAlign) {
				HAlign.LEFT -> padding.left
				HAlign.CENTER -> (maxWidth - element.width) * 0.5f + padding.left
				HAlign.RIGHT -> maxWidth - element.width + padding.left
			}
			element.moveTo(x, y)
			y += element.height + gap
		}
		y += padding.bottom - gap
		out.set(maxWidth + padding.left + padding.right, y)
	}

	override fun createLayoutData() = VerticalLayoutData()
}

open class VerticalLayoutStyle : StyleBase() {

	override val type: StyleType<VerticalLayoutStyle> = Companion

	var gap by prop(5f)

	/**
	 * The Padding object with left, bottom, top, and right padding.
	 */
	var padding: PadRo by prop(Pad())

	/**
	 * The horizontal alignment of each element within the measured width.
	 */
	var horizontalAlign by prop(HAlign.LEFT)

	/**
	 * The vertical alignment of the entire column within the explicit height.
	 * If the explicit height is null, this will have no effect.
	 */
	var verticalAlign by prop(VAlign.TOP)

	companion object : StyleType<VerticalLayoutStyle>

}

class VerticalLayoutData : BasicLayoutData() {

	/**
	 * If set, the horizontal alignment for this item overrides the vertical layout's horizontalAlign.
	 */
	var horizontalAlign: HAlign? by bindable(null)
}

open class VerticalLayoutContainer(owner: Owned) : LayoutContainerImpl<VerticalLayoutStyle, VerticalLayoutData>(owner, VerticalLayout, VerticalLayoutStyle())

fun Owned.vGroup(init: ComponentInit<VerticalLayoutContainer> = {}): VerticalLayoutContainer {
	val verticalGroup = VerticalLayoutContainer(this)
	verticalGroup.init()
	return verticalGroup
}