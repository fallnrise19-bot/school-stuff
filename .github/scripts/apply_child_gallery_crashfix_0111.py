from pathlib import Path

app = Path('app/src/main/java/ca/creativepixels/schoolstuff/SchoolStuffApp.kt')
text = app.read_text()

# Add ImageRequest import for bounded thumbnail/preview decoding.
if 'import coil.request.ImageRequest\n' not in text:
    anchor = 'import coil.compose.AsyncImage\n'
    if anchor not in text:
        raise SystemExit('Missing AsyncImage import anchor')
    text = text.replace(anchor, anchor + 'import coil.request.ImageRequest\n', 1)

# Add gallery-open state.
state_anchor = '    var previewDocument by remember { mutableStateOf<SchoolDocument?>(null) }\n'
if state_anchor not in text:
    raise SystemExit('Missing previewDocument state anchor')
if 'var galleryOpen by remember' not in text:
    text = text.replace(
        state_anchor,
        state_anchor + '    var galleryOpen by remember { mutableStateOf(false) }\n',
        1,
    )

# Replace the inline thumbnail gallery with a lightweight summary so scrolling the
# child profile never causes content:// images to decode.
start_marker = '''        item {\n            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {\n                Section("Papers & Memories", SchoolPink) {\n'''
end_marker = '''\n        }\n    }\n\n    previewDocument?.let'''
start = text.find(start_marker)
end = text.find(end_marker, start)
if start == -1 or end == -1:
    raise SystemExit('Could not locate Papers & Memories block')

replacement = '''        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section("Papers & Memories", SchoolPink) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PapersArtStrip()
                        val childDocs = vm.documents.filter { it.childId == childId }
                        if (childDocs.isEmpty()) {
                            Text(
                                "Nothing saved here yet.",
                                color = Ink.copy(alpha = .58f),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else {
                            val imageCount = childDocs.count { doc ->
                                val lower = doc.title.lowercase()
                                lower.endsWith(".png") || lower.endsWith(".jpg") ||
                                    lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".gif")
                            }
                            Text(
                                "${childDocs.size} saved item${if (childDocs.size == 1) "" else "s"}" +
                                    if (imageCount > 0) " • $imageCount image${if (imageCount == 1) "" else "s"}" else "",
                                color = Ink.copy(alpha = .65f),
                                fontSize = 13.sp
                            )
                            OutlinedButton(onClick = { galleryOpen = true }) {
                                Icon(Icons.Rounded.PhotoCamera, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Open Gallery")
                            }
                        }
                        OutlinedButton(onClick = { documentLauncher.launch(arrayOf("image/*", "application/pdf")) }) {
                            Icon(Icons.Rounded.UploadFile, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Photo or File")
                        }
                    }
                }
            }
        }'''
text = text[:start] + replacement + text[end:]

# Add a separate gallery dialog. Images only decode after the user opens it.
gallery_dialog = '''

    if (galleryOpen) {
        Dialog(onDismissRequest = { galleryOpen = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${child.name}'s Gallery",
                            color = Ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { galleryOpen = false }) { Text("Close") }
                    }
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DocumentType.all.forEach { type ->
                            FilterChip(
                                selected = selectedDocType == type,
                                onClick = { selectedDocType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val galleryDocs = vm.documents.filter { it.childId == childId && it.type == selectedDocType }
                    if (galleryDocs.isEmpty()) {
                        Text(
                            "Nothing saved in $selectedDocType yet.",
                            color = Ink.copy(alpha = .58f),
                            modifier = Modifier.padding(vertical = 18.dp)
                        )
                    } else {
                        Text(
                            "Tap an item to enlarge or open it.",
                            color = Ink.copy(alpha = .58f),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            galleryDocs.forEachIndexed { index, doc ->
                                DocumentGalleryTile(
                                    doc = doc,
                                    index = index,
                                    onOpenImage = { previewDocument = doc },
                                    onRemove = { vm.deleteDocument(doc.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
'''
preview_anchor = '\n    previewDocument?.let'
if preview_anchor not in text:
    raise SystemExit('Missing preview dialog anchor')
text = text.replace(preview_anchor, gallery_dialog + preview_anchor, 1)

# Bound the full preview decode as well. This prevents very large camera images
# from requesting their original multi-megapixel bitmap into memory.
preview_old = '''                    AsyncImage(\n                        model = doc.uri,\n                        contentDescription = doc.title,'''
preview_new = '''                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(doc.uri))
                            .size(1600)
                            .crossfade(false)
                            .build(),
                        contentDescription = doc.title,'''
if preview_old not in text:
    raise SystemExit('Missing preview AsyncImage anchor')
text = text.replace(preview_old, preview_new, 1)

# Bound thumbnail decoding to a few hundred pixels.
tile_old = '''                    AsyncImage(\n                        model = doc.uri,\n                        contentDescription = doc.title,'''
tile_new = '''                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(doc.uri))
                            .size(320)
                            .crossfade(false)
                            .build(),
                        contentDescription = doc.title,'''
if tile_old not in text:
    raise SystemExit('Missing gallery tile AsyncImage anchor')
text = text.replace(tile_old, tile_new, 1)

app.write_text(text)

# Version bump.
gradle = Path('app/build.gradle.kts')
g = gradle.read_text()
if 'versionCode = 11' not in g or 'versionName = "0.1.10"' not in g:
    raise SystemExit('Unexpected current app version')
g = g.replace('versionCode = 11', 'versionCode = 12', 1)
g = g.replace('versionName = "0.1.10"', 'versionName = "0.1.11"', 1)
gradle.write_text(g)

print('Child gallery crash fix applied; version 0.1.11 build 12.')
