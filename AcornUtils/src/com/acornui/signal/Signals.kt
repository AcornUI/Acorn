/*
 * Copyright 2014 Nicholas Bilyk
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

package com.acornui.signal

import com.acornui.core.Disposable

interface Signal<in T : Any> {

	/**
	 * True if the signal has handlers.
	 */
	fun isNotEmpty(): Boolean

	/**
	 * True if the signal has no handlers.
	 */
	fun isEmpty(): Boolean

	fun add(handler: T) = add(handler, isOnce = false)

	/**
	 * Adds a handler to this signal.
	 *
	 * If this signal is currently dispatching, the added handler will not be dispatched until the next dispatch call.
	 *
	 * @param handler The callback that will be invoked when dispatch() is called on the signal.
	 * @param isOnce A flag, where if true, will cause the handler to be removed immediately after the next dispatch.
	 *
	 */
	fun add(handler: T, isOnce: Boolean)

	fun addOnce(handler: T) {
		add(handler, true)
	}

	/**
	 * Removes the given handler from the list.
	 *
	 * If this signal is currently dispatching, the handler will be removed after the dispatch has finished.
	 */
	fun remove(handler: T)

	/**
	 * Returns true if the handler is currently in the list.
	 */
	fun contains(handler: T): Boolean

}


/**
 * Signals are a way to implement an observer relationship for a single event.
 *
 * The expected concurrent behavior for a handler adding or removing is as follows:
 *
 * If a handler is added within a handler, the new handler will be invoked in the current iteration.
 * If a handler is removed within a handler, the removed handler will not be invoked in the current iteration.
 *
 * @author nbilyk
 */
abstract class SignalBase<T : Any> : Signal<T>, Disposable {

	protected val handlers = arrayListOf<T>()
	protected val isOnces = arrayListOf<Boolean>()
	protected var cursor = -1

	/**
	 * True if the signal has handlers.
	 */
	override fun isNotEmpty(): Boolean = handlers.isNotEmpty()

	/**
	 * True if the signal has no handlers.
	 */
	override fun isEmpty(): Boolean = handlers.isEmpty()

	/**
	 * Adds a handler to this signal.
	 *
	 * If this signal is currently dispatching, the added handler will not be dispatched until the next dispatch call.
	 *
	 * @param handler The callback that will be invoked when dispatch() is called on the signal.
	 * @param isOnce A flag, where if true, will cause the handler to be removed immediately after the next dispatch.
	 */
	override fun add(handler: T, isOnce: Boolean) {
		handlers.add(handler)
		isOnces.add(isOnce)
	}

	/**
	 * Removes the given handler from the list.
	 *
	 * If this signal is currently dispatching, the handler will be removed after the dispatch has finished.
	 */
	override fun remove(handler: T) {
		val index = handlers.indexOf(handler)
		if (index != -1) {
			removeAt(index)
		}
	}

	protected fun removeAt(index: Int) {
		if (index <= cursor) {
			cursor--
		}
		handlers.removeAt(index)
		isOnces.removeAt(index)
	}

	/**
	 * Returns true if the handler is currently in the list.
	 */
	override fun contains(handler: T): Boolean = handlers.contains(handler)

	/**
	 * Immediately halts the current dispatch.
	 * @throws Exception if this Signal is not currently dispatching.
	 */
	open fun halt() {
		if (cursor == -1) throw Exception("This signal is not currently dispatching.")
		cursor = 999999999
	}

	/**
	 * Calls executor on each handler in this signal.
	 */
	protected inline fun dispatch(executor: (T) -> Unit): Unit {
		if (cursor != -1) throw Exception("This signal is currently dispatching.")
		cursor = 0
		if (handlers.size <= 4) {
			if (cursor < handlers.size) {
				val isOnce1 = isOnces[cursor]
				val handler1 = handlers[cursor]
				if (isOnce1) removeAt(cursor)
				executor(handler1)
				cursor++
			}
			if (cursor < handlers.size) {
				val isOnce2 = isOnces[cursor]
				val handler2 = handlers[cursor]
				if (isOnce2) removeAt(cursor)
				executor(handler2)
				cursor++
			}
			if (cursor < handlers.size) {
				val isOnce3 = isOnces[cursor]
				val handler3 = handlers[cursor]
				if (isOnce3) removeAt(cursor)
				executor(handler3)
				cursor++
			}
			if (cursor < handlers.size) {
				val isOnce4 = isOnces[cursor]
				val handler4 = handlers[cursor]
				if (isOnce4) removeAt(cursor)
				executor(handler4)
				cursor++
			}
		}
		while (cursor < handlers.size) {
			val isOnce = isOnces[cursor]
			val handler = handlers[cursor]
			if (isOnce) removeAt(cursor)
			executor(handler)
			cursor++
		}

		cursor = -1
	}

	fun clear() {
		handlers.clear()
		isOnces.clear()
	}

	override fun dispose() {
		clear()
	}
}

class Signal0 : SignalBase<() -> Unit>() {
	fun dispatch(): Unit = dispatch { it() }
}

class Signal1<P1> : SignalBase<(P1) -> Unit>() {
	fun dispatch(p1: P1): Unit = dispatch { it(p1) }
}

class Signal2<P1, P2> : SignalBase<(P1, P2) -> Unit>() {
	fun dispatch(p1: P1, p2: P2): Unit = dispatch { it(p1, p2) }
}

class Signal3<P1, P2, P3> : SignalBase<(P1, P2, P3) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3): Unit = dispatch { it(p1, p2, p3) }
}

class Signal4<P1, P2, P3, P4> : SignalBase<(P1, P2, P3, P4) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4): Unit = dispatch { it(p1, p2, p3, p4) }
}

class Signal5<P1, P2, P3, P4, P5> : SignalBase<(P1, P2, P3, P4, P5) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5): Unit = dispatch { it(p1, p2, p3, p4, p5) }
}

class Signal6<P1, P2, P3, P4, P5, P6> : SignalBase<(P1, P2, P3, P4, P5, P6) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6): Unit = dispatch { it(p1, p2, p3, p4, p5, p6) }
}

class Signal7<P1, P2, P3, P4, P5, P6, P7> : SignalBase<(P1, P2, P3, P4, P5, P6, P7) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7): Unit = dispatch { it(p1, p2, p3, p4, p5, p6, p7) }
}

class Signal8<P1, P2, P3, P4, P5, P6, P7, P8> : SignalBase<(P1, P2, P3, P4, P5, P6, P7, P8) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8): Unit = dispatch { it(p1, p2, p3, p4, p5, p6, p7, p8) }
}

class Signal9<P1, P2, P3, P4, P5, P6, P7, P8, P9> : SignalBase<(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> Unit>() {
	fun dispatch(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9): Unit = dispatch { it(p1, p2, p3, p4, p5, p6, p7, p8, p9) }
}

class SignalR0<R> : SignalBase<R.() -> Unit>() {
	fun dispatch(r: R): Unit = dispatch { r.it() }
}

class SignalR1<R, P1> : SignalBase<R.(P1) -> Unit>() {
	fun dispatch(r: R, p1: P1): Unit = dispatch { r.it(p1) }
}

class SignalR2<R, P1, P2> : SignalBase<R.(P1, P2) -> Unit>() {
	fun dispatch(r: R, p1: P1, p2: P2): Unit = dispatch { r.it(p1, p2) }
}

class SignalR3<R, P1, P2, P3> : SignalBase<R.(P1, P2, P3) -> Unit>() {
	fun dispatch(r: R, p1: P1, p2: P2, p3: P3): Unit = dispatch { r.it(p1, p2, p3) }
}

class SignalR4<R, P1, P2, P3, P4> : SignalBase<R.(P1, P2, P3, P4) -> Unit>() {
	fun dispatch(r: R, p1: P1, p2: P2, p3: P3, p4: P4): Unit = dispatch { r.it(p1, p2, p3, p4) }
}

interface Stoppable {
	fun isStopped(): Boolean
}

/**
 * A utility class to use as a parameter within a Signal that indicates that the behavior of signal should be
 * canceled. Typically, a signal that can be canceled should be named as a gerund. Such as, changing, invalidating, etc.
 */
open class Cancel {

	private var _canceled: Boolean = false

	fun canceled(): Boolean {
		return _canceled
	}

	open fun cancel() {
		_canceled = true
	}

	fun reset(): Cancel {
		_canceled = false
		return this
	}
}

/**
 * A convenience interface for a signal that may be halted by its single, stoppable parameter.
 */
interface StoppableSignal<out P1 : Stoppable> : Signal<(P1) -> Unit>

/**
 * A signal where the single argument has the ability to halt the signal.
 * @see [SignalBase.halt]
 */
open class StoppableSignalImpl<P1 : Stoppable> : SignalBase<(P1) -> Unit>(), StoppableSignal<P1> {
	fun dispatch(p1: P1): Unit = dispatch {
		it(p1)
		if (p1.isStopped()) halt()
	}
}