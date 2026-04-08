package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.Config
import svgokt.domain.builder.stringifyOptions
import svgokt.domain.builder.svgo
import svgokt.domain.plugins.PluginConfig
import kotlin.test.Test
import kotlin.test.assertTrue

class AddAttributesToSVGElementTest {

    @Test
    fun `given SVG - when addAttributesToSVGElement with map attrs runs - then attributes are added`() = runTest {
        val pluginFn = AddAttributesToSVGElement.fn ?: error("fn is null")
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
            }
        }

        val result = optimizer.optimize(
            input = TestFixtures.ADD_ATTRS_SVG,
            config = Config(
                js2svg = stringifyOptions { pretty = false },
                plugins = listOf(
                    PluginConfig.Custom(
                        name = "addAttributesToSVGElement",
                        fn = pluginFn,
                        params = AddAttributesToSVGElementParams(
                            attributes = listOf(
                                mapOf("focusable" to "false", "data-image" to "icon"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertTrue(
            actual = result.data.contains("focusable=\"false\""),
            message = "Expected focusable attribute to be added, but not found in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("data-image=\"icon\""),
            message = "Expected data-image attribute to be added, but not found in: ${result.data}",
        )
    }

    @Test
    fun `given SVG with existing attribute - when addAttributesToSVGElement runs - then existing is not overwritten`() =
        runTest {
            val pluginFn = AddAttributesToSVGElement.fn ?: error("fn is null")
            val optimizer = svgo {
                config {
                    js2svg {
                        pretty = false
                    }
                }
            }

            val result = optimizer.optimize(
                input = TestFixtures.ADD_ATTRS_SVG,
                config = Config(
                    js2svg = stringifyOptions { pretty = false },
                    plugins = listOf(
                        PluginConfig.Custom(
                            name = "addAttributesToSVGElement",
                            fn = pluginFn,
                            params = AddAttributesToSVGElementParams(
                                attributes = listOf(
                                    mapOf("viewBox" to "0 0 200 200"),
                                ),
                            ),
                        ),
                    ),
                ),
            )

            assertTrue(
                actual = result.data.contains("viewBox=\"0 0 100 100\""),
                message = "Expected existing viewBox to be kept, but not found in: ${result.data}",
            )
        }
}
