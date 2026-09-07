from pathlib import Path

app_path = Path('app/src/main/java/ca/creativepixels/schoolstuff/SchoolStuffApp.kt')
text = app_path.read_text()


def require(value: str, label: str) -> None:
    if value not in text:
        raise SystemExit(f'Missing patch anchor: {label}')

# Imports for actual image thumbnails and enlarged preview dialog.
if 'import android.net.Uri\n' not in text:
    text = text.replace('import android.database.ContentObserver\n', 'import android.database.ContentObserver\nimport android.net.Uri\n', 1)
if 'import androidx.compose.ui.layout.ContentScale\n' not in text:
    text = text.replace('import androidx.compose.ui.graphics.vector.ImageVector\n', 'import androidx.compose.ui.graphics.vector.ImageVector\nimport androidx.compose.ui.layout.ContentScale\n', 1)
if 'import androidx.compose.ui.window.Dialog\n' not in text:
    text = text.replace('import androidx.compose.ui.unit.sp\n', 'import androidx.compose.ui.unit.sp\nimport androidx.compose.ui.window.Dialog\n', 1)
if 'import coil.compose.AsyncImage\n' not in text:
    text = text.replace('import ca.creativepixels.schoolstuff.data.SchoolItem\n', 'import ca.creativepixels.schoolstuff.data.SchoolItem\nimport coil.compose.AsyncImage\n', 1)

# Track whichever image the parent has opened from the gallery.
state_anchor = '    var editing by remember { mutableStateOf(false) }\n'
require(state_anchor, 'ChildScreen editing state')
if 'var previewDocument by remember' not in text:
    text = text.replace(
        state_anchor,
        state_anchor + '    var previewDocument by remember { mutableStateOf<SchoolDocument?>(null) }\n',
        1,
    )

# Replace the filename-only document rows with a horizontal visual gallery.
old_docs = '''                        val docs = vm.documents.filter { it.childId == childId && it.type == selectedDocType }
                        docs.forEach { doc ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                Icon(documentIcon(doc.type), null, tint = SchoolBlue)
                                Spacer(Modifier.width(10.dp))
                                Text(doc.title, color = Ink, modifier = Modifier.weight(1f), maxLines = 2)
                                TextButton(onClick = { vm.deleteDocument(doc.id) }) { Text("Remove") }
                            }
                        }
'''
require(old_docs, 'old filename-only document list')
new_docs = '''                        val docs = vm.documents.filter { it.childId == childId && it.type == selectedDocType }
                        if (docs.isEmpty()) {
                            Text(
                                "Nothing saved here yet.",
                                color = Ink.copy(alpha = .58f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Text(
                                "Tap a thumbnail to open it.",
                                color = Ink.copy(alpha = .58f),
                                fontSize = 12.sp
                            )
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                docs.forEachIndexed { index, doc ->
                                    DocumentGalleryTile(
                                        doc = doc,
                                        index = index,
                                        onOpenImage = { previewDocument = doc },
                                        onRemove = { vm.deleteDocument(doc.id) }
                                    )
                                }
                            }
                        }
'''
text = text.replace(old_docs, new_docs, 1)

# Add the enlarged image preview before the existing child edit dialog.
preview_anchor = '''    if (editing) {
        EditChildDialog(child, onDismiss = { editing = false }, onSave = { vm.updateChild(it); editing = false })
    }
}

@Composable
private fun InfoLine'''
require(preview_anchor, 'ChildScreen dialog footer')
preview_block = '''    previewDocument?.let { doc ->
        Dialog(onDismissRequest = { previewDocument = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            friendlyDocumentLabel(doc, 0),
                            color = Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { previewDocument = null }) { Text("Close") }
                    }
                    AsyncImage(
                        model = doc.uri,
                        contentDescription = doc.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Paper),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        friendlyDocumentDate(doc),
                        color = Ink.copy(alpha = .55f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (editing) {
        EditChildDialog(child, onDismiss = { editing = false }, onSave = { vm.updateChild(it); editing = false })
    }
}

@Composable
private fun DocumentGalleryTile(
    doc: SchoolDocument,
    index: Int,
    onOpenImage: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val mimeType = remember(doc.uri) {
        runCatching { context.contentResolver.getType(Uri.parse(doc.uri)) }.getOrNull().orEmpty()
    }
    val isImage = mimeType.startsWith("image/") || doc.title.endsWith(".png", true) ||
        doc.title.endsWith(".jpg", true) || doc.title.endsWith(".jpeg", true) ||
        doc.title.endsWith(".webp", true) || doc.title.endsWith(".gif", true)

    Card(
        modifier = Modifier.width(118.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isImage) Paper else SoftBlue)
                    .clickable {
                        if (isImage) {
                            onOpenImage()
                        } else {
                            openDocumentExternally(context, doc, mimeType)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isImage) {
                    AsyncImage(
                        model = doc.uri,
                        contentDescription = doc.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            documentIcon(doc.type),
                            contentDescription = null,
                            tint = SchoolBlue,
                            modifier = Modifier.size(34.dp)
                        )
                        Text("Open file", color = Ink.copy(alpha = .62f), fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                friendlyDocumentLabel(doc, index),
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(friendlyDocumentDate(doc), color = Ink.copy(alpha = .48f), fontSize = 10.sp)
            TextButton(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
            ) {
                Text("Remove", fontSize = 11.sp)
            }
        }
    }
}

private fun friendlyDocumentLabel(doc: SchoolDocument, index: Int): String {
    val genericName = doc.title.startsWith("file_", ignoreCase = true) ||
        doc.title.matches(Regex(".*[0-9a-fA-F]{16,}.*"))
    if (!genericName && doc.title.length <= 28) return doc.title

    val singular = when (doc.type) {
        DocumentType.REPORT_CARD -> "Report Card"
        DocumentType.ARTWORK -> "Artwork"
        DocumentType.PHOTO -> "Photo"
        else -> "Form"
    }
    return "$singular ${index + 1}"
}

private fun friendlyDocumentDate(doc: SchoolDocument): String =
    java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
        .format(java.util.Date(doc.addedAtMillis))

private fun openDocumentExternally(context: android.content.Context, doc: SchoolDocument, knownMime: String) {
    val uri = Uri.parse(doc.uri)
    val mime = knownMime.ifBlank { context.contentResolver.getType(uri).orEmpty().ifBlank { "*/*" } }
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(Intent.createChooser(intent, "Open school file")) }
}

@Composable
private fun InfoLine'''
text = text.replace(preview_anchor, preview_block, 1)

app_path.write_text(text)

# Bump the unmistakable installed version.
gradle_path = Path('app/build.gradle.kts')
gradle = gradle_path.read_text()
gradle = gradle.replace('versionCode = 10', 'versionCode = 11', 1)
gradle = gradle.replace('versionName = "0.1.9"', 'versionName = "0.1.10"', 1)
gradle_path.write_text(gradle)

print('Document thumbnail gallery + image preview + 0.1.10 build 11 patched.')
