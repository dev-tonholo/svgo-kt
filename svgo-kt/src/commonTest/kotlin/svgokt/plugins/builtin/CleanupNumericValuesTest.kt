package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.domain.plugins.PluginInfo
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CleanupNumericValuesTest {

    private val parser = SvgoParser()
    private val defaultParams = CleanupNumericValues.Params()

    @Test
    fun `given element with px units - when plugin runs - then px is removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect width="100px" height="50px"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupNumericValues.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertContains(
            charSequence = result,
            other = "width=\"100\"",
            message = "px unit should be removed from width, but got: $result",
        )
        assertContains(
            charSequence = result,
            other = "height=\"50\"",
            message = "px unit should be removed from height, but got: $result",
        )
    }

    @Test
    fun `given element with precise values - when plugin runs - then values are rounded`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect x="1.23456" y="2.78901"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupNumericValues.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertContains(
            charSequence = result,
            other = "x=\"1.235\"",
            message = "x should be rounded to 3 decimal places, but got: $result",
        )
        assertContains(
            charSequence = result,
            other = "y=\"2.789\"",
            message = "y should be rounded to 3 decimal places, but got: $result",
        )
    }

    @Test
    fun `given element with leading zero - when plugin runs - then leading zero is removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect x="0.5" y="-0.3"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupNumericValues.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertContains(
            charSequence = result,
            other = "x=\".5\"",
            message = "Leading zero should be removed from 0.5, but got: $result",
        )
        assertContains(
            charSequence = result,
            other = "y=\"-.3\"",
            message = "Leading zero should be removed from -0.3, but got: $result",
        )
    }

    @Test
    fun `given element with trailing zeros - when plugin runs - then trailing zeros are removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><rect width="1.0" height="2.50"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupNumericValues.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertContains(
            charSequence = result,
            other = "width=\"1\"",
            message = "1.0 should become 1, but got: $result",
        )
        assertContains(
            charSequence = result,
            other = "height=\"2.5\"",
            message = "2.50 should become 2.5, but got: $result",
        )
    }

    @Test
    fun `given version attribute - when plugin runs - then version is preserved`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" version="1.1"><rect width="10"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupNumericValues.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertContains(
            charSequence = result,
            other = "version=\"1.1\"",
            message = "version attribute should not be modified, but got: $result",
        )
    }

    @Test
    fun `given removeLeadingZero with positive fraction - when called - then zero is removed`() {
        // Arrange & Act
        val result = CleanupNumericValues.removeLeadingZero(value = 0.5)

        // Assert
        assertEquals(expected = ".5", actual = result)
    }

    @Test
    fun `given removeLeadingZero with negative fraction - when called - then zero is removed`() {
        // Arrange & Act
        val result = CleanupNumericValues.removeLeadingZero(value = -0.5)

        // Assert
        assertEquals(expected = "-.5", actual = result)
    }

    @Test
    fun `given removeLeadingZero with whole number - when called - then number is unchanged`() {
        // Arrange & Act
        val result = CleanupNumericValues.removeLeadingZero(value = 5.0)

        // Assert
        assertEquals(expected = "5", actual = result)
    }

    @Test
    fun `given viewBox with precise values - when plugin runs - then values are rounded`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0.123456 0.789012 100.456789 200.123456"/>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = CleanupNumericValues.fn(
            root,
            defaultParams,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("0.123456"),
            message = "viewBox values should be rounded, but got: $result",
        )
    }
}
