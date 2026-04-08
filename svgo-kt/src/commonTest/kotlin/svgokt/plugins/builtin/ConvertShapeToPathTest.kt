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

class ConvertShapeToPathTest {

    private val parser = SvgoParser()

    @Test
    fun `given rect element - when plugin runs - then converted to path`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect x="10" y="20" width="100" height="50"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertShapeToPath.fn(
            root,
            NoPluginParam,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<path"),
            message = "Rect should be converted to path, but not found in: $result",
        )
        assertFalse(
            actual = result.contains("<rect"),
            message = "Rect element should be replaced by path, but still found in: $result",
        )
        assertTrue(
            actual = result.contains("d="),
            message = "Path should have d attribute, but not found in: $result",
        )
        assertFalse(
            actual = result.contains("width="),
            message = "width attribute should be removed, but still found in: $result",
        )
        assertFalse(
            actual = result.contains("height="),
            message = "height attribute should be removed, but still found in: $result",
        )
    }

    @Test
    fun `given circle element - when plugin runs with convertArcs - then converted to path with arcs`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><circle cx="50" cy="50" r="30"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val params = ConvertShapeToPathParams(convertArcs = true)
        val visitor = ConvertShapeToPath.fn(
            root,
            params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<path"),
            message = "Circle should be converted to path, but not found in: $result",
        )
        assertFalse(
            actual = result.contains("<circle"),
            message = "Circle element should be replaced by path, but still found in: $result",
        )
        assertTrue(
            actual = result.contains("d="),
            message = "Path should have d attribute with arc commands, but not found in: $result",
        )
        assertFalse(
            actual = result.contains("cx="),
            message = "cx attribute should be removed, but still found in: $result",
        )
        assertFalse(
            actual = result.contains("r="),
            message = "r attribute should be removed, but still found in: $result",
        )
    }

    @Test
    fun `given line element - when plugin runs - then converted to path`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><line x1="10" y1="20" x2="100" y2="50"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertShapeToPath.fn(
            root,
            NoPluginParam,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<path"),
            message = "Line should be converted to path, but not found in: $result",
        )
        assertFalse(
            actual = result.contains("<line"),
            message = "Line element should be replaced by path, but still found in: $result",
        )
        assertFalse(
            actual = result.contains("x1="),
            message = "x1 attribute should be removed, but still found in: $result",
        )
        assertFalse(
            actual = result.contains("y2="),
            message = "y2 attribute should be removed, but still found in: $result",
        )
    }

    @Test
    fun `given polyline element - when plugin runs - then converted to path`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><polyline points="10,20 30,40 50,60"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertShapeToPath.fn(
            root,
            NoPluginParam,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<path"),
            message = "Polyline should be converted to path, but not found in: $result",
        )
        assertFalse(
            actual = result.contains("<polyline"),
            message = "Polyline element should be replaced by path, but still found in: $result",
        )
        assertFalse(
            actual = result.contains("points="),
            message = "points attribute should be removed, but still found in: $result",
        )
    }

    @Test
    fun `given polygon element - when plugin runs - then converted to closed path`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><polygon points="10,20 30,40 50,60"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertShapeToPath.fn(
            root,
            NoPluginParam,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<path"),
            message = "Polygon should be converted to path, but not found in: $result",
        )
        assertFalse(
            actual = result.contains("<polygon"),
            message = "Polygon element should be replaced by path, but still found in: $result",
        )
        // Polygon path should end with z (close path)
        val dAttr = Regex("""d="([^"]+)"""").find(result)?.groupValues?.get(1)
        assertTrue(
            actual = dAttr?.endsWith("z") == true,
            message = "Polygon path should end with 'z', but d attribute is: $dAttr",
        )
    }

    @Test
    fun `given circle element without convertArcs - when plugin runs - then circle preserved`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><circle cx="50" cy="50" r="30"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertShapeToPath.fn(
            root,
            NoPluginParam,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<circle"),
            message = "Circle without convertArcs should be preserved, but not found in: $result",
        )
    }

    @Test
    fun `given ellipse element - when plugin runs with convertArcs - then converted to path`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><ellipse cx="50" cy="50" rx="40" ry="20"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val params = ConvertShapeToPathParams(convertArcs = true)
        val visitor = ConvertShapeToPath.fn(
            root,
            params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<path"),
            message = "Ellipse should be converted to path, but not found in: $result",
        )
        assertFalse(
            actual = result.contains("<ellipse"),
            message = "Ellipse element should be replaced by path, but still found in: $result",
        )
        assertFalse(
            actual = result.contains("rx="),
            message = "rx attribute should be removed, but still found in: $result",
        )
        assertFalse(
            actual = result.contains("ry="),
            message = "ry attribute should be removed, but still found in: $result",
        )
    }
}
