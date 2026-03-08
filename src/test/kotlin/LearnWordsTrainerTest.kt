import org.example.LearnWordsTrainer
import org.example.NORM_OF_CORRECT_ANSWERS
import org.example.QUESTION_SIZE
import org.example.Statistics
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LearnWordsTrainerTest {
    @Test
    fun `test statistics with 4 words of 7`() {
        val trainer = LearnWordsTrainer("src/test/4_words_of_7.txt")
        assertEquals(Statistics(7, 4, 57), trainer.getStatistics())
    }

    @Test
    fun `test statistics with corrupted file`() {
        val trainer = LearnWordsTrainer("src/test/corrupted_words.txt")
        assertEquals(4, trainer.getStatistics().totalCount)
        assertEquals(1, trainer.getStatistics().learnedCount)
    }

    @Test
    fun `test getNextQuestion() with 5 unlearned words`() {
        val trainer = LearnWordsTrainer("src/test/5_unlearned_words.txt")
        val notLearnedList = trainer.dictionary.filter { it.correctAnswersCount < NORM_OF_CORRECT_ANSWERS }
        assertEquals(5, notLearnedList.size)
        notLearnedList.forEach { assertTrue(3 > it.correctAnswersCount) }
        assertEquals(4, notLearnedList.shuffled().take(QUESTION_SIZE).size)
    }

    @Test
    fun `test getNextQuestion() with 1 unlearned word`() {
        val trainer = LearnWordsTrainer("src/test/1_unlearned_word.txt")
        val notLearnedList = trainer.dictionary.filter { it.correctAnswersCount < NORM_OF_CORRECT_ANSWERS }
        assertEquals(1, notLearnedList.size)
        val learnedList = trainer.dictionary.filter { it.correctAnswersCount >= NORM_OF_CORRECT_ANSWERS }
        val questionWordsList = notLearnedList + learnedList.shuffled().take(QUESTION_SIZE - notLearnedList.size)
        assertEquals(3, questionWordsList.filter { it.correctAnswersCount >= NORM_OF_CORRECT_ANSWERS }.size)
    }

    @Test
    fun `test getNextQuestion() with all words learned`() {
        val trainer = LearnWordsTrainer("src/test/all_words_learned.txt")
        val notLearnedList = trainer.dictionary.filter { it.correctAnswersCount < NORM_OF_CORRECT_ANSWERS }
        assertEquals(0, notLearnedList.size)
        val learnedList = trainer.dictionary.filter { it.correctAnswersCount >= NORM_OF_CORRECT_ANSWERS }
        val questionWordsList = notLearnedList + learnedList.shuffled().take(QUESTION_SIZE - notLearnedList.size)
        assertEquals(4, questionWordsList.filter { it.correctAnswersCount >= NORM_OF_CORRECT_ANSWERS }.size)
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