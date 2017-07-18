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

package com.acornui.gl.component.text

import com.acornui.action.Decorator
import com.acornui.math.IntRectangle
import com.acornui.math.MathUtils
import com.acornui.string.StringParser
import com.acornui.core.replace2

/**
 * http://www.angelcode.com/products/bmfont/doc/file_format.html
 *
 * @author nbilyk
 */
object AngelCodeParser : Decorator<String, BitmapFontData> {

	private val CHAR_MAX_VALUE: Char = '\uFFFF'

	fun parse(str: String): BitmapFontData {
		val parser = StringParser(str)

		// Info
		parser.white()
		parser.consumeString("info")
		val face = parseQuotedStringProp(parser, "face")

		val size = MathUtils.abs(parseIntProp(parser, "size"))
		val isBold = parseBoolProp(parser, "bold")
		val isItalic = parseBoolProp(parser, "italic")

		// Common
		nextLine(parser)
		parser.consumeString("common")
		val lineHeight = parseIntProp(parser, "lineHeight")
		val baseLine = parseIntProp(parser, "base")
		val pageW = parseIntProp(parser, "scaleW")
		val pageH = parseIntProp(parser, "scaleH")
		val totalPages = parseIntProp(parser, "pages")

		val pages = Array(totalPages, {
			// Read each "page" info line.
			nextLine(parser)
			if (!parser.consumeString("page")) throw Exception("Missing page definition.")

			val id = parseIntProp(parser, "id")
			val fileName: String = parseQuotedStringProp(parser, "file")
			val imagePath = fileName.replace2('\\', '/')
			BitmapFontPageData(id, imagePath)
		})

		// Chars
		nextLine(parser)
		if (!parser.consumeString("chars")) throw Exception("Expected chars block")
		// The chars count from Hiero is off, so we're not going to use it.
		val glyphs = HashMap<Char, GlyphData>()

		// Read each glyph definition.
		while (true) {
			nextLine(parser)
			if (!parser.consumeString("char")) break

			val ch = parseIntProp(parser, "id").toChar()
			if (ch > CHAR_MAX_VALUE) continue

			val regionX = parseIntProp(parser, "x")
			val regionY = parseIntProp(parser, "y")
			val regionW = parseIntProp(parser, "width")
			val regionH = parseIntProp(parser, "height")
			val offsetX = parseIntProp(parser, "xoffset")
			val offsetY = parseIntProp(parser, "yoffset")
			val xAdvance = parseIntProp(parser, "xadvance")
			val page = parseIntProp(parser, "page")
			glyphs[ch] = GlyphData(
					char = ch,
					region = IntRectangle(regionX, regionY, regionW,  regionH),
					offsetX = offsetX,
					offsetY = offsetY,
					advanceX = xAdvance,
					page = page
			)
		}

		parser.consumeString("kernings")
		while (true) {
			nextLine(parser)
			if (!parser.consumeString("kerning")) break

			val first = parseIntProp(parser, "first")
			val second = parseIntProp(parser, "second")
			if (first < 0 || second < 0) continue
			val amount = parseIntProp(parser, "amount")
			if (glyphs.containsKey(first.toChar())) {
				// Kerning pairs may exist for glyphs not contained in the font.
				val glyph = glyphs[first.toChar()]!!
				glyph.setKerning(second.toChar(), amount)
			}
		}

		glyphs.addMissingCommonGlyphs()

		return BitmapFontData(
				fontStyle = FontStyle(face, size, isBold, isItalic),
				pages = pages.toList(),
				glyphs = glyphs,
				lineHeight = lineHeight,
				baseLine = baseLine,
				pageW = pageW,
				pageH = pageH
		)
	}

	private fun MutableMap<Char, GlyphData>.addMissingCommonGlyphs() {
		var space = this[' ']
		if (space == null) {
			var copy = this['n']
			if (copy == null) copy = this.values.firstOrNull()
			if (copy == null) return // There's no hope
			space = GlyphData(char = ' ', advanceX = copy.advanceX)
			this[' '] = space
		}
		for (char in arrayOf('\t', '\n', '\r', 0.toChar())) {
			// Invisible characters that take up no space.
			if (this[char] == null) {
				this[char] = GlyphData(char)
			}
		}
		val nbspChar = '�'
		if (this[nbspChar] == null) {
			this[nbspChar] = GlyphData(char = nbspChar, advanceX = space.advanceX)
		}
		val unknownChar = (-1).toChar()
		this[unknownChar] = (this['?'] ?: space).copy(char = unknownChar, advanceX = space.advanceX)
	}

	private fun nextLine(parser: StringParser): Boolean {
		val found = parser.consumeThrough('\n')
		if (found) parser.white()
		return found
	}

	private fun parseFloatProp(parser: StringParser, property: String): Float {
		consumeProperty(parser, property)
		return parser.getFloat()!!
	}

	private fun parseBoolProp(parser: StringParser, property: String): Boolean {
		consumeProperty(parser, property)
		return parser.getBoolean()!!
	}

	private fun parseIntProp(parser: StringParser, property: String): Int {
		consumeProperty(parser, property)
		return parser.getInt()!!
	}

	private fun parseQuotedStringProp(parser: StringParser, property: String): String {
		consumeProperty(parser, property)
		return parser.getQuotedString()!!
	}

	private fun consumeProperty(parser: StringParser, property: String, required: Boolean = true): Boolean {
		parser.white()
		val found = parser.consumeString(property)
		if (!found) {
			if (required) throw Exception("Property not found: $property at: ${parser.data.substring(parser.position, minOf(parser.length, parser.position + 20))}")
			else return false
		}
		parser.white()
		parser.consumeChar('=')
		parser.white()
		return true
	}

	override fun decorate(target: String): BitmapFontData = parse(target)
}