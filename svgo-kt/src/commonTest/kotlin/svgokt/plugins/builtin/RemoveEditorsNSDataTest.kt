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

class RemoveEditorsNSDataTest {

    private val parser = SvgoParser()
    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    private suspend fun runPlugin(svg: String): String {
        val root = parser.parseSvg(data = svg, from = null)
        val visitor = RemoveEditorsNSData.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }
        return stringifySvg(data = root, userOptions = null)
    }

    @Test
    fun `given SVG with Inkscape namespace declaration - when removeEditorsNSData runs - then xmlns declaration is removed`() =
        runTest {
            // Arrange
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg"
                     xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape">
                  <rect width="100" height="100"/>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertFalse(
                actual = result.contains("xmlns:inkscape"),
                message = "Inkscape xmlns declaration should be removed, but found it in: $result",
            )
            assertTrue(
                actual = result.contains("xmlns="),
                message = "Base xmlns should be preserved in: $result",
            )
        }

    @Test
    fun `given SVG with editor-prefixed element - when removeEditorsNSData runs - then prefixed element is detached`() =
        runTest {
            // Arrange - sodipodi:namedview is a common Inkscape element in exported SVGs
            val svg =
                """<svg xmlns="http://www.w3.org/2000/svg" xmlns:sodipodi="http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd"><sodipodi:namedview id="view"/><rect width="50" height="50"/></svg>"""

            // Act
            val result = runPlugin(svg)

            // Assert
            assertFalse(
                actual = result.contains("sodipodi:namedview"),
                message = "sodipodi:namedview element should be removed, but found it in: $result",
            )
            assertTrue(
                actual = result.contains("<rect"),
                message = "Standard rect element should be preserved in: $result",
            )
        }

    @Test
    fun `given SVG with no editor namespaces - when removeEditorsNSData runs - then SVG is unchanged`() =
        runTest {
            // Arrange
            val svg =
                """<svg xmlns="http://www.w3.org/2000/svg"><rect fill="red" width="100" height="50"/></svg>"""

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("fill=\"red\""),
                message = "fill attribute should still be present in: $result",
            )
            assertTrue(
                actual = result.contains("xmlns="),
                message = "xmlns should be preserved in: $result",
            )
        }

    @Test
    fun `given SVG with Figma namespace and editor element - when removeEditorsNSData runs - then Figma xmlns is removed and element is detached`() =
        runTest {
            // Arrange - figma:component is a common element in Figma-exported SVGs
            val svg =
                """<svg xmlns="http://www.w3.org/2000/svg" xmlns:figma="http://www.figma.com/figma/ns"><figma:component/><rect width="100" height="100"/></svg>"""

            // Act
            val result = runPlugin(svg)

            // Assert
            assertFalse(
                actual = result.contains("xmlns:figma"),
                message = "Figma xmlns should be removed, but found in: $result",
            )
            assertFalse(
                actual = result.contains("figma:component"),
                message = "Figma element should be removed, but found in: $result",
            )
            assertTrue(
                actual = result.contains("<rect"),
                message = "Standard rect should be preserved in: $result",
            )
        }
}
