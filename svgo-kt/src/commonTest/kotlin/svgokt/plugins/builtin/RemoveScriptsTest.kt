package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveScriptsTest {

    @Test
    fun `given SVG with script and event handlers - when removeScripts runs - then both are removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveScripts)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_SCRIPTS_WITH_SCRIPT)

        assertFalse(
            actual = result.data.contains("<script"),
            message = "Expected <script> to be removed, but found it in: ${result.data}",
        )
        assertFalse(
            actual = result.data.contains("onclick"),
            message = "Expected onclick to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("<rect"),
            message = "Expected <rect> to still be present in: ${result.data}",
        )
    }
}
