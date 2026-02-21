package org.example

import java.io.File

data class Question(val variants: List<Word>, val correctAnswer: Word)

data class Statistics(val totalCount: Int, val learnedCount: Int, val percent: Int)

class LearnWordsTrainer {
    private var question: Question? = null
    private val dictionary = loadDictionary()

    fun getStatistics(): Statistics {
        val totalCount = dictionary.size
        val learnedCount = dictionary.count { it.correctAnswersCount >= NORM_OF_CORRECT_ANSWERS }
        if (totalCount > 0) {
            val percent = learnedCount * TOTAL_PERCENTS / totalCount
            return Statistics(totalCount, learnedCount, percent)
        } else {
            return Statistics(0, 0, 0)
        }
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < NORM_OF_CORRECT_ANSWERS }
        if (notLearnedList.isEmpty()) return null

        val questionWords = if (notLearnedList.size < QUESTION_SIZE) {
            val learnedList = dictionary.filter { it.correctAnswersCount >= NORM_OF_CORRECT_ANSWERS }
            notLearnedList + learnedList.shuffled().take(QUESTION_SIZE - notLearnedList.size)
        } else notLearnedList.shuffled().take(QUESTION_SIZE)

        val correctAnswer = notLearnedList.shuffled().first()
        question = Question(questionWords, correctAnswer)
        return question
    }

    fun checkAnswer(userAnswerId: Int?): Boolean {
        return question?.let {
            val correctAnswerId = it.variants.indexOf(it.correctAnswer)
            if (correctAnswerId == userAnswerId) {
                it.correctAnswer.correctAnswersCount++
                saveDictionary(dictionary)
                true
            } else false
        } ?: false
    }

    private fun loadDictionary(): MutableList<Word> {
        val wordsFile = File("words.txt")
        wordsFile.createNewFile()

        val dictionary = mutableListOf<Word>()

        for (word in wordsFile.readLines()) {
            val parts = word.split("|")
            dictionary.add(Word(parts[0], parts[1], parts[2].toIntOrNull() ?: 0))
        }

        return dictionary
    }

    private fun saveDictionary(dictionary: MutableList<Word>) {
        val wordsFile = File("words.txt")

        try {
            val content = dictionary.joinToString("\n") {
                "${it.text}|${it.translate}|${it.correctAnswersCount}"
            }

            wordsFile.writeText(content)

        } catch (e: Exception) {
            println("An error occurred while writing to the file: ${e.message}")
        }
    }
}

