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

@file:Suppress("UNUSED_PARAMETER")

package com.acornui.component.text

import com.acornui.component.ContainerRo
import com.acornui.component.ElementContainerImpl
import com.acornui.component.ValidationFlags
import com.acornui.component.layout.algorithm.LineInfo
import com.acornui.component.layout.algorithm.LineInfoRo
import com.acornui.di.Context
import com.acornui.selection.SelectionRange

interface TextNodeContainerRo : ContainerRo, TextNodeRo
interface TextNodeContainer : TextNode, TextNodeContainerRo

abstract class TextElementContainerImpl<E : TextNode>(owner: Context) : ElementContainerImpl<E>(owner), TextNodeContainer {

	private val _lines = ArrayList<LineInfo>()
	private val _textElements = ArrayList<TextElementRo>()

	/**
	 * A list of all the text elements within the text nodes.
	 */
	override val textElements: List<TextElementRo>
		get() {
			validate(TEXT_ELEMENTS)
			return _textElements
		}

	override var allowClipping: Boolean = true
		set(value) {
			if (field == value) return
			field = value
			elements.forEach { it: E ->
				it.allowClipping = value
			}
		}

	init {
		validation.addNode(TEXT_ELEMENTS, dependencies = ValidationFlags.HIERARCHY_ASCENDING, dependents = ValidationFlags.LAYOUT, onValidate = ::updateTextElements)
		validation.addNode(LINES, dependencies = ValidationFlags.LAYOUT, onValidate = ::updateLines)
	}

	override var textField: TextField? = null
		set(value) {
			if (field == value) return
			field = value
			elements.forEach { it: E ->
				it.textField = value
			}
		}

	override val placeholder: TextElementRo?
		get() = elements.lastOrNull()?.placeholder

	override fun setSelection(rangeStart: Int, selection: List<SelectionRange>) {
		var r = rangeStart
		elements.forEach { it: E ->
			it.setSelection(r, selection)
			r += it.textElements.size
		}
	}

	override val lines: List<LineInfoRo>
		get() {
			validate(LINES)
			return _lines
		}

	private fun updateTextElements() {
		_textElements.clear()
		elements.forEach { it: E ->
			_textElements.addAll(it.textElements)
		}
	}

	protected open fun updateLines() {
		// Create line info objects with attributes relative to this container.
		_lines.forEach(action = LineInfo.Companion::free)
		_lines.clear()
		var relativeIndex = 0
		elements.forEach { element: E ->
			val elementLines = element.lines
			for (j in 0..elementLines.lastIndex) {
				val line = LineInfo.obtain()
				line.set(elementLines[j])
				line.x += element.x
				line.y += element.y
				line.startIndex += relativeIndex
				line.endIndex += relativeIndex
				_lines.add(line)
			}
			relativeIndex += element.textElements.size
		}
	}
	override fun toString(builder: StringBuilder) {
		for (i in 0..elements.lastIndex) {
			elements[i].toString(builder)
		}
	}

	companion object {
		private const val TEXT_ELEMENTS = 1 shl 16
		private const val LINES = 1 shl 17
	}

}
