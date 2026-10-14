package com.example.parkmate.ui.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Decodifica un'immagine dal disco ridimensionandola per risparmiare memoria
private fun decodeSampled(path: String, reqSize: Int): Bitmap? {
    // Legge le dimensioni dell'immagine senza caricarla in memoria
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0)
        return null

    // Calcola il fattore di campionamento per avvicinarsi alla dimensione richiesta
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= reqSize && bounds.outHeight / (sample * 2) >= reqSize) {
        sample *= 2
    }
    
    // Decodifica l'immagine reale applicando il sampling
    val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return null

    // Gestione della rotazione automatica
    val rotation = when (
        ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    if (rotation == 0f)
        return bitmap

    // Applica la rotazione se necessario
    val matrix = Matrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// Helper asincrono per caricare e mantenere in memoria un'immagine bitmap in formato Compose.
@Composable
private fun rememberPhotoBitMap(path: String, reqSize: Int): ImageBitmap? = produceState<ImageBitmap?>(initialValue = null, path, reqSize) {
    value = withContext(Dispatchers.IO) {
        // Esegue la decodifica su thread per non bloccare l'interfaccia
        runCatching { decodeSampled(path, reqSize)?.asImageBitmap() }.getOrNull()
    }
}.value

// Visualizza una miniatura quadrata o rettangolare di una foto salvata.
@Composable
fun PhotoThumbnail(path: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val exists = remember(path) { File(path).exists() }
    val bitmap =
        if (exists)
            rememberPhotoBitMap(path, reqSize = 600)
        else
            null
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = bitmap != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            !exists -> Icon(Icons.Default.BrokenImage, contentDescription = "Immagine non trovata")
            bitmap == null -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else -> Image(
                bitmap = bitmap,
                contentDescription = "Foto del parcheggio",
                contentScale = ContentScale.Crop, // Ritaglia l'immagine per riempire lo spazio
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// Mostra la foto a tutto schermo
@Composable
fun FullScreenPhotoDialog(path: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Permette di occupare tutto lo schermo
    ) {
        // Carica una versione a risoluzione maggiore
        val bitmap = rememberPhotoBitMap(path, reqSize = 2048)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Foto a tutto schermo",
                    contentScale = ContentScale.Fit, // Mostra l'intera immagine senza ritagli
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator()
            }
        }
    }
}