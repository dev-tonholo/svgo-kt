package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertTrue

class SortDefsChildrenTest {

    @Test
    fun `given defs with mixed children - when sortDefsChildren runs - then children are sorted by frequency`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(SortDefsChildren)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.SORT_DEFS_MIXED_CHILDREN)

        // linearGradient appears twice, so it should come before filter and clipPath
        val firstLinearGradientPos = result.data.indexOf("linearGradient")
        val filterPos = result.data.indexOf("filter")
        val clipPathPos = result.data.indexOf("clipPath")

        assertTrue(
            actual = firstLinearGradientPos < filterPos,
            message = "Expected 'linearGradient' (higher frequency) before 'filter', but got: ${result.data}",
        )
        assertTrue(
            actual = firstLinearGradientPos < clipPathPos,
            message = "Expected 'linearGradient' (higher frequency) before 'clipPath', but got: ${result.data}",
        )
    }

    @Test
    fun `given defs with single type children - when sortDefsChildren runs - then SVG structure is preserved`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(SortDefsChildren)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.SORT_DEFS_SINGLE_TYPE)

        val clipPathCount = Regex("clipPath").findAll(result.data).count()
        assertTrue(
            actual = clipPathCount == 2,
            message = "Expected 2 clipPath elements to be preserved, but found $clipPathCount in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("defs"),
            message = "Expected <defs> to still be present in: ${result.data}",
        )
    }
}
