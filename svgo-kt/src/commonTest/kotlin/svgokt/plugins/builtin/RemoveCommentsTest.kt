package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveCommentsTest {

    @Test
    fun `given SVG with comments - when removeComments runs - then all comments are removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveComments)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_COMMENTS_WITH_COMMENTS)

        assertFalse(
            actual = result.data.contains("<!--"),
            message = "Expected all comments to be removed, but found one in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("<svg"),
            message = "Expected <svg> to still be present in: ${result.data}",
        )
    }

    @Test
    fun `given SVG without comments - when removeComments runs - then SVG is unchanged structurally`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveComments)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_COMMENTS_WITHOUT_COMMENTS)

        assertTrue(
            actual = result.data.contains("<svg"),
            message = "Expected <svg> to still be present in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("rect"),
            message = "Expected <rect> to still be present in: ${result.data}",
        )
    }
}
