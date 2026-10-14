package com.example.parkmate.ui.common

import java.text.NumberFormat
import java.util.Locale

// Formatta un importo in euro
fun formatPrice(value: Double): String = NumberFormat.getCurrencyInstance(Locale.ITALY).format(value)
