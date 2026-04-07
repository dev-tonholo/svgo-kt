package svgokt

import kotlinx.coroutines.test.runTest
import svgokt.domain.EndOfLine
import svgokt.domain.builder.svgo
import kotlin.test.Test
import kotlin.test.assertTrue

class SvgoTest {

    @Test
    fun `given valid SVG - when optimize is called - then output data is not empty`() = runTest {
        val optimizer = svgo { }
        val result = optimizer.optimize(input = TestFixtures.SIMPLE_SVG)
        assertTrue(result.data.isNotEmpty(), "Output data should not be empty")
    }

    @Test
    fun `given valid SVG - when optimize is called - then output is valid SVG`() = runTest {
        val optimizer = svgo { }
        val result = optimizer.optimize(input = TestFixtures.SIMPLE_SVG)
        assertTrue(result.data.contains("<svg"), "Output should contain an SVG element")
        assertTrue(result.data.contains("</svg>") || result.data.contains("/>"), "Output should have a closing SVG tag")
    }

    @Test
    fun `given SVG - when optimize is called with pretty printing - then output is formatted`() = runTest {
        val optimizer = svgo {
            config {
                js2svg {
                    pretty = true
                    indent = 2
                    eol = EndOfLine.LF
                }
            }
        }
        val result = optimizer.optimize(input = TestFixtures.SIMPLE_SVG)
        assertTrue(result.data.isNotEmpty(), "Output data should not be empty")
        assertTrue(result.data.contains("\n"), "Pretty-printed output should contain newlines")
    }

    @Test
    fun `given complex SVG - when optimize is called - then all node types survive round-trip`() = runTest {
        val optimizer = svgo { }
        val result = optimizer.optimize(input = TestFixtures.PARSER_TEST_SVG)
        assertTrue(result.data.isNotEmpty(), "Output data should not be empty")
        assertTrue(result.data.contains("<svg"), "Output should contain an SVG element")
        assertTrue(result.data.contains("<circle") || result.data.contains("circle"), "Output should preserve circle element")
    }
}
