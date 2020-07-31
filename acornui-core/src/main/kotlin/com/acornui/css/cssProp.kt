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

package com.acornui.css

import org.intellij.lang.annotations.Language
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

fun cssVar(prop: KProperty<String>): String {
	return "var(--${prop.name})"
}

fun cssProp(prop: KProperty0<String>): String {
	return "--${prop.name}:${prop.get()};"
}

/**
 * Syntax highlighting for a css property.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun css(@Language("CSS", prefix = "{ p: ", suffix = "; }") value: String): String {
	return value
}

fun css(value: Length): String = value.toString()