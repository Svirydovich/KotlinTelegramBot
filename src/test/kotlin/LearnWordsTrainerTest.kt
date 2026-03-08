import org.example.LearnWordsTrainer
import org.example.NORM_OF_CORRECT_ANSWERS
import org.example.QUESTION_SIZE
import org.example.Statistics
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LearnWordsTrainerTest {
    @Test
    fun `test statistics with 4 words of 7`() {
        val trainer = LearnWordsTrainer("src/test/4_words_of_7.txt")
        assertEquals(Statistics(7, 4, 57), trainer.getStatistics())
    }

    @Test
    fun `test statistics with corrupted file`() {
        val trainer = assertThrows<IndexOutOfBoundsException> {
            LearnWordsTrainer("src/test/corrupted_words.txt")
        }
        assertTrue(trainer.message?.contains("Некорректный формат строки") == false)
    }

    @Test
    fun `test getNextQuestion() with 5 unlearned words`() {
        val trainer = LearnWordsTrainer("src/test/5_unlearned_words.txt")
        val question = trainer.getNextQuestion()
        assertNotNull(question)
        assertEquals(QUESTION_SIZE, question?.variants?.size)
        assertTrue(question?.variants?.contains(question.correctAnswer) == true)
    }

    @Test
    fun `test getNextQuestion() with 1 unlearned word`() {
        val trainer = LearnWordsTrainer("src/test/1_unlearned_word.txt")
        val question = trainer.getNextQuestion()
        assertNotNull(question)
        assertEquals(QUESTION_SIZE, question?.variants?.size)
        assertTrue(question?.variants?.contains(question.correctAnswer) == true)
        assertEquals(1, question?.variants?.filter { it.correctAnswersCount < NORM_OF_CORRECT_ANSWERS }?.size)
    }

    @Test
    fun `test getNextQuestion() with all words learned`() {
        val trainer = LearnWordsTrainer("src/test/all_words_learned.txt")
        val question = trainer.getNextQuestion()
        assertNull(question)
    }

    @Test
    fun `test checkAnswer() with true`() {
        val trainer = LearnWordsTrainer("src/test/5_unlearned_words.txt")
        val question = trainer.getNextQuestion()
        val userAnswerIndex = question?.variants?.indexOf(question.correctAnswer)
        val result = trainer.checkAnswer(userAnswerIndex)
        assertTrue(result, "Ответ должен быть правильным")
    }

    @Test
    fun `test checkAnswer() with false`() {
        val trainer = LearnWordsTrainer("src/test/5_unlearned_words.txt")
        val question = trainer.getNextQuestion()
        val wrongUserAnswerIndex =
            if (question?.variants?.indexOf(question.correctAnswer) != question?.variants?.lastIndex)
                question?.variants?.indexOf(question.correctAnswer)?.plus(1)
            else question?.variants?.indexOf(question.correctAnswer)?.minus(1)
        val result = trainer.checkAnswer(wrongUserAnswerIndex)
        assertFalse(result, "Ответ должен быть неверным")
    }

    @Test
    fun `test resetProgress() with 2 words in dictionary`() {
        val trainer = LearnWordsTrainer("src/test/2_words_in_dictionary.txt")
        trainer.resetProgress()
        trainer.dictionary.forEach { assertTrue(0 == it.correctAnswersCount) }
    }
}