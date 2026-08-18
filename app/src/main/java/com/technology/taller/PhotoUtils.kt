package com.technology.taller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Convierte fotos a Base64 comprimido para guardarlas dentro del documento
 * de Firestore (igual que la versión web), pero redimensionadas para no
 * exceder el límite de 1MB por documento.
 */
object PhotoUtils {

    private const val ANCHO_MAX = 700
    private const val CALIDAD_JPEG = 55

    fun bitmapABase64(bitmap: Bitmap): String {
        val redimensionado = redimensionar(bitmap)
        val stream = ByteArrayOutputStream()
        redimensionado.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, stream)
        val bytes = stream.toByteArray()
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun uriABitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun base64ABitmap(base64: String): Bitmap? {
        return try {
            val limpio = base64.substringAfter(",", base64)
            val bytes = Base64.decode(limpio, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun redimensionar(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= ANCHO_MAX) return bitmap
        val ratio = ANCHO_MAX.toFloat() / bitmap.width
        val alto = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, ANCHO_MAX, alto, true)
    }

    /**
     * Escribe las fotos (base64) como archivos JPEG temporales en la caché
     * de la app y devuelve sus Uri "content://" (vía FileProvider), listas
     * para compartir por WhatsApp u otra app — no se pueden compartir
     * directamente como texto base64.
     */
    fun fotosATemporales(context: Context, fotos: List<String>): List<Uri> {
        val carpeta = java.io.File(context.cacheDir, "fotos").apply { mkdirs() }
        return fotos.mapIndexedNotNull { index, base64 ->
            try {
                val bitmap = base64ABitmap(base64) ?: return@mapIndexedNotNull null
                val archivo = java.io.File(carpeta, "foto_$index.jpg")
                java.io.FileOutputStream(archivo).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", archivo
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
