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

class MoveGroupAttrsToElemsTest {

    private val parser = SvgoParser()
    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    private suspend fun runPlugin(svg: String): String {
        val root = parser.parseSvg(data = svg, from = null)
        val visitor = MoveGroupAttrsToElems.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }
        return stringifySvg(data = root, userOptions = null)
    }

    @Test
    fun `given group with transform and safe children - when plugin runs - then transform moves to children`() =
        runTest {
            // Arrange
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g transform="translate(10,20)">
                    <rect width="100" height="100"/>
                    <circle cx="50" cy="50" r="30"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertFalse(
                actual = result.contains("<g transform="),
                message = "transform should be removed from group, but found in: $result",
            )
            assertTrue(
                actual = result.contains("rect") && result.contains("translate(10,20)"),
                message = "transform should be on child elements, but not found in: $result",
            )
        }

    @Test
    fun `given group with url reference attr - when plugin runs - then transform stays on group`() =
        runTest {
            // Arrange - fill uses a url() reference
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <defs>
                    <linearGradient id="grad"/>
                  </defs>
                  <g transform="translate(10,20)" fill="url(#grad)">
                    <rect width="100" height="100"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("<g") && result.contains("transform=\"translate(10,20)\""),
                message = "transform should stay on group when url reference exists, but not found in: $result",
            )
        }

    @Test
    fun `given group child with id - when plugin runs - then transform stays on group`() =
        runTest {
            // Arrange - child has an id attribute
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g transform="translate(10,20)">
                    <rect id="my-rect" width="100" height="100"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("transform=\"translate(10,20)\""),
                message = "transform should stay on group when a child has id, but not found in: $result",
            )
            // Verify the rect element tag does not contain a transform attribute
            assertFalse(
                actual = Regex("<rect[^>]*transform=").containsMatchIn(result),
                message = "transform should NOT appear inside <rect when child has id, but found in: $result",
            )
        }

    @Test
    fun `given group with transform and child having existing transform - when plugin runs - then transforms are concatenated`() =
        runTest {
            // Arrange - child already has a transform
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g transform="translate(10,20)">
                    <rect transform="rotate(45)" width="100" height="100"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("translate(10,20) rotate(45)"),
                message = "group transform should be prepended to child transform, but not found in: $result",
            )
            assertFalse(
                actual = result.contains("<g transform="),
                message = "transform should be removed from group, but found in: $result",
            )
        }

    @Test
    fun `given group without transform - when plugin runs - then nothing changes`() =
        runTest {
            // Arrange - no transform on group
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g fill="red">
                    <rect width="100" height="100"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("fill=\"red\""),
                message = "group fill should remain unchanged, but not found in: $result",
            )
            assertFalse(
                actual = result.contains("rect") && result.contains("transform="),
                message = "No transform should appear on rect, but found in: $result",
            )
        }
}
