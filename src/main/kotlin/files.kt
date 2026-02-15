package org.example

import java.io.File
import java.io.IOException

data class Word(val text: String, val translate: String, var correctAnswersCount: Int = 0)

fun main() {
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

    for (word in dictionary) {
        println("Word: ${word.text}, Translate: ${word.translate}, Correct Answers Count: ${word.correctAnswersCount}")
    }
}
