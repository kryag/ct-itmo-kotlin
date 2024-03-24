package chatbot.dsl

import chatbot.api.Keyboard

@ChatBotDSL
class KeyboardBuilder {
    var oneTime: Boolean = false
    var keyboard: MutableList<MutableList<Keyboard.Button>> = mutableListOf()

    inner class KeyboardRowBuilder {
        private val row: MutableList<Keyboard.Button> = mutableListOf()

        fun button(text: String) {
            row.add(Keyboard.Button(text))
        }

        operator fun String.unaryMinus() {
            row.add(Keyboard.Button(this))
        }

        fun build() {
            keyboard.add(row)
        }
    }

    infix fun row(init: KeyboardRowBuilder.() -> Unit) {
        val rowBuilder = KeyboardRowBuilder()
        rowBuilder.init()
        rowBuilder.build()
    }
}
