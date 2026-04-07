package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveDescTest {

    @Test
    fun `given SVG with generator desc - when removeDesc runs - then desc is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveDesc)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_DESC_GENERATOR)

        assertFalse(
            actual = result.data.contains("<desc"),
            message = "Expected generator <desc> to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("<svg"),
            message = "Expected <svg> to still be present in: ${result.data}",
        )
    }

    @Test
    fun `given SVG with empty desc - when removeDesc runs - then desc is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveDesc)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_DESC_EMPTY)

        assertFalse(
            actual = result.data.contains("<desc"),
            message = "Expected empty <desc> to be removed, but found it in: ${result.data}",
        )
    }

    @Test
    fun `given SVG with accessibility desc - when removeDesc runs - then desc is preserved`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveDesc)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_DESC_ACCESSIBILITY)

        assertTrue(
            actual = result.data.contains("<desc"),
            message = "Expected accessibility <desc> to be preserved, but it was removed in: ${result.data}",
        )
    }
}
