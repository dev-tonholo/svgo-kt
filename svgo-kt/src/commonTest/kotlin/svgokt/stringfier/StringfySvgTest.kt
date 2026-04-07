package svgokt.stringfier

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.EndOfLine
import svgokt.domain.builder.stringifyOptions
import svgokt.parser.SvgoParser
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class StringfySvgTest {

    @Test
    fun `given parsed SVG - when stringifySvg is called - then output contains svg element`() = runTest {
        val parser = SvgoParser()
        val root = parser.parseSvg(data = TestFixtures.SIMPLE_SVG, from = null)
        val result = stringifySvg(data = root, userOptions = stringifyOptions { })

        assertContains(result, "<svg")
        assertContains(result, "</svg>")
    }

    @Test
    fun `given parsed SVG - when stringifySvg is called with defaults - then attributes are preserved`() = runTest {
        val parser = SvgoParser()
        val root = parser.parseSvg(data = TestFixtures.SIMPLE_SVG, from = null)
        val result = stringifySvg(data = root, userOptions = stringifyOptions { })

        assertContains(result, "xmlns=\"http://www.w3.org/2000/svg\"")
        assertContains(result, "viewBox=\"0 0 100 100\"")
        assertContains(result, "fill=\"red\"")
    }

    @Test
    fun `given parsed SVG - when stringifySvg is called with pretty printing - then output is indented`() = runTest {
        val parser = SvgoParser()
        val root = parser.parseSvg(data = TestFixtures.SIMPLE_SVG, from = null)
        val result = stringifySvg(
            data = root,
            userOptions = stringifyOptions {
                pretty = true
                indent = 2
                eol = EndOfLine.LF
            },
        )

        assertTrue(
            message = "Expected pretty-printed output with newlines but got: $result",
        ) { result.contains("\n") }
        assertTrue(
            message = "Expected indented output but got: $result",
        ) { result.contains("  ") }
    }

    @Test
    fun `given parsed SVG with CDATA - when stringifySvg is called - then CDATA is preserved`() = runTest {
        val parser = SvgoParser()
        val root = parser.parseSvg(data = TestFixtures.PARSER_TEST_SVG, from = null)
        val result = stringifySvg(data = root, userOptions = stringifyOptions { })

        assertContains(result, "<![CDATA[")
        assertContains(result, "]]>")
    }

    @Test
    fun `given parsed SVG with comment - when stringifySvg is called - then comment is preserved`() = runTest {
        val parser = SvgoParser()
        val root = parser.parseSvg(data = TestFixtures.PARSER_TEST_SVG, from = null)
        val result = stringifySvg(data = root, userOptions = stringifyOptions { })

        assertContains(result, "<!--")
        assertContains(result, "-->")
    }

    @Test
    fun `given parsed SVG - when stringifySvg round-trips - then structure is preserved`() = runTest {
        val parser = SvgoParser()
        val root = parser.parseSvg(data = TestFixtures.SIMPLE_SVG, from = null)
        val result = stringifySvg(data = root, userOptions = stringifyOptions { })

        // Round-trip: parse the stringified output and stringify again.
        // A fresh SvgoParser instance is required because the parser cannot be reused
        // after close() is called internally when the first parse completes.
        val parser2 = SvgoParser()
        val root2 = parser2.parseSvg(data = result, from = null)
        val result2 = stringifySvg(data = root2, userOptions = stringifyOptions { })

        assertTrue(
            message = "Round-trip output should contain svg element",
        ) { result2.contains("<svg") }
        assertTrue(
            message = "Round-trip output should preserve attributes",
        ) { result2.contains("viewBox=\"0 0 100 100\"") }
        assertTrue(
            message = "Round-trip output should be structurally equivalent to first stringify",
        ) { result == result2 }
    }
}
