package svgokt.parser

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.XastCdata
import svgokt.domain.XastChild
import svgokt.domain.XastComment
import svgokt.domain.XastElement
import svgokt.domain.XastInstruction
import svgokt.domain.XastRoot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SvgoParserTest {

    @Test
    fun `given valid SVG - when parseSvg is called - then returns XastRoot with children`() = runTest {
        val parser = SvgoParser()
        val result: XastRoot = parser.parseSvg(data = TestFixtures.PARSER_TEST_SVG, from = null)

        assertTrue(result.children.isNotEmpty(), "XastRoot should have children")
    }

    @Test
    fun `given SVG with xml processing instruction - when parseSvg is called - then first child is XastInstruction`() = runTest {
        val parser = SvgoParser()
        val result: XastRoot = parser.parseSvg(data = TestFixtures.PARSER_TEST_SVG, from = null)

        val firstChild = result.children.firstOrNull()
        assertNotNull(firstChild, "XastRoot should have at least one child")
        assertTrue(firstChild is XastInstruction, "First child should be XastInstruction, was ${firstChild::class.simpleName}")
        assertEquals(expected = "xml", actual = firstChild.name)
    }

    @Test
    fun `given SVG with comment - when parseSvg is called - then comment node is present`() = runTest {
        val parser = SvgoParser()
        val result: XastRoot = parser.parseSvg(data = TestFixtures.PARSER_TEST_SVG, from = null)

        val commentNode = result.children.filterIsInstance<XastComment>().firstOrNull()
        assertNotNull(commentNode, "A comment node should be present in children")
        assertTrue(commentNode.value.contains("Generator"), "Comment value should contain expected text")
    }

    @Test
    fun `given SVG with elements - when parseSvg is called - then svg element has correct attributes`() = runTest {
        val parser = SvgoParser()
        val result: XastRoot = parser.parseSvg(data = TestFixtures.PARSER_TEST_SVG, from = null)

        val svgElement = result.children.filterIsInstance<XastElement>().firstOrNull { it.name == "svg" }
        assertNotNull(svgElement, "An svg element should be present")
        assertEquals(expected = "1.1", actual = svgElement.attributes["version"])
        assertEquals(expected = "120px", actual = svgElement.attributes["width"])
        assertEquals(expected = "120px", actual = svgElement.attributes["height"])
    }

    @Test
    fun `given SVG with CDATA in style - when parseSvg is called - then CDATA content is preserved`() = runTest {
        val parser = SvgoParser()
        val result: XastRoot = parser.parseSvg(data = TestFixtures.PARSER_TEST_SVG, from = null)

        val svgElement = result.children.filterIsInstance<XastElement>().firstOrNull { it.name == "svg" }
        assertNotNull(svgElement, "An svg element should be present")

        val styleElement = svgElement.children.filterIsInstance<XastElement>().firstOrNull { it.name == "style" }
        assertNotNull(styleElement, "A style element should be present inside svg")

        val cdataNode = styleElement.children.filterIsInstance<XastCdata>().firstOrNull()
        assertNotNull(cdataNode, "A CDATA node should be present inside style element")
        assertTrue(cdataNode.value.contains("fill"), "CDATA content should contain style content")
    }

    @Test
    fun `given SVG with text element - when parseSvg is called - then text whitespace is preserved`() = runTest {
        val parser = SvgoParser()
        val result: XastRoot = parser.parseSvg(data = TestFixtures.PARSER_TEST_SVG, from = null)

        val svgElement = result.children.filterIsInstance<XastElement>().firstOrNull { it.name == "svg" }
        assertNotNull(svgElement, "An svg element should be present")

        val textElement = findElement(root = svgElement, name = "text")
        assertNotNull(textElement, "A text element should be found in the SVG tree")
        assertTrue(textElement.children.isNotEmpty(), "Text element should have children")
    }

    @Test
    fun `given simple SVG - when parseSvg is called - then nested elements are parsed`() = runTest {
        val parser = SvgoParser()
        val result: XastRoot = parser.parseSvg(data = TestFixtures.SIMPLE_SVG, from = null)

        val svgElement = result.children.filterIsInstance<XastElement>().firstOrNull { it.name == "svg" }
        assertNotNull(svgElement, "An svg element should be present")
        assertEquals(
            expected = "http://www.w3.org/2000/svg",
            actual = svgElement.attributes["xmlns"],
        )

        val rectElement = svgElement.children.filterIsInstance<XastElement>().firstOrNull { it.name == "rect" }
        assertNotNull(rectElement, "A rect element should be present inside svg")
        assertEquals(expected = "100", actual = rectElement.attributes["width"])
        assertEquals(expected = "100", actual = rectElement.attributes["height"])
        assertEquals(expected = "red", actual = rectElement.attributes["fill"])
    }
}

private fun findElement(root: XastElement, name: String): XastElement? {
    for (child in root.children) {
        if (child is XastElement) {
            if (child.name == name) return child
            val found = findElement(root = child, name = name)
            if (found != null) return found
        }
    }
    return null
}
