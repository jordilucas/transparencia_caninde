package br.gov.caninde.transparencia.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import br.gov.caninde.transparencia.domain.PickedMedia
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerMode
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberMediaPicker(onPicked: (PickedMedia?) -> Unit): () -> Unit {
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>,
            ) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: run {
                    onPicked(null)
                    return
                }
                url.startAccessingSecurityScopedResource()
                val data = NSData.dataWithContentsOfURL(url)
                if (data == null) {
                    url.stopAccessingSecurityScopedResource()
                    onPicked(null)
                    return
                }
                val bytes = data.toByteArray()
                url.stopAccessingSecurityScopedResource()
                if (bytes.isEmpty()) {
                    onPicked(null)
                    return
                }
                val fileName = url.lastPathComponent ?: "anexo"
                onPicked(
                    PickedMedia(
                        bytes = bytes,
                        fileName = fileName,
                        mimeType = guessMimeType(fileName),
                    ),
                )
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                onPicked(null)
            }
        }
    }

    return {
        val picker = UIDocumentPickerViewController(
            documentTypes = listOf("public.image", "public.movie"),
            inMode = UIDocumentPickerMode.UIDocumentPickerModeImport,
        )
        picker.delegate = delegate
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            picker,
            animated = true,
            completion = null,
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val bytes = ByteArray(length)
    if (length == 0) return bytes
    bytes.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}

private fun guessMimeType(fileName: String): String {
    val lower = fileName.lowercase()
    return when {
        lower.endsWith(".mp4") || lower.endsWith(".mov") -> "video/mp4"
        lower.endsWith(".png") -> "image/png"
        lower.endsWith(".webp") -> "image/webp"
        lower.endsWith(".gif") -> "image/gif"
        else -> "image/jpeg"
    }
}
