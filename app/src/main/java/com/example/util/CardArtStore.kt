package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class CardArtPreferences(
    val finishId: String? = null,
    val photoPath: String? = null,
    val logoPath: String? = null,
)

/**
 * Persists user-selected real card photos, bank/brand logos, and card finish presets
 * inside internal app storage (`filesDir/card_art/`) using zero-permission Photo Picker URIs.
 */
object CardArtStore {
    private const val PREFS_NAME = "spendly_card_art_prefs"
    private const val DIR_NAME = "card_art"

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun artDir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCardArt(context: Context, key: String): CardArtPreferences {
        if (key.isBlank()) return CardArtPreferences()
        val p = prefs(context)
        val finishId = p.getString("finish_$key", null)?.takeIf { it.isNotBlank() }
        val photoFile = File(artDir(context), "bg_$key.jpg")
        val logoFile = File(artDir(context), "logo_$key.png")
        val legacyLogo = File(artDir(context), "logo_$key.jpg")
        val resolvedLogo = when {
            logoFile.exists() && logoFile.length() > 0L -> logoFile.absolutePath
            legacyLogo.exists() && legacyLogo.length() > 0L -> legacyLogo.absolutePath
            else -> null
        }
        return CardArtPreferences(
            finishId = finishId,
            photoPath = if (photoFile.exists() && photoFile.length() > 0L) photoFile.absolutePath else null,
            logoPath = resolvedLogo,
        )
    }

    fun setFinishId(context: Context, key: String, finishId: String?) {
        if (key.isBlank()) return
        val editor = prefs(context).edit()
        if (finishId.isNullOrBlank()) {
            editor.remove("finish_$key")
        } else {
            editor.putString("finish_$key", finishId)
        }
        editor.apply()
        _version.update { it + 1 }
    }

    suspend fun saveCardPhotoFromUri(context: Context, key: String, uri: Uri): String? =
        saveScaledBitmap(
            context = context,
            uri = uri,
            outFile = File(artDir(context), "bg_$key.jpg"),
            maxDimPx = 1080,
            format = Bitmap.CompressFormat.JPEG,
            quality = 88,
        )

    suspend fun saveCardLogoFromUri(context: Context, key: String, uri: Uri): String? =
        saveScaledBitmap(
            context = context,
            uri = uri,
            outFile = File(artDir(context), "logo_$key.png"),
            maxDimPx = 360,
            format = Bitmap.CompressFormat.PNG,
            quality = 100,
        )

    fun clearCardPhoto(context: Context, key: String) {
        if (key.isBlank()) return
        File(artDir(context), "bg_$key.jpg").delete()
        _version.update { it + 1 }
    }

    fun clearCardLogo(context: Context, key: String) {
        if (key.isBlank()) return
        File(artDir(context), "logo_$key.png").delete()
        File(artDir(context), "logo_$key.jpg").delete()
        _version.update { it + 1 }
    }

    fun copyDraftArtToAccount(context: Context, draftKey: String, accountKey: String) {
        if (draftKey.isBlank() || accountKey.isBlank() || draftKey == accountKey) return
        val dir = artDir(context)
        val draftFinish = prefs(context).getString("finish_$draftKey", null)
        if (draftFinish != null) {
            prefs(context).edit().putString("finish_$accountKey", draftFinish).remove("finish_$draftKey").apply()
        }
        val draftBg = File(dir, "bg_$draftKey.jpg")
        val targetBg = File(dir, "bg_$accountKey.jpg")
        if (draftBg.exists()) {
            draftBg.copyTo(targetBg, overwrite = true)
            draftBg.delete()
        }
        val draftLogo = File(dir, "logo_$draftKey.png")
        val targetLogo = File(dir, "logo_$accountKey.png")
        if (draftLogo.exists()) {
            draftLogo.copyTo(targetLogo, overwrite = true)
            draftLogo.delete()
        }
        _version.update { it + 1 }
    }

    fun clearAllArt(context: Context, key: String) {
        if (key.isBlank()) return
        prefs(context).edit().remove("finish_$key").apply()
        clearCardPhoto(context, key)
        clearCardLogo(context, key)
    }

    private suspend fun saveScaledBitmap(
        context: Context,
        uri: Uri,
        outFile: File,
        maxDimPx: Int,
        format: Bitmap.CompressFormat,
        quality: Int,
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.applicationContext.contentResolver
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val origW = boundsOpts.outWidth.coerceAtLeast(1)
            val origH = boundsOpts.outHeight.coerceAtLeast(1)
            var sampleSize = 1
            while (origW / sampleSize > maxDimPx * 2 || origH / sampleSize > maxDimPx * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext null

            val scale = minOf(1f, maxDimPx.toFloat() / maxOf(decoded.width, decoded.height).toFloat())
            val finalBitmap = if (scale < 0.99f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true,
                ).also { if (it !== decoded) decoded.recycle() }
            } else {
                decoded
            }

            FileOutputStream(outFile).use { fos ->
                finalBitmap.compress(format, quality, fos)
                fos.flush()
            }
            finalBitmap.recycle()
            _version.update { it + 1 }
            outFile.absolutePath
        }.getOrNull()
    }
}
