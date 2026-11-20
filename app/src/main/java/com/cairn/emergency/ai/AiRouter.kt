package com.cairn.emergency.ai

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.InferenceSource
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.OnDeviceModelStatus
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import kotlinx.coroutines.withTimeout

@OptIn(PublicPreviewAPI::class)
class AiRouter(private val model: GenerativeModel) {
    suspend fun generate(prompt: String): RoutedAnswer = withTimeout(30_000) {
        val response = model.generateContent(prompt)
        val engine = when (response.inferenceSource) {
            InferenceSource.ON_DEVICE -> Engine.LOCAL
            InferenceSource.IN_CLOUD -> Engine.CLOUD
            else -> null
        }
        RoutedAnswer(response.text.orEmpty(), engine)
    }

    companion object {
        private const val CloudModel = "gemini-3.8-flash"

        fun createCloud(): AiRouter = AiRouter(
            FirebaseAI.getInstance(FirebaseApp.getInstance(), GenerativeBackend.googleAI())
                .generativeModel(
                    modelName = CloudModel,
                    onDeviceConfig = OnDeviceConfig(mode = InferenceMode.ONLY_IN_CLOUD),
                ),
        )

        suspend fun createLocal(): AiRouter? = withTimeout(5_000) {
            val model = FirebaseAI.getInstance(FirebaseApp.getInstance(), GenerativeBackend.googleAI())
                .generativeModel(
                    modelName = CloudModel,
                    onDeviceConfig = OnDeviceConfig(mode = InferenceMode.ONLY_ON_DEVICE),
                )
            if (model.onDeviceExtension?.checkStatus() != OnDeviceModelStatus.AVAILABLE) return@withTimeout null
            AiRouter(model)
        }

        internal suspend fun <T : Any> generateValidated(
            prompt: String,
            preferCloud: Boolean,
            validate: (String) -> T?,
        ): Pair<T, Engine>? = validatedInference(
            prompt = prompt,
            preferCloud = preferCloud,
            timeoutMs = 8_000,
            run = { engine, input ->
                Log.d("CairnAi", "Trying $engine")
                val router = if (engine == Engine.LOCAL) createLocal() else createCloud()
                router?.generate(input)
            },
            validate = validate,
        ).also { Log.d("CairnAi", "Validated engine: ${it?.second ?: "none"}") }
    }
}
