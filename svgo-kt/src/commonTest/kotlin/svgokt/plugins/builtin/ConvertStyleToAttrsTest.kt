package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConvertStyleToAttrsTest {

    @Test
    fun `given element with style - when convertStyleToAttrs runs - then style becomes attributes`() =
        runTest {
            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(ConvertStyleToAttrs)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.CONVERT_STYLE_TO_ATTRS)

            assertTrue(
                actual = result.data.contains("""fill="#000""""),
                message = "Expected fill attribute to be set, but got: ${result.data}",
            )
            assertTrue(
                actual = result.data.contains("""stroke="blue""""),
                message = "Expected stroke attribute to be set, but got: ${result.data}",
            )
            assertFalse(
                actual = result.data.contains("style="),
                message = "Expected style attribute to be removed, but got: ${result.data}",
            )
        }

    @Test
    fun `given element with style and existing attr - when convertStyleToAttrs runs - then existing attr not overridden`() =
        runTest {
            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(ConvertStyleToAttrs)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.CONVERT_STYLE_TO_ATTRS_NO_OVERRIDE)

            assertTrue(
                actual = result.data.contains("""fill="green""""),
                message = "Expected existing fill=green to be preserved, but got: ${result.data}",
            )
            assertTrue(
                actual = result.data.contains("""stroke="blue""""),
                message = "Expected stroke=blue from style, but got: ${result.data}",
            )
        }
}
