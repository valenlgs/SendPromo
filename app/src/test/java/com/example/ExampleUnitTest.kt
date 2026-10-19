package com.example

import com.example.data.model.DemoMixType
import com.example.data.model.PromoDraft
import com.example.data.model.ReleaseType
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testDemoSubjectFormatting() {
        val draftExtended = PromoDraft(
            artistNames = "James Hype",
            trackTitle = "Ferrari Groove",
            releaseType = ReleaseType.DEMO.name,
            demoMixType = DemoMixType.EXTENDED_MIX.displayName
        )
        assertEquals("[DEMO] James Hype - Ferrari Groove (Extended Mix)", draftExtended.buildSubject())

        val draftRadio = PromoDraft(
            artistNames = "Fisher",
            trackTitle = "Losing It Again",
            releaseType = ReleaseType.DEMO.name,
            demoMixType = DemoMixType.RADIO_EDIT.displayName
        )
        assertEquals("[DEMO] Fisher - Losing It Again (Radio Edit)", draftRadio.buildSubject())

        val draftOriginalMix = PromoDraft(
            artistNames = "Solardo",
            trackTitle = "Roller",
            releaseType = ReleaseType.DEMO.name,
            demoMixType = DemoMixType.ORIGINAL_MIX.displayName
        )
        assertEquals("[DEMO] Solardo - Roller (Original Mix)", draftOriginalMix.buildSubject())
    }

    @Test
    fun testRemixAndMashupSubjectFormatting() {
        val draftRemix = PromoDraft(
            artistNames = "Cloonee",
            originalArtistName = "Peggy Gou",
            trackTitle = "(It Goes Like) Nanana",
            releaseType = ReleaseType.REMIX.name,
            isLabelRelease = true,
            labelName = "Hellbent Records"
        )
        assertEquals(
            "[PROMO] Peggy Gou - (It Goes Like) Nanana (Cloonee Remix) - Hellbent Records",
            draftRemix.buildSubject()
        )

        val draftMashup = PromoDraft(
            artistNames = "DJ Snake",
            originalArtistName = "Daft Punk",
            trackTitle = "One More Time",
            releaseType = ReleaseType.MASHUP.name,
            isLabelRelease = true,
            labelName = "Interscope"
        )
        assertEquals(
            "[PROMO] Daft Punk - One More Time (DJ Snake Mashup) - Interscope",
            draftMashup.buildSubject()
        )
    }

    @Test
    fun testCsvParserWithSampleLabels() {
        val result = com.example.data.util.CsvContactParser.parse(com.example.data.util.CsvContactParser.SAMPLE_LABELS_CSV)
        assertEquals(15, result.contacts.size)
        assertEquals(0, result.invalidRowsCount)
        assertEquals(0, result.duplicateRowsCount)
        assertEquals(listOf("Labels"), result.detectedGroups)
        val first = result.contacts.first()
        assertEquals("Spinnin' Records", first.name)
        assertEquals("demo@spinninrecords.com", first.email)
        assertEquals("Labels", first.group)
    }

    @Test
    fun testCsvParserWithSemicolonsQuotesAndDuplicates() {
        val rawCsv = """
"Contact Name";"Email Address";"Group"
"Defected Records, UK";submissions@defected.com;Labels
"Armada Music";promo@armadamusic.com;Labels
"Duplicate Submission";submissions@defected.com;Labels
"James Hype";promo@jameshype.com;Resident DJs
invalid row without email
""".trimIndent()

        val result = com.example.data.util.CsvContactParser.parse(rawCsv)
        assertEquals(3, result.contacts.size)
        assertEquals(1, result.duplicateRowsCount)
        assertEquals(1, result.invalidRowsCount)
        assertEquals(listOf("Labels", "Resident DJs"), result.detectedGroups)
    }
}
