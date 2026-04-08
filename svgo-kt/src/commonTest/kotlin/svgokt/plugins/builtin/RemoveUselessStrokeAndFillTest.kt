package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.domain.plugins.PluginInfo
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class RemoveUselessStrokeAndFillTest {

    private val parser = SvgoParser()
    private val defaultParams = RemoveUselessStrokeAndFill.Params()

    @Test
    fun `given element with stroke none - when plugin runs - then stroke attrs removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect stroke="none" stroke-width="2" stroke-dasharray="5"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveUselessStrokeAndFill.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("stroke-width"),
            message = "stroke-width should be removed when stroke=none, but got: $result",
        )
        assertFalse(
            actual = result.contains("stroke-dasharray"),
            message = "stroke-dasharray should be removed when stroke=none, but got: $result",
        )
    }

    @Test
    fun `given element with fill none - when plugin runs - then fill attrs removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect fill="none" fill-opacity="0.5" fill-rule="evenodd"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveUselessStrokeAndFill.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("fill-opacity"),
            message = "fill-opacity should be removed when fill=none, but got: $result",
        )
        assertFalse(
            actual = result.contains("fill-rule"),
            message = "fill-rule should be removed when fill=none, but got: $result",
        )
        assertContains(
            charSequence = result,
            other = "fill=\"none\"",
            message = "fill=none should be preserved, but got: $result",
        )
    }

    @Test
    fun `given element with id - when plugin runs - then element is preserved`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect id="myRect" stroke="none" stroke-width="2"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveUselessStrokeAndFill.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertContains(
            charSequence = result,
            other = "stroke-width=\"2\"",
            message = "stroke-width should be preserved when element has id, but got: $result",
        )
    }

    @Test
    fun `given non-shape element with stroke none - when plugin runs - then attrs preserved`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><g stroke="none" stroke-width="2"><rect/></g></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveUselessStrokeAndFill.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertContains(
            charSequence = result,
            other = "stroke-width",
            message = "stroke-width on non-shape <g> should be preserved, but got: $result",
        )
    }

    @Test
    fun `given element with stroke-opacity 0 - when plugin runs - then stroke attrs removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect stroke="red" stroke-opacity="0" stroke-width="2"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveUselessStrokeAndFill.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("stroke-width"),
            message = "stroke-width should be removed when stroke-opacity=0, but got: $result",
        )
        assertFalse(
            actual = result.contains("stroke-opacity"),
            message = "stroke-opacity should be removed when stroke-opacity=0, but got: $result",
        )
    }
}
