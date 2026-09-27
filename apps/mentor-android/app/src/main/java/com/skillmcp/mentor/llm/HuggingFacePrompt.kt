package com.skillmcp.mentor.llm

import com.skillmcp.mentor.mentor.ChatMessageDto

object HuggingFacePrompt {
    fun toServerlessPrompt(
        system: String,
        history: List<ChatMessageDto>,
        userMessage: String,
    ): String =
        buildString {
            if (system.isNotBlank()) {
                append("<<SYS>>\n")
                append(system.trim())
                append("\n<</SYS>>\n\n")
            }
            history.forEach { msg ->
                when (msg.role) {
                    "assistant" -> {
                        append("Assistant: ")
                        append(msg.content.trim())
                        append("\n\n")
                    }
                    else -> {
                        append("User: ")
                        append(msg.content.trim())
                        append("\n\n")
                    }
                }
            }
            append("User: ")
            append(userMessage.trim())
            append("\n\nAssistant:")
        }
}
