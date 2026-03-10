package org.example

const val TOTAL_PERCENTS = 100
const val NORM_OF_CORRECT_ANSWERS = 3
const val QUESTION_SIZE = 4

fun Question.asConsoleString(): String {
    val variants = this.variants.mapIndexed { index, word -> "${index + 1} - ${word.translate}" }.joinToString("\n")
    return this.correctAnswer.text + "\n" + variants + "\n----------\n0 - Меню\nВведите номер:"
}

fun main() {
    val trainer = try {
        LearnWordsTrainer()
    } catch (e: Exception) {
        println("Невозможно загрузить словарь")
        return
    }

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
                        println(question.asConsoleString())

                        val userAnswerInput = readln().toIntOrNull()

                        if (userAnswerInput != null && userAnswerInput in 0..QUESTION_SIZE) {
                            if (userAnswerInput == 0) break

                            if (trainer.checkAnswer(userAnswerInput.minus(1))) {
                                println("Правильно!")
                            } else println("Неправильно! ${question.correctAnswer.text} - это ${question.correctAnswer.translate}")
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
