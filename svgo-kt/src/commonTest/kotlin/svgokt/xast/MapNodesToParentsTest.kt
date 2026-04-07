package svgokt.xast

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastRoot
import svgokt.domain.XastText
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertEquals

class MapNodesToParentsTest {

    @Test
    fun `given root with direct children - when mapNodesToParents - then children map to root`() {
        // Arrange
        val child1 = XastElement(
            name = "rect",
            attributes = mutableMapOf(),
            children = mutableListOf(),
        )
        val child2 = XastElement(
            name = "circle",
            attributes = mutableMapOf(),
            children = mutableListOf(),
        )
        val root = XastRoot(children = mutableListOf(child1, child2))

        // Act
        val result = mapNodesToParents(root)

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertSame(expected = root, actual = result[child1])
        assertSame(expected = root, actual = result[child2])
    }

    @Test
    fun `given nested elements - when mapNodesToParents - then each child maps to its parent`() {
        // Arrange
        val grandchild = XastText(value = "hello")
        val child = XastElement(
            name = "g",
            attributes = mutableMapOf(),
            children = mutableListOf<XastChild>(grandchild),
        )
        val root = XastRoot(children = mutableListOf<XastChild>(child))

        // Act
        val result = mapNodesToParents(root)

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertSame(expected = root, actual = result[child])
        assertSame(expected = child, actual = result[grandchild])
    }

    @Test
    fun `given root with no children - when mapNodesToParents - then returns empty map`() {
        // Arrange
        val root = XastRoot(children = mutableListOf())

        // Act
        val result = mapNodesToParents(root)

        // Assert
        assertEquals(expected = 0, actual = result.size)
    }

    @Test
    fun `given deeply nested tree - when mapNodesToParents - then all nodes have entries`() {
        // Arrange
        val deepText = XastText(value = "deep")
        val innerElement = XastElement(
            name = "span",
            attributes = mutableMapOf(),
            children = mutableListOf<XastChild>(deepText),
        )
        val middleElement = XastElement(
            name = "g",
            attributes = mutableMapOf(),
            children = mutableListOf<XastChild>(innerElement),
        )
        val outerElement = XastElement(
            name = "svg",
            attributes = mutableMapOf(),
            children = mutableListOf<XastChild>(middleElement),
        )
        val root = XastRoot(children = mutableListOf<XastChild>(outerElement))

        // Act
        val result = mapNodesToParents(root)

        // Assert
        assertEquals(expected = 4, actual = result.size)
        assertSame(expected = root, actual = result[outerElement])
        assertSame(expected = outerElement, actual = result[middleElement])
        assertSame(expected = middleElement, actual = result[innerElement])
        assertSame(expected = innerElement, actual = result[deepText])
    }
}
