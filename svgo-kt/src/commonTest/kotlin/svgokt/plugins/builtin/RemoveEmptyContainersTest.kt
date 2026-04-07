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

class RemoveEmptyContainersTest {

    private val parser = SvgoParser()
    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    private suspend fun runPlugin(svg: String): String {
        val root = parser.parseSvg(data = svg, from = null)
        val visitor = RemoveEmptyContainers.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }
        return stringifySvg(data = root, userOptions = null)
    }

    @Test
    fun `given empty g element - when plugin runs - then g is removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><g/></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        assertFalse(
            actual = result.contains("<g"),
            message = "Empty g element should be removed, but found it in: $result",
        )
    }

    @Test
    fun `given g with children - when plugin runs - then g is preserved`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><g><rect width="10" height="10"/></g></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        assertTrue(
            actual = result.contains("<g"),
            message = "Non-empty g element should be preserved, but not found in: $result",
        )
    }

    @Test
    fun `given empty svg - when plugin runs - then svg is preserved`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"/>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        assertTrue(
            actual = result.contains("<svg"),
            message = "Root svg element should never be removed, but not found in: $result",
        )
    }

    @Test
    fun `given empty g with id - when plugin runs - then g is preserved`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><g id="myGroup"/></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        assertTrue(
            actual = result.contains("<g"),
            message = "Empty g with id attribute should be preserved (may be referenced), but not found in: $result",
        )
    }
}
