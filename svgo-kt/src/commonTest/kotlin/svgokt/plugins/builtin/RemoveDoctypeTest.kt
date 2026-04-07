package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveDoctypeTest {

    @Test
    fun `given SVG with doctype - when removeDoctype runs - then doctype is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveDoctype)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_DOCTYPE_WITH_DOCTYPE)

        assertFalse(
            actual = result.data.contains("<!DOCTYPE"),
            message = "Expected DOCTYPE to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("<svg"),
            message = "Expected <svg> to still be present in: ${result.data}",
        )
    }

    @Test
    fun `given SVG without doctype - when removeDoctype runs - then SVG is unchanged structurally`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveDoctype)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_DOCTYPE_WITHOUT_DOCTYPE)

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
