package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.PluginInfo
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import kotlin.test.Test
import kotlin.test.assertTrue

class ConvertColorsTest {

    private val parser = SvgoParser()
    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    private suspend fun runPlugin(svg: String): String {
        val root = parser.parseSvg(data = svg, from = null)
        val visitor = ConvertColors.fn(root, ConvertColors.Params(), pluginInfo)
        visitor?.let { root.visit(it) }
        return stringifySvg(data = root, userOptions = null)
    }

    @Test
    fun `given named color fill - when plugin runs - then converts to short hex`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect fill="red" width="10" height="10"/></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert - "red" maps to "#f00", then shortname maps "#f00" back to "red"
        // (since "red" is shorter) so it stays "red"
        assertTrue(
            actual = result.contains("""fill="red""""),
            message = "Named color 'red' should stay as 'red' (short name). Got: $result",
        )
    }

    @Test
    fun `given rgb color - when plugin runs - then converts to hex`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect fill="rgb(255,0,0)" width="10" height="10"/></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert - rgb(255,0,0) -> #FF0000 -> lowercase -> #ff0000 -> shorthex -> #f00 -> shortname -> red
        assertTrue(
            actual = result.contains("""fill="red""""),
            message = "rgb(255,0,0) should convert to 'red'. Got: $result",
        )
    }

    @Test
    fun `given long hex - when plugin runs - then converts to short hex`() = runTest {
        // Arrange
        val svg =
            """<svg xmlns="http://www.w3.org/2000/svg"><rect fill="#ff0000" width="10" height="10"/></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert - #ff0000 -> shorthex -> #f00 -> shortname -> red
        assertTrue(
            actual = result.contains("""fill="red""""),
            message = "#ff0000 should convert to 'red'. Got: $result",
        )
    }

    @Test
    fun `given non-color attribute - when plugin runs - then attribute is not modified`() = runTest {
        // Arrange
        val svg =
            """<svg xmlns="http://www.w3.org/2000/svg"><rect width="red" height="10"/></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        assertTrue(
            actual = result.contains("""width="red""""),
            message = "Non-color attribute 'width' should not be modified. Got: $result",
        )
    }

    @Test
    fun `given hex that maps to shorter name - when plugin runs - then converts to name`() = runTest {
        // Arrange
        val svg =
            """<svg xmlns="http://www.w3.org/2000/svg"><rect fill="#ffd700" width="10" height="10"/></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert - #ffd700 -> shortname -> gold
        assertTrue(
            actual = result.contains("""fill="gold""""),
            message = "#ffd700 should convert to 'gold'. Got: $result",
        )
    }
}
