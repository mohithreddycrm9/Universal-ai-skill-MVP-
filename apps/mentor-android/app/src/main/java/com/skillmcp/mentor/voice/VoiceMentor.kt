package com.skillmcp.mentor.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class VoiceMentor(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var ttsReady = CompletableDeferred<Boolean>()

    suspend fun ensureTts(): Boolean =
        withContext(Dispatchers.Main) {
            if (tts != null) return@withContext ttsReady.await()
            ttsReady = CompletableDeferred()
            tts =
                TextToSpeech(context) { status ->
                    ttsReady.complete(status == TextToSpeech.SUCCESS)
                }
            ttsReady.await()
        }

    suspend fun setLocale(localeTag: String) {
        withContext(Dispatchers.Main) {
            if (!ensureTts()) return@withContext
            val locale = Locale.forLanguageTag(localeTag)
            tts?.language = locale
        }
    }

    suspend fun speak(
        text: String,
        localeTag: String,
        elevenLabs: ElevenLabsVoiceClient? = null,
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val played =
            elevenLabs?.speak(trimmed, localeTag)
                ?: run {
                    withContext(Dispatchers.Main) {
                        if (!ensureTts()) return@withContext false
                        setLocale(localeTag)
                        val utteranceId = UUID.randomUUID().toString()
                        val done = CompletableDeferred<Boolean>()
                        tts?.setOnUtteranceProgressListener(
                            object : UtteranceProgressListener() {
                                override fun onStart(utteranceId: String?) = Unit

                                override fun onDone(utteranceId: String?) {
                                    done.complete(true)
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onError(utteranceId: String?) {
                                    done.complete(false)
                                }
                            },
                        )
                        tts?.speak(trimmed, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                        done.await()
                    }
                    true
                }
        if (!played) {
            withContext(Dispatchers.Main) {
                ensureTts()
                setLocale(localeTag)
                tts?.speak(trimmed, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
            }
        }
    }

    suspend fun listenOnce(localeTag: String): Result<String> =
        withContext(Dispatchers.Main) {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                return@withContext Result.failure(IllegalStateException("Speech recognition unavailable"))
            }
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val result = CompletableDeferred<Result<String>>()
            val intent =
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
            recognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onError(error: Int) {
                        result.complete(Result.failure(IllegalStateException("Speech error $error")))
                        recognizer.destroy()
                    }

                    override fun onResults(results: Bundle?) {
                        val text =
                            results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                ?.trim()
                        if (text.isNullOrEmpty()) {
                            result.complete(Result.failure(IllegalStateException("No speech heard")))
                        } else {
                            result.complete(Result.success(text))
                        }
                        recognizer.destroy()
                    }

                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                },
            )
            recognizer.startListening(intent)
            result.await()
        }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
