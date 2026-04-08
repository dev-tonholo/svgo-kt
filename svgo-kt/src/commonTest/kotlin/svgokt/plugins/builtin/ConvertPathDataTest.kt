package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.domain.plugins.PluginInfo
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import kotlin.test.Test
import kotlin.test.assertTrue

class ConvertPathDataTest {

    private val parser = SvgoParser()

    @Test
    fun `given path with verbose coordinates - when convertPathData runs - then coordinates are rounded`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg">""" +
            """<path d="M 10.12345 20.67891 L 30.98765 40.54321"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val plugin = ConvertPathData(
            params = ConvertPathDataParams(floatPrecision = 1),
        )
        val visitor = plugin.fn(
            root,
            plugin.params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        val dAttr = Regex("""d="([^"]+)"""").find(result)?.groupValues?.get(1)
        assertTrue(
            actual = dAttr != null && !dAttr.contains("12345"),
            message = "Long decimal should be rounded, but d attribute is: $dAttr in: $result",
        )
    }

    @Test
    fun `given path with horizontal line - when lineShorthands enabled - then L converted to H`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg">""" +
            """<path d="M0 0 L100 0"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val plugin = ConvertPathData(
            params = ConvertPathDataParams(lineShorthands = true),
        )
        val visitor = plugin.fn(
            root,
            plugin.params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        val dAttr = Regex("""d="([^"]+)"""").find(result)?.groupValues?.get(1)
        assertTrue(
            actual = dAttr != null && dAttr.contains("H"),
            message = "Horizontal line should be converted to H command, but d attribute is: $dAttr in: $result",
        )
    }

    @Test
    fun `given path with vertical line - when lineShorthands enabled - then L converted to V`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg">""" +
            """<path d="M0 0 L0 100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val plugin = ConvertPathData(
            params = ConvertPathDataParams(lineShorthands = true),
        )
        val visitor = plugin.fn(
            root,
            plugin.params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        val dAttr = Regex("""d="([^"]+)"""").find(result)?.groupValues?.get(1)
        assertTrue(
            actual = dAttr != null && dAttr.contains("V"),
            message = "Vertical line should be converted to V command, but d attribute is: $dAttr in: $result",
        )
    }

    @Test
    fun `given path with useless zero-length line - when removeUseless enabled - then line is removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg">""" +
            """<path d="M10 20 l0 0 L30 40"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val plugin = ConvertPathData(
            params = ConvertPathDataParams(removeUseless = true),
        )
        val visitor = plugin.fn(
            root,
            plugin.params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        val dAttr = Regex("""d="([^"]+)"""").find(result)?.groupValues?.get(1)
        assertTrue(
            actual = dAttr != null && !dAttr.contains("l0 0"),
            message = "Zero-length relative line should be removed, but d attribute is: $dAttr in: $result",
        )
    }
}
