package org.example

import java.io.File
import java.io.IOException

const val TOTAL_PERCENTS = 100
const val NORM_OF_CORRECT_ANSWERS = 3
const val QUESTION_SIZE = 4

data class Word(val text: String, val translate: String, var correctAnswersCount: Int = 0)

fun main() {
    val dictionary = loadDictionary()

    while (true) {
        println("Меню:")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("0 - Выйти")
        println("\nВведите 1, 2 или 0")

        val menuChoice = readln()

        when (menuChoice) {
            "1" -> {
                println("Вы выбрали: Учить слова")

                val notLearnedList = dictionary.filter { it.correctAnswersCount < NORM_OF_CORRECT_ANSWERS }

                if (notLearnedList.isEmpty()) {
                    println("Все слова в словаре выучены!")
                    return
                }

                val questionWords = notLearnedList.shuffled().take(QUESTION_SIZE)

                for (correctAnswer in questionWords) {
                    val shuffledOptions = questionWords.shuffled()

                    println("\n${correctAnswer.text}:")
                    for (index in shuffledOptions.indices) {
                        println("${index + 1}. ${shuffledOptions[index].translate}")
                    }

                    println("\nВведите номер правильного ответа:")
                    val userAnswer = readln().toIntOrNull()

                    if (userAnswer != null && userAnswer in 1..shuffledOptions.size) {
                        if (shuffledOptions[userAnswer - 1] == correctAnswer) println("Правильный ответ!")
                        else println("Неправильный ответ")
                    } else println("Неверный ввод. Введите номер от 1 до ${shuffledOptions.size}")
                }
            }

            "2" -> {
                println("Вы выбрали: Статистика")
                val totalCount = dictionary.size
                val learnedCount = dictionary.count { it.correctAnswersCount >= NORM_OF_CORRECT_ANSWERS }
                if (totalCount > 0) {
                    val percent = learnedCount * TOTAL_PERCENTS / totalCount
                    println("Выучено $learnedCount из $totalCount слов | $percent%\n")
                }
                continue
            }

            "0" -> {
                println("Выход из программы...")
                break
            }

            else -> println("Вы ввели неверную команду! Введите число 1, 2 или 0")
        }
    }
}

fun loadDictionary(): MutableList<Word> {
    val wordsFile = File("words.txt")
    wordsFile.createNewFile()

    val dictionary = mutableListOf<Word>()

    try {
        for (word in wordsFile.readLines()) {
            val parts = word.split("|")
            dictionary.add(Word(parts[0], parts[1], parts[2].toIntOrNull() ?: 0))
        }
    } catch (e: IOException) {
        println("An error occurred while reading the file: ${e.message}")
    }

    return dictionary
}
