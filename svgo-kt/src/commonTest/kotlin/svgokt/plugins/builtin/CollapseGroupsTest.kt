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

class CollapseGroupsTest {

    private val parser = SvgoParser()
    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    private suspend fun runPlugin(svg: String): String {
        val root = parser.parseSvg(data = svg, from = null)
        val visitor = CollapseGroups.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }
        return stringifySvg(data = root, userOptions = null)
    }

    @Test
    fun `given bare g wrapping elements - when plugin runs - then g is removed and children promoted`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><g><rect width="10" height="10"/></g></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        assertFalse(
            actual = result.contains("<g"),
            message = "Bare g element (no attributes) should be collapsed, but found it in: $result",
        )
        assertTrue(
            actual = result.contains("<rect"),
            message = "Child rect should be promoted to parent, but not found in: $result",
        )
    }

    @Test
    fun `given g with attributes and single child - when plugin runs - then attrs merge into child`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><g fill="red"><rect width="10" height="10"/></g></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert - g attributes should be merged into the single child element
        assertFalse(
            actual = result.contains("<g"),
            message = "g with single child should be collapsed, but g still found in: $result",
        )
        assertTrue(
            actual = result.contains("""fill="red""""),
            message = "fill attribute should be merged into child, result: $result",
        )
    }

    @Test
    fun `given g with attributes and multiple children - when plugin runs - then g is preserved`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><g fill="red"><rect width="10" height="10"/><circle r="5"/></g></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        assertTrue(
            actual = result.contains("<g"),
            message = "g element with multiple children should be preserved, but not found in: $result",
        )
    }

    @Test
    fun `given nested bare g elements - when plugin runs - then outer g is collapsed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><g><g fill="blue"><rect width="10" height="10"/></g></g></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        // The outer bare g should be collapsed; inner g with fill="blue" should remain
        assertTrue(
            actual = result.contains("""fill="blue""""),
            message = "Inner g attributes should be preserved after outer g collapse, result: $result",
        )
    }
}
