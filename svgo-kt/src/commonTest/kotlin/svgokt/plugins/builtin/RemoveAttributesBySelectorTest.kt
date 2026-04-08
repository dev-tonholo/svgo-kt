package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveAttributesBySelectorTest {

    @Test
    fun `given element matching selector - when removeAttributesBySelector runs - then attribute is removed`() =
        runTest {
            val plugin = RemoveAttributesBySelector
            val customPlugin = object : svgokt.domain.plugins.Plugin<RemoveAttributesBySelector.Params> {
                override val name = plugin.name
                override val description = plugin.description
                override val params = RemoveAttributesBySelector.Params(
                    selector = "[fill='#00ff00']",
                    attributes = listOf("fill"),
                )
                override val fn = plugin.fn
            }

            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(customPlugin)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.REMOVE_ATTRS_BY_SELECTOR)

            assertFalse(
                actual = result.data.contains("""fill="#00ff00""""),
                message = "Expected fill attribute to be removed, but got: ${result.data}",
            )
            assertTrue(
                actual = result.data.contains("""stroke="#00ff00""""),
                message = "Expected stroke attribute to remain, but got: ${result.data}",
            )
        }

    @Test
    fun `given element matching selector - when multiple attributes removed - then all are removed`() =
        runTest {
            val plugin = RemoveAttributesBySelector
            val customPlugin = object : svgokt.domain.plugins.Plugin<RemoveAttributesBySelector.Params> {
                override val name = plugin.name
                override val description = plugin.description
                override val params = RemoveAttributesBySelector.Params(
                    selector = "[fill='#00ff00']",
                    attributes = listOf("fill", "stroke"),
                )
                override val fn = plugin.fn
            }

            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(customPlugin)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.REMOVE_ATTRS_BY_SELECTOR)

            assertFalse(
                actual = result.data.contains("""fill="#00ff00""""),
                message = "Expected fill to be removed, but got: ${result.data}",
            )
            assertFalse(
                actual = result.data.contains("""stroke="#00ff00""""),
                message = "Expected stroke to be removed, but got: ${result.data}",
            )
        }
}
