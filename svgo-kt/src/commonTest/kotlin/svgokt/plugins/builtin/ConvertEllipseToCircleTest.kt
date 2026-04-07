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

class ConvertEllipseToCircleTest {

    @Test
    fun `given ellipse with equal rx and ry - when convertEllipseToCircle runs - then ellipse becomes circle`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><ellipse cx="50" cy="50" rx="30" ry="30"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertEllipseToCircle.fn(
            root,
            NoPluginParam,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<circle"),
            message = "Ellipse with equal rx/ry should be converted to circle, but not found in: $result",
        )
        assertFalse(
            actual = result.contains("<ellipse"),
            message = "Ellipse element should be replaced by circle, but still found in: $result",
        )
        assertTrue(
            actual = result.contains("r=\"30\""),
            message = "Circle should have r=\"30\", but not found in: $result",
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

    @Test
    fun `given ellipse with rx auto - when convertEllipseToCircle runs - then ellipse becomes circle with ry as radius`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><ellipse cx="50" cy="50" rx="auto" ry="25"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertEllipseToCircle.fn(
            root,
            NoPluginParam,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<circle"),
            message = "Ellipse with rx=auto should be converted to circle, but not found in: $result",
        )
        assertTrue(
            actual = result.contains("r=\"25\""),
            message = "Circle should use ry value as radius when rx=auto, but not found in: $result",
        )
    }

    @Test
    fun `given ellipse with different rx and ry - when convertEllipseToCircle runs - then ellipse is preserved`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><ellipse cx="50" cy="50" rx="30" ry="20"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertEllipseToCircle.fn(
            root,
            NoPluginParam,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<ellipse"),
            message = "Eccentric ellipse should be preserved as ellipse, but not found in: $result",
        )
        assertFalse(
            actual = result.contains("<circle"),
            message = "Eccentric ellipse should not be converted to circle, but found circle in: $result",
        )
    }

    @Test
    fun `given ellipse with ry auto - when convertEllipseToCircle runs - then ellipse becomes circle with rx as radius`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><ellipse cx="50" cy="50" rx="40" ry="auto"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertEllipseToCircle.fn(
            root,
            NoPluginParam,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<circle"),
            message = "Ellipse with ry=auto should be converted to circle, but not found in: $result",
        )
        assertTrue(
            actual = result.contains("r=\"40\""),
            message = "Circle should use rx value as radius when ry=auto, but not found in: $result",
        )
    }
}
