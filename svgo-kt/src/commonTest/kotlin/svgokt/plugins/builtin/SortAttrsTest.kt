package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertTrue

class SortAttrsTest {

    @Test
    fun `given element with unordered attrs - when sortAttrs runs - then attrs are reordered`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(SortAttrs)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.SORT_ATTRS_UNORDERED)

        // id should come before fill, width, height in the output
        val idPos = result.data.indexOf("id=")
        val fillPos = result.data.indexOf("fill=")
        val widthPos = result.data.indexOf("width=")
        val heightPos = result.data.indexOf("height=")

        assertTrue(
            actual = idPos < widthPos,
            message = "Expected 'id' to appear before 'width', but got: ${result.data}",
        )
        assertTrue(
            actual = widthPos < heightPos,
            message = "Expected 'width' to appear before 'height', but got: ${result.data}",
        )
        assertTrue(
            actual = heightPos < fillPos || widthPos < fillPos,
            message = "Expected priority attributes before 'fill', but got: ${result.data}",
        )
    }

    @Test
    fun `given element with xmlns attrs - when sortAttrs runs - then xmlns comes first`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(SortAttrs)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.SORT_ATTRS_WITH_XMLNS)

        val xmlnsPos = result.data.indexOf("xmlns=")
        val xmlnsXlinkPos = result.data.indexOf("xmlns:xlink=")
        val viewBoxPos = result.data.indexOf("viewBox=")

        assertTrue(
            actual = xmlnsPos < viewBoxPos,
            message = "Expected 'xmlns' before 'viewBox', but got: ${result.data}",
        )
        assertTrue(
            actual = xmlnsPos < xmlnsXlinkPos,
            message = "Expected 'xmlns' before 'xmlns:xlink', but got: ${result.data}",
        )
        assertTrue(
            actual = xmlnsXlinkPos < viewBoxPos,
            message = "Expected 'xmlns:xlink' before 'viewBox', but got: ${result.data}",
        )
    }
}
