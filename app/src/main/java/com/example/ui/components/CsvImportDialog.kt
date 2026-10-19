package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PromoGroup
import com.example.data.util.CsvContactParser
import com.example.ui.theme.AcidEmerald
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
import com.example.viewmodel.SendPromoViewModel

enum class TargetGroupMode {
    AUTO_FROM_CSV,
    EXISTING_GROUP,
    NEW_GROUP
}

@Composable
fun CsvImportDialog(
    viewModel: SendPromoViewModel,
    existingGroups: List<PromoGroup>,
    initialGroupId: String? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isImporting by viewModel.isImporting.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = File Picker, 1 = Paste / Quick Samples
    var rawCsvText by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    // Target Group destination settings
    var targetMode by remember {
        mutableStateOf(if (initialGroupId != null) TargetGroupMode.EXISTING_GROUP else TargetGroupMode.AUTO_FROM_CSV)
    }
    var selectedExistingGroupId by remember {
        mutableStateOf(initialGroupId ?: existingGroups.firstOrNull { it.isProtected || it.name.equals("Labels", true) }?.id ?: existingGroups.firstOrNull()?.id ?: "")
    }
    var newGroupName by remember { mutableStateOf("") }
    var showGroupDropdown by remember { mutableStateOf(false) }

    // Live parsed result
    val parseResult = remember(rawCsvText) {
        CsvContactParser.parse(rawCsvText)
    }

    // System File Picker for CSV
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                rawCsvText = content
                // Try resolving display name
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val name = cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) it.getString(nameIndex) else null
                    } else null
                }
                selectedFileName = name ?: "imported_contacts.csv"
                Toast.makeText(context, "Loaded: ${selectedFileName}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read CSV file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isImporting) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.5.dp, StudioBorder), RoundedCornerShape(18.dp)),
            color = StudioCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Import DJ & Label Contacts",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "Quickly populate database from CSV lists",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isImporting,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: File Picker vs Text / Samples
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = StudioBlack,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FilePresent,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 0) NeonCyan else TextGray
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Upload File",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 0) NeonCyan else TextGray
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 1) NeonCyan else TextGray
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Paste & Samples",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == 1) NeonCyan else TextGray
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Source Section
                    item {
                        if (selectedTab == 0) {
                            // File Picker Tab
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StudioCardElevated,
                                border = BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (selectedFileName == null) {
                                        Text(
                                            text = "Select a .csv contact file from your device",
                                            fontSize = 13.sp,
                                            color = TextGray
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                filePickerLauncher.launch("*/*")
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NeonCyan,
                                                contentColor = StudioBlack
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("choose_csv_file_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.UploadFile,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Browse CSV File", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = AcidEmerald,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = selectedFileName ?: "contacts.csv",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextWhite,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${parseResult.contacts.size} contacts found",
                                                        fontSize = 11.sp,
                                                        color = AcidEmerald
                                                    )
                                                }
                                            }
                                            TextButton(
                                                onClick = {
                                                    filePickerLauncher.launch("*/*")
                                                }
                                            ) {
                                                Text("Change", color = NeonCyan, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Paste / Quick Samples Tab
                            Column {
                                Text(
                                    text = "Quick DJ/Label presets:",
                                    fontSize = 11.sp,
                                    color = TextGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Preset 1: Labels
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = NeonCyan.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                rawCsvText = CsvContactParser.SAMPLE_LABELS_CSV
                                                selectedFileName = "top_labels_sample.csv"
                                                targetMode = TargetGroupMode.AUTO_FROM_CSV
                                            }
                                    ) {
                                        Text(
                                            text = "🏷️ Labels A&R (15)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NeonCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                        )
                                    }

                                    // Preset 2: DJs
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AcidEmerald.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, AcidEmerald.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                rawCsvText = CsvContactParser.SAMPLE_DJS_CSV
                                                selectedFileName = "top_djs_sample.csv"
                                                targetMode = TargetGroupMode.AUTO_FROM_CSV
                                            }
                                    ) {
                                        Text(
                                            text = "🎧 Top DJs (14)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AcidEmerald,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                        )
                                    }

                                    // Preset 3: Mixed
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SunsetAmber.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, SunsetAmber.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                rawCsvText = CsvContactParser.SAMPLE_MIXED_CSV
                                                selectedFileName = "mixed_contacts_sample.csv"
                                                targetMode = TargetGroupMode.AUTO_FROM_CSV
                                            }
                                    ) {
                                        Text(
                                            text = "🔀 Mixed Pool (9)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SunsetAmber,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = rawCsvText,
                                    onValueChange = {
                                        rawCsvText = it
                                        selectedFileName = if (it.isNotBlank()) "pasted_text.csv" else null
                                    },
                                    placeholder = {
                                        Text(
                                            "Paste CSV text here...\nExample:\nName, Email, Group\nSpinnin Records, demo@spinninrecords.com, Labels\nCarl Cox, promo@carlcox.com, Resident DJs",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = StudioBorder,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedContainerColor = StudioBlack,
                                        unfocusedContainerColor = StudioBlack
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                    maxLines = 5,
                                    minLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("csv_paste_input")
                                )
                            }
                        }
                    }

                    // Target Group Destination Section
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StudioCardElevated,
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "TARGET DESTINATION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray,
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Option 1: Auto from CSV
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { targetMode = TargetGroupMode.AUTO_FROM_CSV }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = targetMode == TargetGroupMode.AUTO_FROM_CSV,
                                        onClick = { targetMode = TargetGroupMode.AUTO_FROM_CSV },
                                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Group by CSV 'Group' column",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextWhite
                                        )
                                        Text(
                                            text = "Auto-assigns to 'Labels' or matches groups named in file",
                                            fontSize = 11.sp,
                                            color = TextGray
                                        )
                                    }
                                }

                                // Option 2: Into specific existing group
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { targetMode = TargetGroupMode.EXISTING_GROUP }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = targetMode == TargetGroupMode.EXISTING_GROUP,
                                        onClick = { targetMode = TargetGroupMode.EXISTING_GROUP },
                                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Import all into an existing group",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextWhite
                                        )

                                        if (targetMode == TargetGroupMode.EXISTING_GROUP) {
                                            val selectedGroup = existingGroups.find { it.id == selectedExistingGroupId }
                                                ?: existingGroups.firstOrNull()
                                            Box(modifier = Modifier.padding(top = 4.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = StudioBlack,
                                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { showGroupDropdown = true }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (selectedGroup?.isProtected == true || selectedGroup?.name.equals("Labels", true)) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Business,
                                                                    contentDescription = null,
                                                                    tint = NeonCyan,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = "${selectedGroup?.name} (System Protected)",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = NeonCyan
                                                                )
                                                            } else {
                                                                Icon(
                                                                    imageVector = Icons.Default.Group,
                                                                    contentDescription = null,
                                                                    tint = TextWhite,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = selectedGroup?.name ?: "Select group",
                                                                    fontSize = 12.sp,
                                                                    color = TextWhite
                                                                )
                                                            }
                                                        }
                                                        Text("▼", fontSize = 10.sp, color = TextGray)
                                                    }
                                                }

                                                DropdownMenu(
                                                    expanded = showGroupDropdown,
                                                    onDismissRequest = { showGroupDropdown = false },
                                                    modifier = Modifier.background(StudioCardElevated)
                                                ) {
                                                    existingGroups.forEach { grp ->
                                                        val isLabels = grp.isProtected || grp.name.equals("Labels", true)
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    if (isLabels) {
                                                                        Icon(Icons.Default.Business, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                                                        Spacer(Modifier.width(6.dp))
                                                                        Text("${grp.name} (Protected • ${grp.emails.size})", color = NeonCyan, fontWeight = FontWeight.Bold)
                                                                    } else {
                                                                        Icon(Icons.Default.Group, null, tint = TextWhite, modifier = Modifier.size(16.dp))
                                                                        Spacer(Modifier.width(6.dp))
                                                                        Text("${grp.name} (${grp.emails.size})", color = TextWhite)
                                                                    }
                                                                }
                                                            },
                                                            onClick = {
                                                                selectedExistingGroupId = grp.id
                                                                showGroupDropdown = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Option 3: Create new group
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { targetMode = TargetGroupMode.NEW_GROUP }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = targetMode == TargetGroupMode.NEW_GROUP,
                                        onClick = { targetMode = TargetGroupMode.NEW_GROUP },
                                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Create a new group for all contacts",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextWhite
                                        )

                                        if (targetMode == TargetGroupMode.NEW_GROUP) {
                                            OutlinedTextField(
                                                value = newGroupName,
                                                onValueChange = { newGroupName = it },
                                                placeholder = { Text("e.g. ADE 2026 Promos", fontSize = 12.sp, color = TextMuted) },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = NeonCyan,
                                                    unfocusedBorderColor = StudioBorder,
                                                    focusedTextColor = TextWhite,
                                                    unfocusedTextColor = TextWhite,
                                                    focusedContainerColor = StudioBlack,
                                                    unfocusedContainerColor = StudioBlack
                                                ),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp)
                                                    .testTag("new_group_import_input")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Parsed Statistics & Preview
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StudioCardElevated,
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "PARSED CONTACTS PREVIEW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray,
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Metric Badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Valid
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AcidEmerald.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, AcidEmerald.copy(alpha = 0.35f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${parseResult.contacts.size}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AcidEmerald
                                            )
                                            Text(text = "Valid Contacts", fontSize = 10.sp, color = TextGray)
                                        }
                                    }

                                    // Duplicates
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SunsetAmber.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, SunsetAmber.copy(alpha = 0.35f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${parseResult.duplicateRowsCount}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SunsetAmber
                                            )
                                            Text(text = "Duplicates Removed", fontSize = 10.sp, color = TextGray)
                                        }
                                    }

                                    // Groups
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = NeonCyan.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            val groupCount = when (targetMode) {
                                                TargetGroupMode.AUTO_FROM_CSV -> parseResult.detectedGroups.size.coerceAtLeast(1)
                                                else -> 1
                                            }
                                            Text(
                                                text = "$groupCount",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonCyan
                                            )
                                            Text(text = "Target Groups", fontSize = 10.sp, color = TextGray)
                                        }
                                    }
                                }

                                if (parseResult.contacts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Sample Rows:",
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        parseResult.contacts.take(5).forEach { contact ->
                                            val assignedGroup = when (targetMode) {
                                                TargetGroupMode.AUTO_FROM_CSV -> contact.group
                                                TargetGroupMode.EXISTING_GROUP -> existingGroups.find { it.id == selectedExistingGroupId }?.name ?: "Selected Group"
                                                TargetGroupMode.NEW_GROUP -> newGroupName.ifBlank { "New Group" }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = StudioBlack,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = contact.name.ifBlank { contact.email },
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = TextWhite,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = contact.email,
                                                            fontSize = 10.sp,
                                                            color = TextGray,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (assignedGroup.equals("Labels", true)) NeonCyan.copy(alpha = 0.15f) else StudioCardElevated,
                                                        border = BorderStroke(1.dp, if (assignedGroup.equals("Labels", true)) NeonCyan.copy(alpha = 0.4f) else StudioBorder)
                                                    ) {
                                                        Text(
                                                            text = assignedGroup,
                                                            fontSize = 10.sp,
                                                            color = if (assignedGroup.equals("Labels", true)) NeonCyan else TextGray,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (parseResult.contacts.size > 5) {
                                            Text(
                                                text = "+ ${parseResult.contacts.size - 5} more contacts ready to import",
                                                fontSize = 11.sp,
                                                color = TextMuted,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                } else if (rawCsvText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, null, tint = SunsetAmber, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "No valid email addresses found in the entered CSV content.",
                                            fontSize = 11.sp,
                                            color = SunsetAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isImporting
                    ) {
                        Text("Cancel", color = TextGray)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val canImport = parseResult.contacts.isNotEmpty() &&
                            (targetMode != TargetGroupMode.NEW_GROUP || newGroupName.isNotBlank()) &&
                            !isImporting

                    Button(
                        onClick = {
                            val targetGroupId = if (targetMode == TargetGroupMode.EXISTING_GROUP) selectedExistingGroupId else null
                            val targetGrpName = if (targetMode == TargetGroupMode.NEW_GROUP) newGroupName else null

                            viewModel.importContacts(
                                contacts = parseResult.contacts,
                                targetGroupId = targetGroupId,
                                targetGroupName = targetGrpName,
                                onComplete = { result ->
                                    Toast.makeText(
                                        context,
                                        "Successfully imported ${result.totalImported} contacts!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onDismiss()
                                }
                            )
                        },
                        enabled = canImport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcidEmerald,
                            contentColor = StudioBlack,
                            disabledContainerColor = StudioCardElevated,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("execute_csv_import_button")
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = StudioBlack,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importing...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val count = parseResult.contacts.size
                            Text(
                                text = if (count > 0) "Import $count Contacts" else "Import Contacts",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
