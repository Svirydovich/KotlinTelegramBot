package org.example

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val BASE_URL = "https://api.telegram.org/bot"
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "statistics_clicked"

class TelegramBotService(val botToken: String) {
    val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Int): String {
        val urlGetUpdates = "$BASE_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(chatId: Long, text: String): String {
        val urlSendMessage = "$BASE_URL$botToken/sendMessage"
        val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val params = "chat_id=$chatId&text=$encodedText"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create("$urlSendMessage?$params")).build()
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }

    fun sendMenu(chatId: Long): String {
        val urlSendMessage = "$BASE_URL$botToken/sendMessage"
        val sendMenuBody = """
            {
                    "chat_id": $chatId,
                    "text": "Основное меню",
                    "reply_markup": {
                        "inline_keyboard": [
                            [
                                {
                                    "text": "Изучать слова",
                                    "callback_data": "$LEARN_WORDS_CLICKED"
                                }, 
                                {
                                    "text": "Статистика",
                                    "callback_data": "$STATISTICS_CLICKED"
                                }
                            ]
                        ]
                    }
            }
        """.trimIndent()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }
}
