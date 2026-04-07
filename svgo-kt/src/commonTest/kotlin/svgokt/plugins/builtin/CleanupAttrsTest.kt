package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.PluginInfo
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CleanupAttrsTest {

    @Test
    fun `given attribute with newline between non-whitespace chars - when cleanupAttrs runs - then newline is replaced with space`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect class=\"foo\nbar\"/></svg>"
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupAttrs.fn(root, NoPluginParam, PluginInfo(path = null, multipassCount = 0))
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("foo\nbar"),
            message = "Newline between non-whitespace chars should be replaced, but found in: $result",
        )
        assertTrue(
            actual = result.contains("foo bar"),
            message = "Newline should be replaced with space, resulting in 'foo bar', but not found in: $result",
        )
    }

    @Test
    fun `given attribute with multiple spaces - when cleanupAttrs runs - then spaces are collapsed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect class=\"foo   bar\"/></svg>"
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupAttrs.fn(root, NoPluginParam, PluginInfo(path = null, multipassCount = 0))
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("foo   bar"),
            message = "Multiple spaces should be collapsed, but found them in: $result",
        )
        assertTrue(
            actual = result.contains("foo bar"),
            message = "Multiple spaces should be collapsed to single space, but not found in: $result",
        )
    }

    @Test
    fun `given attribute with leading and trailing whitespace - when cleanupAttrs runs - then whitespace is trimmed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect id=\"  myId  \"/></svg>"
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupAttrs.fn(root, NoPluginParam, PluginInfo(path = null, multipassCount = 0))
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("id=\"myId\""),
            message = "Leading and trailing whitespace should be trimmed, but not found 'myId' in: $result",
        )
    }

    @Test
    fun `given clean attribute with no whitespace issues - when cleanupAttrs runs - then attribute is unchanged`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect fill="red" width="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupAttrs.fn(root, NoPluginParam, PluginInfo(path = null, multipassCount = 0))
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("fill=\"red\""),
            message = "Clean fill attribute should be unchanged in: $result",
        )
        assertTrue(
            actual = result.contains("width=\"100\""),
            message = "Clean width attribute should be unchanged in: $result",
        )
    }
}
