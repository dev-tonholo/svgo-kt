package svgokt.encoding

import svgokt.domain.DataUri
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataUriEncoderTest {

    private val sampleSvg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="40"/></svg>"""

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `given SVG string - when encodeSvgDataUri with base64 - then returns base64 data URI`() {
        // Arrange
        val type = DataUri.Base64

        // Act
        val result = encodeSvgDataUri(svg = sampleSvg, type = type)

        // Assert
        val prefix = "data:image/svg+xml;base64,"
        assertTrue(result.startsWith(prefix), "Result should start with '$prefix' but was '$result'")
        val encoded = result.removePrefix(prefix)
        val decoded = Base64.decode(encoded).decodeToString()
        assertEquals(expected = sampleSvg, actual = decoded)
    }

    @Test
    fun `given SVG string - when encodeSvgDataUri with encoded - then returns encoded data URI`() {
        // Arrange
        val type = DataUri.Enc

        // Act
        val result = encodeSvgDataUri(svg = sampleSvg, type = type)

        // Assert
        val prefix = "data:image/svg+xml,"
        assertTrue(result.startsWith(prefix), "Result should start with '$prefix' but was '$result'")
        val body = result.removePrefix(prefix)
        assertFalse(body.contains('<'), "Encoded result should not contain raw angle brackets '<'")
        assertFalse(body.contains('>'), "Encoded result should not contain raw angle brackets '>'")
        assertTrue(body.contains("%3C"), "Encoded result should contain percent-encoded '<' (%3C)")
        assertTrue(body.contains("%3E"), "Encoded result should contain percent-encoded '>' (%3E)")
    }

    @Test
    fun `given SVG string - when encodeSvgDataUri with unencoded - then returns unencoded data URI`() {
        // Arrange
        val type = DataUri.UnEnc
        val svgWithQuoteAndHash = """<svg id="test" fill="#fff"/>"""

        // Act
        val result = encodeSvgDataUri(svg = svgWithQuoteAndHash, type = type)

        // Assert
        val prefix = "data:image/svg+xml,"
        assertTrue(result.startsWith(prefix), "Result should start with '$prefix' but was '$result'")
        val body = result.removePrefix(prefix)
        assertFalse(body.contains('"'), "Unencoded result should not contain raw double-quotes")
        assertFalse(body.contains('#'), "Unencoded result should not contain raw '#'")
    }
}
