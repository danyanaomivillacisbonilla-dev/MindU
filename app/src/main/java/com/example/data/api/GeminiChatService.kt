package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiChatService {

    private const val TAG = "GeminiChatService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(userPrompt: String, chatHistory: List<Pair<String, String>>): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is missing or default placeholder.")
            return@withContext getOfflineResponse(userPrompt)
        }

        try {
            val mediaType = "application/json; charset=utf-8".toMediaType()

            // Build request JSON
            val requestJson = JSONObject()

            // Add contents (history + new prompt)
            val contentsArray = JSONArray()

            // Add prior history to maintain context
            chatHistory.forEach { (role, text) ->
                val roleStr = if (role == "user") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", roleStr)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", text)
                        })
                    })
                })
            }

            // Add current prompt
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", userPrompt)
                    })
                })
            })

            requestJson.put("contents", contentsArray)

            // Add system instructions
            requestJson.put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", """Eres MindU Assistant, un chatbot de Inteligencia Artificial integrado en la aplicación MindU para estudiantes universitarios de Psicología.
Tu propósito exclusivo es ayudar a los estudiantes de Psicología de forma amigable, académica, empática y profesional.
Funciones principales:
1. Responder con rigor académico sobre dudas universitarias, teorías psicológicas (Cognitivo Conductual TCC, Neurobiología, Teoría del Apego, Psicoanálisis, Humanismo, Gestalt, etc.) y conceptos clínicos.
2. Recomendar y guiar al estudiante sobre cómo estudiar usando MindU (los recursos académicos, el diario clínico de simulaciones de casos, o el registro emocional).
3. Ofrecer de inmediato sugerencias o guías de bienestar (ejercicios de respiración diafragmática, técnicas de mindfulness o relajación muscular progresiva de Jacobson).
4. El tono debe ser siempre empático, inspirador de calma y profesional. Habla en español de manera clara, estructurada con viñetas elegantes.""")
                    })
                })
            })

            // Set temperature config
            requestJson.put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })

            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val url = "$BASE_URL?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "API Call Failed. Code: ${response.code}, Error: $errorBody")
                    return@withContext "Lo siento, MindU Assistant experimentó una sobrecarga de solicitudes (Código ${response.code}). Esto es lo que puedo decirte: ${getOfflineResponse(userPrompt)}"
                }

                val responseBodyStr = response.body?.string() ?: return@withContext "Error: No se recibió respuesta de MindU Assistant."
                val jsonResponse = JSONObject(responseBodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No se recibió texto.")
                    }
                }
                return@withContext "MindU Assistant analizó tu solicitud pero no pudo procesar un texto coherente."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Chat API call", e)
            return@withContext "Error de conexión. MindU Assistant está trabajando fuera de línea. ${getOfflineResponse(userPrompt)}"
        }
    }

    /**
     * Elegant offline rule-based responder to guarantee that even if the API Key is not set or
     * internet is lost, the MindU Assistant acts extremely helpful and gives amazing psychology advice.
     */
    private fun getOfflineResponse(prompt: String): String {
        val query = prompt.lowercase()
        return when {
            query.contains("hola") || query.contains("salud") || query.contains("bienvenido") -> {
                """¡Hola! Soy **MindU Assistant**, tu tutor de Inteligencia Artificial para Psicología. 🌟 
Como estamos en modo demostrativo / sin llave API de internet, te respondo con mis conocimientos de reserva integrada.

¿En qué te puedo apoyar hoy?
1. Explicar conceptos de **TCC**, **Apego de Bowlby** o **Autónomo**.
2. Sugerir un **ejercicio contra el estrés o respiración**.
3. Guiarte para hacer una **simulación clínica** interactiva con pacientes virtuales (¡Ve al módulo de Casos Clínicos en la barra inferior!)."""
            }
            query.contains("anhedonia") || query.contains("depre") || query.contains("tristeza") -> {
                """La **Anhedonia** es uno de los síntomas nucleares de los trastornos depresivos según el DSM-5. 
Se define como la incapacidad de experimentar placer en actividades que previamente resultaban gratas para el individuo. 

**Abordaje desde la Psicología:**
- **Activación Conductual (enfoque TCC)**: Ayudar al paciente a programar tareas de forma gradual (independiente de su estado de ánimo bajo), enfocándose en la recompensa ambiental natural.
- **Diferencia técnica**: No se debe confundir con la *Abulia* (falta de voluntad/energía para actuar)."""
            }
            query.contains("tcc") || query.contains("cognitivo") || query.contains("conductual") -> {
                """La **Terapia Cognitivo Conductual (TCC)** se enfoca en el presente y trabaja sobre el bucle cognitivo:
- **Cogniciones**: Qué pensamos influye directamente en cómo nos sentimos y actuamos.
- **Emoción**: Respuestas psicofisiológicas.
- **Conducta**: Lo que hacemos (evitación, confrontación).

Una de las herramientas primordiales es la **Reestructuración Cognitiva**, que busca identificar distorsiones del pensamiento como la *Catastrofización* o la *Personalización* y someterlas al debate socrático."""
            }
            query.contains("estrés") || query.contains("respir") || query.contains("relaj") || query.contains("ansie") -> {
                """Para regular la activación psicofisiológica o estrés agudo, te recomiendo practicar la **Respiración Diafragmática Coherente**:

**Instrucción paso a paso:**
1. Coloca una mano sobre el pecho y otra en el abdomen.
2. Inhala lentamente por la nariz durante **4 segundos**, expandiendo el abdomen (no el pecho).
3. Mantén el aire por **2 segundos**.
4. Exhala pausadamente por la boca durante **6 segundos**, soltando la tensión muscular.
5. Repite este ciclo de 5 a 10 veces para estimular el nervio vago y despertar el **sistema parasimpático**. ¡Encuentra guías continuas en el módulo de Bienestar Emocional!"""
            }
            query.contains("apego") || query.contains("bowlby") -> {
                """La teoría del **Apego de John Bowlby** y Mary Ainsworth es un pilar del desarrollo evolutivo. Define los vínculos afectivos tempranos:
- **Seguro**: Cuidador base de seguridad. El menor explora y se reconforta fácilmente al reencuentro.
- **Ansioso-Ambivalente**: Inseguridad e ira; desesperación ante abandono por inconsistencia parental.
- **Evitativo**: Desapego aparente; suprime emociones porque descubrió que el cuidador rechaza su vulnerabilidad.
- **Desorganizado**: Respuestas caóticas y contradictorias, típico ante traumas o abuso de figuras de amparo."""
            }
            else -> {
                """He recibido tu consulta sobre: *"$prompt"* 📖 

Como tu tutor de Psicología, te recomiendo:
- Si buscas consolidar conceptos: explora la **Biblioteca de Apoyo Académico** con resúmenes de TCC y Apego.
- Si quieres entrenar tu juicio clínico: el módulo de **Casos Clínicos** te desafía con pacientes virtuales (Clara, Andrés o Mateo).
- Si necesitas un espacio de autocuidado: regístrate en el seguidor de bienestar emocional.

*(Nota: Para habilitar respuestas de IA avanzadas en tiempo real de cualquier tema con el modelo Gemini-3.5-flash, introduce tu GEMINI_API_KEY en el panel de secretos de AI Studio).*"""
            }
        }
    }
}
