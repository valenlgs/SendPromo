package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PromoGroup
import com.example.ui.components.CsvImportDialog
import com.example.ui.theme.AcidEmerald
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.viewmodel.SendPromoViewModel

@Composable
fun DatabaseScreen(
    viewModel: SendPromoViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val groups by viewModel.groups.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showCsvImportDialog by remember { mutableStateOf(false) }
    var importTargetGroup by remember { mutableStateOf<PromoGroup?>(null) }
    var editingGroup by remember { mutableStateOf<PromoGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<PromoGroup?>(null) }

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
            // Header: Back button, Title, and Top-Right actions: "Import CSV" and "+" (Add Group)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("database_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Contact Database",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "${groups.size} groups configured",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }

                // Top right actions: Import CSV button and Add Group "+" button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = StudioCardElevated,
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showCsvImportDialog = true }
                            .testTag("import_csv_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "Import CSV",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import CSV",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }

                    // Top right "+" button per PDF
                    IconButton(
                        onClick = {
                            editingGroup = null
                            showDialog = true
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                            .testTag("add_group_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add group",
                            tint = StudioBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No contact groups created yet",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Quickly populate your database by importing a DJ or Label CSV file, or tap the '+' icon to create a group manually.",
                            color = TextGray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCsvImportDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = StudioBlack
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("empty_state_import_csv_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import DJ / Label CSV", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val labelsGroup = groups.firstOrNull { it.isProtected || it.name.equals("Labels", ignoreCase = true) }
                val regularGroups = groups.filterNot { it.id == labelsGroup?.id || it.name.equals("Labels", ignoreCase = true) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (labelsGroup != null) {
                        item(key = "labels_section_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 4.dp),
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
                                    text = "RECORD LABELS (SYSTEM GROUP)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NeonCyan.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = "Protected • Editable only",
                                        fontSize = 11.sp,
                                        color = NeonCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        item(key = "labels_group_card") {
                            GroupCardItem(
                                group = labelsGroup,
                                isSpecialLabels = true,
                                canDelete = false,
                                onEdit = {
                                    editingGroup = labelsGroup
                                    showDialog = true
                                },
                                onDelete = {},
                                onImportCsv = {
                                    importTargetGroup = labelsGroup
                                    showCsvImportDialog = true
                                }
                            )
                        }

                        item(key = "regular_section_header") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = TextGray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DJ PROMO GROUPS (${regularGroups.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    items(regularGroups, key = { it.id }) { group ->
                        GroupCardItem(
                            group = group,
                            isSpecialLabels = false,
                            canDelete = true,
                            onEdit = {
                                editingGroup = group
                                showDialog = true
                            },
                            onDelete = {
                                groupToDelete = group
                            },
                            onImportCsv = {
                                importTargetGroup = group
                                showCsvImportDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Create / Edit Group Pop-up Dialog
    if (showDialog) {
        GroupEditDialog(
            existingGroup = editingGroup,
            onDismiss = { showDialog = false },
            onSave = { name, emails ->
                viewModel.saveGroup(name, emails, editingGroup?.id ?: "")
                showDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (groupToDelete != null) {
        val target = groupToDelete!!
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text(text = "Delete group", color = TextWhite) },
            text = {
                Text(
                    text = "Are you sure you want to delete the group '${target.name}' and its ${target.emails.size} emails?",
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(target.id)
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Cancel", color = TextGray)
                }
            },
            containerColor = StudioCardElevated
        )
    }

    // CSV Import Dialog for DJ & Label contact lists
    if (showCsvImportDialog) {
        CsvImportDialog(
            viewModel = viewModel,
            existingGroups = groups,
            initialGroupId = importTargetGroup?.id,
            onDismiss = {
                showCsvImportDialog = false
                importTargetGroup = null
            }
        )
    }
}

@Composable
private fun GroupCardItem(
    group: PromoGroup,
    isSpecialLabels: Boolean = false,
    canDelete: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onImportCsv: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isSpecialLabels) NeonCyan.copy(alpha = 0.6f) else StudioBorder,
                RoundedCornerShape(16.dp)
            ),
        color = if (isSpecialLabels) StudioCardElevated else StudioCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isSpecialLabels) NeonCyan else AcidEmerald)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = group.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isSpecialLabels) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Protected",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isSpecialLabels) "${group.emails.size} label A&R contacts" else "${group.emails.size} assigned contacts",
                    fontSize = 13.sp,
                    color = if (isSpecialLabels) NeonCyan else AcidEmerald,
                    fontWeight = FontWeight.Medium
                )

                if (group.emails.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = group.emails.take(3).joinToString(", ") + if (group.emails.size > 3) "..." else "",
                        fontSize = 12.sp,
                        color = TextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action button / 3 dots menu
            if (!canDelete) {
                // For Labels group: direct Import CSV and Edit buttons (Cannot be deleted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onImportCsv,
                        modifier = Modifier.testTag("group_import_csv_${group.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Import CSV to Labels",
                            tint = NeonCyan
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("group_edit_button_${group.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Labels",
                            tint = NeonCyan
                        )
                    }
                }
            } else {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("group_overflow_menu_${group.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextWhite
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .background(StudioCardElevated)
                            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Import CSV to group", color = TextWhite)
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onImportCsv()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Edit group", color = TextWhite)
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = ErrorRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Delete group", color = ErrorRed)
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

// Pop-up dialog per PDF: "se colocará primero el nombre del grupo y las direcciones de correo se agregarán cada una. Debajo habrá un botón + para agregar más direcciones de correo."
@Composable
private fun GroupEditDialog(
    existingGroup: PromoGroup?,
    onDismiss: () -> Unit,
    onSave: (name: String, emails: List<String>) -> Unit
) {
    var groupName by remember { mutableStateOf(existingGroup?.name ?: "") }
    val emailList = remember {
        mutableStateListOf<String>().apply {
            if (existingGroup != null && existingGroup.emails.isNotEmpty()) {
                addAll(existingGroup.emails)
            } else {
                add("") // Start with one email input
            }
        }
    }
    val isLabelsGroup = existingGroup?.isProtected == true || existingGroup?.name.equals("Labels", ignoreCase = true)
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isLabelsGroup) "Edit 'Labels' Group" else if (existingGroup == null) "New Contact Group" else "Edit Group",
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                if (isLabelsGroup) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Protected group for Demos • Editable only, cannot be deleted",
                        fontSize = 12.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Group Name",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = {
                            groupName = it
                            validationError = null
                        },
                        placeholder = { Text("e.g. Tech House DJs, Melodic Labels...", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Email Addresses",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonCyan
                    )
                }

                items(emailList.indices.toList()) { index ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = emailList[index],
                            onValueChange = { emailList[index] = it },
                            placeholder = { Text("email@domain.com", color = TextMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("group_email_input_$index"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = com.example.ui.theme.ElectricViolet,
                                unfocusedBorderColor = StudioBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        if (emailList.size > 1) {
                            IconButton(
                                onClick = { emailList.removeAt(index) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove email",
                                    tint = ErrorRed
                                )
                            }
                        }
                    }
                }

                item {
                    // "+" button to add more email addresses per PDF
                    TextButton(
                        onClick = { emailList.add("") },
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .testTag("add_more_email_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add another email address",
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (validationError != null) {
                    item {
                        Text(
                            text = validationError ?: "",
                            color = ErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isBlank()) {
                        validationError = "Please enter a group name"
                        return@Button
                    }
                    val validEmails = emailList.map { it.trim() }.filter { it.isNotBlank() }
                    if (validEmails.isEmpty()) {
                        validationError = "Add at least one email address"
                        return@Button
                    }
                    onSave(groupName, validEmails)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = StudioBlack
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_group_button")
            ) {
                Text("Save Group", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        },
        containerColor = StudioCardElevated,
        shape = RoundedCornerShape(20.dp)
    )
}
