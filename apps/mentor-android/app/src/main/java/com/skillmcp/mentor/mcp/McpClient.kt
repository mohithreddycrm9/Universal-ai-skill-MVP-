package com.skillmcp.mentor.mcp

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class McpToolDescriptor(
    val name: String,
    val description: String,
    val inputSchema: String,
)

class McpClient(
    private val http: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build(),
) {
    private val json = "application/json; charset=utf-8".toMediaType()
    private val idSeq = AtomicInteger(1)

    fun listTools(endpointUrl: String, bearerToken: String): Result<List<McpToolDescriptor>> =
        runCatching {
            initialize(endpointUrl, bearerToken)
            val body = rpc("tools/list", JSONObject(), endpointUrl, bearerToken)
            val tools = body.optJSONObject("result")?.optJSONArray("tools") ?: JSONArray()
            buildList {
                for (i in 0 until tools.length()) {
                    val tool = tools.getJSONObject(i)
                    add(
                        McpToolDescriptor(
                            name = tool.optString("name"),
                            description = tool.optString("description"),
                            inputSchema = tool.optJSONObject("inputSchema")?.toString() ?: "{}",
                        ),
                    )
                }
            }
        }

    fun callTool(
        endpointUrl: String,
        bearerToken: String,
        toolName: String,
        arguments: JSONObject,
    ): Result<String> =
        runCatching {
            initialize(endpointUrl, bearerToken)
            val params =
                JSONObject()
                    .put("name", toolName)
                    .put("arguments", arguments)
            val body = rpc("tools/call", params, endpointUrl, bearerToken)
            val result = body.optJSONObject("result")
            val content = result?.optJSONArray("content")
            if (content != null && content.length() > 0) {
                content.optJSONObject(0)?.optString("text")
                    ?: content.optJSONObject(0)?.toString()
                    ?: result.toString()
            } else {
                result?.toString() ?: body.toString()
            }
        }

    private fun initialize(endpointUrl: String, bearerToken: String) {
        val params =
            JSONObject()
                .put("protocolVersion", "2024-11-05")
                .put("capabilities", JSONObject())
                .put(
                    "clientInfo",
                    JSONObject().put("name", "Universal AI").put("version", "1.0"),
                )
        rpc("initialize", params, endpointUrl, bearerToken)
        postNotification(endpointUrl, bearerToken, "notifications/initialized", JSONObject())
    }

    private fun rpc(
        method: String,
        params: JSONObject,
        endpointUrl: String = "",
        bearerToken: String = "",
    ): JSONObject {
        val url = endpointUrl.ifBlank { error("endpoint required") }
        val token = bearerToken
        val payload =
            JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", idSeq.getAndIncrement())
                .put("method", method)
                .put("params", params)
        return post(url, token, payload)
    }

    private fun postNotification(endpointUrl: String, bearerToken: String, method: String, params: JSONObject) {
        val payload =
            JSONObject()
                .put("jsonrpc", "2.0")
                .put("method", method)
                .put("params", params)
        post(endpointUrl, bearerToken, payload)
    }

    private fun post(url: String, bearerToken: String, payload: JSONObject): JSONObject {
        val builder =
            Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(json))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
        if (bearerToken.isNotBlank()) {
            builder.header("Authorization", "Bearer $bearerToken")
        }
        http.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                error("MCP HTTP ${response.code}: ${text.take(400)}")
            }
            val json = JSONObject(text)
            json.optJSONObject("error")?.let { err ->
                error(err.optString("message", err.toString()))
            }
            return json
        }
    }
}
