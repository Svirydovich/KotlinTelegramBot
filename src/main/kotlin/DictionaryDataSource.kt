package org.example

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement
import java.sql.Timestamp

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

class DatabaseUserDictionary(private val dbPath: String = "data.db", private val chatId: Long) : IUserDictionary {

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
            statement.executeUpdate("DROP TABLE IF EXISTS user_answers")
            statement.executeUpdate(
                """
                    CREATE TABLE IF NOT EXISTS user_answers (
                    user_id INTEGER,
                    word_id INTEGER,
                    correct_answer_count INTEGER,
                    updated_at TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (word_id) REFERENCES words(id),
                    UNIQUE(user_id, word_id)
                );
                """.trimIndent()
            )
            connection.commit()
        }
    }

    private fun getOrCreateUser(): Long {
        getConnection().use { connection ->
            val selectStmt = connection.prepareStatement("SELECT id FROM users WHERE chat_id = ?")
            selectStmt.setLong(1, chatId)
            val resultSet = selectStmt.executeQuery()
            if (resultSet.next()) {
                return resultSet.getLong("id")
            }

            val insertStmt = connection.prepareStatement(
                "INSERT INTO users (username, created_at, chat_id) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            insertStmt.setString(1, "user_$chatId")
            insertStmt.setTimestamp(2, Timestamp(System.currentTimeMillis()))
            insertStmt.setLong(3, chatId)
            insertStmt.executeUpdate()

            val generatedKeys = insertStmt.generatedKeys
            val newUserId =
                if (generatedKeys.next()) generatedKeys.getLong(1) else throw SQLException("Не удалось создать пользователя")
            connection.commit()
            return newUserId
        }
    }

    override fun getNumOfLearnedWords(): Int {
        val userId = getOrCreateUser()
        getConnection().use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM user_answers WHERE user_id = ? AND correct_answer_count >= ?"
            )
            stmt.setLong(1, userId)
            stmt.setInt(2, NORM_OF_CORRECT_ANSWERS)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getInt(1) else 0
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
        val userId = getOrCreateUser()
        val words = mutableListOf<Word>()
        getConnection().use { connection ->
            val stmt = connection.prepareStatement(
                """
            SELECT w.text, w.translate, 
                   COALESCE(ua.correct_answer_count, 0) as correct_count
            FROM words w
            LEFT JOIN user_answers ua
                   ON w.id = ua.word_id AND ua.user_id = ?
            WHERE COALESCE(ua.correct_answer_count, 0) >= ?
            """
            )
            stmt.setLong(1, userId)
            stmt.setInt(2, NORM_OF_CORRECT_ANSWERS)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                words.add(
                    Word(
                        rs.getString("text"),
                        rs.getString("translate"),
                        rs.getInt("correct_count")
                    )
                )
            }
        }
        return words
    }

    override fun getUnlearnedWords(): List<Word> {
        val userId = getOrCreateUser()
        val words = mutableListOf<Word>()
        getConnection().use { connection ->
            val stmt = connection.prepareStatement(
                """
            SELECT w.text, w.translate, 
                   COALESCE(ua.correct_answer_count, 0) as correct_count
            FROM words w
            LEFT JOIN user_answers ua
                   ON w.id = ua.word_id AND ua.user_id = ?
            WHERE COALESCE(ua.correct_answer_count, 0) < ?
            """
            )
            stmt.setLong(1, userId)
            stmt.setInt(2, NORM_OF_CORRECT_ANSWERS)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                words.add(
                    Word(
                        rs.getString("text"),
                        rs.getString("translate"),
                        rs.getInt("correct_count")
                    )
                )
            }
        }
        return words
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        val userId = getOrCreateUser()
        getConnection().use { connection ->
            val selectWordStmt = connection.prepareStatement(
                "SELECT id FROM words WHERE text = ?"
            )
            selectWordStmt.setString(1, word)
            val rs = selectWordStmt.executeQuery()
            if (!rs.next()) {
                println("Слово '$word' не найдено в словаре")
                return
            }
            val wordId = rs.getLong("id")

            val insertOrUpdateStmt = connection.prepareStatement(
                """
            INSERT INTO user_answers (user_id, word_id, correct_answer_count, updated_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(user_id, word_id) DO UPDATE SET 
                correct_answer_count = excluded.correct_answer_count,
                updated_at = CURRENT_TIMESTAMP
            """.trimIndent()
            )
            insertOrUpdateStmt.setLong(1, userId)
            insertOrUpdateStmt.setLong(2, wordId)
            insertOrUpdateStmt.setInt(3, correctAnswersCount)
            insertOrUpdateStmt.executeUpdate()

            connection.commit()
        }
    }

    override fun resetUserProgress() {
        val userId = getOrCreateUser()
        getConnection().use { connection ->
            val stmt = connection.prepareStatement(
                "UPDATE user_answers SET correct_answer_count = 0, updated_at = ? WHERE user_id = ?"
            )
            stmt.setTimestamp(1, Timestamp(System.currentTimeMillis()))
            stmt.setLong(2, userId)
            stmt.executeUpdate()
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
