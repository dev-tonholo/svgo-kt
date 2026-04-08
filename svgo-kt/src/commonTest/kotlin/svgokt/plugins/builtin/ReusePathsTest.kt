package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReusePathsTest {

    @Test
    fun `given duplicate paths - when reusePaths runs - then duplicates get xlink href`() =
        runTest {
            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(ReusePaths)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.REUSE_PATHS_DUPLICATES)

            assertTrue(
                actual = result.data.contains("xlink:href"),
                message = "Expected use references to be created, but got: ${result.data}",
            )
            assertTrue(
                actual = result.data.contains("<defs>"),
                message = "Expected defs element to be created, but got: ${result.data}",
            )
        }

    @Test
    fun `given unique paths - when reusePaths runs - then paths are unchanged`() =
        runTest {
            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(ReusePaths)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.REUSE_PATHS_UNIQUE)

            assertFalse(
                actual = result.data.contains("xlink:href"),
                message = "Expected no use references for unique paths, but got: ${result.data}",
            )
        }
}
