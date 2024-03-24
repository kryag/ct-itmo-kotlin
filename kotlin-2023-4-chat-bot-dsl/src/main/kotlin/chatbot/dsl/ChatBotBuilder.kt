package chatbot.dsl

import chatbot.api.*

@ChatBotDSL
class ChatBotBuilder(private val client: Client) {
    private var logLevel: LogLevel = LogLevel.ERROR
    private var behaviour: BehaviourBuilder<ChatContext?> = BehaviourBuilder()
    var contextManager: ChatContextsManager? = null

    fun use(logLevel: LogLevel) {
        this.logLevel = logLevel
    }

    fun use(contextManager: ChatContextsManager) {
        this.contextManager = contextManager
    }

    operator fun LogLevel.unaryPlus() {
        logLevel = this
    }

    fun build(): ChatBot {
        return object : ChatBot {
            override fun processMessages(message: Message) {
                val context = contextManager?.getContext(message.chatId)
                val contextClass = context?.let { it::class }
                behaviour.handlers.firstOrNull {
                    it.canHandle(this, contextClass, context, message)
                }?.handle(message, client, contextManager)
            }

            override val logLevel: LogLevel
                get() = this@ChatBotBuilder.logLevel
        }
    }

    fun behaviour(init: BehaviourBuilder<ChatContext?>.() -> Unit) = behaviour.init()
}

fun chatBot(client: Client, init: ChatBotBuilder.() -> Unit): ChatBot {
    val builder = ChatBotBuilder(client)
    builder.init()
    return builder.build()
}
