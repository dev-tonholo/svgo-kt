package svgokt.xast

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastRoot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CssSelectTest {

    // region querySelectorAll

    @Test
    fun `given tree with rects - when querySelectorAll by type - then returns all rects`() {
        // Arrange
        val rect1 = XastElement(
            name = "rect",
            attributes = mutableMapOf("id" to "r1"),
            children = mutableListOf(),
        )
        val rect2 = XastElement(
            name = "rect",
            attributes = mutableMapOf("id" to "r2"),
            children = mutableListOf(),
        )
        val circle = XastElement(
            name = "circle",
            attributes = mutableMapOf(),
            children = mutableListOf(),
        )
        val root = XastRoot(children = mutableListOf<XastChild>(rect1, rect2, circle))

        // Act
        val result = querySelectorAll(node = root, selector = "rect")

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertTrue(actual = rect1 in result)
        assertTrue(actual = rect2 in result)
        assertFalse(actual = circle in result)
    }

    @Test
    fun `given tree with classes - when querySelectorAll by class - then returns matching elements`() {
        // Arrange
        val highlighted1 = XastElement(
            name = "rect",
            attributes = mutableMapOf("class" to "highlighted"),
            children = mutableListOf(),
        )
        val multiClass = XastElement(
            name = "circle",
            attributes = mutableMapOf("class" to "highlighted bold"),
            children = mutableListOf(),
        )
        val other = XastElement(
            name = "path",
            attributes = mutableMapOf("class" to "other"),
            children = mutableListOf(),
        )
        val root = XastRoot(
            children = mutableListOf<XastChild>(highlighted1, multiClass, other),
        )

        // Act
        val result = querySelectorAll(node = root, selector = ".highlighted")

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertTrue(actual = highlighted1 in result)
        assertTrue(actual = multiClass in result)
        assertFalse(actual = other in result)
    }

    @Test
    fun `given tree with ids - when querySelector by id - then returns correct element`() {
        // Arrange
        val target = XastElement(
            name = "rect",
            attributes = mutableMapOf("id" to "myRect"),
            children = mutableListOf(),
        )
        val other = XastElement(
            name = "circle",
            attributes = mutableMapOf("id" to "otherCircle"),
            children = mutableListOf(),
        )
        val root = XastRoot(children = mutableListOf<XastChild>(target, other))

        // Act
        val result = querySelector(node = root, selector = "#myRect")

        // Assert
        assertNotNull(actual = result)
        assertEquals(expected = target, actual = result)
    }

    @Test
    fun `given tree without matching id - when querySelector by id - then returns null`() {
        // Arrange
        val element = XastElement(
            name = "rect",
            attributes = mutableMapOf("id" to "someRect"),
            children = mutableListOf(),
        )
        val root = XastRoot(children = mutableListOf<XastChild>(element))

        // Act
        val result = querySelector(node = root, selector = "#nonExistent")

        // Assert
        assertNull(actual = result)
    }

    // endregion

    // region matches

    @Test
    fun `given element with class - when matches by class - then returns true`() {
        // Arrange
        val element = XastElement(
            name = "rect",
            attributes = mutableMapOf("class" to "foo bar"),
            children = mutableListOf(),
        )

        // Act
        val result = matches(node = element, selector = ".foo")

        // Assert
        assertTrue(actual = result)
    }

    @Test
    fun `given element without class - when matches by class - then returns false`() {
        // Arrange
        val element = XastElement(
            name = "rect",
            attributes = mutableMapOf("class" to "other"),
            children = mutableListOf(),
        )

        // Act
        val result = matches(node = element, selector = ".foo")

        // Assert
        assertFalse(actual = result)
    }

    @Test
    fun `given element with matching type - when matches by type - then returns true`() {
        // Arrange
        val element = XastElement(
            name = "circle",
            attributes = mutableMapOf(),
            children = mutableListOf(),
        )

        // Act
        val result = matches(node = element, selector = "circle")

        // Assert
        assertTrue(actual = result)
    }

    @Test
    fun `given element with non-matching type - when matches by type - then returns false`() {
        // Arrange
        val element = XastElement(
            name = "rect",
            attributes = mutableMapOf(),
            children = mutableListOf(),
        )

        // Act
        val result = matches(node = element, selector = "circle")

        // Assert
        assertFalse(actual = result)
    }

    @Test
    fun `given element with id - when matches by id - then returns true`() {
        // Arrange
        val element = XastElement(
            name = "rect",
            attributes = mutableMapOf("id" to "myId"),
            children = mutableListOf(),
        )

        // Act
        val result = matches(node = element, selector = "#myId")

        // Assert
        assertTrue(actual = result)
    }

    @Test
    fun `given element with attribute - when matches by attribute presence - then returns true`() {
        // Arrange
        val element = XastElement(
            name = "rect",
            attributes = mutableMapOf("fill" to "red"),
            children = mutableListOf(),
        )

        // Act
        val result = matches(node = element, selector = "[fill]")

        // Assert
        assertTrue(actual = result)
    }

    @Test
    fun `given any element - when matches by universal selector - then returns true`() {
        // Arrange
        val element = XastElement(
            name = "rect",
            attributes = mutableMapOf(),
            children = mutableListOf(),
        )

        // Act
        val result = matches(node = element, selector = "*")

        // Assert
        assertTrue(actual = result)
    }

    // endregion
}
