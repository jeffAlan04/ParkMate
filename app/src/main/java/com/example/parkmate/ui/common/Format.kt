package com.example.parkmate.ui.common

import java.text.NumberFormat
import java.util.Locale

// Formatta un importo in euro
fun formatPrice(value: Double): String = NumberFormat.getCurrencyInstance(Locale.ITALY).format(value)

fun String.toPriceOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

// Formatta un valore in millisecondi in formato HH:mm:ss o mm:ss.
fun formatElapsedDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.ITALY, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ITALY, "%02d:%02d", minutes, seconds)
    }
}