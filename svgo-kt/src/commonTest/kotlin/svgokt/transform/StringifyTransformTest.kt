package svgokt.transform

import kotlin.test.Test
import kotlin.test.assertEquals

class StringifyTransformTest {

    @Test
    fun `given single translate - when stringifyTransform - then returns translate string`() {
        // Arrange
        val transforms = listOf(
            TransformItem(name = "translate", data = doubleArrayOf(10.0, 20.0)),
        )

        // Act
        val result = stringifyTransform(transforms = transforms)

        // Assert
        assertEquals(expected = "translate(10 20)", actual = result)
    }

    @Test
    fun `given multiple transforms - when stringifyTransform - then returns space-separated string`() {
        // Arrange
        val transforms = listOf(
            TransformItem(name = "translate", data = doubleArrayOf(10.0, 20.0)),
            TransformItem(name = "rotate", data = doubleArrayOf(45.0)),
        )

        // Act
        val result = stringifyTransform(transforms = transforms)

        // Assert
        assertEquals(expected = "translate(10 20) rotate(45)", actual = result)
    }

    @Test
    fun `given matrix transform - when stringifyTransform - then returns matrix string`() {
        // Arrange
        val transforms = listOf(
            TransformItem(name = "matrix", data = doubleArrayOf(1.0, 0.0, 0.0, 1.0, 10.0, 20.0)),
        )

        // Act
        val result = stringifyTransform(transforms = transforms)

        // Assert
        assertEquals(expected = "matrix(1 0 0 1 10 20)", actual = result)
    }

    @Test
    fun `given precision of 2 - when stringifyTransform - then rounds values`() {
        // Arrange
        val transforms = listOf(
            TransformItem(name = "translate", data = doubleArrayOf(10.12345, 20.6789)),
        )

        // Act
        val result = stringifyTransform(transforms = transforms, precision = 2)

        // Assert
        assertEquals(expected = "translate(10.12 20.68)", actual = result)
    }

    @Test
    fun `given empty list - when stringifyTransform - then returns empty string`() {
        // Arrange
        val transforms = emptyList<TransformItem>()

        // Act
        val result = stringifyTransform(transforms = transforms)

        // Assert
        assertEquals(expected = "", actual = result)
    }

    @Test
    fun `given values that are whole numbers - when stringifyTransform - then omits decimal point`() {
        // Arrange
        val transforms = listOf(
            TransformItem(name = "scale", data = doubleArrayOf(2.0, 3.0)),
        )

        // Act
        val result = stringifyTransform(transforms = transforms)

        // Assert
        assertEquals(expected = "scale(2 3)", actual = result)
    }
}
