package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveStyleElementTest {

    @Test
    fun `given SVG with style element - when removeStyleElement runs - then style is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveStyleElement)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_STYLE_ELEMENT_WITH_STYLE)

        assertFalse(
            actual = result.data.contains("<style"),
            message = "Expected <style> to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("<rect"),
            message = "Expected <rect> to still be present in: ${result.data}",
        )
    }
}
