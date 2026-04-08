package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertTrue

class AddClassesToSVGElementTest {

    @Test
    fun `given SVG without class - when addClassesToSVGElement runs with classNames - then classes are added`() =
        runTest {
            val plugin = AddClassesToSVGElement
            val customPlugin = object : svgokt.domain.plugins.Plugin<AddClassesToSVGElement.Params> {
                override val name = plugin.name
                override val description = plugin.description
                override val params = AddClassesToSVGElement.Params(
                    classNames = listOf("mySvg", "size-big"),
                )
                override val fn = plugin.fn
            }

            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(customPlugin)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.ADD_CLASSES_SIMPLE)

            assertTrue(
                actual = result.data.contains("""class="mySvg size-big""""),
                message = "Expected class attribute with both classes, but got: ${result.data}",
            )
        }

    @Test
    fun `given SVG with existing class - when addClassesToSVGElement runs - then new classes are appended`() =
        runTest {
            val plugin = AddClassesToSVGElement
            val customPlugin = object : svgokt.domain.plugins.Plugin<AddClassesToSVGElement.Params> {
                override val name = plugin.name
                override val description = plugin.description
                override val params = AddClassesToSVGElement.Params(
                    className = "newClass",
                )
                override val fn = plugin.fn
            }

            val optimizer = svgo {
                config {
                    js2svg { pretty = false }
                    plugin(customPlugin)
                }
            }

            val result = optimizer.optimize(input = TestFixtures.ADD_CLASSES_EXISTING)

            assertTrue(
                actual = result.data.contains("existing"),
                message = "Expected existing class to be preserved, but got: ${result.data}",
            )
            assertTrue(
                actual = result.data.contains("newClass"),
                message = "Expected newClass to be added, but got: ${result.data}",
            )
        }
}
