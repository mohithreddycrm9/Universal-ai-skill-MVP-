package com.skillmcp.mentor.llm

enum class LlmSignInMethod(val label: String) {
    API_KEY("API key"),
    GOOGLE("Google"),
    EMAIL("Email & password"),
    PHONE("Mobile number"),
}

data class LlmSignInDestination(
    val method: LlmSignInMethod,
    val url: String,
    val instructions: String,
)

data class LlmProviderConnectInfo(
    val kind: LlmProviderKind,
    val headline: String,
    val apiKeyHint: String,
    val apiKeyPlaceholder: String,
    val signInMethods: List<LlmSignInMethod>,
    val signInDestinations: List<LlmSignInDestination>,
    val requiresApiKey: Boolean,
)

fun connectInfoFor(kind: LlmProviderKind): LlmProviderConnectInfo =
    when (kind) {
        LlmProviderKind.OPENAI_COMPAT ->
            LlmProviderConnectInfo(
                kind = kind,
                headline = "Connect with your OpenAI account or API key",
                apiKeyHint = "Create a secret key at platform.openai.com and paste it here. Keys stay encrypted on device.",
                apiKeyPlaceholder = "sk-…",
                signInMethods =
                    listOf(
                        LlmSignInMethod.API_KEY,
                        LlmSignInMethod.EMAIL,
                        LlmSignInMethod.GOOGLE,
                        LlmSignInMethod.PHONE,
                    ),
                signInDestinations =
                    listOf(
                        LlmSignInDestination(
                            LlmSignInMethod.EMAIL,
                            "https://platform.openai.com/login",
                            "Sign in with email, then open API keys and paste a new secret key below.",
                        ),
                        LlmSignInDestination(
                            LlmSignInMethod.GOOGLE,
                            "https://platform.openai.com/api-keys",
                            "Sign in with Google on the website, create a key, then paste it below.",
                        ),
                        LlmSignInDestination(
                            LlmSignInMethod.PHONE,
                            "https://platform.openai.com/signup",
                            "Create an account with your mobile number on the website, then add an API key below.",
                        ),
                    ),
                requiresApiKey = true,
            )
        LlmProviderKind.GEMINI ->
            LlmProviderConnectInfo(
                kind = kind,
                headline = "Connect Google AI (Gemini)",
                apiKeyHint = "Use an API key from Google AI Studio, or sign in with Google and create one in the browser.",
                apiKeyPlaceholder = "AIza…",
                signInMethods = listOf(LlmSignInMethod.API_KEY, LlmSignInMethod.GOOGLE, LlmSignInMethod.EMAIL),
                signInDestinations =
                    listOf(
                        LlmSignInDestination(
                            LlmSignInMethod.GOOGLE,
                            "https://aistudio.google.com/apikey",
                            "Sign in with Google, create an API key, then paste it below.",
                        ),
                        LlmSignInDestination(
                            LlmSignInMethod.EMAIL,
                            "https://accounts.google.com/signin",
                            "Sign in with your Google email, open AI Studio, create a key, then paste below.",
                        ),
                    ),
                requiresApiKey = true,
            )
        LlmProviderKind.ANTHROPIC ->
            LlmProviderConnectInfo(
                kind = kind,
                headline = "Connect Anthropic Claude",
                apiKeyHint = "API keys are created in the Anthropic Console after you log in.",
                apiKeyPlaceholder = "sk-ant-…",
                signInMethods = listOf(LlmSignInMethod.API_KEY, LlmSignInMethod.EMAIL, LlmSignInMethod.GOOGLE),
                signInDestinations =
                    listOf(
                        LlmSignInDestination(
                            LlmSignInMethod.EMAIL,
                            "https://console.anthropic.com/login",
                            "Sign in with email, open API keys, then paste your key below.",
                        ),
                        LlmSignInDestination(
                            LlmSignInMethod.GOOGLE,
                            "https://console.anthropic.com/settings/keys",
                            "Use Google SSO on the console if available, then paste your API key below.",
                        ),
                    ),
                requiresApiKey = true,
            )
        LlmProviderKind.HUGGING_FACE ->
            LlmProviderConnectInfo(
                kind = kind,
                headline = "Connect Hugging Face",
                apiKeyHint = "Use a read token with Inference Providers permission, or sign in on the Hub and create one.",
                apiKeyPlaceholder = "hf_…",
                signInMethods =
                    listOf(
                        LlmSignInMethod.API_KEY,
                        LlmSignInMethod.GOOGLE,
                        LlmSignInMethod.EMAIL,
                        LlmSignInMethod.PHONE,
                    ),
                signInDestinations =
                    listOf(
                        LlmSignInDestination(
                            LlmSignInMethod.GOOGLE,
                            "https://huggingface.co/login",
                            "Continue with Google on Hugging Face, then create an access token and paste it below.",
                        ),
                        LlmSignInDestination(
                            LlmSignInMethod.EMAIL,
                            "https://huggingface.co/join",
                            "Create an account with email, then open Settings → Access Tokens.",
                        ),
                        LlmSignInDestination(
                            LlmSignInMethod.PHONE,
                            "https://huggingface.co/join",
                            "Sign up on the website (mobile-friendly), then paste your hf_ token below.",
                        ),
                    ),
                requiresApiKey = true,
            )
        LlmProviderKind.OLLAMA ->
            LlmProviderConnectInfo(
                kind = kind,
                headline = "Connect local Ollama",
                apiKeyHint = "No API key needed. Point to your Ollama host (emulator: 10.0.2.2:11434).",
                apiKeyPlaceholder = "",
                signInMethods = listOf(LlmSignInMethod.API_KEY),
                signInDestinations = emptyList(),
                requiresApiKey = false,
            )
    }

fun LlmProfile.isConfigured(): Boolean =
    when (kind) {
        LlmProviderKind.OLLAMA -> baseUrl.isNotBlank() && model.isNotBlank()
        else -> apiKey.isNotBlank()
    }

fun LlmProfile.connectionLabel(): String =
    when {
        !isConfigured() -> "Setup required"
        linkedAccount.isNotBlank() -> "Signed in · ${linkedAccount.take(32)}"
        kind == LlmProviderKind.OLLAMA -> "Local · ready"
        else -> "API key saved"
    }
