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

class CleanupEnableBackgroundTest {

    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    @Test
    fun `given SVG without filter and with enable-background - when plugin runs - then enable-background is removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" enable-background="new 0 0 100 100"><rect enable-background="new 0 0 50 50"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupEnableBackground.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("enable-background"),
            message = "enable-background should be removed when no filter elements exist, but found it in: $result",
        )
    }

    @Test
    fun `given SVG with filter and enable-background - when plugin runs - then enable-background is preserved`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" enable-background="new 0 0 100 100"><filter id="f1"><feBlend/></filter><rect/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupEnableBackground.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("enable-background"),
            message = "enable-background should be preserved when filter elements exist, but was removed from: $result",
        )
    }

    @Test
    fun `given SVG without filter and without enable-background - when plugin runs - then SVG is unchanged`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect width="100" height="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupEnableBackground.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("rect"),
            message = "rect element should still be present in: $result",
        )
        assertFalse(
            actual = result.contains("enable-background"),
            message = "enable-background should not appear in output, but found it in: $result",
        )
    }
}
