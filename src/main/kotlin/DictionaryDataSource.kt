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

        val insertSql = """
            INSERT INTO words (text, translate) VALUES (?, ?)
            ON CONFLICT(text) DO UPDATE SET translate = excluded.translate
        """.trimIndent()
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
    fun addWord(word: Word)
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

    override fun addWord(word: Word) {
        if (dictionary.none { it.text == word.text }) dictionary.add(word)
        saveDictionary()
    }

    private fun loadDictionary(): MutableList<Word> {
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
            val statement = connection.createStatement()
            statement.executeUpdate("PRAGMA foreign_keys = ON;")
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username VARCHAR,
                    created_at TIMESTAMP,
                    chat_id INTEGER
                );
                """.trimIndent()
            )
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS words (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    text TEXT NOT NULL UNIQUE,
                    translate TEXT NOT NULL,
                    correctAnswersCount INTEGER DEFAULT 0
                );
                """.trimIndent()
            )
            statement.executeUpdate(
                """
                    CREATE TABLE IF NOT EXISTS user_answers (
                    user_id INTEGER,
                    word_id INTEGER,
                    correct_answer_count INTEGER,
                    updated_at TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (word_id) REFERENCES words(id)
                );
                """.trimIndent()
            )
            connection.commit()
        }
    }

    override fun getNumOfLearnedWords(): Int {
        getConnection().use { connection ->
            val statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM words WHERE correctAnswersCount >= ?"
            )
            statement.setInt(1, NORM_OF_CORRECT_ANSWERS)
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getInt(1) else 0
        }
    }

    override fun getSize(): Int {
        getConnection().use { connection ->
            val statement = connection.prepareStatement("SELECT COUNT(*) FROM words")
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getInt(1) else 0
        }
    }

    override fun getLearnedWords(): List<Word> {
        val words = mutableListOf<Word>()
        getConnection().use { connection ->
            val statement = connection.prepareStatement(
                "SELECT text, translate, correctAnswersCount FROM words WHERE correctAnswersCount >= ?"
            )
            statement.setInt(1, NORM_OF_CORRECT_ANSWERS)
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                words.add(
                    Word(
                        resultSet.getString("text"),
                        resultSet.getString("translate"),
                        resultSet.getInt("correctAnswersCount")
                    )
                )
            }
        }
        return words
    }

    override fun getUnlearnedWords(): List<Word> {
        val words = mutableListOf<Word>()
        getConnection().use { connection ->
            val statement = connection.prepareStatement(
                "SELECT text, translate, correctAnswersCount FROM words WHERE correctAnswersCount < ?"
            )
            statement.setInt(1, NORM_OF_CORRECT_ANSWERS)
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                words.add(
                    Word(
                        resultSet.getString("text"),
                        resultSet.getString("translate"),
                        resultSet.getInt("correctAnswersCount")
                    )
                )
            }
        }
        return words
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        getConnection().use { connection ->
            val statement = connection.prepareStatement(
                "UPDATE words SET correctAnswersCount = ? WHERE text = ?"
            )
            statement.setInt(1, correctAnswersCount)
            statement.setString(2, word)
            statement.executeUpdate()
            connection.commit()
        }
    }

    override fun resetUserProgress() {
        getConnection().use { connection ->
            val statement = connection.prepareStatement("UPDATE words SET correctAnswersCount = 0")
            statement.executeUpdate()
            connection.commit()
        }
    }

    override fun addWord(word: Word) {
        getConnection().use { connection ->
            val statement = connection.prepareStatement(
                "INSERT INTO words (text, translate, correctAnswersCount) VALUES (?, ?, ?)"
            )
            statement.setString(1, word.text)
            statement.setString(2, word.translate)
            statement.setInt(3, word.correctAnswersCount)
            statement.executeUpdate()
            connection.commit()
        }
    }
}
