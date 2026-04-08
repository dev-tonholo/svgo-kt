package svgokt.transform

import kotlin.test.Test
import kotlin.test.assertEquals

class ParseTransformTest {

    @Test
    fun `given translate string - when parseTransform - then returns translate item`() {
        // Arrange
        val input = "translate(10, 20)"

        // Act
        val result = parseTransform(transformString = input)

        // Assert
        assertEquals(expected = 1, actual = result.size)
        assertEquals(expected = "translate", actual = result[0].name)
        assertEquals(expected = 10.0, actual = result[0].data[0])
        assertEquals(expected = 20.0, actual = result[0].data[1])
    }

    @Test
    fun `given multiple transforms - when parseTransform - then returns all items`() {
        // Arrange
        val input = "translate(10 20) scale(2 3) rotate(45)"

        // Act
        val result = parseTransform(transformString = input)

        // Assert
        assertEquals(expected = 3, actual = result.size)
        assertEquals(expected = "translate", actual = result[0].name)
        assertEquals(expected = "scale", actual = result[1].name)
        assertEquals(expected = "rotate", actual = result[2].name)
    }

    @Test
    fun `given matrix string - when parseTransform - then returns matrix with 6 values`() {
        // Arrange
        val input = "matrix(1 0 0 1 10 20)"

        // Act
        val result = parseTransform(transformString = input)

        // Assert
        assertEquals(expected = 1, actual = result.size)
        assertEquals(expected = "matrix", actual = result[0].name)
        assertEquals(expected = 6, actual = result[0].data.size)
        assertEquals(
            expected = TransformItem(name = "matrix", data = doubleArrayOf(1.0, 0.0, 0.0, 1.0, 10.0, 20.0)),
            actual = result[0],
        )
    }

    @Test
    fun `given rotate with center - when parseTransform - then returns rotate with 3 args`() {
        // Arrange
        val input = "rotate(45 50 60)"

        // Act
        val result = parseTransform(transformString = input)

        // Assert
        assertEquals(expected = 1, actual = result.size)
        assertEquals(expected = "rotate", actual = result[0].name)
        assertEquals(expected = 3, actual = result[0].data.size)
        assertEquals(expected = 45.0, actual = result[0].data[0])
        assertEquals(expected = 50.0, actual = result[0].data[1])
        assertEquals(expected = 60.0, actual = result[0].data[2])
    }

    @Test
    fun `given empty string - when parseTransform - then returns empty list`() {
        // Arrange
        val input = ""

        // Act
        val result = parseTransform(transformString = input)

        // Assert
        assertEquals(expected = emptyList(), actual = result)
    }

    @Test
    fun `given malformed transform - when parseTransform - then returns empty list`() {
        // Arrange
        val input = "notATransform(1 2 3)"

        // Act
        val result = parseTransform(transformString = input)

        // Assert
        assertEquals(expected = emptyList(), actual = result)
    }

    @Test
    fun `given skewX transform - when parseTransform - then returns skewX item`() {
        // Arrange
        val input = "skewX(30)"

        // Act
        val result = parseTransform(transformString = input)

        // Assert
        assertEquals(expected = 1, actual = result.size)
        assertEquals(expected = "skewX", actual = result[0].name)
        assertEquals(expected = 30.0, actual = result[0].data[0])
    }

    @Test
    fun `given skewY transform - when parseTransform - then returns skewY item`() {
        // Arrange
        val input = "skewY(15)"

        // Act
        val result = parseTransform(transformString = input)

        // Assert
        assertEquals(expected = 1, actual = result.size)
        assertEquals(expected = "skewY", actual = result[0].name)
        assertEquals(expected = 15.0, actual = result[0].data[0])
    }

    @Test
    fun `given transform with comma-separated values - when parseTransform - then parses correctly`() {
        // Arrange
        val input = "translate(10,20)"

        // Act
        val result = parseTransform(transformString = input)

        // Assert
        assertEquals(expected = 1, actual = result.size)
        assertEquals(expected = 10.0, actual = result[0].data[0])
        assertEquals(expected = 20.0, actual = result[0].data[1])
    }

    @Test
    fun `given transform with negative values - when parseTransform - then parses negative numbers`() {
        // Arrange
        val input = "translate(-10 -20.5)"

        // Act
        val result = parseTransform(transformString = input)

        // Assert
        assertEquals(expected = 1, actual = result.size)
        assertEquals(expected = -10.0, actual = result[0].data[0])
        assertEquals(expected = -20.5, actual = result[0].data[1])
    }

    @Test
    fun `given transform with scientific notation - when parseTransform - then parses correctly`() {
        // Arrange
        val input = "translate(1e2 2.5e-1)"

        // Act
        val result = parseTransform(transformString = input)

        // Assert
        assertEquals(expected = 1, actual = result.size)
        assertEquals(expected = 100.0, actual = result[0].data[0])
        assertEquals(expected = 0.25, actual = result[0].data[1])
    }
}
