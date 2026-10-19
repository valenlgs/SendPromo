package com.example.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiPromoService"

class GeminiPromoService {

    // gemini-3.5-flash-lite is optimized for low-latency, high-speed copywriting tasks
    private val fastModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                maxOutputTokens = 220
                temperature = 0.7f
            }
        )
    }

    /**
     * Streams high-energy, club-ready DJ promo pitch in real-time to eliminate perceived latency.
     */
    fun generateClubPitchStream(
        artist: String,
        trackTitle: String,
        releaseType: String,
        originalArtist: String?,
        genre: String?,
        bpm: String?,
        key: String?,
        label: String?,
        soundDescription: String
    ): Flow<String> = flow {
        val origStr = if (!originalArtist.isNullOrBlank()) "of $originalArtist" else ""
        val genreStr = if (!genre.isNullOrBlank()) "Genre: $genre | " else ""
        val bpmStr = if (!bpm.isNullOrBlank()) "BPM: $bpm | " else ""
        val keyStr = if (!key.isNullOrBlank()) "Key: $key | " else ""

        val isDemo = releaseType.equals("DEMO", ignoreCase = true)
        val prompt = if (isDemo) {
            """
                You are an electronic music producer submitting an exclusive unreleased demo to a record label A&R team.
                Write a concise, professional, and compelling demo pitch (around 70 to 100 words) for A&R managers.
                
                Track Info:
                - Artist: $artist
                - Track Title: $trackTitle
                - Format: Exclusive Demo ($origStr)
                - $genreStr$bpmStr$keyStr
                - Vibe & Style: ${soundDescription.ifBlank { "Peak-time club weapon designed for dancefloors" }}
                
                Guidelines:
                - Respectful, direct, tailored for label A&R review and signing consideration.
                - Mention exclusive first listen and seeking release on the label.
                - Ready-to-send text body only, no placeholders.
            """.trimIndent()
        } else {
            """
                You are a top electronic music publicist.
                Write a punchy, energetic DJ promo note (around 90 to 120 words) for club & festival DJs.
                
                Track Info:
                - Artist: $artist
                - Track: $trackTitle
                - Type: $releaseType $origStr
                - $genreStr$bpmStr$keyStr
                - Record Label: ${label?.ifBlank { "Independent" } ?: "Independent"}
                - Vibe: ${soundDescription.ifBlank { "Peak-time dancefloor weapon" }}
                
                Guidelines:
                - Authentic electronic club slang (e.g. "dancefloor weapon", "tested on the floor", "early reaction", "support")
                - Direct, high impact, asking for club/radio set support and feedback.
                - Ready-to-send text body only, no placeholders.
            """.trimIndent()
        }

        val responseStream = fastModel.generateContentStream(prompt)
        var accumulated = ""
        responseStream.collect { chunk ->
            val delta = chunk.text
            if (!delta.isNullOrEmpty()) {
                accumulated += delta
                emit(accumulated)
            }
        }
    }

    /**
     * Streams polished and improved text for the pitch.
     */
    fun polishPitchStream(currentPitch: String): Flow<String> = flow {
        if (currentPitch.isBlank()) return@flow

        val prompt = """
            Polish this DJ promo pitch to make it punchier, authentic, and club-ready under 120 words.
            Keep all key track info and artist names:
            
            "$currentPitch"
        """.trimIndent()

        val responseStream = fastModel.generateContentStream(prompt)
        var accumulated = ""
        responseStream.collect { chunk ->
            val delta = chunk.text
            if (!delta.isNullOrEmpty()) {
                accumulated += delta
                emit(accumulated)
            }
        }
    }

    /**
     * Fast suggestions for Electronic Music genres, BPM range, and tags.
     */
    suspend fun suggestTagsAndBpm(
        trackTitle: String,
        releaseType: String,
        artist: String
    ): String {
        return try {
            withTimeout(10_000L) {
                val prompt = """
                    For '$trackTitle' by '$artist' ($releaseType), suggest:
                    • Sub-genre (e.g. Tech House, Melodic Techno, Afro House)
                    • Recommended club BPM
                    • Key / Scale
                    • 3 hashtags
                    Keep under 30 words in bullet points.
                """.trimIndent()

                val response = fastModel.generateContent(prompt)
                response.text?.trim() ?: "• Genre: Tech House / Melodic\n• BPM: 126-128 BPM\n• Key: 8A / F min\n• Tags: #ClubReady #Promo #NewMusic"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error suggesting tags/BPM", e)
            "• Recommended Genre: Tech House / Melodic\n• BPM Range: 126 - 128 BPM\n• Key: 8A / F min\n• Tags: #Promo #NewMusic #ClubWeapon"
        }
    }
}
