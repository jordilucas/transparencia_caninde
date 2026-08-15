package br.gov.caninde.transparencia.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import br.gov.caninde.transparencia.domain.PickedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberMediaPicker(onPicked: (PickedMedia?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) {
            onPicked(null)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val media = withContext(Dispatchers.IO) { readPickedMedia(context, uri) }
            onPicked(media)
        }
    }
    return { launcher.launch("image/*,video/*") }
}

private fun readPickedMedia(context: Context, uri: Uri): PickedMedia? {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri) ?: guessMimeType(uri)
    val fileName = queryDisplayName(context, uri) ?: defaultFileName(mimeType)
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.isEmpty()) return null
    return PickedMedia(bytes = bytes, fileName = fileName, mimeType = mimeType)
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            return cursor.getString(index)
        }
    }
    return null
}

private fun guessMimeType(uri: Uri): String {
    val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
}

private fun defaultFileName(mimeType: String): String =
    if (mimeType.startsWith("video/")) "video.mp4" else "foto.jpg"
