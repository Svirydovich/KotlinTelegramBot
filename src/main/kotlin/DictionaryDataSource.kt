package org.example

import java.io.File
import java.sql.Connection
import java.sql.DriverManager


fun main() {
    DriverManager.getConnection("jdbc:sqlite:data.db")
        .use { connection ->
            val statement = connection.createStatement()
            statement.executeUpdate(
                """
                      CREATE TABLE IF NOT EXISTS "words" (
                          "id" integer PRIMARY KEY,
                          "text" varchar,
                          "translate" varchar
                      );
              """.trimIndent()
            )
            statement.executeUpdate("insert into words values(0, 'hello', 'привет')")
        }
}

fun updateDictionary(wordsFile: File) {
    DriverManager.getConnection("jdbc:sqlite:data.db").use { connection ->
        connection.autoCommit = false

        val insertSql = "INSERT INTO words (text, translate) VALUES (?, ?)"
        connection.prepareStatement(insertSql).use { stmt ->
            wordsFile.readLines().forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 2) {
                    val text = parts[0].trim()
                    val translate = parts[1].trim()
                    stmt.setString(1, text)
                    stmt.setString(2, translate)
                    stmt.addBatch()
                } else {
                    println("Skipping malformed line: $line")
                }
            }
            stmt.executeBatch()
        }

        connection.commit()
    }
}

interface IUserDictionary {
    fun getNumOfLearnedWords(): Int
    fun getSize(): Int
    fun getLearnedWords(): List<Word>
    fun getUnlearnedWords(): List<Word>
    fun setCorrectAnswersCount(word: String, correctAnswersCount: Int)
    fun resetUserProgress()
}

class FileUserDictionary(
    private val fileName: String = "words.txt",
    private val learningThreshold: Int = NORM_OF_CORRECT_ANSWERS,
) : IUserDictionary {

    private val dictionary = try {
        loadDictionary()
    } catch (e: Exception) {
        throw IllegalArgumentException("Некорректный файл")
    }

    override fun getNumOfLearnedWords() = dictionary.count { it.correctAnswersCount >= NORM_OF_CORRECT_ANSWERS }


    override fun getSize() = dictionary.size

    override fun getLearnedWords() = dictionary.filter { it.correctAnswersCount >= NORM_OF_CORRECT_ANSWERS }

    override fun getUnlearnedWords() = dictionary.filter { it.correctAnswersCount < NORM_OF_CORRECT_ANSWERS }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        dictionary.find { it.text == word }?.correctAnswersCount = correctAnswersCount
        saveDictionary()
    }

    override fun resetUserProgress() {
        dictionary.forEach { it.correctAnswersCount = 0 }
        saveDictionary()
    }

    private fun loadDictionary(): List<Word> {
        val wordsFile = File(fileName)
        if (!wordsFile.exists()) File("words.txt").copyTo(wordsFile)
        wordsFile.createNewFile()

        val dictionary = mutableListOf<Word>()

        for (word in wordsFile.readLines()) {
            val parts = word.split("|")
            dictionary.add(Word(parts[0], parts[1], parts[2].toIntOrNull() ?: 0))
        }

        return dictionary
    }

    private fun saveDictionary() {
        val file = File(fileName)
        val newFileContent = dictionary.map { "${it.text}|${it.translate}|${it.correctAnswersCount}" }
        file.writeText(newFileContent.joinToString(separator = "\n"))
    }
}

class DatabaseUserDictionary(private val dbPath: String = "data.db") : IUserDictionary {

    init {
        createTableIfNotExists()
    }

    private fun getConnection(): Connection =
        DriverManager.getConnection("jdbc:sqlite:$dbPath").apply {
            autoCommit = false
        }

    private fun createTableIfNotExists() {
        getConnection().use { connection ->
            val stmt = connection.createStatement()
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS words (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    text TEXT NOT NULL,
                    translate TEXT NOT NULL,
                    correctAnswersCount INTEGER DEFAULT 0
                );
                """.trimIndent()
            )
            connection.commit()
        }
    }

    override fun getNumOfLearnedWords(): Int {
        getConnection().use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM words WHERE correctAnswersCount >= ?"
            )
            stmt.setInt(1, NORM_OF_CORRECT_ANSWERS)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getInt(1) else 0
        }
    }

    override fun getSize(): Int {
        getConnection().use { connection ->
            val stmt = connection.prepareStatement("SELECT COUNT(*) FROM words")
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getInt(1) else 0
        }
    }

    override fun getLearnedWords(): List<Word> {
        val words = mutableListOf<Word>()
        getConnection().use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT text, translate, correctAnswersCount FROM words WHERE correctAnswersCount >= ?"
            )
            stmt.setInt(1, NORM_OF_CORRECT_ANSWERS)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                words.add(
                    Word(
                        rs.getString("text"),
                        rs.getString("translate"),
                        rs.getInt("correctAnswersCount")
                    )
                )
            }
        }
        return words
    }

    override fun getUnlearnedWords(): List<Word> {
        val words = mutableListOf<Word>()
        getConnection().use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT text, translate, correctAnswersCount FROM words WHERE correctAnswersCount < ?"
            )
            stmt.setInt(1, NORM_OF_CORRECT_ANSWERS)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                words.add(
                    Word(
                        rs.getString("text"),
                        rs.getString("translate"),
                        rs.getInt("correctAnswersCount")
                    )
                )
            }
        }
        return words
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        getConnection().use { connection ->
            val stmt = connection.prepareStatement(
                "UPDATE words SET correctAnswersCount = ? WHERE text = ?"
            )
            stmt.setInt(1, correctAnswersCount)
            stmt.setString(2, word)
            stmt.executeUpdate()
            connection.commit()
        }
    }

    override fun resetUserProgress() {
        getConnection().use { connection ->
            val stmt = connection.prepareStatement("UPDATE words SET correctAnswersCount = 0")
            stmt.executeUpdate()
            connection.commit()
        }
    }

    fun addWord(word: Word) {
        getConnection().use { connection ->
            val stmt = connection.prepareStatement(
                "INSERT INTO words (text, translate, correctAnswersCount) VALUES (?, ?, ?)"
            )
            stmt.setString(1, word.text)
            stmt.setString(2, word.translate)
            stmt.setInt(3, word.correctAnswersCount)
            stmt.executeUpdate()
            connection.commit()
        }
    }
}