package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.domain.plugins.PluginInfo
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InlineStylesTest {

    private val parser = SvgoParser()

    @Test
    fun `given SVG with class selector matching one element - when inlineStyles runs - then style is inlined`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg">""" +
            """<style>.red{fill:red}</style>""" +
            """<rect class="red" width="100" height="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val plugin = InlineStyles()
        val visitor = plugin.fn(
            root,
            plugin.params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("style=") && result.contains("fill:red"),
            message = "Style should be inlined on the matching element, but not found in: $result",
        )
    }

    @Test
    fun `given SVG with selector matching multiple elements - when onlyMatchedOnce is true - then style is not inlined`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg">""" +
            """<style>.shared{fill:blue}</style>""" +
            """<rect class="shared" width="50" height="50"/>""" +
            """<circle class="shared" cx="50" cy="50" r="25"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val plugin = InlineStyles(params = InlineStyles.Params(onlyMatchedOnce = true))
        val visitor = plugin.fn(
            root,
            plugin.params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<style"),
            message = "Style element should be preserved when selector matches multiple elements, but not found in: $result",
        )
    }

    @Test
    fun `given SVG with ID selector - when inlineStyles runs - then style is inlined on ID element`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg">""" +
            """<style>#myRect{stroke:green;stroke-width:2}</style>""" +
            """<rect id="myRect" width="100" height="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val plugin = InlineStyles()
        val visitor = plugin.fn(
            root,
            plugin.params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("style=") && result.contains("stroke:green"),
            message = "ID-selector style should be inlined, but not found in: $result",
        )
    }
}
