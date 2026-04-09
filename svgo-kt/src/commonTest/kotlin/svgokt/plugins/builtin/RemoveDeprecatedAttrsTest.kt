package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.domain.plugins.PluginInfo
import svgokt.domain.plugins.PluginParams
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveDeprecatedAttrsTest {

    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    private fun params(vararg pairs: Pair<String, Any>): PluginParams {
        val map = pairs.toMap()
        return object : PluginParams, Map<String, Any> by map {}
    }

    private suspend fun runPlugin(
        svg: String,
        pluginParams: PluginParams = RemoveDeprecatedAttrs.params
            ?: error("Expected non-null default params"),
    ): String {
        val parser = SvgoParser()
        val root = parser.parseSvg(data = svg, from = null)
        val visitor = RemoveDeprecatedAttrs.fn?.invoke(root, pluginParams, pluginInfo)
        visitor?.let { root.visit(it) }
        return stringifySvg(data = root, userOptions = null)
    }

    @Test
    fun `given svg with version attr - when plugin runs - then version is removed`() = runTest {
        // Arrange - version is a safe deprecated attr on svg
        val svg = """<svg version="1.1" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg"><rect/></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        assertFalse(
            actual = result.contains("version"),
            message = "version should be removed as safe deprecated attr. Got: $result",
        )
    }

    @Test
    fun `given element with xml lang and lang - when plugin runs - then xml lang is removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" xml:lang="en" lang="en"><rect/></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        assertFalse(
            actual = result.contains("xml:lang"),
            message = "xml:lang should be removed when lang is also present. Got: $result",
        )
        assertTrue(
            actual = result.contains("lang=\"en\""),
            message = "lang attribute should be preserved. Got: $result",
        )
    }

    @Test
    fun `given element with only xml lang - when plugin runs - then xml lang is preserved`() = runTest {
        // Arrange - xml:lang is unsafe deprecated, not removed by default
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" xml:lang="en"><rect/></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        assertTrue(
            actual = result.contains("xml:lang"),
            message = "xml:lang should be preserved when lang is not present and removeUnsafe is false. Got: $result",
        )
    }

    @Test
    fun `given element with only xml lang - when removeUnsafe true - then xml lang is removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" xml:lang="en"><rect/></svg>"""

        // Act
        val result = runPlugin(svg, params("removeUnsafe" to true))

        // Assert
        assertFalse(
            actual = result.contains("xml:lang"),
            message = "xml:lang should be removed with removeUnsafe=true. Got: $result",
        )
    }

    @Test
    fun `given view with viewTarget - when plugin runs - then viewTarget is preserved`() = runTest {
        // Arrange - viewTarget is unsafe deprecated on view element
        val svg = """<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg"><view id="one" viewBox="0 0 100 100" viewTarget=""/></svg>"""

        // Act
        val result = runPlugin(svg)

        // Assert
        assertTrue(
            actual = result.contains("viewTarget"),
            message = "viewTarget should be preserved without removeUnsafe. Got: $result",
        )
    }

    @Test
    fun `given view with viewTarget - when removeUnsafe true - then viewTarget is removed`() = runTest {
        // Arrange
        val svg = """<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg"><view id="one" viewBox="0 0 100 100" viewTarget=""/></svg>"""

        // Act
        val result = runPlugin(svg, params("removeUnsafe" to true))

        // Assert
        assertFalse(
            actual = result.contains("viewTarget"),
            message = "viewTarget should be removed with removeUnsafe=true. Got: $result",
        )
    }

    @Test
    fun `given svg with enable-background - when removeUnsafe true - then enable-background is removed`() = runTest {
        // Arrange - enable-background is unsafe deprecated in presentation group
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" enable-background="new 0 0 100 100"><rect/></svg>"""

        // Act
        val result = runPlugin(svg, params("removeUnsafe" to true))

        // Assert
        assertFalse(
            actual = result.contains("enable-background"),
            message = "enable-background should be removed with removeUnsafe. Got: $result",
        )
    }
}
