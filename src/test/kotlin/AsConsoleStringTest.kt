import org.example.Question
import org.example.Word
import org.example.asConsoleString
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AsConsoleStringTest {
    @Test
    fun `normal case with 4 variants`() {
        val question = Question(
            variants = listOf(
                Word("cat", "кот"),
                Word("dog", "собака"),
                Word("bird", "птица"),
                Word("fish", "рыба")
            ),
            correctAnswer = Word("cat", "кот")
        )

        val output = question.asConsoleString()
        assertTrue(output.contains("cat"))
        assertTrue(output.contains("1 - кот"))
        assertTrue(output.contains("4 - рыба"))
        assertTrue(output.contains("0 - Меню"))
    }

    @Test
    fun `shuffled variants`() {
        val question = Question(
            variants = listOf(
                Word("fish", "рыба"),
                Word("cat", "кот"),
                Word("bird", "птица"),
                Word("dog", "собака")
            ),
            correctAnswer = Word("cat", "кот")
        )

        val output = question.asConsoleString()
        assertTrue(output.indexOf("1 - рыба") < output.indexOf("2 - кот"))
        assertTrue(output.contains("0 - Меню"))
    }

    @Test
    fun `empty variants list`() {
        val question = Question(
            variants = emptyList(),
            correctAnswer = Word("cat", "кот")
        )

        val output = question.asConsoleString()
        assertTrue(output.contains("cat"))
        assertTrue(output.contains("0 - Меню"))
    }

    @Test
    fun `ten variants list`() {
        val question = Question(
            variants = listOf(
                Word("fish", "рыба"),
                Word("cat", "кот"),
                Word("bird", "птица"),
                Word("dog", "собака"),
                Word("hello", "привет"),
                Word("house", "дом"),
                Word("black", "чёрный"),
                Word("white", "белый"),
                Word("red", "красный"),
                Word("blue", "синий")
            ),
            correctAnswer = Word("cat", "кот")
        )

        val output = question.asConsoleString()
        assertTrue(output.contains("1 - рыба"))
        assertTrue(output.contains("10 - синий"))
        assertTrue(output.contains("0 - Меню"))
    }

    @Test
    fun `large number of variants 200`() {
        val variants = (1..200).map { Word("word$it", "перевод$it") }
        val question = Question(
            variants = variants,
            correctAnswer = variants[199]
        )

        val output = question.asConsoleString()
        assertTrue(output.contains("1 - перевод1"))
        assertTrue(output.contains("200 - перевод200"))
    }

    @Test
    fun `special characters in words`() {
        val question = Question(
            variants = listOf(
                Word("ca/t", "ко(т)"),
                Word("d.og", "со.ба|ка"),
                Word("!bird", "птица!"),
                Word("fis?h", "рыба?")
            ),
            correctAnswer = Word("ca/t", "ко(т)")
        )

        val output = question.asConsoleString()
        assertTrue(output.contains("1 - ко(т)"))
        assertTrue(output.contains("2 - со.ба|ка"))
        assertTrue(output.contains("4 - рыба?"))
        assertTrue(output.contains("0 - Меню"))
    }

    @Test
    fun `words consist of spaces`() {
        val question = Question(
            variants = listOf(
                Word("a", " "),
                Word("b", "   "),
                Word("c", "      ")
            ),
            correctAnswer = Word("a", " ")
        )

        val output = question.asConsoleString()
        assertTrue(output.contains("1 - "))
        assertTrue(output.contains("2 -    "))
        assertTrue(output.contains("3 -       "))
        assertTrue(output.contains("0 - Меню"))
    }

}