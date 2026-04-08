package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConvertOneStopGradientsTest {

    @Test
    fun `given single stop gradient - when convertOneStopGradients runs - then gradient replaced with color`() =
        runTest {
            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(ConvertOneStopGradients)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.ONE_STOP_GRADIENT)

            assertTrue(
                actual = result.data.contains("""fill="red""""),
                message = "Expected fill to be replaced with stop color, but got: ${result.data}",
            )
            assertFalse(
                actual = result.data.contains("linearGradient"),
                message = "Expected gradient to be removed, but got: ${result.data}",
            )
        }

    @Test
    fun `given two stop gradient - when convertOneStopGradients runs - then gradient is preserved`() =
        runTest {
            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(ConvertOneStopGradients)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.TWO_STOP_GRADIENT)

            assertTrue(
                actual = result.data.contains("linearGradient"),
                message = "Expected two-stop gradient to be preserved, but got: ${result.data}",
            )
            assertTrue(
                actual = result.data.contains("url(#g1)"),
                message = "Expected url reference to remain, but got: ${result.data}",
            )
        }
}
