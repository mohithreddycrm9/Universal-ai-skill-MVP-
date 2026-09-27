package com.skillmcp.mentor.llm

import com.skillmcp.mentor.mentor.ChatMessageDto
import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts one provider-neutral history (already trimmed by [ConversationContext]) into each provider's
 * wire format, so a chat can move between OpenAI-compatible, Ollama, Anthropic and Gemini models mid-way.
 */
internal object ProviderMessages {
    /** OpenAI-compatible chat/completions and Ollama /api/chat: system + alternating turns + user. */
    fun openAi(
        system: String,
        history: List<ChatMessageDto>,
        userMessage: String,
        vision: ChatVisionAttachment?,
    ): JSONArray =
        JSONArray().apply {
            if (system.isNotBlank()) put(JSONObject().put("role", "system").put("content", system))
            history.forEach {
                put(JSONObject().put("role", ConversationContext.wireRole(LlmProviderKind.OPENAI_COMPAT, it.role)).put("content", it.content))
            }
            put(VisionJson.openAiUserMessage(userMessage, vision))
        }

    /** Anthropic Messages API: system goes in the top-level field; content is a list of text blocks. */
    fun anthropic(
        history: List<ChatMessageDto>,
        userMessage: String,
        vision: ChatVisionAttachment?,
    ): JSONArray =
        JSONArray().apply {
            history.forEach {
                put(
                    JSONObject()
                        .put("role", ConversationContext.wireRole(LlmProviderKind.ANTHROPIC, it.role))
                        .put("content", JSONArray().put(JSONObject().put("type", "text").put("text", it.content))),
                )
            }
            put(JSONObject().put("role", "user").put("content", VisionJson.anthropicUserContent(userMessage, vision)))
        }

    /** Gemini generateContent: the assistant role is "model"; system goes in system_instruction. */
    fun gemini(
        history: List<ChatMessageDto>,
        userMessage: String,
        vision: ChatVisionAttachment?,
    ): JSONArray =
        JSONArray().apply {
            history.forEach {
                put(
                    JSONObject()
                        .put("role", ConversationContext.wireRole(LlmProviderKind.GEMINI, it.role))
                        .put("parts", JSONArray().put(JSONObject().put("text", it.content))),
                )
            }
            put(JSONObject().put("role", "user").put("parts", VisionJson.geminiUserParts(userMessage, vision)))
        }
}
