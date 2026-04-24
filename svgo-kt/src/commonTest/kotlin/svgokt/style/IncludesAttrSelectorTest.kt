package svgokt.style

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IncludesAttrSelectorTest {

    @Test
    fun `given selector with bare attribute reference - when includesAttrSelector - then returns true`() {
        // Arrange
        val selector = "path[fill]"

        // Act
        val result = includesAttrSelector(selector = selector, name = "fill")

        // Assert
        assertTrue(actual = result)
    }

    @Test
    fun `given selector with equality matcher - when includesAttrSelector - then returns true`() {
        // Arrange
        val selector = """path[fill="red"]"""

        // Act
        val result = includesAttrSelector(selector = selector, name = "fill")

        // Assert
        assertTrue(actual = result)
    }

    @Test
    fun `given selector with other attributes only - when includesAttrSelector - then returns false`() {
        // Arrange
        val selector = "path[stroke]"

        // Act
        val result = includesAttrSelector(selector = selector, name = "fill")

        // Assert
        assertFalse(actual = result)
    }

    @Test
    fun `given selector with no attribute selectors - when includesAttrSelector - then returns false`() {
        // Arrange
        val selector = "svg > g.class #id"

        // Act
        val result = includesAttrSelector(selector = selector, name = "fill")

        // Assert
        assertFalse(actual = result)
    }

    @Test
    fun `given comma selector list with one match - when includesAttrSelector - then returns true`() {
        // Arrange
        val selector = "rect, path[fill], circle"

        // Act
        val result = includesAttrSelector(selector = selector, name = "fill")

        // Assert
        assertTrue(actual = result)
    }

    @Test
    fun `given attribute nested inside not pseudo - when includesAttrSelector - then returns true`() {
        // Arrange
        val selector = "path:not([fill])"

        // Act
        val result = includesAttrSelector(selector = selector, name = "fill")

        // Assert
        assertTrue(actual = result)
    }

    @Test
    fun `given unparseable selector - when includesAttrSelector - then returns false`() {
        // Arrange
        val selector = "]][not css"

        // Act
        val result = includesAttrSelector(selector = selector, name = "fill")

        // Assert
        assertFalse(actual = result)
    }

    @Test
    fun `given attribute name that is substring of another - when includesAttrSelector - then returns false`() {
        // Arrange
        val selector = "path[fill-opacity]"

        // Act
        val result = includesAttrSelector(selector = selector, name = "fill")

        // Assert
        assertFalse(actual = result)
    }

    @Test
    fun `given equality selector with matching value - when includesAttrSelector with value - then returns true`() {
        // Arrange
        val selector = """path[class="foo"]"""

        // Act
        val result = includesAttrSelector(selector = selector, name = "class", value = "foo")

        // Assert
        assertTrue(actual = result)
    }

    @Test
    fun `given equality selector with different value - when includesAttrSelector with value - then returns false`() {
        // Arrange
        val selector = """path[class="bar"]"""

        // Act
        val result = includesAttrSelector(selector = selector, name = "class", value = "foo")

        // Assert
        assertFalse(actual = result)
    }

    @Test
    fun `given whitespace list selector containing token - when includesAttrSelector with value - then returns true`() {
        // Arrange
        val selector = """path[class~="foo bar"]"""

        // Act
        val result = includesAttrSelector(selector = selector, name = "class", value = "foo")

        // Assert
        assertTrue(actual = result)
    }

    @Test
    fun `given prefix selector matching value - when includesAttrSelector with value - then returns true`() {
        // Arrange
        val selector = """path[class^="foo"]"""

        // Act
        val result = includesAttrSelector(selector = selector, name = "class", value = "foobar")

        // Assert
        assertTrue(actual = result)
    }

    @Test
    fun `given bare attribute with value check - when includesAttrSelector with value - then returns true`() {
        // Arrange
        val selector = "path[class]"

        // Act
        val result = includesAttrSelector(selector = selector, name = "class", value = "foo")

        // Assert
        assertTrue(actual = result)
    }

    @Test
    fun `given equality selector on wrong attr - when includesAttrSelector with value - then returns false`() {
        // Arrange
        val selector = """path[id="foo"]"""

        // Act
        val result = includesAttrSelector(selector = selector, name = "class", value = "foo")

        // Assert
        assertFalse(actual = result)
    }
}
