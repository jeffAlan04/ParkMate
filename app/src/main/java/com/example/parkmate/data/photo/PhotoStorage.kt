package com.example.parkmate.data.photo

import android.content.Context
import java.io.File

fun createPhotoFile(context: Context): File {
    val dir = File(context.filesDir, "photos").apply { mkdirs() }
    return File(dir, "session_${System.currentTimeMillis()}.jpg")
}
