package com.example.parkmate.data.photo

import android.content.Context
import java.io.File

// Crea un nuovo file temporaneo per memorizzare una foto scattata durante una sessione di parcheggio
fun createPhotoFile(context: Context): File {
    // Definisce la cartella "photos" all'interno della memoria privata dell'app
    val dir = File(context.filesDir, "photos").apply { mkdirs() }
    
    // Genera un nome file univoco basato sul timestamp corrente per evitare sovrascritture
    return File(dir, "session_${System.currentTimeMillis()}.jpg")
}
