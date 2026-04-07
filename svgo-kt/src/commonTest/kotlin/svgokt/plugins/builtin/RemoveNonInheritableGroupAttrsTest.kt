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

class RemoveNonInheritableGroupAttrsTest {

    private val parser = SvgoParser()
    private val pluginInfo = PluginInfo(path = null, multipassCount = 0)

    private suspend fun runPlugin(svg: String): String {
        val root = parser.parseSvg(data = svg, from = null)
        val visitor = RemoveNonInheritableGroupAttrs.fn(root, NoPluginParam, pluginInfo)
        visitor?.let { root.visit(it) }
        return stringifySvg(data = root, userOptions = null)
    }

    @Test
    fun `given g element with non-inheritable non-allowed attr - when removeNonInheritableGroupAttrs runs - then attr is removed`() =
        runTest {
            // Arrange - alignment-baseline is a presentation attr, not inheritable, not in the allow-list
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g alignment-baseline="middle">
                    <rect width="100" height="100"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertFalse(
                actual = result.contains("alignment-baseline"),
                message = "alignment-baseline should be removed from g, but found in: $result",
            )
        }

    @Test
    fun `given g element with inheritable attr - when removeNonInheritableGroupAttrs runs - then attr is preserved`() =
        runTest {
            // Arrange - fill is inheritable, should be kept
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
                message = "Inheritable fill attribute should be preserved on g, but not found in: $result",
            )
        }

    @Test
    fun `given g element with allowed non-inheritable attr transform - when removeNonInheritableGroupAttrs runs - then attr is preserved`() =
        runTest {
            // Arrange - transform is in presentationNonInheritableGroupAttrs allow-list
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g transform="translate(10,20)">
                    <rect width="100" height="100"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("transform=\"translate(10,20)\""),
                message = "transform should be preserved on g (in allow-list), but not found in: $result",
            )
        }

    @Test
    fun `given g element with allowed non-inheritable attr opacity - when removeNonInheritableGroupAttrs runs - then attr is preserved`() =
        runTest {
            // Arrange - opacity is in presentationNonInheritableGroupAttrs allow-list
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g opacity="0.5">
                    <rect width="100" height="100"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("opacity=\"0.5\""),
                message = "opacity should be preserved on g (in allow-list), but not found in: $result",
            )
        }

    @Test
    fun `given non-g element with non-inheritable attr - when removeNonInheritableGroupAttrs runs - then attr is preserved`() =
        runTest {
            // Arrange - alignment-baseline on a rect should NOT be touched
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <rect alignment-baseline="middle" width="100" height="100"/>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("alignment-baseline=\"middle\""),
                message = "alignment-baseline on rect should NOT be removed, but not found in: $result",
            )
        }

    @Test
    fun `given g element with non-presentation attr - when removeNonInheritableGroupAttrs runs - then attr is preserved`() =
        runTest {
            // Arrange - id is not a presentation attr at all
            val svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <g id="my-group">
                    <rect width="100" height="100"/>
                  </g>
                </svg>
            """.trimIndent()

            // Act
            val result = runPlugin(svg)

            // Assert
            assertTrue(
                actual = result.contains("id=\"my-group\""),
                message = "id attribute should be preserved on g, but not found in: $result",
            )
        }
}
