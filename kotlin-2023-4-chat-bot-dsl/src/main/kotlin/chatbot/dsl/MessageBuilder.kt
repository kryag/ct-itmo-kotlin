package chatbot.dsl

import chatbot.api.Keyboard
import chatbot.api.MessageId

class MessageBuilder {
    var text: String = ""
    var keyboard: Keyboard? = null
    var replyTo: MessageId? = null

    fun removeKeyboard() {
        keyboard = Keyboard.Remove
    }

    fun withKeyboard(init: KeyboardBuilder.() -> Unit) {
        val builder = KeyboardBuilder()
        builder.init()
        keyboard = Keyboard.Markup(builder.oneTime, builder.keyboard)
    }

    fun keyboardIsEmpty(): Boolean {
        return when (keyboard) {
            is Keyboard.Markup -> (keyboard as Keyboard.Markup).keyboard.all { row ->
                row.all { button -> button.text.isEmpty() }
            }
            else -> false
        }
    }
}
