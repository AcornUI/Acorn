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

package com.acornui.component

import com.acornui.core.Disposable
import com.acornui.math.MathUtils
import com.acornui.signal.Signal
import com.acornui.string.toRadix
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

/**
 * A component that can be invalidated
 *
 * @author nbilyk
 */
interface Validatable : Disposable {

	/**
	 * Dispatched when this component has been invalidated.
	 */
	val invalidated: Signal<(Validatable, Int) -> Unit>

	/**
	 * Invalidates the given flag.
	 * Returns a bit mask representing the flags newly invalidated.
	 * Dispatches [invalidated] with the newly invalidated flags.
	 *
	 * @param flags The bit flags to invalidate. Use an `or` operation to invalidate multiple flags at once.
	 * @see [ValidationFlags]
	 */
	fun invalidate(flags: Int): Int

	/**
	 * Validates the specified flags for this component.
	 *
	 * @param flags A bit mask for which flags to validate. (Use -1 to validate all)
	 * Example: validate(ValidationFlags.LAYOUT or ValidationFlags.PROPERTIES) to validate both layout an properties.
	 * @param force If true, the provided flags will be validated, even if they are not currently invalid.
	 */
	fun validate(flags: Int, force: Boolean)

	fun validate() {
		validate(-1, false)
	}

	fun validate(flags: Int) {
		validate(flags, false)
	}

}


private class ValidationNode(

		/**
		 * This node's flag.
		 */
		val flag: Int,

		/**
		 * When this node's flag is invalidated, the flags defined in invalidationMask will also be invalidated.
		 */
		var invalidationMask: Int,

		/**
		 * When flags is being validated, if any of them are in this mask, this node will also be validated.
		 */
		var validationMask: Int,

		val onValidate: () -> Unit
) {

	var isValid: Boolean = false

}

/**
 * A dependency graph of validation flags.
 * When a flag is invalidated, all dependents are also invalidated. When a flag is validated, all dependencies are
 * guaranteed to be valid first.
 *
 * The UI Component implementation will work in this way:
 * UI Components will validate the validation tree top down, level order. When a component validates a flag such as
 * layout, that component may require that a child component have a valid layout in order to determine its measured
 * size. On retrieving the child component size, that child component may then validate its layout, thus effectively
 * validating certain flags in a bottom-up manner.
 */
class ValidationTree {

	private val nodes = ArrayList<ValidationNode>()

	/**
	 * The current node being validated.
	 */
	private var currentIndex = -1

	private var invalidFlags = -1

	fun addNode(flag: Int, onValidate: () -> Unit) = addNode(flag, 0, 0, onValidate)

	fun addNode(flag: Int, dependencies: Int, onValidate: () -> Unit) = addNode(flag, dependencies, 0, onValidate)

	/**
	 * Appends a validation node.
	 * @param dependencies If any of these dependencies become invalid, this node will also become invalid.
	 */
	fun addNode(flag: Int, dependencies: Int, dependants: Int, onValidate: () -> Unit) {
		if (!MathUtils.isPowerOfTwo(flag)) throw IllegalArgumentException("flag ${flag.toRadix(2)} is not a power of 2.")
		val newNode = ValidationNode(flag, dependants or flag, dependencies or flag, onValidate)
		var dependenciesNotFound = dependencies
		var dependantsNotFound = dependants
		var insertIndex = nodes.size
		for (i in 0..nodes.lastIndex) {
			val previousNode = nodes[i]
			if (previousNode.flag == flag) throw Exception("flag $flag already exists.")
			val flagInv = previousNode.flag.inv()
			dependenciesNotFound = dependenciesNotFound and flagInv
			dependantsNotFound = dependantsNotFound and flagInv
			if (previousNode.validationMask and newNode.invalidationMask > 0) {
				previousNode.validationMask = previousNode.validationMask or previousNode.validationMask
				newNode.invalidationMask = newNode.invalidationMask or previousNode.invalidationMask
				if (insertIndex > i)
					insertIndex = i
			}
			if (previousNode.invalidationMask and newNode.validationMask > 0) {
				newNode.validationMask = newNode.validationMask or previousNode.validationMask
				previousNode.invalidationMask = previousNode.invalidationMask or newNode.invalidationMask
				if (insertIndex <= i) {
					throw Exception("Validation node cannot be added after dependency ${previousNode.flag.toRadix(2)} and before all dependants ${dependants.toRadix(2)}")
				}
			}
		}
		nodes.add(insertIndex, newNode)
		if (dependantsNotFound != 0)
			throw Exception("validation node added, but the dependant flags: ${dependantsNotFound.toRadix(2)} were not found.")
		if (dependenciesNotFound != 0)
			throw Exception("validation node added, but the dependency flags: ${dependenciesNotFound.toRadix(2)} were not found.")
	}

	fun invalidate(flags: Int = -1): Int {
		var flagsInvalidated = 0
		var flagsToInvalidate = flags and invalidFlags.inv()
		if (flagsToInvalidate == 0) return 0
		for (i in (currentIndex + 1)..nodes.lastIndex) {
			val n = nodes[i]
			if (flagsToInvalidate and n.flag > 0) {
				if (n.isValid) {
					n.isValid = false
					flagsToInvalidate = flagsToInvalidate or n.invalidationMask
					flagsInvalidated = flagsInvalidated or n.flag
				}
			}
		}
		invalidFlags = invalidFlags or flagsInvalidated
		return flagsInvalidated
	}

	fun validate(flags: Int = -1, force: Boolean = false): Int {
		var flagsValidated = 0
		var flagsToValidate = flags and invalidFlags
		if (flagsToValidate == 0) return 0
		for (i in 0..nodes.lastIndex) {
			currentIndex = i
			val n = nodes[i]
			if ((!n.isValid || (force && flags and n.flag > 0)) && flagsToValidate and n.invalidationMask > 0) {
				n.onValidate()
				n.isValid = true
				flagsToValidate = flagsToValidate or n.validationMask
				flagsValidated = flagsValidated or n.flag
			}
		}
		currentIndex = -1
		invalidFlags = invalidFlags and flagsValidated.inv()
		return flagsValidated
	}

	fun isValid(flag: Int): Boolean {
		for (i in 0..nodes.lastIndex) {
			val n = nodes[i]
			if (n.flag == flag) return n.isValid
		}
		throw Exception("Validation node with the flag $flag not found.")
	}
}

fun validationTree(init: ValidationTree.() -> Unit): ValidationTree {
	val v = ValidationTree()
	v.init()
	return v
}

fun <T> Validatable.validationProp(initialValue : T, flags: Int) : ReadWriteProperty<Any, T> {
	return Delegates.observable(initialValue, {
		prop, old, new ->
		if (old != new) {
			invalidate(flags)
		}
	})
}