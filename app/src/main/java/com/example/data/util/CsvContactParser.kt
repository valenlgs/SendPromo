package com.example.data.util

data class ParsedContact(
    val name: String,
    val email: String,
    val group: String
)

data class CsvParseResult(
    val contacts: List<ParsedContact>,
    val totalRowsProcessed: Int,
    val invalidRowsCount: Int,
    val duplicateRowsCount: Int,
    val detectedGroups: List<String>,
    val detectedHeaders: List<String>
)

object CsvContactParser {

    private val EMAIL_REGEX = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

    // Sample pre-built DJ & Label CSV templates for quick testing & immediate population
    val SAMPLE_LABELS_CSV = """
Label Name,A&R Email,Group
Spinnin' Records,demo@spinninrecords.com,Labels
Defected Records,submissions@defected.com,Labels
Toolroom Records,demos@toolroomrecords.com,Labels
Armada Music,promo@armadamusic.com,Labels
Drumcode,demo@drumcode.se,Labels
Anjunadeep,ar@anjunadeep.com,Labels
Afterlife Recordings,demo@afterlife.it,Labels
Revealed Recordings,demo@revealedrecordings.com,Labels
Ultra Music,promo@ultramusic.com,Labels
Positiva Records,ar@positivarecords.com,Labels
Ministry of Sound,clubpromo@ministryofsound.com,Labels
Axtone Records,demos@axtone.com,Labels
Subliminal Records,ar@subliminalrecords.com,Labels
Dim Mak Records,promo@dimmak.com,Labels
Enormous Tunes,demo@enormoustunes.com,Labels
""".trimIndent()

    val SAMPLE_DJS_CSV = """
Artist / DJ,Promo Email,Category
David Guetta,promo@davidguetta.com,Festival Headliners
Martin Garrix,promos@stmpdrcrds.com,Festival Headliners
Tiësto,tiesto@musicalfreedom.com,Festival Headliners
James Hype,promo@jameshype.com,Club & Radio DJs
Fisher,catchandrelease@fishermusic.com,Club & Radio DJs
Michael Bibi,promos@solidgrooves.co.uk,Tech House & Underground
Carl Cox,carlcox@safehouse.com,Tech House & Underground
Vintage Culture,promo@vintageculture.com,Festival Headliners
John Summit,expertsonly@johnsummit.com,Club & Radio DJs
Cloonee,promo@hellbentrecords.com,Tech House & Underground
Nora En Pure,promo@purifiedrecords.com,Melodic & Deep
Boris Brejcha,promo@fckng-serious.de,Melodic & Deep
Meduza,promo@meduzamusic.net,Club & Radio DJs
Gorgon City,promos@realmrecords.com,Club & Radio DJs
""".trimIndent()

    val SAMPLE_MIXED_CSV = """
Contact Name,Email,Group
Spinnin' A&R,demo@spinninrecords.com,Labels
Defected Submissions,submissions@defected.com,Labels
Toolroom A&R,demos@toolroomrecords.com,Labels
Club Space Residents,residents@clubspace.com,Resident DJs
Amnesia Ibiza Residents,djs@amnesia.es,Resident DJs
Pacha Ibiza Curators,promos@pacha.com,Resident DJs
BBC Radio 1 Dance,dancepromo@bbc.co.uk,Radio & Tastemakers
SiriusXM BPM,bpmcurator@siriusxm.com,Radio & Tastemakers
KISS FM Dance,promo@kissfm.co.uk,Radio & Tastemakers
""".trimIndent()

    /**
     * Parses CSV text with support for commas, semicolons, tabs, quotes, and header detection.
     */
    fun parse(csvContent: String): CsvParseResult {
        if (csvContent.isBlank()) {
            return CsvParseResult(emptyList(), 0, 0, 0, emptyList(), emptyList())
        }

        val rawLines = csvContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (rawLines.isEmpty()) {
            return CsvParseResult(emptyList(), 0, 0, 0, emptyList(), emptyList())
        }

        // Detect delimiter: check first few lines for comma, semicolon, or tab
        val sampleText = rawLines.take(3).joinToString("\n")
        val delimiter = detectDelimiter(sampleText)

        val parsedRows = rawLines.map { parseCsvLine(it, delimiter) }
        if (parsedRows.isEmpty()) {
            return CsvParseResult(emptyList(), 0, 0, 0, emptyList(), emptyList())
        }

        // Determine if first row is a header
        val firstRow = parsedRows.first()
        val hasHeader = isHeaderRow(firstRow)

        var emailCol = -1
        var nameCol = -1
        var groupCol = -1
        val headers: List<String>

        val dataRows: List<List<String>>
        if (hasHeader) {
            headers = firstRow.map { it.trim() }
            dataRows = parsedRows.drop(1)
            // Match columns by header names
            headers.forEachIndexed { index, header ->
                val lower = header.lowercase()
                when {
                    emailCol == -1 && (lower.contains("email") || lower.contains("mail") || lower.contains("correo") || lower.contains("contact")) -> {
                        emailCol = index
                    }
                    nameCol == -1 && (lower.contains("name") || lower.contains("nombre") || lower.contains("artist") || lower.contains("dj") || lower.contains("label") || lower.contains("sello")) -> {
                        nameCol = index
                    }
                    groupCol == -1 && (lower.contains("group") || lower.contains("grupo") || lower.contains("category") || lower.contains("categoria") || lower.contains("list") || lower.contains("lista") || lower.contains("tag") || lower.contains("type")) -> {
                        groupCol = index
                    }
                }
            }
        } else {
            headers = emptyList()
            dataRows = parsedRows
        }

        // If emailCol was not identified by header, find the first column in data rows that contains '@'
        if (emailCol == -1) {
            val maxCols = dataRows.maxOfOrNull { it.size } ?: 0
            for (col in 0 until maxCols) {
                val emailMatches = dataRows.count { row ->
                    val cell = row.getOrNull(col) ?: ""
                    EMAIL_REGEX.containsMatchIn(cell)
                }
                if (emailMatches > dataRows.size / 3 || (dataRows.size <= 3 && emailMatches > 0)) {
                    emailCol = col
                    break
                }
            }
        }

        // Fallbacks for name and group
        if (nameCol == -1) {
            // Pick the first non-email column
            for (col in 0 until (dataRows.firstOrNull()?.size ?: 0)) {
                if (col != emailCol && col != groupCol) {
                    nameCol = col
                    break
                }
            }
        }

        if (groupCol == -1) {
            // Pick a column that is neither email nor name if 3 or more columns exist
            for (col in 0 until (dataRows.firstOrNull()?.size ?: 0)) {
                if (col != emailCol && col != nameCol) {
                    groupCol = col
                    break
                }
            }
        }

        val seenEmails = mutableSetOf<String>()
        val contacts = mutableListOf<ParsedContact>()
        var invalidCount = 0
        var duplicateCount = 0

        for (row in dataRows) {
            val rawEmailCell = if (emailCol in row.indices) row[emailCol] else ""
            val emailMatch = EMAIL_REGEX.find(rawEmailCell)?.value
                ?: row.firstNotNullOfOrNull { cell -> EMAIL_REGEX.find(cell)?.value }

            if (emailMatch != null) {
                val normalizedEmail = emailMatch.trim().lowercase()
                if (seenEmails.contains(normalizedEmail)) {
                    duplicateCount++
                } else {
                    seenEmails.add(normalizedEmail)
                    val rawName = if (nameCol in row.indices && nameCol != emailCol) row[nameCol].trim() else ""
                    val rawGroup = if (groupCol in row.indices && groupCol != emailCol && groupCol != nameCol) row[groupCol].trim() else ""

                    val contactName = if (rawName.isNotBlank() && !rawName.equals(normalizedEmail, ignoreCase = true)) {
                        rawName
                    } else {
                        normalizedEmail.substringBefore("@").replace(".", " ").capitalizeWords()
                    }

                    val finalGroup = when {
                        rawGroup.isNotBlank() -> rawGroup
                        rawName.contains("Records", ignoreCase = true) || rawName.contains("Music", ignoreCase = true) || rawName.contains("A&R", ignoreCase = true) -> "Labels"
                        normalizedEmail.contains("records") || normalizedEmail.contains("ar@") || normalizedEmail.contains("demo@") -> "Labels"
                        else -> "Imported Contacts"
                    }

                    contacts.add(
                        ParsedContact(
                            name = contactName,
                            email = normalizedEmail,
                            group = finalGroup
                        )
                    )
                }
            } else {
                invalidCount++
            }
        }

        val detectedGroups = contacts.map { it.group }.distinct().filter { it.isNotBlank() }

        return CsvParseResult(
            contacts = contacts,
            totalRowsProcessed = dataRows.size,
            invalidRowsCount = invalidCount,
            duplicateRowsCount = duplicateCount,
            detectedGroups = detectedGroups,
            detectedHeaders = headers
        )
    }

    private fun detectDelimiter(text: String): Char {
        val commaCount = text.count { it == ',' }
        val semiCount = text.count { it == ';' }
        val tabCount = text.count { it == '\t' }

        return when {
            semiCount > commaCount && semiCount > tabCount -> ';'
            tabCount > commaCount && tabCount > semiCount -> '\t'
            else -> ','
        }
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // Escaped quote: "" -> "
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        if (row.isEmpty()) return false
        val rowText = row.joinToString(" ").lowercase()
        val hasEmailHeader = rowText.contains("email") || rowText.contains("mail") || rowText.contains("correo") || rowText.contains("contact")
        val hasNameHeader = rowText.contains("name") || rowText.contains("nombre") || rowText.contains("artist") || rowText.contains("dj") || rowText.contains("label")
        val hasGroupHeader = rowText.contains("group") || rowText.contains("category") || rowText.contains("lista")

        // If it looks like header words and doesn't contain an actual @ symbol
        return (hasEmailHeader || hasNameHeader || hasGroupHeader) && row.none { it.contains("@") }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
