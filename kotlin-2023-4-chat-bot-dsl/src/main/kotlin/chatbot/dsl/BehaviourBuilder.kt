package chatbot.dsl

import chatbot.api.ChatContext

@ChatBotDSL
class BehaviourBuilder<T : ChatContext?>(
    private val contextClass: ContextClass = null,
    private val contextInstance: T? = null,
) {
    val handlers = mutableListOf<Handler<out ChatContext?>>()

    fun onCommand(command: String, handle: MessageProcessor<T>) {
        onMessagePrefix("/$command", handle)
    }

    fun onMessage(predicate: Predicate, handle: MessageProcessor<T>) {
        handlers.add(Handler(predicate, handle, contextClass, contextInstance))
    }

    fun onMessagePrefix(prefix: String, handle: MessageProcessor<T>) {
        onMessage({ it.text.startsWith(prefix) }, handle)
    }

    fun onMessageContains(text: String, handle: MessageProcessor<T>) {
        onMessage({ it.text.contains(text) }, handle)
    }

    fun onMessage(messageTextExactly: String, handle: MessageProcessor<T>) {
        onMessage({ it.text == messageTextExactly }, handle)
    }

    fun onMessage(action: MessageProcessor<T>) {
        onMessage({ true }, action)
    }

    inline fun <reified T : ChatContext> into(init: BehaviourBuilder<T>.() -> Unit) {
        val behaviour = BehaviourBuilder<T>(T::class)
        behaviour.init()
        handlers.addAll(behaviour.handlers)
    }

    inline infix fun <reified T : ChatContext> T.into(init: BehaviourBuilder<T>.() -> Unit) {
        val behaviour = BehaviourBuilder(this::class, this@into)
        behaviour.init()
        handlers.addAll(behaviour.handlers)
    }
}
