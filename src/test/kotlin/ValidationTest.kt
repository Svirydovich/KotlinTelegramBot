import org.example.validateLine
import kotlin.test.Test
import kotlin.test.assertNull

class ValidationTest {
    @Test
    fun `test SQL injection with dash-dash`() {
        val result = validateLine(1L, "Текст | Текст --")
        assertNull(result)
    }

    @Test
    fun `test SQL injection with quote`() {
        val result = validateLine(1L, "Текст | Текст'")
        assertNull(result)
    }

    @Test
    fun `test SQL injection with semicolon`() {
        val result = validateLine(1L, "Текст | ;Текст")
        assertNull(result)
    }
}