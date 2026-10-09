package fr.tbrun.djkids.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/** Nom lisible d'un fichier choisi via le sélecteur système. */
fun queryDisplayName(context: Context, uri: Uri): String {
    var name = "Morceau"
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = c.getString(idx) ?: name
                }
            }
    }
    return name.substringBeforeLast('.')
}
