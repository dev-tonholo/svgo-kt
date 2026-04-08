package svgokt.encoding

import svgokt.domain.DataUri
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val SVG_DATA_URI_PREFIX = "data:image/svg+xml,"
private const val SVG_DATA_URI_BASE64_PREFIX = "data:image/svg+xml;base64,"

/**
 * Characters that are safe to leave unencoded in a URI component.
 * This mirrors the characters that JS `encodeURIComponent` leaves unencoded:
 * A-Z a-z 0-9 - _ . ! ~ * ' ( )
 */
private val URI_COMPONENT_SAFE = Regex("[A-Za-z0-9\\-_.!~*'()]")

/** Bitmask to convert a signed byte to an unsigned int value. */
private const val BYTE_MASK = 0xFF

/**
 * Encodes an SVG string as a Data URI, matching svgo's `encodeSVGDatauri` behavior.
 *
 * @param svg the SVG string to encode
 * @param type the [DataUri] encoding type - base64, URL-encoded, or minimally-encoded
 * @return the encoded Data URI string
 */
@OptIn(ExperimentalEncodingApi::class)
fun encodeSvgDataUri(svg: String, type: DataUri): String = when (type) {
    DataUri.Base64 -> {
        val encoded = Base64.encode(svg.encodeToByteArray())
        "$SVG_DATA_URI_BASE64_PREFIX$encoded"
    }
    DataUri.Enc -> {
        val encoded = encodeUriComponent(svg)
        "$SVG_DATA_URI_PREFIX$encoded"
    }
    DataUri.UnEnc -> {
        val encoded = encodeMinimal(svg)
        "$SVG_DATA_URI_PREFIX$encoded"
    }
}

/**
 * Percent-encodes a string the same way JavaScript's `encodeURIComponent` does.
 * All characters except A-Z a-z 0-9 and `- _ . ! ~ * ' ( )` are encoded.
 */
private fun encodeUriComponent(input: String): String = buildString {
    for (char in input) {
        if (URI_COMPONENT_SAFE.matches(char.toString())) {
            append(char)
        } else {
            val bytes = char.toString().encodeToByteArray()
            for (byte in bytes) {
                append('%')
                val hex = byte.toInt().and(BYTE_MASK)
                    .toString(radix = 16).uppercase()
                    .padStart(length = 2, padChar = '0')
                append(hex)
            }
        }
    }
}

/**
 * Minimally encodes an SVG string for use in a Data URI - only encodes characters
 * that would break the URI: double-quotes and `#`.
 * Matches svgo's "unenc" behavior.
 */
private fun encodeMinimal(input: String): String = buildString {
    for (char in input) {
        when (char) {
            '"' -> append("%22")
            '#' -> append("%23")
            else -> append(char)
        }
    }
}
