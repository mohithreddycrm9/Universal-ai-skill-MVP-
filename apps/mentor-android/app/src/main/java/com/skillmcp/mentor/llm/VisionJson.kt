package com.skillmcp.mentor.llm

import org.json.JSONArray
import org.json.JSONObject

internal object VisionJson {
    fun openAiUserMessage(text: String, vision: ChatVisionAttachment?): JSONObject {
        if (vision == null) {
            return JSONObject().put("role", "user").put("content", text)
        }
        val parts =
            JSONArray().apply {
                put(JSONObject().put("type", "text").put("text", text))
                put(
                    JSONObject()
                        .put("type", "image_url")
                        .put(
                            "image_url",
                            JSONObject().put("url", "data:${vision.mimeType};base64,${vision.jpegBase64}"),
                        ),
                )
            }
        return JSONObject().put("role", "user").put("content", parts)
    }

    fun anthropicUserContent(text: String, vision: ChatVisionAttachment?): JSONArray {
        val arr = JSONArray()
        if (vision != null) {
            arr.put(
                JSONObject()
                    .put("type", "image")
                    .put(
                        "source",
                        JSONObject()
                            .put("type", "base64")
                            .put("media_type", vision.mimeType)
                            .put("data", vision.jpegBase64),
                    ),
            )
        }
        arr.put(JSONObject().put("type", "text").put("text", text))
        return arr
    }

    fun geminiUserParts(text: String, vision: ChatVisionAttachment?): JSONArray {
        val parts = JSONArray()
        if (vision != null) {
            parts.put(
                JSONObject()
                    .put("inline_data", JSONObject().put("mime_type", vision.mimeType).put("data", vision.jpegBase64)),
            )
        }
        parts.put(JSONObject().put("text", text))
        return parts
    }
}
