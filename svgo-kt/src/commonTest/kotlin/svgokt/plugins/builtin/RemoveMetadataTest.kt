package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveMetadataTest {

    @Test
    fun `given SVG with metadata element - when removeMetadata runs - then metadata is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveMetadata)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_METADATA_WITH_METADATA)

        assertFalse(
            actual = result.data.contains("<metadata"),
            message = "Expected <metadata> to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("<svg"),
            message = "Expected <svg> to still be present in: ${result.data}",
        )
    }

    @Test
    fun `given SVG without metadata element - when removeMetadata runs - then SVG is unchanged structurally`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveMetadata)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_METADATA_WITHOUT_METADATA)

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
