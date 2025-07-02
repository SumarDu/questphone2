package neth.iecal.questphone.data.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig

object GeminiPro {

    private val config = generationConfig {
        temperature = 0.7f
    }

    private fun getGenerativeModel(apiKey: String, modelName: String): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = config
        )
    }

    suspend fun generate(prompt: String, apiKey: String): String? {
        if (apiKey.isBlank()) {
            return "API Key not set. Please add it in the settings."
        }

        val generativeModel = getGenerativeModel(apiKey, "gemini-pro")
        return try {
            val response = generativeModel.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            e.printStackTrace()
            e.message
        }
    }

    suspend fun generateFromImage(prompt: String, apiKey: String, image: Bitmap): String? {
        if (apiKey.isBlank()) {
            return "API Key not set. Please add it in the settings."
        }

        val generativeModel = getGenerativeModel(apiKey, "models/gemini-2.5-flash-lite-preview-06-17")

        val inputContent = content {
            image(image)
            text(prompt)
        }

        return try {
            val response = generativeModel.generateContent(inputContent)
            response.text
        } catch (e: Exception) {
            e.printStackTrace()
            e.message
        }
    }

    suspend fun verifyImage(prompt: String, apiKey: String, image: Bitmap): Pair<Boolean, String?> {
        if (apiKey.isBlank()) {
            return Pair(false, "API Key not set. Please add it in the settings.")
        }

        val generativeModel = getGenerativeModel(apiKey, "models/gemini-2.5-flash-lite-preview-06-17")

        val inputContent = content {
            image(image)
            text("Does this image match the following description? Answer with only 'True' or 'False' and then a brief explanation. Description: $prompt")
        }

        return try {
            val response = generativeModel.generateContent(inputContent)
            val responseText = response.text?.trim() ?: ""
            val isMatch = responseText.startsWith("True", ignoreCase = true)
            Pair(isMatch, responseText)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, e.message)
        }
    }
}
