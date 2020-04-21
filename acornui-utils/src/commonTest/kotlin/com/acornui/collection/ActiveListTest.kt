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

package com.acornui.collection

import kotlin.test.Test

import com.acornui.test.*

/**
 * @author nbilyk
 */
class ActiveListTest {

	@Test fun concurrentRemove() {
		val actual = ArrayList<Char>()
		val list = ActiveList(arrayListOf('a', 'b', 'c', 'd'))
		list.forEach(0) {
			actual.add(it)
			if (it == 'b') list.remove('c')
		}
		assertListEquals(arrayOf('a', 'b', 'c', 'd'), actual)
		assertListEquals(arrayOf('a', 'b', 'd'), list.toTypedArray())
	}

	@Test fun concurrentRemove2() {
		val actual = ArrayList<Char>()
		val list = ActiveList(arrayListOf('a', 'b', 'c', 'd'))
		list.forEach {
			actual.add(it)
			if (it == 'b') list.remove('b')
		}
		assertListEquals(arrayOf('a', 'b', 'c', 'd'), actual)
		assertListEquals(arrayOf('a', 'c', 'd'), list.toTypedArray())
	}

	@Test fun concurrentAdd() {
		val actual = ArrayList<Char>()
		val list = ActiveList(arrayListOf('a', 'b', 'c', 'd'))
		list.forEach {
			actual.add(it)
			if (it == 'b') list.add(1, 'e')
		}
		assertListEquals(arrayOf('a', 'b', 'c', 'd'), actual)
		assertListEquals(arrayOf('a', 'e', 'b', 'c', 'd'), list.toTypedArray())
	}

	@Test fun concurrentAdd2() {
		val actual = ArrayList<Char>()
		val list = ActiveList(arrayListOf('a', 'b', 'c', 'd'))
		list.forEach() {
			actual.add(it)
			if (it == 'b') list.add(2, 'e')
		}
		assertListEquals(arrayOf('a', 'b', 'c', 'd'), actual)
		assertListEquals(arrayOf('a', 'b', 'e', 'c', 'd'), list.toTypedArray())
	}

	@Test fun concurrentClear() {
		val actual = ArrayList<Char>()
		val list = ActiveList(arrayListOf('a', 'b', 'c', 'd'))
		list.forEach {
			actual.add(it)
			if (it == 'b') list.clear()
		}
		assertListEquals(arrayOf('a', 'b', 'c', 'd'), actual)
		assertListEquals(arrayOf(), list.toTypedArray())
	}

	@Test fun nested() {
		val actual = ArrayList<Char>()
		val list = ActiveList(arrayListOf('a', 'b', 'c', 'd'))
		list.forEach {
			actual.add(it)
			list.forEach { it2 ->
				actual.add(it2)
			}
		}
		assertListEquals(arrayOf('a', 'a', 'b', 'c', 'd', 'b', 'a', 'b', 'c', 'd', 'c', 'a', 'b', 'c', 'd', 'd', 'a', 'b', 'c', 'd'), actual)
		assertListEquals(listOf('a', 'b', 'c', 'd'), list)
	}

	@Test fun nested2() {
		val actual = ArrayList<Char>()
		val list = ActiveList(arrayListOf('a', 'b', 'c', 'd'))
		list.forEach {
			if (it != 'e') {
				actual.add(it)
				list.forEach { it2 ->
					actual.add(it2)
					if (it2 == 'b') list.add(2, 'e')
				}
			}
		}
		assertListEquals(arrayOf('a', 'a', 'b', 'c', 'd', 'b', 'a', 'b', 'c', 'd', 'c', 'a', 'b', 'c', 'd', 'd', 'a', 'b', 'c', 'd'), actual)
		assertListEquals(arrayOf('a', 'b', 'e', 'e', 'e', 'e', 'c', 'd'), list.toTypedArray())
	}

	@Test fun nested3() {
		val list = ActiveList(arrayListOf('a', 'b', 'c', 'd'))
		list.forEach {
			val iterator = list.iterator()
			while (iterator.hasNext()) {
				iterator.next()
				iterator.remove()
			}
		}
		assertListEquals(arrayOf(), list.toTypedArray())
	}

	@Test fun addAll() {

	}
}