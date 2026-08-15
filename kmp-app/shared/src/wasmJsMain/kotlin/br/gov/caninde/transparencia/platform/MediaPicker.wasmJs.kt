package br.gov.caninde.transparencia.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import br.gov.caninde.transparencia.domain.PickedMedia
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.files.FileReader
import org.w3c.files.get
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get

@Composable
actual fun rememberMediaPicker(onPicked: (PickedMedia?) -> Unit): () -> Unit {
    val input = remember {
        (document.createElement("input") as HTMLInputElement).apply {
            type = "file"
            accept = "image/*,video/*"
            style.display = "none"
            document.body?.appendChild(this)
            addEventListener("change", { _: Event ->
                val file = files?.get(0)
                if (file == null) {
                    onPicked(null)
                    value = ""
                } else {
                    val reader = FileReader()
                    reader.onload = { _: Event ->
                        val buffer = reader.result as? ArrayBuffer
                        if (buffer == null) {
                            onPicked(null)
                        } else {
                            onPicked(
                                PickedMedia(
                                    bytes = arrayBufferToByteArray(buffer),
                                    fileName = file.name,
                                    mimeType = file.type.ifBlank { "application/octet-stream" },
                                ),
                            )
                        }
                        value = ""
                    }
                    reader.onerror = { _: Event ->
                        onPicked(null)
                        value = ""
                    }
                    reader.readAsArrayBuffer(file)
                }
            })
        }
    }
    return { input.click() }
}

private fun arrayBufferToByteArray(buffer: ArrayBuffer): ByteArray {
    val view = Int8Array(buffer)
    val length = view.length
    val bytes = ByteArray(length)
    for (index in 0 until length) {
        bytes[index] = view[index]
    }
    return bytes
}
