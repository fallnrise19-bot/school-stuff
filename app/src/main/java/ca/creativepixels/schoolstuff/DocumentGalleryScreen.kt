package ca.creativepixels.schoolstuff

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ca.creativepixels.schoolstuff.data.DocumentType
import ca.creativepixels.schoolstuff.data.SchoolDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * The gallery deliberately lives outside ChildScreen. Opening a child profile must not initialize
 * file providers, image decoders, preview dialogs, or gallery scroll state.
 */
@Composable
internal fun DocumentGalleryScreen(
    vm: SchoolStuffViewModel,
    childId: String,
    onBack: () -> Unit
) {
    val child = vm.child(childId) ?: return
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(DocumentType.PHOTO) }
    var preview by remember { mutableStateOf<SchoolDocument?>(null) }

    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val name = displayName(context, uri) ?: "School file"
            vm.addDocument(
                SchoolDocument(
                    childId = childId,
                    title = name,
                    type = selectedType,
                    uri = uri.toString()
                )
            )
        }
    }

    val documents = vm.documents.filter { it.childId == childId && it.type == selectedType }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Ink)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "${child.name}'s Gallery",
                        color = Ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                    Text(
                        "Artwork, photos, report cards and forms",
                        color = Ink.copy(alpha = .6f),
                        fontSize = 13.sp
                    )
                }
                MockupArtImage(MockupAsset.CAMERA, Modifier.size(58.dp), "Gallery")
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                DocumentType.all.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type) }
                    )
                }
            }
        }
        item {
            Button(
                onClick = { documentLauncher.launch(arrayOf("image/*", "application/pdf")) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Rounded.UploadFile, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Add to $selectedType")
            }
        }
        if (documents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftPink),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MockupArtImage(documentArt(selectedType), Modifier.size(82.dp), selectedType)
                        Text(
                            "Nothing saved in $selectedType yet.",
                            color = Ink.copy(alpha = .65f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        } else {
            itemsIndexed(documents, key = { _, document -> document.id }) { index, document ->
                DocumentRow(
                    document = document,
                    index = index,
                    onPreview = { preview = document },
                    onRemove = { vm.deleteDocument(document.id) }
                )
            }
        }
    }

    preview?.let { document ->
        DocumentPreviewDialog(document = document, onDismiss = { preview = null })
    }
}

@Composable
private fun DocumentRow(
    document: SchoolDocument,
    index: Int,
    onPreview: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val mimeType = remember(document.uri) { documentMimeType(context, document) }
    val image = isImage(document, mimeType)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (image) onPreview() else openDocument(context, document, mimeType)
                }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (image) Paper else SoftBlue),
                contentAlignment = Alignment.Center
            ) {
                if (image) {
                    SafeDocumentImage(
                        uri = document.uri,
                        maxDimension = 320,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(documentIcon(document.type), null, tint = SchoolBlue, modifier = Modifier.size(38.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    friendlyDocumentLabel(document, index),
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(friendlyDocumentDate(document), color = Ink.copy(alpha = .52f), fontSize = 11.sp)
                Text(
                    if (image) "Tap to view" else "Tap to open file",
                    color = SchoolBlue,
                    fontSize = 12.sp
                )
            }
            TextButton(onClick = onRemove) { Text("Remove") }
        }
    }
}

@Composable
private fun DocumentPreviewDialog(document: SchoolDocument, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = androidx.compose.ui.graphics.Color.White, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        friendlyDocumentLabel(document, 0),
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                SafeDocumentImage(
                    uri = document.uri,
                    maxDimension = 1400,
                    modifier = Modifier.fillMaxWidth().height(480.dp).clip(RoundedCornerShape(16.dp)).background(Paper),
                    contentScale = ContentScale.Fit
                )
                Text(
                    friendlyDocumentDate(document),
                    color = Ink.copy(alpha = .52f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun SafeDocumentImage(
    uri: String,
    maxDimension: Int,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri, maxDimension) {
        value = decodeBoundedBitmap(context, uri, maxDimension)
    }

    if (bitmap != null) {
        Image(bitmap!!, contentDescription = null, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PhotoCamera, null, tint = Ink.copy(alpha = .35f), modifier = Modifier.size(38.dp))
        }
    }
}

private suspend fun decodeBoundedBitmap(context: Context, uriText: String, maxDimension: Int): ImageBitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(uriText)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sample = 1
            val largestSide = max(bounds.outWidth, bounds.outHeight)
            while (largestSide / (sample * 2) >= maxDimension) sample *= 2

            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
            }
        }.getOrNull()
    }

private fun displayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()

private fun documentMimeType(context: Context, document: SchoolDocument): String =
    runCatching { context.contentResolver.getType(Uri.parse(document.uri)) }.getOrNull().orEmpty()

private fun isImage(document: SchoolDocument, mimeType: String): Boolean =
    mimeType.startsWith("image/") || document.title.endsWith(".png", true) ||
        document.title.endsWith(".jpg", true) || document.title.endsWith(".jpeg", true) ||
        document.title.endsWith(".webp", true) || document.title.endsWith(".gif", true) ||
        document.title.endsWith(".heic", true) || document.title.endsWith(".heif", true)

private fun friendlyDocumentLabel(document: SchoolDocument, index: Int): String {
    val genericName = document.title.startsWith("file_", ignoreCase = true) ||
        document.title.matches(Regex(".*[0-9a-fA-F]{16,}.*"))
    if (!genericName && document.title.length <= 34) return document.title

    val singular = when (document.type) {
        DocumentType.REPORT_CARD -> "Report Card"
        DocumentType.ARTWORK -> "Artwork"
        DocumentType.PHOTO -> "Photo"
        else -> "Form"
    }
    return "$singular ${index + 1}"
}

private fun friendlyDocumentDate(document: SchoolDocument): String =
    java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
        .format(java.util.Date(document.addedAtMillis))

private fun openDocument(context: Context, document: SchoolDocument, knownMimeType: String) {
    val uri = Uri.parse(document.uri)
    val mimeType = knownMimeType.ifBlank { "*/*" }
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(Intent.createChooser(intent, "Open school file")) }
}

private fun documentIcon(type: String): ImageVector = when (type) {
    DocumentType.REPORT_CARD -> Icons.Rounded.Star
    DocumentType.ARTWORK -> Icons.Rounded.Palette
    DocumentType.PHOTO -> Icons.Rounded.PhotoCamera
    else -> Icons.Rounded.Description
}

private fun documentArt(type: String): MockupAsset = when (type) {
    DocumentType.ARTWORK -> MockupAsset.PALETTE
    DocumentType.PHOTO -> MockupAsset.CAMERA
    DocumentType.REPORT_CARD -> MockupAsset.FOLDER
    else -> MockupAsset.DOCUMENT
}
