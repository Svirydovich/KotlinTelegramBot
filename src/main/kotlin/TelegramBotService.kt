package org.example

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val BASE_URL = "https://api.telegram.org/bot"
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "statistics_clicked"
const val BOT_FILE_URL = "https://api.telegram.org/file/bot/"

class TelegramBotService(val botToken: String) {
    val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Long): String {
        val urlGetUpdates = "$BASE_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(json: Json, chatId: Long, text: String): String {
        val urlSendMessage = "$BASE_URL$botToken/sendMessage"
        val requestBody = SendMessageRequest(chatId, text)

        val requestBodyString = json.encodeToString(requestBody)

        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }

    fun sendMenu(json: Json, chatId: Long): String {
        val urlSendMessage = "$BASE_URL$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId,
            "Основное меню",
            ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboard(LEARN_WORDS_CLICKED, "Изучать слова"),
                        InlineKeyboard(STATISTICS_CLICKED, "Статистика"),
                    ),
                    listOf(InlineKeyboard(RESET_CLICKED, "Сбросить прогресс")),
                )
            ),
        )

        val requestBodyString = json.encodeToString(requestBody)

        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }

    fun sendQuestion(json: Json, chatId: Long, question: Question): String {
        val urlSendMessage = "$BASE_URL$botToken/sendMessage"

        val requestBody = SendMessageRequest(
            chatId,
            question.correctAnswer.text,
            ReplyMarkup(
                listOf(
                    question.variants.mapIndexed { index, word ->
                        InlineKeyboard("$CALLBACK_DATA_ANSWER_PREFIX$index", word.translate)
                    },
                    listOf(InlineKeyboard(MENU, "Выйти в меню")),
                )
            )
        )

        val requestBodyString = json.encodeToString(requestBody)

        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(urlSendMessage))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }

    fun getFile(fileId: String, json: Json): String {
        val urlGetFile = "$BASE_URL$botToken/getFile"
        val requestBody = GetFileRequest(fileId = fileId)
        val requestBodyString = json.encodeToString(requestBody)
        val client = this.client
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(urlGetFile))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        )
        return response.body()
    }

    fun downloadFile(filePath: String, fileName: String): String {
        val urlGetFile = "$BOT_FILE_URL$botToken/$filePath"
        val request = HttpRequest
            .newBuilder()
            .uri(URI.create(urlGetFile))
            .GET()
            .build()

        val response: HttpResponse<InputStream> = HttpClient
            .newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofInputStream())

        val outputFile = File(fileName)
        response.body().use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output, 16 * 1024)
            }
        }

        return outputFile.absolutePath
    }
}
