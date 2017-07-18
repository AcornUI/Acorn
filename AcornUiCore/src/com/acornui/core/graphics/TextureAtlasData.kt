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

package com.acornui.core.graphics

import com.acornui.gl.core.TextureMagFilter
import com.acornui.gl.core.TextureMinFilter
import com.acornui.gl.core.TexturePixelFormat
import com.acornui.math.IntRectangle
import com.acornui.math.IntRectangleRo
import com.acornui.math.IntRectangleSerializer
import com.acornui.serialization.*

/**
 * @author nbilyk
 */
@Suppress("ArrayInDataClass") // Lazy
data class TextureAtlasData(
		val pages: Array<AtlasPageData>
) {

	/**
	 * Does a search for a region with the given name.
	 * This result should be cached instead of searching every frame.
	 */
	fun findRegion(name: String): Pair<AtlasPageData, AtlasRegionData>? {
		for (page in pages) {
			for (region in page.regions) {
				if (region.name.equalsIgnoreExtension(name)) {
					return Pair(page, region)
				}
			}
		}
		return null
	}

	private fun String.equalsIgnoreExtension(name: String): Boolean {
		if (this.startsWith(name)) {
			if (this.length == name.length) return true
			if (this[name.length] == '.') return true
		}
		return false
	}
}

object TextureAtlasDataSerializer : To<TextureAtlasData>, From<TextureAtlasData> {

	override fun read(reader: Reader): TextureAtlasData {
		return TextureAtlasData(
				pages = reader.array2("pages", AtlasPageSerializer)!!
		)
	}

	override fun TextureAtlasData.write(writer: Writer) {
		writer.array("pages", pages, AtlasPageSerializer)
	}
}

/**
 * Represents a single texture atlas page.
 */
@Suppress("ArrayInDataClass") // Lazy
data class AtlasPageData(
		var texturePath: String,
		var width: Int,
		var height: Int,
		var pixelFormat: TexturePixelFormat,
		var premultipliedAlpha: Boolean,
		var filterMin: TextureMinFilter,
		var filterMag: TextureMagFilter,
		var regions: Array<AtlasRegionData>,
		var hasWhitePixel: Boolean = false
) {

	fun containsRegion(regionName: String): Boolean {
		return regions.find { it.name == regionName } != null
	}

	fun getRegion(regionName: String): AtlasRegionData? {
		return regions.find { it.name == regionName }
	}
}

object AtlasPageSerializer : To<AtlasPageData>, From<AtlasPageData> {

	override fun read(reader: Reader): AtlasPageData {
		return AtlasPageData(
				texturePath = reader.string("texturePath")!!,
				width = reader.int("width")!!,
				height = reader.int("height")!!,
				pixelFormat = TexturePixelFormat.valueOf(reader.string("pixelFormat") ?: TexturePixelFormat.RGBA.name),
				premultipliedAlpha = reader.bool("premultipliedAlpha") ?: false,
				filterMin = TextureMinFilter.valueOf(reader.string("filterMin") ?: TextureMinFilter.LINEAR.name),
				filterMag = TextureMagFilter.valueOf(reader.string("filterMag") ?: TextureMagFilter.LINEAR.name),
				regions = reader.array2("regions", AtlasRegionDataSerializer)!!,
				hasWhitePixel = reader.bool("hasWhitePixel") ?: false
		)
	}

	override fun AtlasPageData.write(writer: Writer) {
		writer.string("texturePath", texturePath)
		writer.int("width", width)
		writer.int("height", height)
		writer.string("pixelFormat", pixelFormat.name)
		writer.bool("premultipliedAlpha", premultipliedAlpha)
		writer.string("filterMin", filterMin.name)
		writer.string("filterMag", filterMag.name)
		writer.array("regions", regions, AtlasRegionDataSerializer)
		writer.bool("hasWhitePixel", hasWhitePixel)
	}
}

/**
 * A model representing a region within an atlas page.
 */
@Suppress("ArrayInDataClass") // Lazy
data class AtlasRegionData(

		/**
		 * The name of the region. This is typically the path of the image used to pack into the region.
		 */
		val name: String,

		/**
		 * True if this region was rotated clockwise 90 degrees.
		 */
		val isRotated: Boolean,

		/**
		 * The bounding rectangle of the region within the page.
		 */
		val bounds: IntRectangleRo,

		/**
		 * Used for 9 patches. An int array of left, top, right, bottom
		 */
		val splits: FloatArray? = null,

		/**
		 * The whitespace padding stripped from the original image, that should be added by the component.
		 * An int array of left, top, right, bottom
		 * The padding values will not be rotated.
		 */
		var padding: IntArray = intArrayOf(0, 0, 0, 0)
) {

	/**
	 * The original width of the image, before whitespace was stripped.
	 */
	val originalWidth: Int
		get() {
			if (isRotated)
				return padding[0] + padding[2] + bounds.height
			else
				return padding[0] + padding[2] + bounds.width
		}

	/**
	 * The original width of the image, before whitespace was stripped.
	 */
	val originalHeight: Int
		get() {
			if (isRotated)
				return padding[1] + padding[3] + bounds.width
			else
				return padding[1] + padding[3] + bounds.height
		}

	/**
	 * The packed width of the image, after whitespace was stripped.
	 */
	val packedWidth: Int
		get() = if (isRotated) bounds.height else bounds.width

	/**
	 * The packed height of the image, after whitespace was stripped.
	 */
	val packedHeight: Int
		get() = if (isRotated) bounds.width else bounds.height
}

object AtlasRegionDataSerializer : To<AtlasRegionData>, From<AtlasRegionData> {

	override fun read(reader: Reader): AtlasRegionData {
		return AtlasRegionData(
				name = reader.string("name") ?: "",
				isRotated = reader.bool("isRotated") ?: false,
				bounds = reader.obj("bounds", IntRectangleSerializer)!!,
				splits = reader.floatArray("splits"),
				padding = reader.intArray("padding") ?: intArrayOf(0, 0, 0, 0)
		)
	}

	override fun AtlasRegionData.write(writer: Writer) {
		writer.string("name", name)
		writer.bool("isRotated", isRotated)
		writer.obj("bounds", bounds, IntRectangleSerializer)
		if (splits != null) writer.floatArray("splits", splits!!)
		writer.intArray("padding", padding)
	}
}