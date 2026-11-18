package com.cairn.emergency.ai

import com.google.firebase.ai.FirebaseAI
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import kotlinx.coroutines.withTimeout

/** The single entry point for model calls. The SDK response reports which engine actually ran. */
@OptIn(PublicPreviewAPI::class)
class AiRouter(private val model: GenerativeModel) {
    suspend fun generate(prompt: String): RoutedAnswer = withTimeout(30_000) {
        val response = model.generateContent(prompt)
        val source = response.inferenceSource.toString()
        RoutedAnswer(
            text = response.text.orEmpty(),
            engine = if (source.contains("ON_DEVICE", ignoreCase = true)) Engine.LOCAL else Engine.CLOUD,
        )
    }

    companion object {
        @OptIn(PublicPreviewAPI::class)
        fun create(): AiRouter {
            val model = FirebaseAI.getInstance(FirebaseApp.getInstance(), GenerativeBackend.googleAI()).generativeModel(
                modelName = "gemini-3.8-flash",
                onDeviceConfig = OnDeviceConfig(mode = InferenceMode.PREFER_ON_DEVICE),
            )
            return AiRouter(model)
        }
    }
}

enum class Engine { LOCAL, CLOUD }
data class RoutedAnswer(val text: String, val engine: Engine)
