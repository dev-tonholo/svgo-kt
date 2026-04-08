package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertTrue

class PrefixIdsTest {

    @Test
    fun `given SVG with ids - when prefixIds runs with custom prefix - then ids are prefixed`() =
        runTest {
            val plugin = PrefixIds
            val customPlugin = object : svgokt.domain.plugins.Plugin<PrefixIds.Params> {
                override val name = plugin.name
                override val description = plugin.description
                override val params = PrefixIds.Params(prefix = "test")
                override val fn = plugin.fn
            }

            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(customPlugin)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.PREFIX_IDS_SIMPLE)

            assertTrue(
                actual = result.data.contains("""id="test__grad1""""),
                message = "Expected gradient id to be prefixed, but got: ${result.data}",
            )
            assertTrue(
                actual = result.data.contains("""id="test__myRect""""),
                message = "Expected rect id to be prefixed, but got: ${result.data}",
            )
        }

    @Test
    fun `given SVG with url references - when prefixIds runs - then references are updated`() =
        runTest {
            val plugin = PrefixIds
            val customPlugin = object : svgokt.domain.plugins.Plugin<PrefixIds.Params> {
                override val name = plugin.name
                override val description = plugin.description
                override val params = PrefixIds.Params(prefix = "pfx")
                override val fn = plugin.fn
            }

            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(customPlugin)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.PREFIX_IDS_SIMPLE)

            assertTrue(
                actual = result.data.contains("url(#pfx__grad1)"),
                message = "Expected url reference to be prefixed, but got: ${result.data}",
            )
        }
}
