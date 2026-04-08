package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveOffCanvasPathsTest {

    @Test
    fun `given path outside viewBox - when removeOffCanvasPaths runs - then path is removed`() =
        runTest {
            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(RemoveOffCanvasPaths)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.REMOVE_OFF_CANVAS_OUTSIDE)

            assertFalse(
                actual = result.data.contains("<path"),
                message = "Expected off-canvas path to be removed, but got: ${result.data}",
            )
            assertTrue(
                actual = result.data.contains("<rect"),
                message = "Expected rect to remain, but got: ${result.data}",
            )
        }

    @Test
    fun `given path inside viewBox - when removeOffCanvasPaths runs - then path is preserved`() =
        runTest {
            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(RemoveOffCanvasPaths)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.REMOVE_OFF_CANVAS_INSIDE)

            assertTrue(
                actual = result.data.contains("<path"),
                message = "Expected in-canvas path to be preserved, but got: ${result.data}",
            )
        }
}
