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

class RemoveEmptyTextTest {

    @Test
    fun `given empty text element - when removeEmptyText runs - then text element is removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><text/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveEmptyText.fn(root, NoPluginParam, PluginInfo(path = null, multipassCount = 0))
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("<text"),
            message = "Empty text element should be removed, but found it in: $result",
        )
    }

    @Test
    fun `given empty tspan element - when removeEmptyText runs - then tspan element is removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><text><tspan/></text></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveEmptyText.fn(root, NoPluginParam, PluginInfo(path = null, multipassCount = 0))
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("<tspan"),
            message = "Empty tspan element should be removed, but found it in: $result",
        )
    }

    @Test
    fun `given tref without xlink href - when removeEmptyText runs - then tref is removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><text><tref/></text></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveEmptyText.fn(root, NoPluginParam, PluginInfo(path = null, multipassCount = 0))
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("<tref"),
            message = "tref without xlink:href should be removed, but found it in: $result",
        )
    }

    @Test
    fun `given text element with children - when removeEmptyText runs - then text element is preserved`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><text>Hello</text></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveEmptyText.fn(root, NoPluginParam, PluginInfo(path = null, multipassCount = 0))
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<text"),
            message = "Non-empty text element should be preserved, but not found in: $result",
        )
    }
}
