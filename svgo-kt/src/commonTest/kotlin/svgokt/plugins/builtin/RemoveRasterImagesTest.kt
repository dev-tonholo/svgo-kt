package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveRasterImagesTest {

    @Test
    fun `given SVG with raster image - when removeRasterImages runs - then image is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveRasterImages)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_RASTER_IMAGES_WITH_PNG)

        assertFalse(
            actual = result.data.contains("<image"),
            message = "Expected <image> to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("<rect"),
            message = "Expected <rect> to still be present in: ${result.data}",
        )
    }

    @Test
    fun `given SVG with vector image reference - when removeRasterImages runs - then image is kept`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveRasterImages)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_RASTER_IMAGES_SVG_ONLY)

        assertTrue(
            actual = result.data.contains("<image"),
            message = "Expected <image> to be kept, but not found in: ${result.data}",
        )
    }
}
