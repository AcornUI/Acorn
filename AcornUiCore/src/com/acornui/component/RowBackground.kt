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

package com.acornui.component

import com.acornui.component.style.*
import com.acornui.core.di.Owned
import com.acornui.graphics.Color
import com.acornui.math.Bounds


interface RowBackground : UiComponent, Selectable {
	var rowIndex: Int

	companion object : StyleTag
}

class RowBackgroundImpl(owned: Owned) : ContainerImpl(owned), RowBackground {

	override var selected: Boolean by validationProp(false, ValidationFlags.PROPERTIES)
	override var rowIndex: Int by validationProp(0, ValidationFlags.PROPERTIES)



	val style = bind(RowBackgroundStyle())

	private val bg = addChild(rect())

	init {
		styleTags.add(RowBackground)
		watch(style) {
			invalidate(ValidationFlags.PROPERTIES)
		}
	}

	override fun updateProperties() {
		if (selected) {
			if (rowIndex % 2 == 0) {
				bg.style.backgroundColor = style.selectedEvenColor
			} else {
				bg.style.backgroundColor = style.selectedOddColor
			}
		} else {
			if (rowIndex % 2 == 0) {
				bg.style.backgroundColor = style.evenColor
			} else {
				bg.style.backgroundColor = style.oddColor
			}
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		bg.setSize(explicitWidth, explicitHeight)
		out.set(bg.bounds)
	}
}

class RowBackgroundStyle : StyleBase() {
	override val type = Companion

	var selectedEvenColor by prop(Color(1f, 1f, 0f, 0.4f))
	var selectedOddColor by prop(Color(0.8f, 0.8f, 0f, 0.4f))
	var evenColor by prop(Color(0f, 0f, 0f, 0.05f))
	var oddColor by prop(Color(1f, 1f, 1f, 0.05f))

	companion object : StyleType<RowBackgroundStyle>
}

fun Owned.rowBackground(init: ComponentInit<RowBackgroundImpl> = {}): RowBackgroundImpl {
	val r = RowBackgroundImpl(this)
	r.init()
	return r
}
