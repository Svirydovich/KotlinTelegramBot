package org.example

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val BASE_URL = "https://api.telegram.org/bot"

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
}

fun main(args: Array<String>) {
    val botToken = args[0]
    val telegramBotService = TelegramBotService(botToken)
    var updateId = 0

    val updateIdRegex = "\"update_id\":\\s(\\d+)".toRegex()
    val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val chatIdRegex = "\"chat\":\\{\"id\":\\s(\\d+)".toRegex()

    while (true) {
        Thread.sleep(2000)
        val updates: String = telegramBotService.getUpdates(updateId)
        println(updates)

        val matchResult = updateIdRegex.find(updates)

        if (matchResult != null) updateId = matchResult.groups[1]?.value?.toInt()?.plus(1) ?: updateId

        val messageMatchResult: MatchResult? = messageTextRegex.find(updates)
        val chatIdMatchResult = chatIdRegex.find(updates)

        if (messageMatchResult != null && chatIdMatchResult != null) {
            val text = messageMatchResult.groups[1]?.value
            val chatId = chatIdMatchResult.groups[1]?.value?.toLong()

            if (text != null && chatId != null) {
                println("$chatId отправил сообщение: \"$text\"")

                if (text == "Hello") telegramBotService.sendMessage(chatId, text)
            }
        }
    }
}
