package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ReleaseType(val displayName: String) {
    ORIGINAL("Original"),
    REMIX("Remix"),
    MASHUP("Mashup"),
    DEMO("Demo")
}

enum class DemoMixType(val displayName: String) {
    EXTENDED_MIX("Extended Mix"),
    RADIO_EDIT("Radio Edit"),
    ORIGINAL_MIX("Original Mix")
}

enum class DeliveryType(val displayName: String, val iconDescription: String) {
    LINK("Direct Link", "Direct download URL"),
    ATTACHMENT("File Attachment", "Audio file (.mp3, .wav, .flac, etc.)"),
    GOOGLE_DRIVE("Google Drive", "Google Drive sharing link"),
    SOUNDCLOUD("SoundCloud", "SoundCloud track or secret link")
}

@IgnoreExtraProperties
data class DeliveryOption(
    val type: String = DeliveryType.LINK.name,
    val value: String = "",
    val isPrivateSoundCloud: Boolean = false,
    val secretToken: String = "",
    val uriString: String = "",
    val fileName: String = ""
)

@IgnoreExtraProperties
data class PromoGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val emails: List<String> = emptyList(),
    val isProtected: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PromoDraft(
    @DocumentId val id: String = "",
    val realName: String = "",
    val cityCountry: String = "",
    val artistNames: String = "",
    val trackTitle: String = "",
    val releaseType: String = ReleaseType.ORIGINAL.name,
    val demoMixType: String = DemoMixType.EXTENDED_MIX.displayName,
    val originalArtistName: String = "",
    val genre: String = "",
    val bpm: String = "",
    val musicalKey: String = "",
    val releaseDate: String = "",
    val isLabelRelease: Boolean = false,
    val labelName: String = "",
    val pitchMessage: String = "",
    val deliveryOptions: List<DeliveryOption> = emptyList(),
    val selectedGroupIds: List<String> = emptyList(),
    val excludedEmails: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun buildSubject(): String {
        val original = originalArtistName.trim().ifBlank { "Original Artist" }
        val artist = artistNames.trim().ifBlank { "Artist" }
        val title = trackTitle.trim().ifBlank { "Track" }

        // If Demo format: [DEMO] Nombre Artistico - Nombre del Track (Extended Mix, Radio Edit, Original Mix)
        if (releaseType == ReleaseType.DEMO.name) {
            val mix = demoMixType.trim().ifBlank { DemoMixType.EXTENDED_MIX.displayName }
            return "[DEMO] $artist - $title ($mix)"
        }

        val labelPart = if (isLabelRelease && labelName.isNotBlank()) {
            " - ${labelName.trim()}"
        } else if (labelName.isNotBlank()) {
            " - ${labelName.trim()}"
        } else {
            ""
        }

        val core = when (releaseType) {
            ReleaseType.REMIX.name -> "$original - $title ($artist Remix)$labelPart"
            ReleaseType.MASHUP.name -> "$original - $title ($artist Mashup)$labelPart"
            else -> "$artist - $title (Original)$labelPart"
        }
        return "[PROMO] $core"
    }

    fun buildBody(): String {
        val sb = StringBuilder()
        if (realName.isNotBlank()) {
            sb.append("Artist / Producer: ").append(realName).append("\n")
        }
        if (cityCountry.isNotBlank()) {
            sb.append("Location: ").append(cityCountry).append("\n")
        }
        if ((releaseType == ReleaseType.REMIX.name || releaseType == ReleaseType.MASHUP.name) && originalArtistName.isNotBlank()) {
            sb.append("Original Artist: ").append(originalArtistName).append("\n")
        }
        if (genre.isNotBlank()) {
            sb.append("Genre: ").append(genre).append("\n")
        }
        if (bpm.isNotBlank()) {
            sb.append("BPM: ").append(bpm).append("\n")
        }
        if (musicalKey.isNotBlank()) {
            sb.append("Key / Scale: ").append(musicalKey).append("\n")
        }
        if (releaseType == ReleaseType.DEMO.name) {
            val mix = demoMixType.trim().ifBlank { DemoMixType.EXTENDED_MIX.displayName }
            sb.append("Format: Demo ($mix)\n")
            sb.append("Submission: Exclusive Demo Submission for Label / A&R Evaluation\n")
        } else {
            if (isLabelRelease) {
                sb.append("Record Label: ").append(labelName.ifBlank { "Yes" }).append("\n")
            } else {
                sb.append("Record Label: No (Independent / Self-Released)\n")
            }
        }
        if (releaseDate.isNotBlank()) {
            sb.append("Release Date: ").append(releaseDate).append("\n")
        }

        sb.append("\n").append(pitchMessage).append("\n\n")

        // Links section
        val nonAttachmentOptions = deliveryOptions.filter { it.type != DeliveryType.ATTACHMENT.name && it.value.isNotBlank() }
        if (nonAttachmentOptions.isNotEmpty()) {
            sb.append("--- PROMO & STREAMING LINKS ---\n")
            for (opt in nonAttachmentOptions) {
                when (opt.type) {
                    DeliveryType.LINK.name -> sb.append("Download Link: ").append(opt.value).append("\n")
                    DeliveryType.GOOGLE_DRIVE.name -> sb.append("Google Drive: ").append(opt.value).append("\n")
                    DeliveryType.SOUNDCLOUD.name -> {
                        sb.append("SoundCloud: ").append(opt.value)
                        if (opt.isPrivateSoundCloud && opt.secretToken.isNotBlank()) {
                            sb.append(" (Secret Token: ").append(opt.secretToken).append(")")
                        }
                        sb.append("\n")
                    }
                }
            }
        }

        val attachments = deliveryOptions.filter { it.type == DeliveryType.ATTACHMENT.name }
        if (attachments.isNotEmpty()) {
            sb.append("\n[Attached audio files included for high-definition club playback]\n")
            attachments.forEach { att ->
                val name = att.fileName.ifBlank { att.value }
                sb.append("• Attachment: ").append(name).append("\n")
            }
        }

        return sb.toString().trim()
    }
}

@IgnoreExtraProperties
data class SentCampaign(
    @DocumentId val id: String = "",
    val subject: String = "",
    val bodyPreview: String = "",
    val recipientCount: Int = 0,
    val recipients: List<String> = emptyList(),
    val artistNames: String = "",
    val trackTitle: String = "",
    @ServerTimestamp val sentAt: Timestamp? = null,
    val status: String = "SENT"
)
