package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveViewBoxTest {

    @Test
    fun `given SVG with matching viewBox - when removeViewBox runs - then viewBox is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveViewBox)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_VIEWBOX_MATCHING)

        assertFalse(
            actual = result.data.contains("viewBox"),
            message = "Expected viewBox to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("width=\"100\""),
            message = "Expected width to still be present in: ${result.data}",
        )
    }

    @Test
    fun `given SVG with non-matching viewBox - when removeViewBox runs - then viewBox is kept`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveViewBox)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_VIEWBOX_NON_MATCHING)

        assertTrue(
            actual = result.data.contains("viewBox"),
            message = "Expected viewBox to be kept, but not found in: ${result.data}",
        )
    }
}
