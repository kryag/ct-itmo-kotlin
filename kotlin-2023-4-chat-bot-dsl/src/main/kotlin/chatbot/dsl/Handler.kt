package chatbot.dsl

import chatbot.api.*
import kotlin.reflect.KClass

@ChatBotDSL
data class Handler<T : ChatContext?>(
    private val predicate: Predicate,
    private val handle: MessageProcessor<T>,
    private val contextClass: ContextClass,
    private val contextInstance: T?,
) {
    fun canHandle(
        chatBot: ChatBot,
        contextClass: ContextClass,
        contextInstance: ChatContext?,
        message: Message,
    ): Boolean {
        return this.contextClass == contextClass &&
            (this.contextInstance == null || this.contextInstance == (contextInstance as T)) &&
            chatBot.predicate(message)
    }

    fun handle(message: Message, client: Client, contextManager: ChatContextsManager?) {
        val context = contextManager?.getContext(message.chatId) as T
        MessageProcessorContext(message, client, context) {
            contextManager?.setContext(message.chatId, it)
        }.handle()
    }
}

typealias Predicate = ChatBot.(Message) -> Boolean
typealias ContextClass = KClass<out ChatContext>?
