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

class RemoveUnknownsAndDefaultsTest {

    private val parser = SvgoParser()
    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    private suspend fun runPlugin(svg: String): String {
        val root = parser.parseSvg(data = svg, from = null)
        val visitor = RemoveUnknownsAndDefaults.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }
        return stringifySvg(data = root, userOptions = null)
    }

    @Test
    fun `given default fill-rule - when plugin runs - then attribute is removed`() = runTest {
        // Arrange
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg">
                <path fill-rule="nonzero" d="M0 0h10v10H0z"/>
            </svg>
        """.trimIndent()

        // Act
        val result = runPlugin(svg)

        // Assert
        assertFalse(
            actual = result.contains("fill-rule"),
            message = "Default fill-rule='nonzero' should be removed. Got: $result",
        )
    }

    @Test
    fun `given non-default fill-rule - when plugin runs - then attribute is preserved`() = runTest {
        // Arrange
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg">
                <path fill-rule="evenodd" d="M0 0h10v10H0z"/>
            </svg>
        """.trimIndent()

        // Act
        val result = runPlugin(svg)

        // Assert
        assertTrue(
            actual = result.contains("fill-rule"),
            message = "Non-default fill-rule='evenodd' should be preserved. Got: $result",
        )
    }

    @Test
    fun `given default stroke-linecap and opacity - when plugin runs - then both are removed`() = runTest {
        // Arrange
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg">
                <rect stroke-linecap="butt" opacity="1" width="10" height="10"/>
            </svg>
        """.trimIndent()

        // Act
        val result = runPlugin(svg)

        // Assert
        assertFalse(
            actual = result.contains("stroke-linecap"),
            message = "Default stroke-linecap='butt' should be removed. Got: $result",
        )
        assertFalse(
            actual = result.contains("opacity"),
            message = "Default opacity='1' should be removed. Got: $result",
        )
    }

    @Test
    fun `given element with id and default attrs - when plugin runs - then defaults are kept`() = runTest {
        // Arrange
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg">
                <path id="myPath" fill-rule="nonzero" d="M0 0h10v10H0z"/>
            </svg>
        """.trimIndent()

        // Act
        val result = runPlugin(svg)

        // Assert
        assertTrue(
            actual = result.contains("fill-rule"),
            message = "Default attrs on elements with id should be preserved. Got: $result",
        )
    }
}
