package com.example.viewmodel

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiPromoService
import com.example.data.model.DeliveryOption
import com.example.data.model.DeliveryType
import com.example.data.model.PromoDraft
import com.example.data.model.PromoGroup
import com.example.data.model.ReleaseType
import com.example.data.model.SentCampaign
import com.example.data.repository.ImportContactsResult
import com.example.data.repository.SendPromoRepository
import com.example.data.util.ParsedContact
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "SendPromoVM"

enum class AppScreen {
    HOME,
    COMPOSER,
    DATABASE,
    ACTIVITY,
    SUCCESS
}

class SendPromoViewModel(
    private val repository: SendPromoRepository,
    val currentUserId: String
) : ViewModel() {

    private val geminiService = GeminiPromoService()

    // Screen State
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Screen history for back-handling
    private val screenStack = mutableListOf(AppScreen.HOME)

    // Groups from Firestore
    val groups: StateFlow<List<PromoGroup>> = repository.observeGroups(currentUserId)
        .catch { e ->
            Log.e(TAG, "Error observing groups", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Drafts from Firestore
    val drafts: StateFlow<List<PromoDraft>> = repository.observeDrafts(currentUserId)
        .catch { e ->
            Log.e(TAG, "Error observing drafts", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Campaigns from Firestore
    val campaigns: StateFlow<List<SentCampaign>> = repository.observeCampaigns(currentUserId)
        .catch { e ->
            Log.e(TAG, "Error observing campaigns", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Active Composer Draft
    private val _activeDraft = MutableStateFlow(PromoDraft(releaseType = ReleaseType.ORIGINAL.name))
    val activeDraft: StateFlow<PromoDraft> = _activeDraft.asStateFlow()

    // Composer step: 0 = Info, 1 = Pitch, 2 = Delivery, 3 = Recipients/Preview
    private val _composerStep = MutableStateFlow(0)
    val composerStep: StateFlow<Int> = _composerStep.asStateFlow()

    // AI Status
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiSuggestion = MutableStateFlow<String?>(null)
    val aiSuggestion: StateFlow<String?> = _aiSuggestion.asStateFlow()

    // Dispatch status
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Last sent summary
    private val _lastSentSubject = MutableStateFlow("")
    val lastSentSubject: StateFlow<String> = _lastSentSubject.asStateFlow()

    private val _lastSentRecipientCount = MutableStateFlow(0)
    val lastSentRecipientCount: StateFlow<Int> = _lastSentRecipientCount.asStateFlow()

    // CSV Import status
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _lastImportResult = MutableStateFlow<ImportContactsResult?>(null)
    val lastImportResult: StateFlow<ImportContactsResult?> = _lastImportResult.asStateFlow()

    fun clearImportResult() {
        _lastImportResult.value = null
    }

    // Auto-save Job
    private var autoSaveJob: Job? = null

    init {
        // Seed default sample groups if user has none, and ensure Labels group exists
        viewModelScope.launch {
            repository.seedDefaultGroupsIfEmpty(currentUserId)
            repository.ensureLabelsGroupExists(currentUserId)
        }
        startAutoSaveTimer()
    }

    private fun startAutoSaveTimer() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000L) // 5 minutes
                if (_currentScreen.value == AppScreen.COMPOSER) {
                    saveCurrentDraft(silent = true)
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenStack.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = screenStack.last()
            return true
        } else if (_currentScreen.value != AppScreen.HOME) {
            _currentScreen.value = AppScreen.HOME
            return true
        }
        return false
    }

    fun startNewPromo() {
        val allGroupIds = groups.value.map { it.id }
        _activeDraft.value = PromoDraft(
            selectedGroupIds = allGroupIds,
            releaseType = ReleaseType.ORIGINAL.name
        )
        _composerStep.value = 0
        _aiSuggestion.value = null
        navigateTo(AppScreen.COMPOSER)
    }

    fun resumeDraft(draft: PromoDraft) {
        _activeDraft.value = draft
        _composerStep.value = 0
        _aiSuggestion.value = null
        navigateTo(AppScreen.COMPOSER)
    }

    fun setComposerStep(step: Int) {
        _composerStep.value = step.coerceIn(0, 3)
        // Auto-save as user advances
        saveCurrentDraft(silent = true)
    }

    fun updateRealName(value: String) {
        _activeDraft.value = _activeDraft.value.copy(realName = value)
    }

    fun updateCityCountry(value: String) {
        _activeDraft.value = _activeDraft.value.copy(cityCountry = value)
    }

    fun updateArtistNames(value: String) {
        _activeDraft.value = _activeDraft.value.copy(artistNames = value)
    }

    fun updateTrackTitle(value: String) {
        _activeDraft.value = _activeDraft.value.copy(trackTitle = value)
    }

    fun updateReleaseType(value: String) {
        if (value == ReleaseType.DEMO.name) {
            val labelsGroup = groups.value.find { it.isProtected || it.name.equals("Labels", ignoreCase = true) }
            _activeDraft.value = _activeDraft.value.copy(
                releaseType = value,
                isLabelRelease = false,
                labelName = "",
                selectedGroupIds = if (labelsGroup != null) listOf(labelsGroup.id) else emptyList()
            )
        } else {
            _activeDraft.value = _activeDraft.value.copy(releaseType = value)
        }
    }

    fun updateDemoMixType(value: String) {
        _activeDraft.value = _activeDraft.value.copy(demoMixType = value)
    }

    fun updateOriginalArtistName(value: String) {
        _activeDraft.value = _activeDraft.value.copy(originalArtistName = value)
    }

    fun updateGenre(value: String) {
        _activeDraft.value = _activeDraft.value.copy(genre = value)
    }

    fun updateBpm(value: String) {
        _activeDraft.value = _activeDraft.value.copy(bpm = value)
    }

    fun updateMusicalKey(value: String) {
        _activeDraft.value = _activeDraft.value.copy(musicalKey = value)
    }

    fun updateReleaseDate(value: String) {
        _activeDraft.value = _activeDraft.value.copy(releaseDate = value)
    }

    fun updateLabelRelease(isLabel: Boolean, labelName: String = "") {
        _activeDraft.value = _activeDraft.value.copy(
            isLabelRelease = isLabel,
            labelName = if (isLabel) labelName else ""
        )
    }

    fun updatePitchMessage(message: String) {
        if (message.length <= 2000) {
            _activeDraft.value = _activeDraft.value.copy(pitchMessage = message)
        }
    }

    fun addDeliveryOption(option: DeliveryOption): Boolean {
        val current = _activeDraft.value.deliveryOptions
        if (current.size >= 2) return false // max 2 as per specification
        _activeDraft.value = _activeDraft.value.copy(deliveryOptions = current + option)
        return true
    }

    fun removeDeliveryOption(index: Int) {
        val current = _activeDraft.value.deliveryOptions.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _activeDraft.value = _activeDraft.value.copy(deliveryOptions = current)
        }
    }

    fun toggleGroupSelection(groupId: String) {
        val current = _activeDraft.value.selectedGroupIds.toMutableList()
        if (current.contains(groupId)) {
            current.remove(groupId)
        } else {
            current.add(groupId)
        }
        _activeDraft.value = _activeDraft.value.copy(selectedGroupIds = current)
    }

    fun selectAllGroups() {
        _activeDraft.value = _activeDraft.value.copy(
            selectedGroupIds = groups.value.map { it.id }
        )
    }

    fun toggleEmailExclusion(email: String) {
        val current = _activeDraft.value.excludedEmails.toMutableList()
        if (current.contains(email)) {
            current.remove(email)
        } else {
            current.add(email)
        }
        _activeDraft.value = _activeDraft.value.copy(excludedEmails = current)
    }

    fun isEmailSelected(email: String): Boolean {
        return !_activeDraft.value.excludedEmails.contains(email)
    }

    fun getFinalRecipients(): List<String> {
        val draft = _activeDraft.value
        // If Demo format: strictly send to the Labels group only
        if (draft.releaseType == ReleaseType.DEMO.name) {
            val labelsGroup = groups.value.find { it.isProtected || it.name.equals("Labels", ignoreCase = true) }
            return labelsGroup?.emails?.filter { !draft.excludedEmails.contains(it) } ?: emptyList()
        }

        val selectedGrpIds = if (draft.selectedGroupIds.isEmpty()) {
            groups.value.map { it.id }
        } else {
            draft.selectedGroupIds
        }

        val allEmails = mutableSetOf<String>()
        for (g in groups.value) {
            if (selectedGrpIds.contains(g.id)) {
                allEmails.addAll(g.emails)
            }
        }
        return allEmails.filter { !draft.excludedEmails.contains(it) }
    }

    fun saveCurrentDraft(silent: Boolean = false) {
        val draft = _activeDraft.value
        if (draft.artistNames.isBlank() && draft.trackTitle.isBlank() && draft.pitchMessage.isBlank()) {
            return
        }
        viewModelScope.launch {
            try {
                val savedId = repository.saveDraft(currentUserId, draft)
                if (draft.id.isBlank()) {
                    _activeDraft.value = draft.copy(id = savedId)
                }
                if (!silent) {
                    Log.d(TAG, "Draft saved with ID: $savedId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving draft", e)
            }
        }
    }

    fun deleteDraft(draftId: String) {
        viewModelScope.launch {
            try {
                repository.deleteDraft(currentUserId, draftId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting draft", e)
            }
        }
    }

    fun saveGroup(name: String, emails: List<String>, existingGroupId: String = "") {
        viewModelScope.launch {
            try {
                val isExistingLabels = groups.value.find { it.id == existingGroupId }?.isProtected == true
                val isLabels = isExistingLabels || name.trim().equals("Labels", ignoreCase = true)
                val group = PromoGroup(
                    id = existingGroupId,
                    name = name.trim(),
                    emails = emails.map { it.trim() }.filter { it.isNotBlank() },
                    isProtected = isLabels
                )
                repository.saveGroup(currentUserId, group)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving group", e)
            }
        }
    }

    fun deleteGroup(groupId: String) {
        val target = groups.value.find { it.id == groupId }
        if (target?.isProtected == true || target?.name.equals("Labels", ignoreCase = true)) {
            Log.w(TAG, "Cannot delete protected Labels group")
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteGroup(currentUserId, groupId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting group", e)
            }
        }
    }

    fun importContacts(
        contacts: List<ParsedContact>,
        targetGroupId: String? = null,
        targetGroupName: String? = null,
        onComplete: ((ImportContactsResult) -> Unit)? = null
    ) {
        if (contacts.isEmpty()) return
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val result = repository.importContacts(
                    userId = currentUserId,
                    contacts = contacts,
                    targetGroupId = targetGroupId,
                    targetGroupName = targetGroupName
                )
                _lastImportResult.value = result
                onComplete?.invoke(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error importing contacts", e)
            } finally {
                _isImporting.value = false
            }
        }
    }

    // Gemini AI Helpers (Ultra-fast streaming with gemini-3.5-flash-lite)
    fun generateAiPitch(soundDescription: String = "") {
        val draft = _activeDraft.value
        viewModelScope.launch {
            _isAiLoading.value = true
            _activeDraft.value = _activeDraft.value.copy(pitchMessage = "")
            try {
                withTimeout(15_000L) {
                    geminiService.generateClubPitchStream(
                        artist = draft.artistNames.ifBlank { "Featured Artist" },
                        trackTitle = draft.trackTitle.ifBlank { "New Single" },
                        releaseType = draft.releaseType,
                        originalArtist = if (draft.releaseType == ReleaseType.REMIX.name || draft.releaseType == ReleaseType.MASHUP.name) draft.originalArtistName else null,
                        genre = draft.genre,
                        bpm = draft.bpm,
                        key = draft.musicalKey,
                        label = if (draft.isLabelRelease) draft.labelName else null,
                        soundDescription = soundDescription
                    ).collect { streamedPitch ->
                        _activeDraft.value = _activeDraft.value.copy(pitchMessage = streamedPitch)
                    }
                    _aiSuggestion.value = "Pitch generated instantly!"
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI error during generation", e)
                if (_activeDraft.value.pitchMessage.isBlank()) {
                    val fallback = "Excited to share '${draft.trackTitle.ifBlank { "New Release" }}' (${draft.releaseType}) by ${draft.artistNames.ifBlank { "Featured Artist" }}. Tested on the floor with massive reaction. Would love your feedback and club support!"
                    _activeDraft.value = _activeDraft.value.copy(pitchMessage = fallback)
                    _aiSuggestion.value = "Pitch generated!"
                }
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun polishCurrentPitch() {
        val currentPitch = _activeDraft.value.pitchMessage
        if (currentPitch.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                withTimeout(12_000L) {
                    geminiService.polishPitchStream(currentPitch).collect { streamedPitch ->
                        _activeDraft.value = _activeDraft.value.copy(pitchMessage = streamedPitch)
                    }
                    _aiSuggestion.value = "Pitch polished with club-ready clarity!"
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI polish error", e)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun generateTagsAndBpm() {
        val draft = _activeDraft.value
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val tags = geminiService.suggestTagsAndBpm(
                    trackTitle = draft.trackTitle.ifBlank { "New Track" },
                    releaseType = draft.releaseType,
                    artist = draft.artistNames.ifBlank { "Artist" }
                )
                _aiSuggestion.value = tags
            } catch (e: Exception) {
                Log.e(TAG, "AI error", e)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearAiSuggestion() {
        _aiSuggestion.value = null
    }

    // Dispatch Promo (Send)
    fun dispatchPromo(context: Context) {
        val draft = _activeDraft.value
        val recipients = getFinalRecipients()
        val subject = draft.buildSubject()
        val body = draft.buildBody()

        _isSending.value = true
        _lastSentSubject.value = subject
        _lastSentRecipientCount.value = recipients.size

        viewModelScope.launch {
            // Simulated network send animation delay (1.5 seconds)
            delay(1500L)

            // Save Campaign Record to Firestore
            val campaign = SentCampaign(
                subject = subject,
                bodyPreview = body.take(250),
                recipientCount = recipients.size,
                recipients = recipients,
                artistNames = draft.artistNames,
                trackTitle = draft.trackTitle,
                status = "SENT"
            )
            repository.recordCampaign(currentUserId, campaign)

            // If this was a saved draft, remove it from drafts
            if (draft.id.isNotBlank()) {
                repository.deleteDraft(currentUserId, draft.id)
            }

            // Launch standard Gmail / Email Intent
            launchEmailIntent(context, recipients, subject, body)

            _isSending.value = false
            _currentScreen.value = AppScreen.SUCCESS
        }
    }

    private fun launchEmailIntent(
        context: Context,
        recipients: List<String>,
        subject: String,
        body: String
    ) {
        try {
            val attachments = _activeDraft.value.deliveryOptions.filter {
                it.type == DeliveryType.ATTACHMENT.name && it.uriString.isNotBlank()
            }
            val attachmentUris = attachments.mapNotNull {
                try {
                    Uri.parse(it.uriString)
                } catch (e: Exception) {
                    null
                }
            }

            val hasAttachments = attachmentUris.isNotEmpty()
            val intent = if (attachmentUris.size > 1) {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "audio/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachmentUris))
                }
            } else if (attachmentUris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, attachmentUris.first())
                }
            } else {
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                }
            }

            // Put recipients in BCC to respect DJ privacy and avoid reply-all spam
            if (recipients.isNotEmpty()) {
                intent.putExtra(Intent.EXTRA_BCC, recipients.toTypedArray())
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(recipients.first()))
            }
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
            intent.putExtra(Intent.EXTRA_TEXT, body)

            if (hasAttachments) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val clipData = ClipData.newRawUri("SendPromo Audio Attachment", attachmentUris.first())
                for (i in 1 until attachmentUris.size) {
                    clipData.addItem(ClipData.Item(attachmentUris[i]))
                }
                intent.clipData = clipData
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val chooser = Intent.createChooser(intent, "Send promo with Gmail / Mail app").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Explicitly grant read permission to all potential handling activities
            if (hasAttachments) {
                try {
                    val resolvedActivities = context.packageManager.queryIntentActivities(
                        chooser,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )
                    for (resolveInfo in resolvedActivities) {
                        val packageName = resolveInfo.activityInfo.packageName
                        for (uri in attachmentUris) {
                            try {
                                context.grantUriPermission(
                                    packageName,
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not grantUriPermission to package manager activities", e)
                }
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to launch email chooser directly", e)
        }
    }
}
