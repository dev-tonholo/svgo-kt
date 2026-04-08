package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveTitleTest {

    @Test
    fun `given SVG with title element - when removeTitle runs - then title is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveTitle)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_TITLE_WITH_TITLE)

        assertFalse(
            actual = result.data.contains("<title"),
            message = "Expected <title> to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("<rect"),
            message = "Expected <rect> to still be present in: ${result.data}",
        )
    }

    @Test
    fun `given SVG without title element - when removeTitle runs - then SVG is unchanged`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveTitle)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_TITLE_WITHOUT_TITLE)

        assertTrue(
            actual = result.data.contains("<svg"),
            message = "Expected <svg> to still be present in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("<rect"),
            message = "Expected <rect> to still be present in: ${result.data}",
        )
    }
}
