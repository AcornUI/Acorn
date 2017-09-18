/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.component.scroll

import com.acornui.component.ComponentInit
import com.acornui.component.ElementContainer
import com.acornui.component.StackLayoutData
import com.acornui.component.UiComponent
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.style.*
import com.acornui.core.di.DKey
import com.acornui.core.di.DependencyKeyImpl
import com.acornui.core.di.Owned
import com.acornui.math.Corners
import com.acornui.math.CornersRo

interface ScrollArea : LayoutDataProvider<StackLayoutData>, ElementContainer<UiComponent> {

	val style: ScrollAreaStyle

	val hScrollModel: ClampedScrollModelRo
	val vScrollModel: ClampedScrollModelRo

	var hScrollPolicy: ScrollPolicy
	var vScrollPolicy: ScrollPolicy

	val contentsWidth: Float
	val contentsHeight: Float

	override fun createLayoutData(): StackLayoutData = StackLayoutData()

	companion object : StyleTag {
		val FACTORY_KEY: DKey<(owner: Owned) -> ScrollArea> = DependencyKeyImpl()

		val VBAR_STYLE = styleTag()
		val HBAR_STYLE = styleTag()

		/**
		 * The validation flag used for scrolling.
		 */
		val SCROLLING: Int = 1 shl 16
	}
}

enum class ScrollPolicy {
	OFF,
	ON,
	AUTO
}

fun ScrollPolicy.toCssString(): String {
	return when (this) {
		ScrollPolicy.OFF -> "hidden"
		ScrollPolicy.ON -> "scroll"
		ScrollPolicy.AUTO -> "auto"
	}
}

fun Owned.scrollArea(init: ComponentInit<ScrollArea> = {}): ScrollArea {
	val s = injector.inject(ScrollArea.FACTORY_KEY).invoke(this)
	s.init()
	return s
}

class ScrollAreaStyle : StyleBase() {

	override val type: StyleType<ScrollAreaStyle> = Companion

	var corner by prop(noSkin)

	var tossScrolling by prop(false)

	var borderRadius: CornersRo by prop(Corners())

	companion object : StyleType<ScrollAreaStyle>
}