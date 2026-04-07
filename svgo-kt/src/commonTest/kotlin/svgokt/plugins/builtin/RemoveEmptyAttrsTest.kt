package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.domain.builder.svgo
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.PluginInfo
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveEmptyAttrsTest {

    @Test
    fun `given element with empty attr - when removeEmptyAttrs runs - then empty attr is removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect fill="" width="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveEmptyAttrs.fn(root, NoPluginParam, PluginInfo(path = null, multipassCount = 0))
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("fill=\"\""),
            message = "Empty fill attribute should be removed, but found it in: $result",
        )
        assertTrue(
            actual = result.contains("width=\"100\""),
            message = "Non-empty width attribute should be preserved in: $result",
        )
    }

    @Test
    fun `given element with empty conditional processing attr - when removeEmptyAttrs runs - then attr is preserved`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><g requiredFeatures="" fill="red"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveEmptyAttrs.fn(root, NoPluginParam, PluginInfo(path = null, multipassCount = 0))
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("requiredFeatures"),
            message = "requiredFeatures conditional processing attr should be preserved, but not found in: $result",
        )
        assertFalse(
            actual = result.contains("fill=\"\""),
            message = "Empty fill attribute should be removed, but found it in: $result",
        )
    }

    @Test
    fun `given element with no empty attrs - when removeEmptyAttrs runs - then all attrs are preserved`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect fill="red" width="100" height="50"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveEmptyAttrs.fn(root, NoPluginParam, PluginInfo(path = null, multipassCount = 0))
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("fill=\"red\""),
            message = "fill attr should still be present in: $result",
        )
        assertTrue(
            actual = result.contains("width=\"100\""),
            message = "width attr should still be present in: $result",
        )
    }

    @Test
    fun `given SVG pipeline - when removeEmptyAttrs plugin is used - then empty attrs are removed`() = runTest {
        // Arrange
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
            }
        }

        // Act
        val result = optimizer.optimize(
            input = """<svg xmlns="http://www.w3.org/2000/svg"><rect fill="" width="100"/></svg>""",
        )

        // Assert
        assertFalse(
            actual = result.data.contains("fill=\"\""),
            message = "Empty fill attribute should be removed by pipeline, but found it in: ${result.data}",
        )
    }
}
