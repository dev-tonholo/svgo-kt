package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.domain.plugins.PluginInfo
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CleanupIdsTest {

    private val parser = SvgoParser()

    @Test
    fun `given SVG with unused ID - when cleanupIds runs - then unused ID is removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect id="unused" width="100" height="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val plugin = CleanupIds()
        val visitor = plugin.fn(
            root,
            plugin.params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("id="),
            message = "Unused ID should be removed, but found in: $result",
        )
    }

    @Test
    fun `given SVG with referenced ID via url() - when cleanupIds runs - then referenced ID is kept`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg">""" +
            """<defs><linearGradient id="grad1"><stop offset="0" stop-color="red"/></linearGradient></defs>""" +
            """<rect fill="url(#grad1)" width="100" height="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val plugin = CleanupIds()
        val visitor = plugin.fn(
            root,
            plugin.params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("id="),
            message = "Referenced ID should be kept, but not found in: $result",
        )
        assertTrue(
            actual = result.contains("url(#"),
            message = "url() reference should still be present, but not found in: $result",
        )
    }

    @Test
    fun `given SVG with referenced ID via href - when cleanupIds runs with minify - then ID is minified`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">""" +
            """<defs><linearGradient id="longGradientName"><stop offset="0" stop-color="red"/></linearGradient></defs>""" +
            """<rect fill="url(#longGradientName)" width="100" height="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val plugin = CleanupIds(params = CleanupIds.Params(minify = true))
        val visitor = plugin.fn(
            root,
            plugin.params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("longGradientName"),
            message = "Long ID should be minified, but still found in: $result",
        )
        assertTrue(
            actual = result.contains("id="),
            message = "Referenced ID should still exist (minified), but not found in: $result",
        )
    }

    @Test
    fun `given SVG with style element - when cleanupIds runs without force - then IDs are preserved`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg">""" +
            """<style>.cls{fill:red}</style>""" +
            """<rect id="myRect" class="cls" width="100" height="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val plugin = CleanupIds(params = CleanupIds.Params(force = false))
        val visitor = plugin.fn(
            root,
            plugin.params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("id=\"myRect\""),
            message = "ID should be preserved when style element is present, but not found in: $result",
        )
    }
}
