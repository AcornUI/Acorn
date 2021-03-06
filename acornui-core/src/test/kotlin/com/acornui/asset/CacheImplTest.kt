@file:Suppress("RemoveExplicitTypeArguments")

package com.acornui.asset

import com.acornui.async.delay
import com.acornui.di.ContextImpl
import com.acornui.test.initMockDom
import com.acornui.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.milliseconds


class CacheImplTest {

	private val context = ContextImpl()

	@BeforeTest
	fun setup() {
		initMockDom()
	}

	@Test fun testSet() {
		val cache = CacheImpl(context)
		val key = "key"
		cache[key] = "Test"
		assertEquals<String?>("Test", cache[key])
	}

	@Test fun testGc() = runTest {
		val cache = CacheImpl(context, disposalTime = 10.milliseconds)
		val key = "key"
		cache[key] = "Test"

		// Ensure keys with a reference are not discarded.
		cache.refInc(key)
		delay(20.milliseconds)
		assertEquals<String?>("Test", cache[key])

		// Ensure keys without a reference are discarded, but only after at least gcFrames.
		cache.refDec(key)
		delay(2.milliseconds)
		assertEquals<String?>("Test", cache[key])
		delay(50.milliseconds)
		assertEquals<String?>(null, cache[key])
	}

	@Test fun testGc2() = runTest {
		val cache = CacheImpl(context, disposalTime = 10.milliseconds)
		val key = "key"
		cache[key] = "Test"

		// Ensure keys with no references are not immediately discarded.
		cache.refInc(key)
		cache.refDec(key)
		delay(2.milliseconds)
		assertEquals<String?>("Test", cache[key])
		cache.refInc(key)
		delay(20.milliseconds)
		assertEquals<String?>("Test", cache[key])
	}
}