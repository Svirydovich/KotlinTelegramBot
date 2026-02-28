package org.example

const val BASE_URL = "https://api.telegram.org/bot"

fun main(args: Array<String>) {
    val botToken = args[0]
    val telegramBotService = TelegramBotService(botToken)
    var updateId = 0

    val updateIdRegex = "\"update_id\":\\s(\\d+)".toRegex()
    val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val chatIdRegex = "\"chat\":\\{\"id\":\\s(\\d+)".toRegex()
    val dataRegex: Regex = "\"data\":\"(.+?)\"".toRegex()

    val trainer = LearnWordsTrainer()

    while (true) {
        Thread.sleep(2000)
        val updates: String = telegramBotService.getUpdates(updateId)
        println(updates)

        val matchResult = updateIdRegex.find(updates)

        if (matchResult != null) updateId = matchResult.groups[1]?.value?.toInt()?.plus(1) ?: updateId

        val messageMatchResult: MatchResult? = messageTextRegex.find(updates)
        val chatIdMatchResult = chatIdRegex.find(updates)
        val data = dataRegex.find(updates)?.groups?.get(1)?.value

        if (messageMatchResult != null && chatIdMatchResult != null) {
            val text = messageMatchResult.groups[1]?.value
            val chatId = chatIdMatchResult.groups[1]?.value?.toLong()

            if (text != null && chatId != null) {
                println("$chatId отправил сообщение: \"$text\"")

                when {
                    text.lowercase() == "hello" -> telegramBotService.sendMessage(chatId, text)
                    text.lowercase() == "/start" -> telegramBotService.sendMenu(chatId)
                    data?.lowercase() == "statistics_clicked" -> telegramBotService.sendMessage(
                        chatId,
                        "Выучено ${trainer.getStatistics().learnedCount} из ${trainer.getStatistics().totalCount} слов | ${trainer.getStatistics().percent}%\n"
                    )
                }

            }
        }
    }
}
