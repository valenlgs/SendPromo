package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.DeliveryOption
import com.example.data.model.DeliveryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AttachmentHelper {
    private const val TAG = "AttachmentHelper"

    suspend fun prepareAttachmentOption(context: Context, sourceUri: Uri): DeliveryOption? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // Attempt to take persistable URI permission if permitted
            try {
                contentResolver.takePersistableUriPermission(
                    sourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Not all document providers support persistable permissions; continue
            }

            var fileName = "promo_audio_${System.currentTimeMillis()}.mp3"
            try {
                contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            fileName = name
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not resolve display name, using fallback", e)
            }

            // Copy file to cacheDir/promo_attachments for reliable FileProvider sharing with Gmail
            val promoDir = File(context.cacheDir, "promo_attachments")
            if (!promoDir.exists()) {
                promoDir.mkdirs()
            }

            val sanitizedFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(promoDir, sanitizedFileName)

            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val shareableUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )

            DeliveryOption(
                type = DeliveryType.ATTACHMENT.name,
                value = fileName,
                fileName = fileName,
                uriString = shareableUri.toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error copying promo attachment to cache", e)
            null
        }
    }
}
