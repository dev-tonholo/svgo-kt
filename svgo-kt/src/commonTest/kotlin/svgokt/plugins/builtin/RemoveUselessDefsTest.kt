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

class RemoveUselessDefsTest {

    private val parser = SvgoParser()
    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    private suspend fun runPlugin(svg: String): String {
        val root = parser.parseSvg(data = svg, from = null)
        val visitor = RemoveUselessDefs.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }
        return stringifySvg(data = root, userOptions = null)
    }

    @Test
    fun `given defs with child without id - when plugin runs - then child is removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><defs><rect width="10" height="10"/></defs></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert - defs with no useful children should be removed entirely
        assertFalse(
            actual = result.contains("<defs"),
            message = "Defs with no id'd children should be removed. Got: $result",
        )
    }

    @Test
    fun `given defs with child with id - when plugin runs - then child is preserved`() = runTest {
        // Arrange
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg">
                <defs>
                    <linearGradient id="grad1"/>
                </defs>
            </svg>
        """.trimIndent()

        // Act
        val result = runPlugin(svg)

        // Assert
        assertTrue(
            actual = result.contains("linearGradient") && result.contains("grad1"),
            message = "Child with id should be preserved. Got: $result",
        )
    }

    @Test
    fun `given defs with style element - when plugin runs - then style is preserved`() = runTest {
        // Arrange
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg">
                <defs>
                    <style>.cls{fill:red}</style>
                </defs>
            </svg>
        """.trimIndent()

        // Act
        val result = runPlugin(svg)

        // Assert
        assertTrue(
            actual = result.contains("<style"),
            message = "Style element should be preserved in defs. Got: $result",
        )
    }

    @Test
    fun `given empty defs - when plugin runs - then defs is removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><defs/></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        assertFalse(
            actual = result.contains("<defs"),
            message = "Empty defs should be removed. Got: $result",
        )
    }
}
