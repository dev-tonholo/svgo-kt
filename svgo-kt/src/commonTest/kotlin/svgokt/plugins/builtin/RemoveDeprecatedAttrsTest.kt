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

class RemoveDeprecatedAttrsTest {

    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    @Test
    fun `given element with xml lang and lang - when plugin runs - then xml lang is removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" xml:lang="en" lang="en"><rect/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveDeprecatedAttrs.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("xml:lang"),
            message = "xml:lang should be removed when lang is also present, but found it in: $result",
        )
        assertTrue(
            actual = result.contains("lang=\"en\""),
            message = "lang attribute should be preserved in: $result",
        )
    }

    @Test
    fun `given element with only xml lang - when plugin runs - then xml lang is preserved`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" xml:lang="en"><rect/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveDeprecatedAttrs.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("xml:lang"),
            message = "xml:lang should be preserved when lang is not present, but was removed from: $result",
        )
    }

    @Test
    fun `given element with xlink type - when plugin runs - then xlink type is removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><a xlink:type="simple" href="#id"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveDeprecatedAttrs.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("xlink:type"),
            message = "xlink:type should be removed, but found it in: $result",
        )
    }

    @Test
    fun `given element with xlink role - when plugin runs - then xlink role is removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><a xlink:role="http://example.com/role" href="#id"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveDeprecatedAttrs.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("xlink:role"),
            message = "xlink:role should be removed, but found it in: $result",
        )
    }

    @Test
    fun `given element with xml space - when plugin runs - then xml space is removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" xml:space="preserve"><rect/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveDeprecatedAttrs.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("xml:space"),
            message = "xml:space should be removed as it is deprecated in SVG2, but found it in: $result",
        )
    }

    @Test
    fun `given element with xlink arcrole and xlink show and xlink actuate - when plugin runs - then all are removed`() = runTest {
        // Arrange
        val parser = SvgoParser()
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><a xlink:arcrole="http://example.com/arc" xlink:show="new" xlink:actuate="onRequest" href="#id"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = RemoveDeprecatedAttrs.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("xlink:arcrole"),
            message = "xlink:arcrole should be removed, but found it in: $result",
        )
        assertFalse(
            actual = result.contains("xlink:show"),
            message = "xlink:show should be removed, but found it in: $result",
        )
        assertFalse(
            actual = result.contains("xlink:actuate"),
            message = "xlink:actuate should be removed, but found it in: $result",
        )
    }
}
