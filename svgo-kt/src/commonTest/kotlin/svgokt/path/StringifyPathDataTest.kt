package svgokt.path

import kotlin.test.Test
import kotlin.test.assertEquals

class StringifyPathDataTest {

    @Test
    fun `given path items - when stringifyPathData - then produces valid path string`() {
        // Arrange
        val pathData = listOf(
            PathDataItem(command = 'M', args = mutableListOf(10.0, 20.0)),
            PathDataItem(command = 'L', args = mutableListOf(30.0, 40.0)),
            PathDataItem(command = 'Z', args = mutableListOf()),
        )

        // Act
        val result = stringifyPathData(pathData = pathData)

        // Assert
        assertEquals(expected = "M10 20 30 40Z", actual = result)
    }

    @Test
    fun `given path with precision - when stringifyPathData - then numbers are rounded`() {
        // Arrange
        val pathData = listOf(
            PathDataItem(command = 'M', args = mutableListOf(10.123456, 20.654321)),
            PathDataItem(command = 'L', args = mutableListOf(30.999, 40.001)),
        )

        // Act
        val result = stringifyPathData(pathData = pathData, precision = 2)

        // Assert
        assertEquals(expected = "M10.12 20.65 31 40", actual = result)
    }

    @Test
    fun `given consecutive same commands - when stringifyPathData - then commands are combined`() {
        // Arrange
        val pathData = listOf(
            PathDataItem(command = 'M', args = mutableListOf(0.0, 0.0)),
            PathDataItem(command = 'L', args = mutableListOf(10.0, 10.0)),
            PathDataItem(command = 'L', args = mutableListOf(20.0, 20.0)),
            PathDataItem(command = 'L', args = mutableListOf(30.0, 30.0)),
        )

        // Act
        val result = stringifyPathData(pathData = pathData)

        // Assert
        // M + L combined, then all L's combined
        assertEquals(expected = "M0 0 10 10 20 20 30 30", actual = result)
    }

    @Test
    fun `given single item - when stringifyPathData - then single command output`() {
        // Arrange
        val pathData = listOf(
            PathDataItem(command = 'M', args = mutableListOf(5.0, 10.0)),
        )

        // Act
        val result = stringifyPathData(pathData = pathData)

        // Assert
        assertEquals(expected = "M5 10", actual = result)
    }

    @Test
    fun `given empty list - when stringifyPathData - then returns empty string`() {
        // Arrange
        val pathData = emptyList<PathDataItem>()

        // Act
        val result = stringifyPathData(pathData = pathData)

        // Assert
        assertEquals(expected = "", actual = result)
    }

    @Test
    fun `given arc with disableSpaceAfterFlags - when stringifyPathData - then flags compacted`() {
        // Arrange
        val pathData = listOf(
            PathDataItem(command = 'M', args = mutableListOf(0.0, 0.0)),
            PathDataItem(command = 'A', args = mutableListOf(25.0, 26.0, -30.0, 0.0, 1.0, 50.0, 25.0)),
        )

        // Act
        val result = stringifyPathData(
            pathData = pathData,
            disableSpaceAfterFlags = true,
        )

        // Assert
        assertEquals(expected = "M0 0A25 26-30 0150 25", actual = result)
    }

    @Test
    fun `given leading zero values - when stringifyPathData - then leading zeros removed`() {
        // Arrange
        val pathData = listOf(
            PathDataItem(command = 'M', args = mutableListOf(0.5, 0.5)),
            PathDataItem(command = 'L', args = mutableListOf(1.0, 0.5)),
        )

        // Act
        val result = stringifyPathData(pathData = pathData)

        // Assert
        assertEquals(expected = "M.5.5 1 .5", actual = result)
    }

    @Test
    fun `given negative values - when stringifyPathData - then no extra space before negatives`() {
        // Arrange
        val pathData = listOf(
            PathDataItem(command = 'M', args = mutableListOf(10.0, -20.0)),
            PathDataItem(command = 'L', args = mutableListOf(-5.0, -10.0)),
        )

        // Act
        val result = stringifyPathData(pathData = pathData)

        // Assert
        assertEquals(expected = "M10-20-5-10", actual = result)
    }

    @Test
    fun `given moveto followed by relative lineto - when stringifyPathData - then combined as m`() {
        // Arrange
        val pathData = listOf(
            PathDataItem(command = 'M', args = mutableListOf(0.0, 0.0)),
            PathDataItem(command = 'l', args = mutableListOf(10.0, 10.0)),
            PathDataItem(command = 'l', args = mutableListOf(20.0, 20.0)),
        )

        // Act
        val result = stringifyPathData(pathData = pathData)

        // Assert
        assertEquals(expected = "m0 0 10 10 20 20", actual = result)
    }
}
