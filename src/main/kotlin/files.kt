package org.example

const val TOTAL_PERCENTS = 100
const val NORM_OF_CORRECT_ANSWERS = 3
const val QUESTION_SIZE = 4

data class Word(val text: String, val translate: String, var correctAnswersCount: Int = 0)

fun Question.printQuestion(question: Question) {
    val shuffledOptions = question.variants.shuffled()

    println("\n${question.correctAnswer.text}:")
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
                        question.printQuestion(question)

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
                trainer.getStatistics()
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
