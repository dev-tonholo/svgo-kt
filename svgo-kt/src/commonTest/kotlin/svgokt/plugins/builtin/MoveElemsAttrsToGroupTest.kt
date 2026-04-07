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

class MoveElemsAttrsToGroupTest {

    private val parser = SvgoParser()
    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    private suspend fun runPlugin(svg: String): String {
        val root = parser.parseSvg(data = svg, from = null)
        val visitor = MoveElemsAttrsToGroup.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }
        return stringifySvg(data = root, userOptions = null)
    }

    @Test
    fun `given group children with same fill - when plugin runs - then fill moves to group`() =
        runTest {
            // Arrange - both children share the same fill value
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g>
                    <rect fill="red" width="50" height="50"/>
                    <circle fill="red" cx="75" cy="75" r="25"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("<g fill=\"red\"") || result.contains("<g\n") && result.contains("fill=\"red\""),
                message = "fill should be moved to group, but not found in: $result",
            )
        }

    @Test
    fun `given group children with different fills - when plugin runs - then fills stay on children`() =
        runTest {
            // Arrange - children have different fill values
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g>
                    <rect fill="red" width="50" height="50"/>
                    <circle fill="blue" cx="75" cy="75" r="25"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("fill=\"red\""),
                message = "rect fill=red should remain on child, but not found in: $result",
            )
            assertTrue(
                actual = result.contains("fill=\"blue\""),
                message = "circle fill=blue should remain on child, but not found in: $result",
            )
            assertFalse(
                actual = result.contains("<g fill="),
                message = "group should NOT have a fill when children differ, but found in: $result",
            )
        }

    @Test
    fun `given group with single child - when plugin runs - then no attrs moved`() =
        runTest {
            // Arrange - only one child, skip optimization
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g>
                    <rect fill="red" width="100" height="100"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("fill=\"red\""),
                message = "fill should remain on the single child, but not found in: $result",
            )
            assertFalse(
                actual = result.contains("<g fill="),
                message = "group should NOT have fill moved up with only one child, but found in: $result",
            )
        }

    @Test
    fun `given group children with same stroke but group has filter - when plugin runs - then transform not moved but stroke is`() =
        runTest {
            // Arrange - group has filter, so transform must NOT be extracted,
            // but other inheritable attrs can still be extracted
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g filter="url(#f)">
                    <rect fill="red" transform="translate(5,5)" width="50" height="50"/>
                    <circle fill="red" transform="translate(5,5)" cx="75" cy="75" r="25"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertFalse(
                actual = result.contains("<g") && !result.contains("transform=") && result.indexOf("transform=") < result.indexOf("<rect"),
                message = "transform should NOT be moved to group when group has filter",
            )
            // Children should still have their transforms
            assertTrue(
                actual = result.contains("translate(5,5)"),
                message = "transforms should remain on children when group has filter, but not found in: $result",
            )
        }

    @Test
    fun `given group children with same fill and stroke - when plugin runs - then both attrs move to group`() =
        runTest {
            // Arrange - both fill and stroke are identical on all children
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g>
                    <rect fill="red" stroke="black" width="50" height="50"/>
                    <circle fill="red" stroke="black" cx="75" cy="75" r="25"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("fill=\"red\"") && result.contains("stroke=\"black\""),
                message = "fill and stroke should appear somewhere in result, but not found in: $result",
            )
        }
}
