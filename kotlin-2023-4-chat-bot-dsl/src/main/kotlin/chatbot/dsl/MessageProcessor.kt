package chatbot.dsl

import chatbot.api.ChatContext
import chatbot.api.ChatId
import chatbot.api.Client
import chatbot.api.Message

@ChatBotDSL
class MessageProcessorContext<C : ChatContext?>(
    val message: Message,
    val client: Client,
    val context: C,
    val setContext: (c: ChatContext?) -> Unit,
) {
    fun sendMessage(chatId: ChatId, init: MessageBuilder.() -> Unit) {
        val builder = MessageBuilder()
        builder.init()
        when {
            builder.text.isEmpty() && builder.keyboard == null -> return
            builder.keyboardIsEmpty() -> return
            else -> client.sendMessage(chatId, builder.text, builder.keyboard, builder.replyTo)
        }
    }
}

typealias MessageProcessor<C> = MessageProcessorContext<C>.() -> Unit
