package svgokt.path

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParsePathDataTest {

    @Test
    fun `given simple moveto lineto - when parsePathData - then returns correct items`() {
        // Arrange
        val path = "M 10 20 L 30 40"

        // Act
        val result = parsePathData(path)

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertEquals(expected = 'M', actual = result[0].command)
        assertEquals(expected = listOf(10.0, 20.0), actual = result[0].args)
        assertEquals(expected = 'L', actual = result[1].command)
        assertEquals(expected = listOf(30.0, 40.0), actual = result[1].args)
    }

    @Test
    fun `given path with all command types - when parsePathData - then all commands parsed`() {
        // Arrange
        val path = "M0 0L10 10H20V30C1 2 3 4 5 6S7 8 9 10Q11 12 13 14T15 16A1 2 3 0 1 20 25Z"

        // Act
        val result = parsePathData(path)

        // Assert
        val commands = result.map { it.command }
        assertEquals(
            expected = listOf('M', 'L', 'H', 'V', 'C', 'S', 'Q', 'T', 'A', 'Z'),
            actual = commands,
        )
        // Verify arg counts
        assertEquals(expected = 2, actual = result[0].args.size) // M
        assertEquals(expected = 2, actual = result[1].args.size) // L
        assertEquals(expected = 1, actual = result[2].args.size) // H
        assertEquals(expected = 1, actual = result[3].args.size) // V
        assertEquals(expected = 6, actual = result[4].args.size) // C
        assertEquals(expected = 4, actual = result[5].args.size) // S
        assertEquals(expected = 4, actual = result[6].args.size) // Q
        assertEquals(expected = 2, actual = result[7].args.size) // T
        assertEquals(expected = 7, actual = result[8].args.size) // A
        assertEquals(expected = 0, actual = result[9].args.size) // Z
    }

    @Test
    fun `given path with implicit lineto after moveto - when parsePathData - then implicit L used`() {
        // Arrange
        val path = "M 10 20 30 40 50 60"

        // Act
        val result = parsePathData(path)

        // Assert
        assertEquals(expected = 3, actual = result.size)
        assertEquals(expected = 'M', actual = result[0].command)
        assertEquals(expected = listOf(10.0, 20.0), actual = result[0].args)
        assertEquals(expected = 'L', actual = result[1].command)
        assertEquals(expected = listOf(30.0, 40.0), actual = result[1].args)
        assertEquals(expected = 'L', actual = result[2].command)
        assertEquals(expected = listOf(50.0, 60.0), actual = result[2].args)
    }

    @Test
    fun `given path with implicit lineto after relative moveto - when parsePathData - then implicit l used`() {
        // Arrange
        val path = "m 10 20 30 40"

        // Act
        val result = parsePathData(path)

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertEquals(expected = 'm', actual = result[0].command)
        assertEquals(expected = 'l', actual = result[1].command)
        assertEquals(expected = listOf(30.0, 40.0), actual = result[1].args)
    }

    @Test
    fun `given path with scientific notation - when parsePathData - then numbers parsed correctly`() {
        // Arrange
        val path = "M 1e2 2.5e1 L 1.5E3 -2e-1"

        // Act
        val result = parsePathData(path)

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertEquals(expected = 100.0, actual = result[0].args[0])
        assertEquals(expected = 25.0, actual = result[0].args[1])
        assertEquals(expected = 1500.0, actual = result[1].args[0])
        assertEquals(expected = -0.2, actual = result[1].args[1])
    }

    @Test
    fun `given empty string - when parsePathData - then returns empty list`() {
        // Arrange
        val path = ""

        // Act
        val result = parsePathData(path)

        // Assert
        assertTrue(actual = result.isEmpty())
    }

    @Test
    fun `given path without leading moveto - when parsePathData - then returns empty list`() {
        // Arrange
        val path = "L 10 20"

        // Act
        val result = parsePathData(path)

        // Assert
        assertTrue(actual = result.isEmpty())
    }

    @Test
    fun `given path with decimal numbers - when parsePathData - then decimals parsed correctly`() {
        // Arrange
        val path = "M.5.5L1.5 2.5"

        // Act
        val result = parsePathData(path)

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertEquals(expected = 0.5, actual = result[0].args[0])
        assertEquals(expected = 0.5, actual = result[0].args[1])
        assertEquals(expected = 1.5, actual = result[1].args[0])
        assertEquals(expected = 2.5, actual = result[1].args[1])
    }

    @Test
    fun `given arc command with flags - when parsePathData - then arc parsed correctly`() {
        // Arrange
        val path = "M0 0A25 26 -30 0 1 50 25"

        // Act
        val result = parsePathData(path)

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertEquals(expected = 'A', actual = result[1].command)
        assertEquals(expected = 25.0, actual = result[1].args[0])
        assertEquals(expected = 26.0, actual = result[1].args[1])
        assertEquals(expected = -30.0, actual = result[1].args[2])
        assertEquals(expected = 0.0, actual = result[1].args[3])
        assertEquals(expected = 1.0, actual = result[1].args[4])
        assertEquals(expected = 50.0, actual = result[1].args[5])
        assertEquals(expected = 25.0, actual = result[1].args[6])
    }

    @Test
    fun `given arc with compact flags - when parsePathData - then flags parsed correctly`() {
        // Arrange
        val path = "M0 0A25 26 -30 01 50 25"

        // Act
        val result = parsePathData(path)

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertEquals(expected = 0.0, actual = result[1].args[3])
        assertEquals(expected = 1.0, actual = result[1].args[4])
    }

    @Test
    fun `given path with negative numbers - when parsePathData - then negatives parsed as separators`() {
        // Arrange
        val path = "M10-20L30-40"

        // Act
        val result = parsePathData(path)

        // Assert
        assertEquals(expected = 2, actual = result.size)
        assertEquals(expected = 10.0, actual = result[0].args[0])
        assertEquals(expected = -20.0, actual = result[0].args[1])
        assertEquals(expected = 30.0, actual = result[1].args[0])
        assertEquals(expected = -40.0, actual = result[1].args[1])
    }
}
