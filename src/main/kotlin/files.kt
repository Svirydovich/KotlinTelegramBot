package org.example

const val TOTAL_PERCENTS = 100
const val NORM_OF_CORRECT_ANSWERS = 3
const val QUESTION_SIZE = 4

data class Word(val text: String, val translate: String, var correctAnswersCount: Int = 0)

fun Question.printQuestion() {
    val shuffledOptions = variants

    println("\n${correctAnswer.text}:")
    shuffledOptions.forEachIndexed { index, word -> println("${index + 1} - ${word.translate}") }
    println("----------\n0 - Меню")
    println("\nВведите номер:")
}

fun main() {
    val trainer = LearnWordsTrainer()

    while (true) {
        println("Меню:")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("0 - Выйти")
        println("\nВведите 1, 2 или 0")

        val menuChoice = readln()

        when (menuChoice) {
            "1" -> {
                while (true) {
                    val question = trainer.getNextQuestion()
                    if (question == null) {
                        println("Все слова в словаре выучены!")
                        break
                    } else {
                        question.printQuestion()

                        val userAnswerInput = readln().toIntOrNull()

                        if (userAnswerInput != null && userAnswerInput in 0..QUESTION_SIZE) {
                            if (userAnswerInput == 0) break

                            if (trainer.checkAnswer(userAnswerInput.minus(1))) {
                                println("Правильно!\n")
                            } else println("Неправильно! ${question.correctAnswer.text} - это ${question.correctAnswer.translate}\n")
                        } else println("Некорректный ввод. Введите номер от 0 до $QUESTION_SIZE")
                    }
                }
            }

            "2" -> {
                val statistics = trainer.getStatistics()
                if (statistics.totalCount > 0)
                    println("Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}%\n")
                else println("В словаре нет слов")
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
