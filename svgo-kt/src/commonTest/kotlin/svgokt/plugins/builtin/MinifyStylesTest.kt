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

class MinifyStylesTest {

    private val parser = SvgoParser()

    @Test
    fun `given SVG with verbose CSS - when minifyStyles runs - then CSS is minified`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><style>
            .cls1 {
                fill: red;
                stroke: blue;
            }
            .cls2 {
                opacity: 0.5;
            }
        </style><rect class="cls1" width="100" height="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = MinifyStyles.fn(
            root,
            NoPluginParam,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("\n"),
            message = "CSS should not contain newlines after minification, but found in: $result",
        )
        assertTrue(
            actual = result.contains(".cls1{fill:red;stroke:blue}"),
            message = "CSS should be minified (no trailing semicolons, no extra spaces), but found: $result",
        )
    }

    @Test
    fun `given SVG with CSS comments - when minifyStyles runs - then comments are removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><style>
            /* This is a comment */
            .cls1 { fill: red; }
        </style><rect class="cls1" width="100" height="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = MinifyStyles.fn(
            root,
            NoPluginParam,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("comment"),
            message = "CSS comments should be removed, but found in: $result",
        )
        assertTrue(
            actual = result.contains(".cls1{fill:red}"),
            message = "CSS rules should still be present after removing comments, but not found in: $result",
        )
    }

    @Test
    fun `given SVG with empty style after minification - when minifyStyles runs - then style element is removed`() = runTest {
        // Arrange
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><style>
            /* only comments here */
        </style><rect width="100" height="100"/></svg>"""
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val visitor = MinifyStyles.fn(
            root,
            NoPluginParam,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertFalse(
            actual = result.contains("<style"),
            message = "Empty style element should be removed, but found in: $result",
        )
    }
}
