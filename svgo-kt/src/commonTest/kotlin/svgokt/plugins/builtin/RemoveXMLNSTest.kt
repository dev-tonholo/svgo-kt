package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveXMLNSTest {

    @Test
    fun `given SVG with xmlns - when removeXMLNS runs - then xmlns is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveXMLNS)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_XMLNS_WITH_XMLNS)

        assertFalse(
            actual = result.data.contains("xmlns="),
            message = "Expected xmlns to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("<svg"),
            message = "Expected <svg> to still be present in: ${result.data}",
        )
    }
}
