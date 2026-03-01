package org.example

const val MENU = "/start"
const val HELLO = "Hello"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

fun main(args: Array<String>) {
    val botToken = args[0]
    val telegramBotService = TelegramBotService(botToken)
    var updateId = 0

    val updateIdRegex = "\"update_id\":\\s*(\\d+)".toRegex()
    val messageTextRegex = "\"text\":\"(.+?)\"".toRegex()
    val chatIdRegex = "\"chat\":\\{\"id\":\\s*(\\d+)".toRegex()
    val dataRegex = "\"data\":\"(.+?)\"".toRegex()

    val trainer = LearnWordsTrainer()
    val statistics = trainer.getStatistics()

    while (true) {
        Thread.sleep(2000)
        val updates: String = telegramBotService.getUpdates(updateId)
        println(updates)

        val matchResult = updateIdRegex.find(updates)

        val receivedUpdateId = matchResult?.groups[1]?.value?.toIntOrNull() ?: continue
        updateId = receivedUpdateId + 1

        val messageMatchResult = messageTextRegex.find(updates)?.groups?.get(1)?.value
        val chatIdMatchResult = chatIdRegex.find(updates)?.groups?.get(1)?.value?.toLong()
        val data = dataRegex.find(updates)?.groups?.get(1)?.value

        if (messageMatchResult.equals(HELLO, ignoreCase = true) && chatIdMatchResult != null) {
            telegramBotService.sendMessage(chatIdMatchResult, HELLO)
        }

        if (messageMatchResult?.lowercase() == MENU && chatIdMatchResult != null) {
            telegramBotService.sendMenu(chatIdMatchResult)
        }

        if (data?.lowercase() == STATISTICS_CLICKED && chatIdMatchResult != null) telegramBotService.sendMessage(
            chatIdMatchResult,
            "Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}%"
        )

        if (data?.lowercase() == LEARN_WORDS_CLICKED && chatIdMatchResult != null) {
            trainer.checkNextQuestionAndSend(trainer, telegramBotService, chatIdMatchResult)
        }
    }
}
