package org.example

import java.io.File
import java.io.IOException

fun main() {
    val wordsFile = File("words.txt")
    wordsFile.createNewFile()
    wordsFile.writeText("hello привет\n")
    wordsFile.appendText("dog собака\n")
    wordsFile.appendText("cat кошка\n")

    try {
        for (word in wordsFile.readLines()) println(word)
    } catch (e: IOException) {
        println("An error occurred while reading the file: ${e.message}")
    }
}