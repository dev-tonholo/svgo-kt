package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertTrue

class CleanupListOfValuesTest {

    @Test
    fun `given viewBox with long decimals - when cleanupListOfValues runs - then values are rounded`() =
        runTest {
            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(CleanupListOfValues)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.CLEANUP_LIST_VALUES_VIEWBOX)

            assertTrue(
                actual = result.data.contains("200.284"),
                message = "Expected viewBox value to be rounded to 3 decimal places, but got: ${result.data}",
            )
        }

    @Test
    fun `given polygon with long decimal points - when cleanupListOfValues runs - then points are rounded`() =
        runTest {
            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(CleanupListOfValues)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.CLEANUP_LIST_VALUES_POINTS)

            assertTrue(
                actual = result.data.contains("208.251"),
                message = "Expected points to be rounded, but got: ${result.data}",
            )
            assertTrue(
                actual = result.data.contains("77.131"),
                message = "Expected points to be rounded, but got: ${result.data}",
            )
        }
}
