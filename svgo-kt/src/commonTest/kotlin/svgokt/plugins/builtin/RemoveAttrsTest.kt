package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import svgokt.domain.plugins.PluginConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveAttrsTest {

    @Test
    fun `given SVG with fill attribute - when removeAttrs with fill pattern runs - then fill is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveAttrs)
            }
        }

        val result = optimizer.optimize(
            input = TestFixtures.REMOVE_ATTRS_WITH_FILL,
            config = svgokt.domain.Config(
                js2svg = svgokt.domain.builder.stringifyOptions { pretty = false },
                plugins = listOf(
                    PluginConfig.Custom(
                        name = "removeAttrs",
                        fn = RemoveAttrs.fn ?: error("fn is null"),
                        params = RemoveAttrsParams(
                            attrs = listOf("fill"),
                        ),
                    ),
                ),
            ),
        )

        assertFalse(
            actual = result.data.contains("fill="),
            message = "Expected fill to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("stroke="),
            message = "Expected stroke to still be present in: ${result.data}",
        )
    }

    @Test
    fun `given SVG with fill and stroke - when removeAttrs with both patterns runs - then both are removed`() =
        runTest {
            val optimizer = svgo {
                config {
                    js2svg {
                        pretty = false
                    }
                }
            }

            val result = optimizer.optimize(
                input = TestFixtures.REMOVE_ATTRS_WITH_FILL,
                config = svgokt.domain.Config(
                    js2svg = svgokt.domain.builder.stringifyOptions { pretty = false },
                    plugins = listOf(
                        PluginConfig.Custom(
                            name = "removeAttrs",
                            fn = RemoveAttrs.fn ?: error("fn is null"),
                            params = RemoveAttrsParams(
                                attrs = listOf("(fill|stroke)"),
                            ),
                        ),
                    ),
                ),
            )

            assertFalse(
                actual = result.data.contains("fill="),
                message = "Expected fill to be removed, but found it in: ${result.data}",
            )
            assertFalse(
                actual = result.data.contains("stroke="),
                message = "Expected stroke to be removed, but found it in: ${result.data}",
            )
        }
}
