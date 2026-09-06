package ca.creativepixels.schoolstuff

import android.Manifest
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import ca.creativepixels.schoolstuff.calendar.CalendarProviderRepository
import ca.creativepixels.schoolstuff.data.*
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class V2Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    CALENDAR("Calendar", Icons.Rounded.CalendarMonth),
    KIDS("Kids", Icons.Rounded.Groups),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@Composable
fun SchoolStuffAppV2(vm: SchoolStuffViewModel) {
    SchoolStuffTheme {
        var tab by remember { mutableStateOf(V2Tab.HOME) }
        var childPage by remember { mutableStateOf<String?>(null) }
        var addingThing by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = Paper,
            bottomBar = {
                if (childPage == null && !addingThing) {
                    NavigationBar(containerColor = Color.White) {
                        V2Tab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    addingThing -> AddThingScreenV2(vm, onBack = { addingThing = false })
                    childPage != null -> ChildScreenV2(
                        vm = vm,
                        childId = childPage!!,
                        onBack = { childPage = null },
                        onAddThing = { addingThing = true }
                    )
                    tab == V2Tab.HOME -> HomeScreenV2(vm, onAdd = { addingThing = true }, onChild = { childPage = it })
                    tab == V2Tab.CALENDAR -> CalendarScreenV2(vm)
                    tab == V2Tab.KIDS -> KidsScreenV2(vm, onChild = { childPage = it })
                    else -> SettingsScreenV2(vm)
                }
            }
        }
    }
}

@Composable
private fun HeaderV2(title: String, subtitle: String, back: (() -> Unit)? = null, note: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (back != null) {
            IconButton(onClick = back) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Ink) }
        }
        Column(Modifier.weight(1f)) {
            Box {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .widthIn(min = 90.dp, max = 260.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SchoolYellow.copy(alpha = .55f))
                )
                Text(title, color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
            Text(subtitle, color = Ink.copy(alpha = .65f), fontSize = 14.sp)
        }
        if (!note.isNullOrBlank()) {
            Surface(color = SoftYellow, shape = RoundedCornerShape(12.dp)) {
                Text(note, color = Ink, fontSize = 11.sp, modifier = Modifier.padding(9.dp), lineHeight = 14.sp)
            }
        }
    }
}

private fun childColorV2(key: String): Color = when (key) {
    "green" -> SchoolGreen
    "pink" -> SchoolPink
    "yellow" -> SchoolYellow
    else -> SchoolBlue
}

@Composable
private fun ChildBadgeV2(child: ChildProfile?, size: Int = 42) {
    val color = childColorV2(child?.colorKey ?: "yellow")
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(color.copy(alpha = .28f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            child?.name?.firstOrNull()?.uppercase() ?: "F",
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 2.5).sp
        )
    }
}

private fun categoryIconV2(category: String): ImageVector = when (category) {
    Category.FOOD_DAY -> Icons.Rounded.LocalPizza
    Category.FORM_DUE -> Icons.Rounded.Description
    Category.SPIRIT_DAY -> Icons.Rounded.Checkroom
    Category.BRING_ITEM -> Icons.Rounded.Backpack
    Category.LIBRARY -> Icons.Rounded.MenuBook
    Category.GYM -> Icons.Rounded.DirectionsRun
    Category.HOMEWORK -> Icons.Rounded.Assignment
    else -> Icons.Rounded.Event
}

private fun categoryColorV2(category: String): Color = when (category) {
    Category.FOOD_DAY -> SchoolRed
    Category.FORM_DUE -> SchoolPink
    Category.SPIRIT_DAY -> SchoolBlue
    Category.BRING_ITEM -> SchoolGreen
    Category.LIBRARY -> SchoolGreen
    Category.GYM -> SchoolBlue
    Category.HOMEWORK -> SchoolYellow
    else -> SchoolBlue
}

@Composable
private fun SectionV2(title: String, tint: Color = SchoolBlue, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().background(tint.copy(alpha = .15f)).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(title, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), content = content)
        }
    }
}

@Composable
private fun SchoolRowV2(vm: SchoolStuffViewModel, item: SchoolItem, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChildBadgeV2(vm.child(item.childId), 38)
        Spacer(Modifier.width(10.dp))
        Icon(categoryIconV2(item.category), null, tint = categoryColorV2(item.category), modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${vm.childName(item.childId)} • ${item.category}", color = Ink.copy(alpha = .55f), fontSize = 12.sp)
        }
        trailing?.invoke()
    }
}

@Composable
private fun SchoolVisualHeroV2() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("The stuff the fridge keeps pretending you can see.", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black, lineHeight = 22.sp)
                Spacer(Modifier.height(5.dp))
                Text("Lunch days, library books, forms, homework and the infamous surprise picture day.", color = Ink.copy(alpha = .62f), fontSize = 12.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    StickerV2(Icons.Rounded.Backpack, SoftBlue, SchoolBlue)
                    StickerV2(Icons.Rounded.MenuBook, SoftGreen, SchoolGreen)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    StickerV2(Icons.Rounded.PhotoCamera, SoftPink, SchoolRed)
                    StickerV2(Icons.Rounded.Palette, SoftYellow, SchoolYellow)
                }
            }
        }
    }
}

@Composable
private fun StickerV2(icon: ImageVector, background: Color, tint: Color) {
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(29.dp))
    }
}

@Composable
private fun HomeScreenV2(vm: SchoolStuffViewModel, onAdd: () -> Unit, onChild: (String) -> Unit) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    var selectedChild by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderV2("School Stuff", "Little things. Big school days. ♥", note = "You got this!") }
        item { SchoolVisualHeroV2() }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = selectedChild == null, onClick = { selectedChild = null }, label = { Text("All") })
                vm.children.forEach { child ->
                    FilterChip(
                        selected = selectedChild == child.id,
                        onClick = { selectedChild = child.id },
                        label = { Text(child.name) }
                    )
                }
            }
        }
        val upcomingPicture = vm.upcoming(14).firstOrNull { it.second.title.contains("picture", ignoreCase = true) }
        if (upcomingPicture != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftYellow),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = .8f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PhotoCamera, null, tint = SchoolRed, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Picture Day coming up", color = Ink, fontWeight = FontWeight.Bold)
                            Text(upcomingPicture.first.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), color = Ink.copy(alpha = .65f))
                        }
                    }
                }
            }
        }
        item { DaySectionV2("Today", today, SoftBlue, vm, selectedChild) }
        item { DaySectionV2("Tomorrow", tomorrow, SoftGreen, vm, selectedChild) }
        item {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 50.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SchoolRed)
            ) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add School Thing")
            }
        }
        item {
            Text(
                "Tap a child for teacher info, homework, artwork and papers.",
                color = Ink.copy(alpha = .6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                vm.children.forEach { child ->
                    AssistChip(onClick = { onChild(child.id) }, label = { Text(child.name) }, leadingIcon = { ChildBadgeV2(child, 28) })
                }
            }
        }
    }
}

@Composable
private fun DaySectionV2(title: String, date: LocalDate, tint: Color, vm: SchoolStuffViewModel, selectedChild: String?) {
    val list = vm.itemsFor(date).filter { selectedChild == null || it.childId == selectedChild }
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionV2("$title  •  ${date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}", tint) {
            if (list.isEmpty()) {
                Text("Nothing on the list. Suspiciously peaceful.", color = Ink.copy(alpha = .6f), modifier = Modifier.padding(vertical = 14.dp))
            } else {
                list.forEachIndexed { index, item ->
                    SchoolRowV2(vm, item)
                    if (index != list.lastIndex) HorizontalDivider(color = Ink.copy(alpha = .08f))
                }
            }
        }
    }
}

@Composable
private fun KidsScreenV2(vm: SchoolStuffViewModel, onChild: (String) -> Unit) {
    var addDialog by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderV2("Kids", "Different schedules. One parent brain. ♥", note = "Same kids.\nBrighter days.") }
        items(vm.children, key = { it.id }) { child ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onChild(child.id) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = childColorV2(child.colorKey).copy(alpha = .10f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChildBadgeV2(child, 58)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(child.name, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Ink)
                            Text(child.grade.ifBlank { "Grade not set" }, color = Ink.copy(alpha = .58f))
                        }
                        StickerV2(Icons.Rounded.School, SoftYellow, SchoolYellow)
                    }
                    Spacer(Modifier.height(10.dp))
                    val routines = vm.items.filter { it.childId == child.id && it.repeat == Repeat.WEEKLY }.take(3)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        routines.forEach { item ->
                            AssistChip(onClick = { onChild(child.id) }, label = { Text(item.title, maxLines = 1) })
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { addDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 50.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Rounded.PersonAdd, null)
                Spacer(Modifier.width(6.dp))
                Text("Add Child")
            }
        }
    }

    if (addDialog) {
        var name by remember { mutableStateOf("") }
        var grade by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addDialog = false },
            title = { Text("Add Child") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(grade, { grade = it }, label = { Text("Grade") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = { vm.addChild(name, grade); addDialog = false }, enabled = name.isNotBlank()) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ChildScreenV2(vm: SchoolStuffViewModel, childId: String, onBack: () -> Unit, onAddThing: () -> Unit) {
    val child = vm.child(childId) ?: return
    val context = LocalContext.current
    var selectedDocType by remember { mutableStateOf(DocumentType.FORM) }
    var editing by remember { mutableStateOf(false) }

    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull() ?: "School file"
            vm.addDocument(SchoolDocument(childId = childId, title = name, type = selectedDocType, uri = uri.toString()))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderV2(child.name, "Your school dashboard. ♥", back = onBack, note = child.grade.ifBlank { "School year" }) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = childColorV2(child.colorKey).copy(alpha = .10f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChildBadgeV2(child, 62)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(child.name, fontSize = 24.sp, color = Ink, fontWeight = FontWeight.Black)
                            Text(child.grade, color = Ink.copy(alpha = .58f))
                        }
                        IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, "Edit") }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StickerV2(Icons.Rounded.MenuBook, SoftGreen, SchoolGreen)
                        StickerV2(Icons.Rounded.PhotoCamera, SoftPink, SchoolRed)
                        StickerV2(Icons.Rounded.Palette, SoftYellow, SchoolYellow)
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SectionV2("This Week", SchoolGreen) {
                    val next = vm.upcoming(7).filter { it.second.childId == childId }.take(6)
                    if (next.isEmpty()) Text("Nothing scheduled yet.", modifier = Modifier.padding(vertical = 12.dp))
                    next.forEachIndexed { index, pair ->
                        val (date, item) = pair
                        SchoolRowV2(vm, item) {
                            Text(date.format(DateTimeFormatter.ofPattern("EEE")), color = Ink.copy(alpha = .6f), fontSize = 12.sp)
                        }
                        if (index != next.lastIndex) HorizontalDivider(color = Ink.copy(alpha = .07f))
                    }
                    TextButton(onClick = onAddThing) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(4.dp)); Text("Add school thing") }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SectionV2("Teacher & School", SchoolBlue) {
                    InfoLineV2(Icons.Rounded.Person, "Teacher", child.teacherName.ifBlank { "Not added" })
                    InfoLineV2(Icons.Rounded.Email, "Email", child.teacherEmail.ifBlank { "Not added" })
                    InfoLineV2(Icons.Rounded.Call, "Class phone", child.classroomPhone.ifBlank { "Not added" })
                    InfoLineV2(Icons.Rounded.School, "School", child.schoolName.ifBlank { "Not added" })
                    InfoLineV2(Icons.Rounded.Call, "Office", child.schoolPhone.ifBlank { "Not added" })
                    if (child.specialNotes.isNotBlank()) Text("Note: ${child.specialNotes}", color = Ink.copy(alpha = .7f), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    TextButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, null); Spacer(Modifier.width(4.dp)); Text("Edit school info") }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SectionV2("Homework & Forms", SchoolYellow) {
                    val tasks = vm.items.filter { it.childId == childId && it.category in listOf(Category.HOMEWORK, Category.FORM_DUE, Category.BRING_ITEM) }
                    if (tasks.isEmpty()) Text("No homework or forms tracked yet.", modifier = Modifier.padding(vertical = 12.dp))
                    tasks.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item.completed, onCheckedChange = { vm.toggleComplete(item.id) })
                            Box(Modifier.weight(1f)) { SchoolRowV2(vm, item) }
                        }
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SectionV2("Papers & Memories", SchoolPink) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DocumentType.all.forEach { type ->
                            FilterChip(selected = selectedDocType == type, onClick = { selectedDocType = type }, label = { Text(type) })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    val docs = vm.documents.filter { it.childId == childId && it.type == selectedDocType }
                    if (docs.isEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = SoftYellow), shape = RoundedCornerShape(16.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                StickerV2(documentIconV2(selectedDocType), Color.White, SchoolBlue)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Nothing saved here yet", color = Ink, fontWeight = FontWeight.Bold)
                                    Text("Add artwork, photos, forms or report cards and they’ll show up visually here.", color = Ink.copy(alpha = .6f), fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        docs.forEach { doc -> DocumentCardV2(doc, onOpen = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(doc.uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            }
                        }, onRemove = { vm.deleteDocument(doc.id) }) }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { documentLauncher.launch(arrayOf("image/*", "application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.UploadFile, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Photo or File")
                    }
                }
            }
        }
    }

    if (editing) {
        EditChildDialogV2(child, onDismiss = { editing = false }, onSave = { vm.updateChild(it); editing = false })
    }
}

@Composable
private fun DocumentCardV2(doc: SchoolDocument, onOpen: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(17.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (doc.type == DocumentType.ARTWORK || doc.type == DocumentType.PHOTO) {
                AsyncImage(
                    model = doc.uri,
                    contentDescription = doc.title,
                    modifier = Modifier.size(82.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.size(82.dp).clip(RoundedCornerShape(14.dp)).background(if (doc.type == DocumentType.REPORT_CARD) SoftGreen else SoftYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(documentIconV2(doc.type), null, tint = SchoolBlue, modifier = Modifier.size(38.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(doc.title, color = Ink, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(doc.type, color = Ink.copy(alpha = .55f), fontSize = 12.sp)
                if (doc.needsSignature) Text("Needs signature", color = SchoolRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            IconButton(onClick = onRemove) { Icon(Icons.Rounded.Delete, "Remove", tint = SchoolRed) }
        }
    }
}

@Composable
private fun InfoLineV2(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Icon(icon, null, tint = SchoolBlue, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Ink.copy(alpha = .62f), fontSize = 12.sp, modifier = Modifier.width(72.dp))
        Text(value, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun documentIconV2(type: String): ImageVector = when (type) {
    DocumentType.REPORT_CARD -> Icons.Rounded.Assessment
    DocumentType.ARTWORK -> Icons.Rounded.Palette
    DocumentType.PHOTO -> Icons.Rounded.PhotoCamera
    else -> Icons.Rounded.Description
}

@Composable
private fun EditChildDialogV2(child: ChildProfile, onDismiss: () -> Unit, onSave: (ChildProfile) -> Unit) {
    var grade by remember { mutableStateOf(child.grade) }
    var teacher by remember { mutableStateOf(child.teacherName) }
    var email by remember { mutableStateOf(child.teacherEmail) }
    var phone by remember { mutableStateOf(child.classroomPhone) }
    var room by remember { mutableStateOf(child.room) }
    var school by remember { mutableStateOf(child.schoolName) }
    var schoolPhone by remember { mutableStateOf(child.schoolPhone) }
    var notes by remember { mutableStateOf(child.specialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${child.name}'s school info") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.heightIn(max = 470.dp)) {
                item { OutlinedTextField(grade, { grade = it }, label = { Text("Grade") }, singleLine = true) }
                item { OutlinedTextField(teacher, { teacher = it }, label = { Text("Teacher") }, singleLine = true) }
                item { OutlinedTextField(email, { email = it }, label = { Text("Teacher email") }, singleLine = true) }
                item { OutlinedTextField(phone, { phone = it }, label = { Text("Classroom phone") }, singleLine = true) }
                item { OutlinedTextField(room, { room = it }, label = { Text("Room") }, singleLine = true) }
                item { OutlinedTextField(school, { school = it }, label = { Text("School") }, singleLine = true) }
                item { OutlinedTextField(schoolPhone, { schoolPhone = it }, label = { Text("School phone") }, singleLine = true) }
                item { OutlinedTextField(notes, { notes = it }, label = { Text("Special notes") }) }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(child.copy(grade = grade, teacherName = teacher, teacherEmail = email, classroomPhone = phone, room = room, schoolName = school, schoolPhone = schoolPhone, specialNotes = notes))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CalendarScreenV2(vm: SchoolStuffViewModel) {
    val today = LocalDate.now()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { HeaderV2("Calendar", "The week without the fridge blindness. ♥", note = "This week") }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StickerV2(Icons.Rounded.CalendarMonth, SoftBlue, SchoolBlue)
                StickerV2(Icons.Rounded.LocalPizza, SoftPink, SchoolRed)
                StickerV2(Icons.Rounded.MenuBook, SoftGreen, SchoolGreen)
                StickerV2(Icons.Rounded.PhotoCamera, SoftYellow, SchoolYellow)
            }
        }
        items((0L..13L).toList()) { offset ->
            val date = today.plusDays(offset)
            val dayItems = vm.itemsFor(date)
            if (dayItems.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (offset == 0L) SoftBlue else Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")), color = Ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        dayItems.forEach { SchoolRowV2(vm, it) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddThingScreenV2(vm: SchoolStuffViewModel, onBack: () -> Unit) {
    var childId by remember { mutableStateOf(vm.children.firstOrNull()?.id.orEmpty()) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.SCHOOL_EVENT) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var repeat by remember { mutableStateOf(Repeat.ONE_TIME) }
    var reminder by remember { mutableStateOf(Reminder.NIGHT_BEFORE) }
    var notes by remember { mutableStateOf("") }
    var needsHome by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderV2("Add School Thing", "Get it out of your brain and into the app. ♥", back = onBack, note = "Future you says thanks.") }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SchoolVisualHeroV2()
                Text("Who is this for?", color = Ink, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip(selected = childId.isBlank(), onClick = { childId = "" }, label = { Text("Family") })
                    vm.children.forEach { child ->
                        FilterChip(selected = childId == child.id, onClick = { childId = child.id }, label = { Text(child.name) })
                    }
                }
                OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("What's the school thing?") }, singleLine = true)
                Text("Category", color = Ink, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Category.all.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) }, leadingIcon = { Icon(categoryIconV2(cat), null, modifier = Modifier.size(17.dp)) })
                    }
                }
                Text("Date", color = Ink, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { date = date.minusDays(1) }) { Text("−") }
                    Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), color = Ink, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { date = date.plusDays(1) }) { Text("+") }
                }
                Text("Repeat", color = Ink, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Repeat.all.forEach { value -> FilterChip(selected = repeat == value, onClick = { repeat = value }, label = { Text(value) }) }
                }
                Text("Reminder", color = Ink, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Reminder.all.forEach { value -> FilterChip(selected = reminder == value, onClick = { reminder = value }, label = { Text(value) }) }
                }
                OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Notes") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = needsHome, onCheckedChange = { needsHome = it })
                    Text("Needs item from home", color = Ink)
                }
                Button(
                    onClick = {
                        vm.addItem(SchoolItem(childId = childId, title = title.trim(), category = category, dateIso = date.toString(), repeat = repeat, reminder = reminder, notes = notes.trim(), needsItemFromHome = needsHome))
                        onBack()
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolRed)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun SettingsScreenV2(vm: SchoolStuffViewModel) {
    val context = LocalContext.current
    val repo = remember { CalendarProviderRepository(context) }
    val scope = rememberCoroutineScope()
    var calendars by remember { mutableStateOf<List<DeviceCalendar>>(emptyList()) }
    var selected by remember { mutableStateOf(vm.selectedCalendarIds()) }
    var defaultId by remember { mutableStateOf(vm.defaultCalendarId()) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var expandedAccounts by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun refreshCalendars(showMessage: Boolean = false) {
        if (!repo.hasReadPermission()) return
        scope.launch {
            loading = true
            val found = withContext(Dispatchers.IO) { repo.queryCalendars() }
            calendars = found
            if (expandedAccounts.isEmpty()) {
                expandedAccounts = found.groupBy { it.accountName.ifBlank { "Device calendars" } }
                    .filterValues { list -> list.any { it.id in selected } }
                    .keys
            }
            loading = false
            if (showMessage) message = "Calendar list refreshed."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.READ_CALENDAR] == true) refreshCalendars()
        else message = "Calendar permission was not granted."
    }

    LifecycleResumeEffect(Unit) {
        if (repo.hasReadPermission()) refreshCalendars()
        onPauseOrDispose { }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderV2("Settings", "Calendar connection and the boring useful bits. ♥", note = "No account required") }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SectionV2("Google / Android Calendar", SchoolBlue) {
                    Text("Calendars load as soon as this screen opens. Accounts are collapsed so a giant Google list doesn't eat the whole screen.", color = Ink.copy(alpha = .68f), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    if (!repo.hasReadPermission() || !repo.hasWritePermission()) {
                        Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) }) {
                            Icon(Icons.Rounded.CalendarMonth, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Connect Calendar")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Loading calendars…", color = Ink.copy(alpha = .65f), fontSize = 12.sp)
                            } else {
                                Text("${calendars.size} calendars found", color = Ink.copy(alpha = .62f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { refreshCalendars(true) }) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Refresh") }
                            }
                        }
                        val grouped = calendars.groupBy { it.accountName.ifBlank { "Device calendars" } }
                        grouped.forEach { (account, accountCalendars) ->
                            val expanded = account in expandedAccounts
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            expandedAccounts = if (expanded) expandedAccounts - account else expandedAccounts + account
                                        }.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.AccountCircle, null, tint = SchoolBlue)
                                        Spacer(Modifier.width(9.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(account, color = Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${accountCalendars.size} calendar${if (accountCalendars.size == 1) "" else "s"}", color = Ink.copy(alpha = .55f), fontSize = 11.sp)
                                        }
                                        val selectedCount = accountCalendars.count { it.id in selected }
                                        if (selectedCount > 0) {
                                            Surface(color = SoftGreen, shape = RoundedCornerShape(10.dp)) {
                                                Text("$selectedCount selected", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Ink, fontSize = 10.sp)
                                            }
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Ink.copy(alpha = .6f))
                                    }
                                    if (expanded) {
                                        HorizontalDivider(color = Ink.copy(alpha = .08f))
                                        accountCalendars.forEach { cal ->
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp)) {
                                                Checkbox(
                                                    checked = cal.id in selected,
                                                    onCheckedChange = { checked ->
                                                        selected = if (checked) selected + cal.id else selected - cal.id
                                                        vm.saveSelectedCalendarIds(selected)
                                                        if (!checked && defaultId == cal.id) {
                                                            defaultId = null
                                                            vm.saveDefaultCalendarId(null)
                                                        }
                                                    }
                                                )
                                                Column(Modifier.weight(1f)) {
                                                    Text(cal.displayName, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(if (cal.canWrite) "Can read & write" else "Read only", color = Ink.copy(alpha = .50f), fontSize = 10.sp)
                                                }
                                                if (cal.canWrite && cal.id in selected) {
                                                    IconButton(onClick = {
                                                        defaultId = cal.id
                                                        vm.saveDefaultCalendarId(cal.id)
                                                    }) {
                                                        Icon(if (defaultId == cal.id) Icons.Rounded.Star else Icons.Rounded.StarBorder, "Write here", tint = SchoolYellow)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    val events = withContext(Dispatchers.IO) { repo.importInstances(selected) }
                                    val imported = vm.importCalendarEvents(events)
                                    message = "$imported new calendar event${if (imported == 1) "" else "s"} imported."
                                }
                            }, enabled = selected.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Import") }
                            Button(onClick = {
                                val target = defaultId
                                if (target == null) {
                                    message = "Choose a writable calendar with the star first."
                                } else {
                                    val toSync = vm.items
                                    scope.launch {
                                        val count = withContext(Dispatchers.IO) { toSync.count { repo.upsertSchoolItem(target, it) } }
                                        message = "$count School Stuff events synced."
                                    }
                                }
                            }, enabled = defaultId != null, modifier = Modifier.weight(1f)) { Text("Sync Out") }
                        }
                        if (message.isNotBlank()) Text(message, color = Ink.copy(alpha = .68f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SectionV2("About this build", SchoolGreen) {
                    Text("School Stuff 0.1.1", color = Ink, fontWeight = FontWeight.Bold)
                    Text("Calendar loads immediately, Google account lists collapse, and artwork/photos now show actual thumbnails.", color = Ink.copy(alpha = .65f), fontSize = 13.sp)
                }
            }
        }
    }
}
