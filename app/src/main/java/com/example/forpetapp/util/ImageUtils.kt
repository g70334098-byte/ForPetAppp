package com.example.forpetapp.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

fun savePetImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val outputFile = File(context.filesDir, "saved_pet_profile.jpg")
        val outputStream = FileOutputStream(outputFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(outputFile).toString()
    } catch (_: Exception) {
        uri.toString()
    }
}
