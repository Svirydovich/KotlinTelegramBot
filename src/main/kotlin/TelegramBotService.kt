package org.example

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Random

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

        question.correctAnswer.imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                sendPhoto(file, chatId, json, hasSpoiler = true, word = question.correctAnswer)
            }
        }

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

    fun sendPhoto(file: File?, chatId: Long, json: Json, hasSpoiler: Boolean, word: Word): String? {
        word.imageFileId?.let { existingFileId ->
            val urlSendPhoto = "$BASE_URL$botToken/sendPhoto"
            val requestBody = mapOf(
                "chat_id" to chatId.toString(),
                "photo" to existingFileId
            )
            val jsonBody = json.encodeToString(requestBody)
            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlSendPhoto))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val sendPhotoResponse = json.decodeFromString<SendPhotoResponse>(response.body())
            return sendPhotoResponse.result?.photo?.lastOrNull()?.fileId
        }

        file?.exists()?.let { if (!it) return null }

        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["photo"] = file as Any
        data["has_spoiler"] = hasSpoiler

        val boundary: String = BigInteger(35, Random()).toString()

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASE_URL$botToken/sendPhoto"))
            .postMultipartFormData(boundary, data)
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val sendPhotoResponse = json.decodeFromString<SendPhotoResponse>(response.body())

        val fileId = sendPhotoResponse.result?.photo?.lastOrNull()?.fileId
        if (fileId != null) {
            word.imageFileId = fileId
        }

        return fileId
    }

    private fun HttpRequest.Builder.postMultipartFormData(
        boundary: String,
        data: Map<String, Any>
    ): HttpRequest.Builder {
        val byteArrays = ArrayList<ByteArray>()
        val separator = "--$boundary\r\nContent-Disposition: form-data; name=".toByteArray(StandardCharsets.UTF_8)

        for (entry in data.entries) {
            byteArrays.add(separator)
            when (entry.value) {
                is File -> {
                    val file = entry.value as File
                    val path = Path.of(file.toURI())
                    val mimeType = Files.probeContentType(path)
                    byteArrays.add(
                        "\"${entry.key}\"; filename=\"${path.fileName}\"\r\nContent-Type: $mimeType\r\n\r\n".toByteArray(
                            StandardCharsets.UTF_8
                        )
                    )
                    byteArrays.add(Files.readAllBytes(path))
                    byteArrays.add("\r\n".toByteArray(StandardCharsets.UTF_8))
                }

                else -> byteArrays.add("\"${entry.key}\"\r\n\r\n${entry.value}\r\n".toByteArray(StandardCharsets.UTF_8))
            }
        }
        byteArrays.add("--$boundary--".toByteArray(StandardCharsets.UTF_8))

        this.header("Content-Type", "multipart/form-data;boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
        return this
    }

}
