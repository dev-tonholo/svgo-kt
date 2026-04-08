package svgokt.transform

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransformMathTest {

    private fun assertMatrixEquals(
        expected: DoubleArray,
        actual: DoubleArray,
        tolerance: Double = 1e-10,
    ) {
        assertEquals(expected = expected.size, actual = actual.size, message = "Matrix size mismatch")
        for (i in expected.indices) {
            assertTrue(
                actual = abs(expected[i] - actual[i]) < tolerance,
                message = "Matrix element [$i] differs: expected=${expected[i]}, actual=${actual[i]}",
            )
        }
    }

    @Test
    fun `given translate transform - when transformToMatrix - then returns identity with translation`() {
        // Arrange
        val transform = TransformItem(name = "translate", data = doubleArrayOf(10.0, 20.0))

        // Act
        val result = transformToMatrix(transform = transform)

        // Assert
        assertMatrixEquals(
            expected = doubleArrayOf(1.0, 0.0, 0.0, 1.0, 10.0, 20.0),
            actual = result,
        )
    }

    @Test
    fun `given scale transform - when transformToMatrix - then returns scale matrix`() {
        // Arrange
        val transform = TransformItem(name = "scale", data = doubleArrayOf(2.0, 3.0))

        // Act
        val result = transformToMatrix(transform = transform)

        // Assert
        assertMatrixEquals(
            expected = doubleArrayOf(2.0, 0.0, 0.0, 3.0, 0.0, 0.0),
            actual = result,
        )
    }

    @Test
    fun `given scale with single value - when transformToMatrix - then uses same value for both axes`() {
        // Arrange
        val transform = TransformItem(name = "scale", data = doubleArrayOf(2.0))

        // Act
        val result = transformToMatrix(transform = transform)

        // Assert
        assertMatrixEquals(
            expected = doubleArrayOf(2.0, 0.0, 0.0, 2.0, 0.0, 0.0),
            actual = result,
        )
    }

    @Test
    fun `given rotate transform - when transformToMatrix - then returns rotation matrix`() {
        // Arrange
        val transform = TransformItem(name = "rotate", data = doubleArrayOf(90.0))

        // Act
        val result = transformToMatrix(transform = transform)

        // Assert
        assertMatrixEquals(
            expected = doubleArrayOf(0.0, 1.0, -1.0, 0.0, 0.0, 0.0),
            actual = result,
            tolerance = 1e-10,
        )
    }

    @Test
    fun `given rotate with center - when transformToMatrix - then applies translate-rotate-translate`() {
        // Arrange
        val transform = TransformItem(name = "rotate", data = doubleArrayOf(90.0, 50.0, 50.0))

        // Act
        val result = transformToMatrix(transform = transform)

        // Assert
        // rotate(90, 50, 50) = translate(50,50) * rotate(90) * translate(-50,-50)
        // = [0, 1, -1, 0, 100, 0]
        assertMatrixEquals(
            expected = doubleArrayOf(0.0, 1.0, -1.0, 0.0, 100.0, 0.0),
            actual = result,
            tolerance = 1e-10,
        )
    }

    @Test
    fun `given skewX transform - when transformToMatrix - then returns skewX matrix`() {
        // Arrange
        val transform = TransformItem(name = "skewX", data = doubleArrayOf(45.0))

        // Act
        val result = transformToMatrix(transform = transform)

        // Assert
        assertMatrixEquals(
            expected = doubleArrayOf(1.0, 0.0, 1.0, 1.0, 0.0, 0.0),
            actual = result,
            tolerance = 1e-10,
        )
    }

    @Test
    fun `given skewY transform - when transformToMatrix - then returns skewY matrix`() {
        // Arrange
        val transform = TransformItem(name = "skewY", data = doubleArrayOf(45.0))

        // Act
        val result = transformToMatrix(transform = transform)

        // Assert
        assertMatrixEquals(
            expected = doubleArrayOf(1.0, 1.0, 0.0, 1.0, 0.0, 0.0),
            actual = result,
            tolerance = 1e-10,
        )
    }

    @Test
    fun `given two translates - when transformsMultiply - then translations add`() {
        // Arrange
        val transforms = listOf(
            TransformItem(name = "translate", data = doubleArrayOf(10.0, 20.0)),
            TransformItem(name = "translate", data = doubleArrayOf(30.0, 40.0)),
        )

        // Act
        val result = transformsMultiply(transforms = transforms)

        // Assert
        assertEquals(expected = "matrix", actual = result.name)
        assertMatrixEquals(
            expected = doubleArrayOf(1.0, 0.0, 0.0, 1.0, 40.0, 60.0),
            actual = result.data,
        )
    }

    @Test
    fun `given scale and translate - when transformsMultiply - then produces correct matrix`() {
        // Arrange
        val transforms = listOf(
            TransformItem(name = "scale", data = doubleArrayOf(2.0, 2.0)),
            TransformItem(name = "translate", data = doubleArrayOf(10.0, 20.0)),
        )

        // Act
        val result = transformsMultiply(transforms = transforms)

        // Assert
        // scale(2,2) * translate(10,20) = [2,0,0,2,20,40]
        assertEquals(expected = "matrix", actual = result.name)
        assertMatrixEquals(
            expected = doubleArrayOf(2.0, 0.0, 0.0, 2.0, 20.0, 40.0),
            actual = result.data,
        )
    }

    @Test
    fun `given two identity matrices - when multiplyTransformMatrices - then returns identity`() {
        // Arrange
        val m1 = doubleArrayOf(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        val m2 = doubleArrayOf(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

        // Act
        val result = multiplyTransformMatrices(a = m1, b = m2)

        // Assert
        assertMatrixEquals(
            expected = doubleArrayOf(1.0, 0.0, 0.0, 1.0, 0.0, 0.0),
            actual = result,
        )
    }

    @Test
    fun `given matrix transform - when transformToMatrix - then returns the data as-is`() {
        // Arrange
        val transform = TransformItem(name = "matrix", data = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0))

        // Act
        val result = transformToMatrix(transform = transform)

        // Assert
        assertMatrixEquals(
            expected = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0),
            actual = result,
        )
    }

    @Test
    fun `given empty transforms list - when transformsMultiply - then returns matrix with empty data`() {
        // Arrange
        val transforms = emptyList<TransformItem>()

        // Act
        val result = transformsMultiply(transforms = transforms)

        // Assert
        assertEquals(expected = "matrix", actual = result.name)
        assertEquals(expected = 0, actual = result.data.size)
    }
}
