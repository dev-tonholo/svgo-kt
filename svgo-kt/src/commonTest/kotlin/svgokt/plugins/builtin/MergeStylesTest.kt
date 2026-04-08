package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MergeStylesTest {

    @Test
    fun `given SVG with two style elements - when mergeStyles runs - then styles are merged into one`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
            }
        }

        val result = optimizer.optimize(input = TestFixtures.MERGE_STYLES_INPUT)

        val styleCount = Regex("<style").findAll(result.data).count()
        assertTrue(
            actual = styleCount <= 1,
            message = "Expected at most 1 <style> element, but found $styleCount in: ${result.data}",
        )
    }

    @Test
    fun `given SVG with no style elements - when mergeStyles runs - then SVG is unchanged structurally`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
            }
        }

        val result = optimizer.optimize(input = TestFixtures.MERGE_STYLES_NO_STYLES)

        assertTrue(
            actual = result.data.contains("class=\"st0\""),
            message = "Expected element with class st0 to still be present in: ${result.data}",
        )
        assertFalse(
            actual = result.data.contains("<style"),
            message = "Expected no <style> element to be added, but found one in: ${result.data}",
        )
    }

    @Test
    fun `given SVG with style in foreignObject - when mergeStyles runs - then foreignObject style is untouched`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
            }
        }

        val result = optimizer.optimize(input = TestFixtures.MERGE_STYLES_FOREIGN_OBJECT)

        assertTrue(
            actual = result.data.contains("foreignObject"),
            message = "Expected <foreignObject> to still be present in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains(".foreign{color:blue}"),
            message = "Expected inner foreignObject style '.foreign{color:blue}' to remain unmerged in: ${result.data}",
        )
    }
}
