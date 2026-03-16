package org.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

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
data class Document(
    @SerialName("file_name")
    val fileName: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
)

@Serializable
data class SendResponse(
    val ok: Boolean,
    val result: Message? = null
)

@Serializable
data class PhotoSize(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
    val width: Int,
    val height: Int
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String? = null,
    @SerialName("chat")
    val chat: Chat,
    @SerialName("document")
    val document: Document? = null,
    @SerialName("message_id")
    val messageId: Long,
    val photo: List<PhotoSize>? = null,
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

@Serializable
data class GetFileRequest(
    @SerialName("file_id")
    val fileId: String
)

@Serializable
data class GetFileResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("result")
    val result: TelegramFile? = null,
)

@Serializable
data class TelegramFile(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
    @SerialName("file_path")
    val filePath: String,
)

fun main(args: Array<String>) {
    val botToken = args[0]
    var updateId = 0L
    val telegramBotService = TelegramBotService(botToken)
    val dynamicMessage = DynamicMessage(telegramBotService)

    val json = Json { ignoreUnknownKeys = true }

    val trainers = HashMap<Long, LearnWordsTrainer>()


    while (true) {
        Thread.sleep(2000)
        val responseString: String = telegramBotService.getUpdates(updateId + 1)
        println(responseString)
        val response: Response = json.decodeFromString(responseString)
        if (response.result.isNullOrEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdates(telegramBotService, it, json, trainers, dynamicMessage) }
        updateId = sortedUpdates.lastOrNull()?.updateId ?: updateId
    }
}

fun statsText(statistics: Statistics) =
    "Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}%"

fun handleUpdates(
    telegramBotService: TelegramBotService,
    update: Update,
    json: Json,
    trainers: HashMap<Long, LearnWordsTrainer>,
    dynamicMessage: DynamicMessage
) {
    val messageMatchResult = update.message?.text
    val chatIdMatchResult = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val data = update.callbackQuery?.data
    val document = update.message?.document

    val trainer = trainers.getOrPut(chatIdMatchResult) { LearnWordsTrainer("$chatIdMatchResult.txt") }

    val statistics = trainer.getStatistics()

    if (messageMatchResult.equals(HELLO, ignoreCase = true)) {
        telegramBotService.sendMessage(json, chatIdMatchResult, HELLO)
    }

    if (messageMatchResult?.lowercase() == MENU || data == MENU) {
        telegramBotService.sendMenu(json, chatIdMatchResult)
    }

    if (data?.lowercase() == STATISTICS_CLICKED) {
        val responseString = telegramBotService.sendMessage(json, chatIdMatchResult, statsText(statistics))
        val sendPhotoResponse = json.decodeFromString<SendResponse>(responseString)
        sendPhotoResponse.result?.messageId?.let { messageId ->
            dynamicMessage.setMessageId(chatIdMatchResult, messageId, statsText(statistics))
        }
    }

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
            val updatedStatistics = trainer.getStatistics()
            dynamicMessage.updateMessage(json, chatIdMatchResult, statsText(updatedStatistics))

            trainer.checkNextQuestionAndSend(json, telegramBotService, chatIdMatchResult)
        }
    }

    if (data == RESET_CLICKED) {
        trainer.resetProgress()
        telegramBotService.sendMessage(json, chatIdMatchResult, "Прогресс сброшен")
    }

    if (document != null) {
        val jsonResponse = telegramBotService.getFile(document.fileId, json)
        val response: GetFileResponse = json.decodeFromString(jsonResponse)
        response.result?.let { it ->
            val downloadedFile = telegramBotService.downloadFile(it.filePath, it.fileUniqueId)

            val newWords = mutableListOf<Word>()
            for (word in File(downloadedFile).readLines()) {
                val parts = word.split("|")
                newWords.add(
                    Word(
                        parts[0],
                        parts[1],
                        parts[2].toIntOrNull() ?: 0,
                        parts.getOrNull(3)?.ifEmpty { null },
                        parts.getOrNull(4)?.ifEmpty { null }
                    )
                )
            }

            newWords.forEach { word ->
                if (trainer.dictionary.none { it.text == word.text }) {
                    trainer.dictionary.add(word)
                }
            }

            trainer.saveDictionary()
        }
    }
}
