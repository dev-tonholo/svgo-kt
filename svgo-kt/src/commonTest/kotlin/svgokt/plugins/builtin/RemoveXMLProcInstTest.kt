package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.TestFixtures
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveXMLProcInstTest {

    @Test
    fun `given SVG with xml processing instruction - when removeXMLProcInst runs - then xml declaration is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveXMLProcInst)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_XML_PROC_INST_WITH_XML_DECL)

        assertFalse(
            actual = result.data.contains("<?xml"),
            message = "Expected <?xml ...?> to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("<svg"),
            message = "Expected <svg> to still be present in: ${result.data}",
        )
    }

    @Test
    fun `given SVG with xml and custom processing instructions - when removeXMLProcInst runs - then only xml declaration is removed`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = false
                }
                plugin(RemoveXMLProcInst)
            }
        }

        val result = optimizer.optimize(input = TestFixtures.REMOVE_XML_PROC_INST_WITH_CUSTOM_PI)

        assertFalse(
            actual = result.data.contains("<?xml"),
            message = "Expected <?xml ...?> to be removed, but found it in: ${result.data}",
        )
        assertTrue(
            actual = result.data.contains("<?custom-pi"),
            message = "Expected custom processing instruction to be preserved in: ${result.data}",
        )
    }
}
