package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.domain.plugins.PluginInfo
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class RemoveHiddenElemsTest {

    private val parser = SvgoParser()
    private val defaultParams = RemoveHiddenElems.Params()

    @Test
    fun `given element with display none - when plugin runs - then element removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect display="none" width="100"/><circle r="5"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveHiddenElems.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("display=\"none\""),
            message = "Element with display=none should be removed, but got: $result",
        )
        assertContains(
            charSequence = result,
            other = "<circle",
            message = "Visible circle should be preserved, but got: $result",
        )
    }

    @Test
    fun `given circle with zero radius - when plugin runs - then circle removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><circle r="0"/><rect width="10" height="10"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveHiddenElems.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("<circle"),
            message = "Circle with r=0 should be removed, but got: $result",
        )
        assertContains(
            charSequence = result,
            other = "<rect",
            message = "Rect should be preserved, but got: $result",
        )
    }

    @Test
    fun `given defs child with display none - when plugin runs - then element preserved`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><defs><linearGradient display="none" id="g1"/></defs></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveHiddenElems.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertContains(
            charSequence = result,
            other = "linearGradient",
            message = "Elements inside defs should be preserved even with display=none, but got: $result",
        )
    }

    @Test
    fun `given ellipse with zero rx - when plugin runs - then ellipse removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><ellipse rx="0" ry="5"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveHiddenElems.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("<ellipse"),
            message = "Ellipse with rx=0 should be removed, but got: $result",
        )
    }

    @Test
    fun `given rect with zero width - when plugin runs - then rect removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect width="0" height="10"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveHiddenElems.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("<rect"),
            message = "Rect with width=0 should be removed, but got: $result",
        )
    }

    @Test
    fun `given path with empty d - when plugin runs - then path removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><path d=""/><rect width="10" height="10"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveHiddenElems.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("<path"),
            message = "Path with empty d should be removed, but got: $result",
        )
    }

    @Test
    fun `given element with visibility hidden - when plugin runs - then element removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect visibility="hidden" width="100"/><circle r="5"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveHiddenElems.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("visibility=\"hidden\""),
            message = "Element with visibility=hidden should be removed, but got: $result",
        )
    }

    @Test
    fun `given image with zero height - when plugin runs - then image removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><image width="10" height="0"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveHiddenElems.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("<image"),
            message = "Image with height=0 should be removed, but got: $result",
        )
    }
}
