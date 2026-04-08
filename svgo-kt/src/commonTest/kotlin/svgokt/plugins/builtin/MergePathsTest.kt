package svgokt.plugins.builtin

import kotlinx.coroutines.test.runTest
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.PluginInfo
import svgokt.parser.SvgoParser
import svgokt.plugins.xast.visit
import svgokt.stringfier.stringifySvg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MergePathsTest {

    private val parser = SvgoParser()

    @Test
    fun `given two paths with same attrs - when plugin runs - then paths merged`() = runTest {
        // Arrange
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg">
                <path d="M0 0L10 10" fill="red"/>
                <path d="M20 20L30 30" fill="red"/>
            </svg>
        """.trimIndent()
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val params = MergePathsParams(force = true)
        val visitor = MergePaths.fn(
            root,
            params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        val pathCount = Regex("<path ").findAll(result).count()
        assertEquals(
            expected = 1,
            actual = pathCount,
            message = "Two paths with same attrs should be merged into one, but found $pathCount paths in: $result",
        )
    }

    @Test
    fun `given two paths with different attrs - when plugin runs - then paths not merged`() = runTest {
        // Arrange
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg">
                <path d="M0 0L10 10" fill="red"/>
                <path d="M20 20L30 30" fill="blue"/>
            </svg>
        """.trimIndent()
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val params = MergePathsParams(force = true)
        val visitor = MergePaths.fn(
            root,
            params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        val pathCount = Regex("<path ").findAll(result).count()
        assertEquals(
            expected = 2,
            actual = pathCount,
            message = "Two paths with different attrs should not be merged, but found $pathCount paths in: $result",
        )
    }

    @Test
    fun `given three consecutive paths with same attrs - when plugin runs - then all merged`() = runTest {
        // Arrange
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg">
                <path d="M0 0L10 10" fill="red"/>
                <path d="M20 20L30 30" fill="red"/>
                <path d="M40 40L50 50" fill="red"/>
            </svg>
        """.trimIndent()
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val params = MergePathsParams(force = true)
        val visitor = MergePaths.fn(
            root,
            params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        val pathCount = Regex("<path ").findAll(result).count()
        assertEquals(
            expected = 1,
            actual = pathCount,
            message = "Three paths with same attrs should be merged into one, but found $pathCount paths in: $result",
        )
    }

    @Test
    fun `given single path child - when plugin runs - then path unchanged`() = runTest {
        // Arrange
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg">
                <path d="M0 0L10 10" fill="red"/>
            </svg>
        """.trimIndent()
        val root = parser.parseSvg(data = svg, from = null)

        // Act
        val params = MergePathsParams(force = true)
        val visitor = MergePaths.fn(
            root,
            params,
            PluginInfo(path = null, multipassCount = 0),
        )
        visitor?.let { root.visit(it) }

        // Assert
        val result = stringifySvg(data = root, userOptions = null)
        assertTrue(
            actual = result.contains("<path"),
            message = "Single path should remain unchanged, but not found in: $result",
        )
    }
}
