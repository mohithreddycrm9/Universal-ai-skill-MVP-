package com.skillmcp.mentor.llm

/**
 * Image prepared for provider-native vision APIs (JPEG, max edge ~1024px).
 * Not embedded as fake text in the user message.
 */
data class ChatVisionAttachment(
    val jpegBase64: String,
    val mimeType: String = "image/jpeg",
)

object VisionCapabilities {
    fun supportsVision(profile: LlmProfile): Boolean =
        when (profile.kind) {
            LlmProviderKind.OPENAI_COMPAT,
            LlmProviderKind.GEMINI,
            LlmProviderKind.ANTHROPIC,
            -> true
            LlmProviderKind.HUGGING_FACE -> profile.model.contains("vision", ignoreCase = true) ||
                profile.model.contains("llava", ignoreCase = true)
            LlmProviderKind.OLLAMA -> false
        }
}
