/*
 * Copyright 2020 Poly Forest, LLC
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

import com.acornui.asset.loadText
import com.acornui.component.style.cssClass
import com.acornui.component.style.cssProp
import com.acornui.css.percent
import com.acornui.di.Context
import com.acornui.dom.add
import com.acornui.dom.addStyleToHead
import com.acornui.dom.createElement
import com.acornui.graphic.Color
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import org.w3c.dom.svg.SVGElement
import org.w3c.dom.svg.SVGStopElement
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class TintableSvg(owner: Context) : Div(owner) {

	init {
		addClass(TintableSvgStyle.tintableSvg)
	}

	var src: String = ""
		set(value) {
			launch {
				val text = loadText(value)
				val index = text.indexOf("<svg")
				dom.clear()
				val svg = createElement<SVGElement>("svg")
				dom.add(svg)
				svg.outerHTML = text.substring(index, text.length)

				val stops = dom.getElementsByTagNameNS("http://www.w3.org/2000/svg", "stop")
				println("stops ${stops.length}")
				for (i in 0 until stops.length) {
					val stop = stops.item(i).unsafeCast<SVGStopElement>()
					val stopColor = stop.style.getPropertyValue("stop-color")
					stop.style.removeProperty("stop-color")
					val color = Color.fromStr(stopColor)
					val hsl = color.toHsl()
					stop.style.setProperty(TintableSvgStyle.luminanceSelf.toString(), (hsl.l * 100.0).percent.toString())
					stop.style.setProperty(TintableSvgStyle.alphaSelf.toString(), hsl.a.toString())
				}
			}
		}

	var tint: Color = Color.WHITE
		set(value) {
			if (field == value) return
			field = value
			val hsl = value.toHsl()
			style.setProperty(TintableSvgStyle.hue.toString(), hsl.h.toString())
			style.setProperty(TintableSvgStyle.saturation.toString(), hsl.s.toString())
			style.setProperty(TintableSvgStyle.luminance.toString(), hsl.l.toString())
			style.setProperty(TintableSvgStyle.alpha.toString(), hsl.a.toString())
		}
}

object TintableSvgStyle {

	val tintableSvg by cssClass()

	val luminanceSelf by cssProp()
	val alphaSelf by cssProp()

	val hue by cssProp()
	val saturation by cssProp()
	val luminance by cssProp()
	val alpha by cssProp()

	init {
		addStyleToHead(
			"""

$tintableSvg stop {
	stop-color: hsla(var($hue, 0), calc(var($saturation, 1.0) * 100%), calc(var($luminance, 1.0) * var($luminanceSelf, 100%)), calc(var($alpha, 1.0) * var($alphaSelf, 1.0)));
}
		
		"""
		)
	}
}

inline fun Context.tintableSvg(init: ComponentInit<TintableSvg> = {}): TintableSvg {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return TintableSvg(this).apply(init)
}