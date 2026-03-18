import org.example.ValidationResult
import org.example.validateLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidateLineTest {
    @Test
    fun `test empty string`() {
        assertNull(validateLine(123, " "))
    }

    @Test
    fun `test invalid number of pipes`() {
        assertNull(validateLine(123, "text|translate|extra"))
    }

    @Test
    fun `test blank fields`() {
        assertNull(validateLine(123, "|"))
    }

    @Test
    fun `test regex mismatch`() {
        assertNull(validateLine(123, "text|1=1; DROP TABLE users"))
    }

    @Test
    fun `test successful validation`() {
        val expected = ValidationResult("apple", "яблоко")
        assertEquals(expected, validateLine(123, "apple|яблоко"))
    }

    @Test
    fun `test UNION SELECT`() {
        assertNull(validateLine(123, "admin|UNION SELECT * FROM users"))
    }

    @Test
    fun `test DROP TABLE`() {
        assertNull(validateLine(123, "DROP|TABLE words"))
    }

    @Test
    fun `test or condition`() {
        assertNull(validateLine(123, "admin' OR '1'='1|password"))
    }
}
