package org.example

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Word(
    val text: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
    val imagePath: String? = null,
    var imageFileId: String? = null,
)

data class Question(val variants: List<Word>, val correctAnswer: Word)

data class Statistics(val totalCount: Int, val learnedCount: Int, val percent: Int)

class LearnWordsTrainer(private val userDictionary: IUserDictionary) {
    var question: Question? = null

    fun addWord(word: Word) = userDictionary.addWord(word)

    fun checkNextQuestionAndSend(json: Json, telegramBotService: TelegramBotService, chatId: Long) {
        val question = getNextQuestion()

        if (question == null) telegramBotService.sendMessage(json, chatId, "Все слова в словаре выучены")
        else telegramBotService.sendQuestion(json, chatId, question)
    }

    fun getStatistics(): Statistics {
        val totalCount = userDictionary.getSize()
        val learnedCount = userDictionary.getNumOfLearnedWords()
        if (totalCount > 0) {
            val percent = learnedCount * TOTAL_PERCENTS / totalCount
            return Statistics(totalCount, learnedCount, percent)
        } else {
            return Statistics(0, 0, 0)
        }
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = userDictionary.getUnlearnedWords()
        if (notLearnedList.isEmpty()) return null

        val questionWords = if (notLearnedList.size < QUESTION_SIZE) {
            val learnedList = userDictionary.getLearnedWords()
            notLearnedList + learnedList.shuffled().take(QUESTION_SIZE - notLearnedList.size)
        } else notLearnedList.shuffled().take(QUESTION_SIZE)

        val correctAnswer = questionWords.shuffled().first()
        question = Question(questionWords, correctAnswer)
        return question
    }

    fun checkAnswer(userAnswerId: Int?): Boolean {
        return question?.let {
            val correctAnswerId = it.variants.indexOf(it.correctAnswer)
            if (correctAnswerId == userAnswerId) {
                userDictionary.setCorrectAnswersCount(it.correctAnswer.text, it.correctAnswer.correctAnswersCount + 1)
                true
            } else false
        } ?: false
    }

    fun resetProgress() = userDictionary.resetUserProgress()
}
