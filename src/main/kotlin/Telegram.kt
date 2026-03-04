package org.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

const val MENU = "/start"
const val HELLO = "Hello"
const val RESET_CLICKED = "reset_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
)

@Serializable
data class Response(
    val ok: Boolean,
    @SerialName("result")
    val result: List<Update>? = null,
    @SerialName("error_code")
    val errorCode: Int? = null,
    val description: String? = null,
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>,
)

@Serializable
data class InlineKeyboard(
    @SerialName("callback_data")
    val callbackData: String,
    @SerialName("text")
    val text: String,
)

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0L
    val telegramBotService = TelegramBotService(botToken)

    val json = Json { ignoreUnknownKeys = true }

    val trainers = HashMap<Long, LearnWordsTrainer>()

    while (true) {
        Thread.sleep(2000)
        val responseString: String = telegramBotService.getUpdates(updateId)
        println(responseString)
        val response: Response = json.decodeFromString(responseString)
        if (response.result.isNullOrEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdates(telegramBotService, it, json, trainers) }
        updateId = sortedUpdates.lastOrNull()?.updateId ?: updateId
    }
}

fun handleUpdates(
    telegramBotService: TelegramBotService,
    update: Update,
    json: Json,
    trainers: HashMap<Long, LearnWordsTrainer>
) {
    val messageMatchResult = update.message?.text
    val chatIdMatchResult = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val data = update.callbackQuery?.data

    val trainer = trainers.getOrPut(chatIdMatchResult) { LearnWordsTrainer("$chatIdMatchResult.txt") }

    val statistics = trainer.getStatistics()

    if (messageMatchResult.equals(HELLO, ignoreCase = true)) {
        telegramBotService.sendMessage(json, chatIdMatchResult, HELLO)
    }

    if (messageMatchResult?.lowercase() == MENU) {
        telegramBotService.sendMenu(json, chatIdMatchResult)
    }

    if (data?.lowercase() == STATISTICS_CLICKED) telegramBotService.sendMessage(
        json,
        chatIdMatchResult,
        "Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}%"
    )

    if (data?.lowercase() == LEARN_WORDS_CLICKED) {
        trainer.checkNextQuestionAndSend(json, telegramBotService, chatIdMatchResult)
    }

    if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
        val userAnswerIndex = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull()

        if (userAnswerIndex != null) {
            val isCorrect = trainer.checkAnswer(userAnswerIndex)

            if (isCorrect) telegramBotService.sendMessage(json, chatIdMatchResult, "Правильно!")
            else {
                val correctAnswer = trainer.question?.correctAnswer
                val correctAnswerText = correctAnswer?.translate ?: "В словаре нет перевода"
                telegramBotService.sendMessage(
                    json,
                    chatIdMatchResult,
                    "Неправильно! ${correctAnswer?.text} – это $correctAnswerText"
                )
            }

            trainer.checkNextQuestionAndSend(json, telegramBotService, chatIdMatchResult)
        }
    }

    if (data == RESET_CLICKED) {
        trainer.resetProgress()
        telegramBotService.sendMessage(json, chatIdMatchResult, "Прогресс сброшен")
    }
}
