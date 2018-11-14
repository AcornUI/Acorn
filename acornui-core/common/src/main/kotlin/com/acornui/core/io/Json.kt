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

package com.acornui.core.io

import com.acornui.core.di.Scoped
import com.acornui.serialization.From
import com.acornui.serialization.To

@Deprecated("Use com.acornui.serialization.fromJson")
fun <T> Scoped.parseJson(json: String, factory: From<T>): T {
	return com.acornui.serialization.fromJson(json, factory)
}

@Deprecated("Use com.acornui.serialization.toJson")
fun <T> Scoped.toJson(value: T, factory: To<T>): String {
	return com.acornui.serialization.toJson(value, factory)
}