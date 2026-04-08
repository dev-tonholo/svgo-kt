package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.PluginInfo
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConvertTransformTest {

    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    @Test
    fun `given element with multiple transforms - when plugin runs - then collapsed to one`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect transform="translate(10 20) scale(2)"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertTransform.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        // Multiple transforms should be collapsed into a single matrix
        assertFalse(
            actual = result.contains("translate") && result.contains("scale"),
            message = "Multiple transforms should be collapsed, but found both in: $result",
        )
        assertTrue(
            actual = result.contains("transform="),
            message = "transform attribute should still exist after collapse, but not found in: $result",
        )
    }

    @Test
    fun `given element with identity translate - when plugin runs - then transform removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect transform="translate(0 0)"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertTransform.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("transform"),
            message = "Identity transform should be removed, but found it in: $result",
        )
    }

    @Test
    fun `given element with identity scale - when plugin runs - then transform removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect transform="scale(1 1)"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertTransform.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("transform"),
            message = "Identity scale should be removed, but found it in: $result",
        )
    }

    @Test
    fun `given element with identity rotate - when plugin runs - then transform removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect transform="rotate(0)"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertTransform.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("transform"),
            message = "Identity rotate should be removed, but found it in: $result",
        )
    }

    @Test
    fun `given element with single non-identity transform - when plugin runs - then transform preserved`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect transform="translate(10 20)"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertTransform.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("transform="),
            message = "Non-identity single transform should be preserved, but not found in: $result",
        )
    }

    @Test
    fun `given element without transform - when plugin runs - then element unchanged`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect width="100" height="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)
        val originalResult = stringifySvg(data = root, userOptions = null)

        // Act
        val visitor = ConvertTransform.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertEquals(
            expected = originalResult,
            actual = result,
            message = "Element without transform should be unchanged",
        )
    }

    @Test
    fun `given element with identity skewX - when plugin runs - then transform removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect transform="skewX(0)"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertTransform.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("transform"),
            message = "Identity skewX should be removed, but found it in: $result",
        )
    }

    @Test
    fun `given element with gradientTransform - when plugin runs - then gradientTransform is also optimized`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><linearGradient gradientTransform="translate(0 0)"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = ConvertTransform.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("gradientTransform"),
            message = "Identity gradientTransform should be removed, but found it in: $result",
        )
    }
}
