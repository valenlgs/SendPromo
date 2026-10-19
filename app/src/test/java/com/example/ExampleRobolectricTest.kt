package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DeliveryOption
import com.example.data.model.DeliveryType
import com.example.data.model.PromoDraft
import com.example.data.model.ReleaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SendPromo", appName)
  }

  @Test
  fun `promo draft builds subject correctly without label`() {
    val draft = PromoDraft(
      artistNames = "Fisher & Cloonee",
      trackTitle = "Dancefloor Weapon",
      releaseType = ReleaseType.ORIGINAL.name,
      isLabelRelease = false
    )
    val subject = draft.buildSubject()
    assertEquals("[PROMO] Fisher & Cloonee - Dancefloor Weapon (Original)", subject)
  }

  @Test
  fun `promo draft builds subject correctly for remix with original artist and label`() {
    val draft = PromoDraft(
      artistNames = "Michael Bibi",
      trackTitle = "Got The Groove",
      releaseType = ReleaseType.REMIX.name,
      originalArtistName = "Peggy Gou",
      isLabelRelease = true,
      labelName = "Solid Grooves"
    )
    val subject = draft.buildSubject()
    assertEquals("[PROMO] Peggy Gou - Got The Groove (Michael Bibi Remix) - Solid Grooves", subject)
  }

  @Test
  fun `promo draft builds subject correctly for mashup with original artist and label`() {
    val draft = PromoDraft(
      artistNames = "James Hype",
      trackTitle = "One More Time",
      releaseType = ReleaseType.MASHUP.name,
      originalArtistName = "Daft Punk",
      isLabelRelease = true,
      labelName = "Stereohype"
    )
    val subject = draft.buildSubject()
    assertEquals("[PROMO] Daft Punk - One More Time (James Hype Mashup) - Stereohype", subject)
  }

  @Test
  fun `promo draft formats body with BPM Key Genre and delivery attachments`() {
    val draft = PromoDraft(
      realName = "John Doe",
      cityCountry = "Ibiza, Spain",
      artistNames = "DJ Example",
      trackTitle = "Midnight Peak",
      releaseType = ReleaseType.REMIX.name,
      originalArtistName = "Peggy Gou",
      genre = "Tech House",
      bpm = "128",
      musicalKey = "F minor",
      pitchMessage = "Exclusive peak-time promo for your sets.",
      deliveryOptions = listOf(
        DeliveryOption(
          type = DeliveryType.ATTACHMENT.name,
          value = "track_master.wav",
          fileName = "track_master.wav",
          uriString = "content://com.android.providers.media.documents/document/audio%3A123"
        ),
        DeliveryOption(
          type = DeliveryType.SOUNDCLOUD.name,
          value = "https://soundcloud.com/test/track",
          isPrivateSoundCloud = true,
          secretToken = "s-xyz123"
        )
      )
    )
    val body = draft.buildBody()
    assertTrue(body.contains("Artist / Producer: John Doe"))
    assertTrue(body.contains("Location: Ibiza, Spain"))
    assertTrue(body.contains("Original Artist: Peggy Gou"))
    assertTrue(body.contains("Genre: Tech House"))
    assertTrue(body.contains("BPM: 128"))
    assertTrue(body.contains("Key / Scale: F minor"))
    assertTrue(body.contains("track_master.wav"))
    assertTrue(body.contains("s-xyz123"))
  }
}
