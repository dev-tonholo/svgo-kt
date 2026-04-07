package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveUnusedNSTest {

    @Test
    fun `given SVG with unused namespace - when removeUnusedNS runs - then unused xmlns is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveUnusedNS)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_UNUSED_NS_UNUSED)

        assertFalse(
            actual = result.data.contains("xmlns:xlink"),
            message = "Expected unused 'xmlns:xlink' to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("xmlns="),
            message = "Expected base 'xmlns' to be preserved in: ${result.data}",
        )
    }

    @Test
    fun `given SVG with used namespace - when removeUnusedNS runs - then used xmlns is preserved`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveUnusedNS)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_UNUSED_NS_USED)

        assertTrue(
            actual = result.data.contains("xmlns:custom"),
            message = "Expected used 'xmlns:custom' to be preserved in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("custom:shape"),
            message = "Expected 'custom:shape' element to still be present in: ${result.data}",
        )
    }
}
