package svgokt.path

import svgokt.domain.XastElement
import svgokt.domain.XastElementType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PathHelpersTest {

    @Test
    fun `given element with d attribute - when path2js - then returns parsed path data`() {
        // Arrange
        val element = XastElement(
            name = "path",
            attributes = mutableMapOf("d" to "M10 20L30 40"),
            children = mutableListOf(),
        )

        // Act
        val result = path2js(element)

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertEquals(expected = 'M', actual = result[0].command)
        assertEquals(expected = 'L', actual = result[1].command)
    }

    @Test
    fun `given element with relative moveto - when path2js - then first moveto normalized to absolute`() {
        // Arrange
        val element = XastElement(
            name = "path",
            attributes = mutableMapOf("d" to "m10 20l30 40"),
            children = mutableListOf(),
        )

        // Act
        val result = path2js(element)

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertEquals(expected = 'M', actual = result[0].command)
        assertEquals(expected = 'l', actual = result[1].command)
    }

    @Test
    fun `given element without d attribute - when path2js - then returns empty list`() {
        // Arrange
        val element = XastElement(
            name = "path",
            attributes = mutableMapOf(),
            children = mutableListOf(),
        )

        // Act
        val result = path2js(element)

        // Assert
        assertTrue(actual = result.isEmpty())
    }

    @Test
    fun `given path data - when js2path - then element d attribute is set`() {
        // Arrange
        val element = XastElement(
            name = "path",
            attributes = mutableMapOf("d" to ""),
            children = mutableListOf(),
        )
        val data = listOf(
            PathDataItem(command = 'M', args = mutableListOf(10.0, 20.0)),
            PathDataItem(command = 'L', args = mutableListOf(30.0, 40.0)),
            PathDataItem(command = 'Z', args = mutableListOf()),
        )

        // Act
        js2path(element = element, data = data)

        // Assert
        assertEquals(expected = "M10 20 30 40Z", actual = element.attributes["d"])
    }

    @Test
    fun `given consecutive movetos - when js2path - then only last moveto kept`() {
        // Arrange
        val element = XastElement(
            name = "path",
            attributes = mutableMapOf("d" to ""),
            children = mutableListOf(),
        )
        val data = listOf(
            PathDataItem(command = 'M', args = mutableListOf(0.0, 0.0)),
            PathDataItem(command = 'M', args = mutableListOf(10.0, 20.0)),
            PathDataItem(command = 'L', args = mutableListOf(30.0, 40.0)),
        )

        // Act
        js2path(element = element, data = data)

        // Assert
        assertEquals(expected = "M10 20 30 40", actual = element.attributes["d"])
    }
}
