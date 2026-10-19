package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryOption
import com.example.data.model.DeliveryType
import com.example.data.model.DemoMixType
import com.example.data.model.PromoDraft
import com.example.data.model.PromoGroup
import com.example.data.model.ReleaseType
import com.example.ui.theme.AcidEmerald
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.AttachmentHelper
import com.example.viewmodel.SendPromoViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposerScreen(
    viewModel: SendPromoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val draft by viewModel.activeDraft.collectAsState()
    val step by viewModel.composerStep.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiSuggestion by viewModel.aiSuggestion.collectAsState()

    var showDeliveryDialog by remember { mutableStateOf<DeliveryType?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isAttachingFile by remember { mutableStateOf(false) }

    // Audio file picker (.mp3, .wav, .flac, .ogg, .aiff, .rar, .zip per PDF)
    // Copies file to cache and creates FileProvider URI to ensure external apps (Gmail) receive full read permissions
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isAttachingFile = true
                val option = AttachmentHelper.prepareAttachmentOption(context, uri)
                isAttachingFile = false
                if (option != null) {
                    val added = viewModel.addDeliveryOption(option)
                    if (!added) {
                        Toast.makeText(context, "Maximum 2 delivery methods allowed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Failed to attach file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BackHandler {
        if (step > 0) {
            viewModel.setComposerStep(step - 1)
        } else {
            viewModel.saveCurrentDraft(silent = true)
            viewModel.navigateBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header: Navigation, Step Indicators & Save Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (step > 0) {
                                viewModel.setComposerStep(step - 1)
                            } else {
                                viewModel.saveCurrentDraft(silent = true)
                                viewModel.navigateBack()
                            }
                        },
                        modifier = Modifier.testTag("composer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = when (step) {
                                0 -> "1. Track Information"
                                1 -> "2. Message & AI Pitch"
                                2 -> "3. Delivery Format (Max 2)"
                                else -> "4. Recipients & Dispatch"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "SendPromo • Auto-save active",
                            fontSize = 11.sp,
                            color = AcidEmerald
                        )
                    }
                }

                // Step Pills
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0..3).forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(width = 20.dp, height = 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (index == step) NeonCyan else if (index < step) ElectricViolet else StudioBorder
                                )
                        )
                    }
                }
            }

            // Main Content Area based on Step
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (step) {
                    0 -> Step1TrackInfo(
                        draft = draft,
                        viewModel = viewModel,
                        validationError = validationError
                    )
                    1 -> Step2PitchAndAi(
                        draft = draft,
                        viewModel = viewModel,
                        isAiLoading = isAiLoading,
                        aiSuggestion = aiSuggestion
                    )
                    2 -> Step3DeliveryOptions(
                        draft = draft,
                        viewModel = viewModel,
                        onOpenUrlDialog = { showDeliveryDialog = DeliveryType.LINK },
                        onPickFile = {
                            if (draft.deliveryOptions.size >= 2) {
                                Toast.makeText(context, "Maximum 2 delivery methods allowed", Toast.LENGTH_SHORT).show()
                            } else {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "audio/*",
                                        "application/zip",
                                        "application/x-rar-compressed",
                                        "application/octet-stream"
                                    )
                                )
                            }
                        },
                        onOpenDriveDialog = { showDeliveryDialog = DeliveryType.GOOGLE_DRIVE },
                        onOpenSoundCloudDialog = { showDeliveryDialog = DeliveryType.SOUNDCLOUD }
                    )
                    3 -> Step4RecipientsAndPreview(
                        draft = draft,
                        groups = groups,
                        viewModel = viewModel
                    )
                }
            }

            // Bottom Navigation Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        OutlinedButton(
                            onClick = { viewModel.setComposerStep(step - 1) },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.testTag("composer_previous_step_button")
                        ) {
                            Text("Back", color = TextGray)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    if (step < 3) {
                        // Button "Continue" per PDF: "Finalmente, el usuario presionará 'Continuar' y la aplicación organizará el mensaje a enviar"
                        Button(
                            onClick = {
                                if (step == 0 || step == 1) {
                                    if (draft.artistNames.isBlank()) {
                                        validationError = "Artist name is required"
                                        return@Button
                                    }
                                    if (draft.trackTitle.isBlank()) {
                                        validationError = "Track title is required"
                                        return@Button
                                    }
                                    if ((draft.releaseType == ReleaseType.REMIX.name || draft.releaseType == ReleaseType.MASHUP.name) && draft.originalArtistName.isBlank()) {
                                        validationError = "Original artist name is required for ${if (draft.releaseType == ReleaseType.REMIX.name) "Remixes" else "Mashups"}"
                                        return@Button
                                    }
                                    if (draft.genre.isBlank()) {
                                        validationError = "Genre is required"
                                        return@Button
                                    }
                                    if (draft.bpm.isBlank()) {
                                        validationError = "BPM is required"
                                        return@Button
                                    }
                                    if (draft.musicalKey.isBlank()) {
                                        validationError = "Key / Scale is required"
                                        return@Button
                                    }
                                    validationError = null
                                }
                                viewModel.setComposerStep(step + 1)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = StudioBlack
                            ),
                            modifier = Modifier.testTag("composer_continue_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        // Button "Send Promo" per PDF: "Debajo estará el botón 'Enviar', donde al apretar, el botón hará una animación de carga y finalmente otra animación ocupará la pantalla entera"
                        Button(
                            onClick = {
                                viewModel.dispatchPromo(context)
                            },
                            enabled = !isSending,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AcidEmerald,
                                contentColor = StudioBlack
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(50.dp)
                                .testTag("composer_enviar_button")
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = StudioBlack,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Sending...", fontWeight = FontWeight.Bold)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Send Promo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogs for URL, Google Drive and SoundCloud options
    if (showDeliveryDialog != null) {
        DeliveryOptionDialog(
            type = showDeliveryDialog!!,
            onDismiss = { showDeliveryDialog = null },
            onAdd = { option ->
                val added = viewModel.addDeliveryOption(option)
                if (!added) {
                    Toast.makeText(context, "Maximum 2 delivery methods allowed", Toast.LENGTH_SHORT).show()
                }
                showDeliveryDialog = null
            }
        )
    }
}

// STEP 1: Apartado Mensaje (Metadata)
@Composable
private fun Step1TrackInfo(
    draft: PromoDraft,
    viewModel: SendPromoViewModel,
    validationError: String?
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                viewModel.updateReleaseDate(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Artist name(s) (mandatory)
            Text(
                text = "Artist Name(s) *",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = draft.artistNames,
                onValueChange = { viewModel.updateArtistNames(it) },
                placeholder = { Text("e.g. Fisher, Cloonee, Michael Bibi...", color = TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("artist_names_input"),
                shape = RoundedCornerShape(12.dp),
                colors = defaultTextFieldColors(),
                singleLine = true
            )
        }

        item {
            // Track title (mandatory)
            Text(
                text = "Track Title *",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = draft.trackTitle,
                onValueChange = { viewModel.updateTrackTitle(it) },
                placeholder = { Text("e.g. Dancefloor Weapon, Sunset Groove...", color = TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("track_title_input"),
                shape = RoundedCornerShape(12.dp),
                colors = defaultTextFieldColors(),
                singleLine = true
            )
        }

        item {
            // Release type: Original, Remix, Mashup (per PDF)
            Text(
                text = "Release Type",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReleaseType.entries.forEach { type ->
                    val isSelected = draft.releaseType == type.name
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (isSelected) NeonCyan else StudioBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.updateReleaseType(type.name) }
                            .testTag("release_type_${type.name}"),
                        color = if (isSelected) StudioCardElevated else StudioCard
                    ) {
                        Text(
                            text = type.displayName,
                            color = if (isSelected) NeonCyan else TextGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // If Demo is selected, display the 3 options (Extended Mix, Radio Edit, Original Mix) - choose one
        if (draft.releaseType == ReleaseType.DEMO.name) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    color = StudioCardElevated
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Demo Mix Version * (Select one)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DemoMixType.entries.forEach { mixType ->
                                val isChosen = draft.demoMixType == mixType.displayName
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            1.dp,
                                            if (isChosen) NeonCyan else StudioBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.updateDemoMixType(mixType.displayName) }
                                        .testTag("demo_mix_${mixType.name}"),
                                    color = if (isChosen) NeonCyan.copy(alpha = 0.15f) else StudioCard
                                ) {
                                    Text(
                                        text = mixType.displayName,
                                        color = if (isChosen) NeonCyan else TextGray,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // If Remix or Mashup is selected, display mandatory text box for original artist per request
        if (draft.releaseType == ReleaseType.REMIX.name || draft.releaseType == ReleaseType.MASHUP.name) {
            val isMashup = draft.releaseType == ReleaseType.MASHUP.name
            item {
                Text(
                    text = "Original Artist Name *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SunsetAmber
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = draft.originalArtistName,
                    onValueChange = { viewModel.updateOriginalArtistName(it) },
                    placeholder = {
                        Text(
                            if (isMashup) "e.g. Daft Punk, Queen, Madonna..." else "e.g. Peggy Gou, Jamie Jones, CamelPhat...",
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("original_artist_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = defaultTextFieldColors(),
                    singleLine = true
                )
            }
        }

        // Mandatory Genre field
        item {
            Text(
                text = "Genre *",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = draft.genre,
                onValueChange = { viewModel.updateGenre(it) },
                placeholder = { Text("e.g. Tech House, Melodic Techno, Afro House...", color = TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("genre_input"),
                shape = RoundedCornerShape(12.dp),
                colors = defaultTextFieldColors(),
                singleLine = true
            )
        }

        // Mandatory BPM and Musical Key / Scale fields
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BPM *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = draft.bpm,
                        onValueChange = { viewModel.updateBpm(it) },
                        placeholder = { Text("e.g. 126", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bpm_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = defaultTextFieldColors(),
                        singleLine = true
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Key / Scale *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = draft.musicalKey,
                        onValueChange = { viewModel.updateMusicalKey(it) },
                        placeholder = { Text("e.g. F min, 8A, G#", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("musical_key_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = defaultTextFieldColors(),
                        singleLine = true
                    )
                }
            }
        }

        item {
            // Release date (per PDF)
            Text(
                text = "Release Date",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                    .clickable { datePickerDialog.show() }
                    .testTag("release_date_picker_button"),
                color = StudioCard
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = draft.releaseDate.ifBlank { "Select release date" },
                        color = if (draft.releaseDate.isNotBlank()) TextWhite else TextMuted,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Calendar",
                        tint = NeonCyan
                    )
                }
            }
        }

        item {
            val isDemo = draft.releaseType == ReleaseType.DEMO.name
            // Record label release: Disabled if Demo format per user request
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        if (isDemo) StudioBorder.copy(alpha = 0.3f) else StudioBorder,
                        RoundedCornerShape(14.dp)
                    ),
                color = if (isDemo) StudioCard.copy(alpha = 0.4f) else StudioCard
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Record Label Release?",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDemo) TextMuted else TextWhite,
                                    fontSize = 14.sp
                                )
                                if (isDemo) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(Disabled for Demos)",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                text = if (isDemo) {
                                    "Ineligible: Demos are submitted directly to labels for signing consideration"
                                } else if (draft.isLabelRelease) {
                                    "Yes (Signed to label)"
                                } else {
                                    "No (Independent / Self-Released)"
                                },
                                color = if (isDemo) TextMuted else if (draft.isLabelRelease) AcidEmerald else TextGray,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = !isDemo && draft.isLabelRelease,
                            onCheckedChange = { checked ->
                                if (!isDemo) {
                                    viewModel.updateLabelRelease(checked, draft.labelName)
                                }
                            },
                            enabled = !isDemo,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StudioBlack,
                                checkedTrackColor = AcidEmerald,
                                uncheckedTrackColor = StudioCardElevated,
                                disabledCheckedTrackColor = StudioCardElevated,
                                disabledUncheckedTrackColor = StudioCardElevated.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("is_label_switch")
                        )
                    }

                    if (!isDemo && draft.isLabelRelease) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = draft.labelName,
                            onValueChange = { viewModel.updateLabelRelease(true, it) },
                            placeholder = { Text("Label name (e.g. Defected, Afterlife, Drumcode)", color = TextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("label_name_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = defaultTextFieldColors(),
                            singleLine = true
                        )
                    }
                }
            }
        }

        item {
            // Real name (optional)
            Text(
                text = "Real Name (optional)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = draft.realName,
                onValueChange = { viewModel.updateRealName(it) },
                placeholder = { Text("Your full legal name", color = TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("real_name_input"),
                shape = RoundedCornerShape(12.dp),
                colors = defaultTextFieldColors(),
                singleLine = true
            )
        }

        item {
            // City, country (optional)
            Text(
                text = "City, Country (optional)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = draft.cityCountry,
                onValueChange = { viewModel.updateCityCountry(it) },
                placeholder = { Text("e.g. Ibiza, Spain / London, UK", color = TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("city_country_input"),
                shape = RoundedCornerShape(12.dp),
                colors = defaultTextFieldColors(),
                singleLine = true
            )
        }

        if (validationError != null) {
            item {
                Text(
                    text = validationError,
                    color = ErrorRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

// STEP 2: Mensaje (2000 caracteres) + Mandatory Track details + Fast Gemini AI Assistant
@Composable
private fun Step2PitchAndAi(
    draft: PromoDraft,
    viewModel: SendPromoViewModel,
    isAiLoading: Boolean,
    aiSuggestion: String?
) {
    var soundNotes by remember { mutableStateOf("") }
    val isRemixOrMashup = draft.releaseType == ReleaseType.REMIX.name || draft.releaseType == ReleaseType.MASHUP.name

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mandatory metadata fields inside Message section per user instruction:
        // "En el apartado de Mensaje sumar casillas obligatorias de Texto para BPM, Escala y Genero. Si se seleccionó remix, desplejar cuadro de texto obligatorio para poner el nombre del artista original"
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
                color = StudioCard
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Track Details (Mandatory)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    // If Demo format, display selected mix version
                    if (draft.releaseType == ReleaseType.DEMO.name) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonCyan.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Demo Submission: ${draft.demoMixType} (Restricted to 'Labels' group)",
                                    fontSize = 12.sp,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // If Remix or Mashup, show mandatory original artist
                    if (isRemixOrMashup) {
                        Column {
                            Text(
                                text = "Original Artist Name *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SunsetAmber
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = draft.originalArtistName,
                                onValueChange = { viewModel.updateOriginalArtistName(it) },
                                placeholder = {
                                    Text(
                                        if (draft.releaseType == ReleaseType.MASHUP.name) "Original artist being mashed up..." else "Original artist of the track...",
                                        color = TextMuted
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("step2_original_artist_name_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = defaultTextFieldColors(),
                                singleLine = true
                            )
                        }
                    }

                    // Genre
                    Column {
                        Text(
                            text = "Genre *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = draft.genre,
                            onValueChange = { viewModel.updateGenre(it) },
                            placeholder = { Text("e.g. Tech House, Melodic Techno, Afro House", color = TextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("step2_genre_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = defaultTextFieldColors(),
                            singleLine = true
                        )
                    }

                    // BPM & Scale side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BPM *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = draft.bpm,
                                onValueChange = { viewModel.updateBpm(it) },
                                placeholder = { Text("e.g. 126", color = TextMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("step2_bpm_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = defaultTextFieldColors(),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Key / Scale *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = draft.musicalKey,
                                onValueChange = { viewModel.updateMusicalKey(it) },
                                placeholder = { Text("e.g. F min / 8A", color = TextMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("step2_musical_key_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = defaultTextFieldColors(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        item {
            // Gemini AI Quick Actions
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(NeonCyan, ElectricViolet)),
                        RoundedCornerShape(16.dp)
                    ),
                color = StudioCardElevated
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini AI DJ Assistant",
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                fontSize = 14.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AcidEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⚡ Ultra Fast",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcidEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Generate a club-ready pitch or polish your text with authentic electronic music terminology in seconds.",
                        color = TextGray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.generateAiPitch(soundNotes) },
                            enabled = !isAiLoading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricViolet,
                                contentColor = TextWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("generate_pitch_button")
                        ) {
                            Text("✨ Create Pitch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.polishCurrentPitch() },
                            enabled = !isAiLoading && draft.pitchMessage.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioBorder,
                                contentColor = NeonCyan
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("polish_pitch_button")
                        ) {
                            Text("🔥 Polish Text", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.generateTagsAndBpm() },
                            enabled = !isAiLoading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioBorder,
                                contentColor = AcidEmerald
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tags_bpm_button")
                        ) {
                            Text("🎧 BPM & Tags", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isAiLoading) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚡ Generating club pitch with Gemini Flash Lite...",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (aiSuggestion != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            color = StudioBlack.copy(alpha = 0.6f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = aiSuggestion,
                                    fontSize = 12.sp,
                                    color = AcidEmerald,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Dismiss",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    modifier = Modifier
                                        .clickable { viewModel.clearAiSuggestion() }
                                        .align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // Message (2000 characters) per PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Promo Message",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = "${draft.pitchMessage.length} / 2000",
                    fontSize = 12.sp,
                    color = if (draft.pitchMessage.length > 1900) SunsetAmber else TextMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = draft.pitchMessage,
                onValueChange = { viewModel.updatePitchMessage(it) },
                placeholder = {
                    Text(
                        "Write your promo message for DJs, requested feedback, set support, club highlights...",
                        color = TextMuted
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .testTag("pitch_message_input"),
                shape = RoundedCornerShape(12.dp),
                colors = defaultTextFieldColors(),
                maxLines = 12
            )
        }
    }
}

// STEP 3: Forma de envio (dos maximo per PDF)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step3DeliveryOptions(
    draft: PromoDraft,
    viewModel: SendPromoViewModel,
    onOpenUrlDialog: () -> Unit,
    onPickFile: () -> Unit,
    onOpenDriveDialog: () -> Unit,
    onOpenSoundCloudDialog: () -> Unit
) {
    val selectedCount = draft.deliveryOptions.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(14.dp)),
                color = StudioCard
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Delivery Method",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedCount <= 2) StudioCardElevated else ErrorRed.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$selectedCount / 2 selected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCount <= 2) NeonCyan else ErrorRed
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose up to two options maximum as specified.",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }
        }

        // Active delivery options chips
        if (draft.deliveryOptions.isNotEmpty()) {
            item {
                Text(
                    text = "Configured options:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    draft.deliveryOptions.forEachIndexed { index, opt ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, AcidEmerald.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                            color = StudioCardElevated
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (opt.type) {
                                            DeliveryType.ATTACHMENT.name -> Icons.Default.AttachFile
                                            DeliveryType.GOOGLE_DRIVE.name -> Icons.Default.CloudQueue
                                            DeliveryType.SOUNDCLOUD.name -> Icons.Default.GraphicEq
                                            else -> Icons.Default.Link
                                        },
                                        contentDescription = null,
                                        tint = AcidEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = DeliveryType.valueOf(opt.type).displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextWhite
                                        )
                                        Text(
                                            text = opt.value,
                                            fontSize = 12.sp,
                                            color = TextGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (opt.isPrivateSoundCloud && opt.secretToken.isNotBlank()) {
                                            Text(
                                                text = "🔒 Secret link: ${opt.secretToken}",
                                                fontSize = 11.sp,
                                                color = SunsetAmber
                                            )
                                        }
                                    }
                                }

                                TextButton(onClick = { viewModel.removeDeliveryOption(index) }) {
                                    Text("Remove", color = ErrorRed, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4 Available options per PDF: Link, Attachment, Google Drive, SoundCloud
        item {
            DeliveryChoiceItem(
                title = "Direct Download Link",
                description = "Dropbox, WeTransfer, Hypeddit, custom web link...",
                icon = Icons.Default.Link,
                enabled = selectedCount < 2,
                onClick = onOpenUrlDialog
            )
        }

        item {
            DeliveryChoiceItem(
                title = "Audio Attachment (.mp3, .wav, .flac, .zip...)",
                description = "Select audio file from local storage",
                icon = Icons.Default.AttachFile,
                enabled = selectedCount < 2,
                onClick = onPickFile
            )
        }

        item {
            DeliveryChoiceItem(
                title = "Connect with Google Drive",
                description = "Access Google Drive account and share file links",
                icon = Icons.Default.CloudQueue,
                enabled = selectedCount < 2,
                onClick = onOpenDriveDialog
            )
        }

        item {
            DeliveryChoiceItem(
                title = "Connect with SoundCloud",
                description = "Select SoundCloud track. If private, secret token included",
                icon = Icons.Default.GraphicEq,
                enabled = selectedCount < 2,
                onClick = onOpenSoundCloudDialog
            )
        }
    }
}

@Composable
private fun DeliveryChoiceItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, if (enabled) StudioBorder else StudioBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick() },
        color = if (enabled) StudioCard else StudioCard.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (enabled) StudioCardElevated else StudioBlack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) NeonCyan else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (enabled) TextWhite else TextMuted
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextGray
                )
            }
            Text(
                text = "+ Add",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) NeonCyan else TextMuted
            )
        }
    }
}

// STEP 4: Apartado Destinatarios & Preview (per PDF)
@Composable
private fun Step4RecipientsAndPreview(
    draft: PromoDraft,
    groups: List<PromoGroup>,
    viewModel: SendPromoViewModel
) {
    val finalRecipients = viewModel.getFinalRecipients()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Formatted Preview Summary per PDF:
        // Subject: Artist Name - Track Title (Release Type) - Record Label
        // Body preview
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
                color = StudioCardElevated
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Formatted message ready to send",
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Subject:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = draft.buildSubject(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = StudioBorder
                    )

                    Text(
                        text = "Message Body:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = draft.buildBody(),
                        fontSize = 12.sp,
                        color = TextGray,
                        lineHeight = 18.sp,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // If Demo format is active: Inform user that promo is restricted to Labels group
        val isDemo = draft.releaseType == ReleaseType.DEMO.name
        val labelsGroup = groups.firstOrNull { it.isProtected || it.name.equals("Labels", ignoreCase = true) }
        val displayedGroups = if (isDemo) {
            if (labelsGroup != null) listOf(labelsGroup) else groups.filter { it.name.equals("Labels", ignoreCase = true) }
        } else {
            groups
        }

        if (isDemo) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    color = StudioCardElevated
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Demo Submission Mode Active",
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Demo format can ONLY be sent to the 'Labels' group for A&R consideration.",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Recipients section per PDF
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isDemo) "Target Record Label Group" else "Target Contact Groups",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "${finalRecipients.size} emails will receive this ${if (isDemo) "demo" else "promo"}",
                        fontSize = 12.sp,
                        color = AcidEmerald,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (!isDemo) {
                    TextButton(onClick = { viewModel.selectAllGroups() }) {
                        Text("Select all", color = NeonCyan, fontSize = 12.sp)
                    }
                }
            }
        }

        if (displayedGroups.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = StudioCard
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isDemo) "No 'Labels' group found in database." else "No contact groups in database.",
                            color = TextWhite,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isDemo) "Please go to the Database to configure your record label submission emails." else "You can add new groups from the 'Database' button on the home screen.",
                            color = TextGray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(displayedGroups, key = { it.id }) { group ->
                GroupRecipientAccordion(
                    group = group,
                    isGroupSelected = if (isDemo) true else (draft.selectedGroupIds.contains(group.id) || draft.selectedGroupIds.isEmpty()),
                    onToggleGroup = {
                        if (!isDemo) {
                            viewModel.toggleGroupSelection(group.id)
                        }
                    },
                    isEmailSelected = { email -> viewModel.isEmailSelected(email) },
                    onToggleEmail = { email -> viewModel.toggleEmailExclusion(email) }
                )
            }
        }
    }
}

// Accordion item for each group with dropdown arrow, search box, and individual email checkboxes per PDF
@Composable
private fun GroupRecipientAccordion(
    group: PromoGroup,
    isGroupSelected: Boolean,
    onToggleGroup: () -> Unit,
    isEmailSelected: (String) -> Boolean,
    onToggleEmail: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var emailSearchQuery by remember { mutableStateOf("") }

    val filteredEmails = remember(group.emails, emailSearchQuery) {
        if (emailSearchQuery.isBlank()) {
            group.emails
        } else {
            group.emails.filter { it.contains(emailSearchQuery, ignoreCase = true) }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (isGroupSelected) NeonCyan.copy(alpha = 0.5f) else StudioBorder,
                RoundedCornerShape(14.dp)
            ),
        color = StudioCard
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Group Row with Checkbox, Title, and Dropdown Arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isGroupSelected,
                    onCheckedChange = { onToggleGroup() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NeonCyan,
                        checkmarkColor = StudioBlack
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded }
                ) {
                    Text(
                        text = group.name,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${group.emails.size} contacts",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                // Dropdown arrow per PDF: "Al lado de cada grupo habrá una flecha hacia abajo que al presionar mostrará..."
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("group_expand_arrow_${group.id}")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = NeonCyan
                    )
                }
            }

            // Accordion expanded body
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCardElevated)
                        .padding(14.dp)
                ) {
                    // Search box for emails per PDF: "Un cuadro de búsqueda para correos"
                    OutlinedTextField(
                        value = emailSearchQuery,
                        onValueChange = { emailSearchQuery = it },
                        placeholder = { Text("Search emails in this group...", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_emails_input_${group.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = defaultTextFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Assigned contacts (uncheck to exclude):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Individual email checkboxes per PDF: "Los correos asignados a ese grupo, que podrán desmarcarse"
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        filteredEmails.forEach { email ->
                            val selected = isEmailSelected(email)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleEmail(email) }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { onToggleEmail(email) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AcidEmerald,
                                        checkmarkColor = StudioBlack
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = email,
                                    fontSize = 13.sp,
                                    color = if (selected) TextWhite else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog for Link, Google Drive and SoundCloud input
@Composable
private fun DeliveryOptionDialog(
    type: DeliveryType,
    onDismiss: () -> Unit,
    onAdd: (DeliveryOption) -> Unit
) {
    var urlValue by remember { mutableStateOf("") }
    var isPrivateSoundCloud by remember { mutableStateOf(false) }
    var secretToken by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (type) {
                    DeliveryType.LINK -> "Add Download Link"
                    DeliveryType.GOOGLE_DRIVE -> "Google Drive Link"
                    DeliveryType.SOUNDCLOUD -> "Connect with SoundCloud"
                    else -> "Configure Delivery"
                },
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when (type) {
                        DeliveryType.LINK -> "Enter direct download URL (WeTransfer, Dropbox, etc.):"
                        DeliveryType.GOOGLE_DRIVE -> "Enter shared Google Drive link:"
                        DeliveryType.SOUNDCLOUD -> "Enter SoundCloud track URL:"
                        else -> "Enter URL:"
                    },
                    fontSize = 13.sp,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = urlValue,
                    onValueChange = {
                        urlValue = it
                        error = null
                    },
                    placeholder = {
                        Text(
                            when (type) {
                                DeliveryType.LINK -> "https://..."
                                DeliveryType.GOOGLE_DRIVE -> "https://drive.google.com/..."
                                DeliveryType.SOUNDCLOUD -> "https://soundcloud.com/..."
                                else -> "https://..."
                            },
                            color = TextMuted
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = defaultTextFieldColors(),
                    singleLine = true
                )

                // SoundCloud private track option per PDF: "Si la pista está en privado, se enviará un enlace secreto"
                if (type == DeliveryType.SOUNDCLOUD) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SunsetAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Is this track private?",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Switch(
                            checked = isPrivateSoundCloud,
                            onCheckedChange = { isPrivateSoundCloud = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StudioBlack,
                                checkedTrackColor = SunsetAmber
                            )
                        )
                    }

                    if (isPrivateSoundCloud) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = secretToken,
                            onValueChange = { secretToken = it },
                            placeholder = { Text("Secret token or private URL (s-xxxx...)", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = defaultTextFieldColors(),
                            singleLine = true
                        )
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = error!!, color = ErrorRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlValue.isBlank()) {
                        error = "Please enter a valid URL"
                        return@Button
                    }
                    onAdd(
                        DeliveryOption(
                            type = type.name,
                            value = urlValue.trim(),
                            isPrivateSoundCloud = isPrivateSoundCloud,
                            secretToken = secretToken.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = StudioBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        },
        containerColor = StudioCardElevated
    )
}

@Composable
private fun defaultTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonCyan,
    unfocusedBorderColor = StudioBorder,
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite
)
