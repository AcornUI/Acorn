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

package com.acornui.logging

import com.acornui.collection.poll
import com.acornui.error.stack
import com.acornui.recycle.Clearable
import com.acornui.string.lineSeparator
import com.acornui.isDebug as debugFlag

interface Logger {

	companion object {

		const val ERROR: Int = 1
		const val WARN: Int = 2
		const val INFO: Int = 3
		const val DEBUG: Int = 4
		const val VERBOSE: Int = 5

		fun getLogLevelFromString(str: String): Int {
			return when (str.toLowerCase()) {
				"error" -> ERROR
				"warn" -> WARN
				"info" -> INFO
				"debug" -> DEBUG
				"verbose" -> VERBOSE
				else -> throw IllegalArgumentException("Unknown log level $str")
			}

		}
	}

	var level: Int

	fun log(message: Any?, level: Int)

	fun log(message: () -> Any?, level: Int) = log(message(), level)

	fun verbose(message: Any?) = log(message, VERBOSE)

	fun verbose(message: () -> Any?) = log(message, VERBOSE)

	fun debug(message: Any?) = log(message, DEBUG)

	fun debug(message: () -> Any?) = log(message, DEBUG)

	fun info(message: Any?) = log(message, INFO)

	fun info(message: () -> Any?) = log(message, INFO)

	fun warn(message: Any?) = log(message, WARN)

	fun warn(message: () -> Any?) = log(message, WARN)

	fun error(message: Any?) = log(message, ERROR)

	fun error(e: Throwable, message: String = "") {
		var str = ""
		if (message.isNotEmpty()) str += "$message\n"
		str += e.stack
		log(str, ERROR)
	}

	fun error(message: () -> Any?) = log(message, ERROR)

}

/**
 * @author nbilyk
 */
object Log : Logger {

	val targets: MutableList<Logger> = arrayListOf(PrintTarget())

	override var level: Int = if (debugFlag) Logger.DEBUG else Logger.INFO

	override fun log(message: Any?, level: Int) {
		if (level <= this.level) {
			for (i in 0..targets.lastIndex) {
				val target = targets[i]
				if (level <= target.level) {
					target.log(message, level)
				}
			}
		}
	}

	override fun log(message: () -> Any?, level: Int) {
		val m by lazy(message)
		if (level <= this.level) {
			for (i in 0..targets.lastIndex) {
				val target = targets[i]
				if (level <= target.level) {
					target.log(m, level)
				}
			}
		}
	}
}

class PrintTarget : Logger {

	override var level: Int = Logger.VERBOSE

	override fun log(message: Any?, level: Int) {
		val m = message.toString()
		when (level) {
			Logger.DEBUG -> console.log(m)
			Logger.INFO -> console.info(m)
			Logger.WARN -> console.warn(m)
			Logger.ERROR -> console.error(m)
			else -> console.log(m)
		}

	}
}

class ArrayTarget : Logger, Clearable {

	override var level: Int = Logger.DEBUG

	var maxLogs: Int = 1000

	var separator = lineSeparator
	val prefixes: Array<String> = arrayOf("[NONE] ", "[ERROR] ", "[WARN] ", "[INFO] ", "[DEBUG] ")

	private val _list: MutableList<ArrayTargetEntry> = ArrayList()
	val list: List<ArrayTargetEntry> = _list

	override fun log(message: Any?, level: Int) {
		_list.add(ArrayTargetEntry(level, message.toString()))
		if (_list.size > maxLogs) _list.poll()
	}

	override fun clear() {
		_list.clear()
	}

	override fun toString(): String {
		val buffer = StringBuilder()
		var isFirst = true
		for ((level, message) in _list) {
			if (isFirst) {
				isFirst = false
			} else {
				buffer.append(separator)
			}
			val prefix = if (level < prefixes.size) prefixes[level] else ""
			buffer.append(prefix + message)
		}
		return buffer.toString()
	}

	data class ArrayTargetEntry(val level: Int, val message: String)
}
