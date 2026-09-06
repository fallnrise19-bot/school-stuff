package ca.creativepixels.schoolstuff

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ca.creativepixels.schoolstuff.calendar.CalendarProviderRepository
import ca.creativepixels.schoolstuff.data.Category
import ca.creativepixels.schoolstuff.data.ChildProfile
import ca.creativepixels.schoolstuff.data.DeviceCalendar
import ca.creativepixels.schoolstuff.data.DocumentType
import ca.creativepixels.schoolstuff.data.Reminder
import ca.creativepixels.schoolstuff.data.Repeat
import ca.creativepixels.schoolstuff.data.SchoolDocument
import ca.creativepixels.schoolstuff.data.SchoolItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    CALENDAR("Calendar", Icons.Rounded.CalendarMonth),
    KIDS("Kids", Icons.Rounded.Groups),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@Composable
fun SchoolStuffApp(vm: SchoolStuffViewModel) {
    SchoolStuffTheme {
        var tab by remember { mutableStateOf(MainTab.HOME) }
        var childPage by remember { mutableStateOf<String?>(null) }
        var addingThing by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = Paper,
            bottomBar = {
                if (childPage == null && !addingThing) {
                    NavigationBar(containerColor = Color.White) {
                        MainTab.entries.forEach { item ->
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
                    addingThing -> AddThingScreen(vm, onBack = { addingThing = false })
                    childPage != null -> ChildScreen(vm, childPage!!, onBack = { childPage = null }, onAddThing = { addingThing = true })
                    tab == MainTab.HOME -> HomeScreen(vm, onAdd = { addingThing = true }, onChild = { childPage = it })
                    tab == MainTab.CALENDAR -> CalendarScreen(vm)
                    tab == MainTab.KIDS -> KidsScreen(vm, onChild = { childPage = it })
                    else -> SettingsScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String, back: (() -> Unit)? = null, note: String? = null) {
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

private fun childColor(key: String): Color = when (key) {
    "green" -> SchoolGreen
    "pink" -> SchoolPink
    "yellow" -> SchoolYellow
    else -> SchoolBlue
}

@Composable
private fun ChildBadge(child: ChildProfile?, size: Int = 42) {
    val color = childColor(child?.colorKey ?: "yellow")
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

private fun categoryIcon(category: String): ImageVector = when (category) {
    Category.FOOD_DAY -> Icons.Rounded.LocalPizza
    Category.FORM_DUE -> Icons.Rounded.Description
    Category.SPIRIT_DAY -> Icons.Rounded.Checkroom
    Category.BRING_ITEM -> Icons.Rounded.Backpack
    Category.LIBRARY -> Icons.Rounded.MenuBook
    Category.GYM -> Icons.Rounded.DirectionsRun
    Category.HOMEWORK -> Icons.Rounded.Assignment
    else -> Icons.Rounded.Event
}

private fun categoryColor(category: String): Color = when (category) {
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
private fun Section(title: String, tint: Color = SchoolBlue, content: @Composable () -> Unit) {
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
            Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) { content() }
        }
    }
}

@Composable
private fun SchoolRow(vm: SchoolStuffViewModel, item: SchoolItem, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChildBadge(vm.child(item.childId), 38)
        Spacer(Modifier.width(10.dp))
        Icon(categoryIcon(item.category), null, tint = categoryColor(item.category), modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${vm.childName(item.childId)} • ${item.category}", color = Ink.copy(alpha = .55f), fontSize = 12.sp)
        }
        trailing?.invoke()
    }
}

@Composable
private fun HomeScreen(vm: SchoolStuffViewModel, onAdd: () -> Unit, onChild: (String) -> Unit) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    var selectedChild by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header("School Stuff", "Little things. Big school days. ♥", note = "You got this!") }
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
                        Icon(Icons.Rounded.PhotoCamera, null, tint = SchoolRed, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Picture Day coming up", color = Ink, fontWeight = FontWeight.Bold)
                            Text(upcomingPicture.first.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), color = Ink.copy(alpha = .65f))
                        }
                    }
                }
            }
        }
        item {
            DaySection("Today", today, SoftBlue, vm, selectedChild)
        }
        item {
            DaySection("Tomorrow", tomorrow, SoftGreen, vm, selectedChild)
        }
        item {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 50.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add School Thing")
            }
        }
        item {
            Text(
                "Tap a child below for teacher info, homework and papers.",
                color = Ink.copy(alpha = .6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                vm.children.forEach { child ->
                    AssistChip(onClick = { onChild(child.id) }, label = { Text(child.name) }, leadingIcon = { ChildBadge(child, 28) })
                }
            }
        }
    }
}

@Composable
private fun DaySection(
    title: String,
    date: LocalDate,
    tint: Color,
    vm: SchoolStuffViewModel,
    selectedChild: String?
) {
    val list = vm.itemsFor(date).filter { selectedChild == null || it.childId == selectedChild }
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Section("$title  •  ${date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}", tint) {
            Column {
                if (list.isEmpty()) {
                    Text("Nothing on the list. Suspiciously peaceful.", color = Ink.copy(alpha = .6f), modifier = Modifier.padding(vertical = 14.dp))
                } else {
                    list.forEachIndexed { index, item ->
                        SchoolRow(vm, item)
                        if (index != list.lastIndex) HorizontalDivider(color = Ink.copy(alpha = .08f))
                    }
                }
            }
        }
    }
}

@Composable
private fun KidsScreen(vm: SchoolStuffViewModel, onChild: (String) -> Unit) {
    var addDialog by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header("Kids", "Different schedules. One parent brain. ♥", note = "Same kids.\nBrighter days.") }
        items(vm.children, key = { it.id }) { child ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onChild(child.id) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = childColor(child.colorKey).copy(alpha = .10f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChildBadge(child, 56)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(child.name, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Ink)
                            Text(child.grade.ifBlank { "Grade not set" }, color = Ink.copy(alpha = .58f))
                        }
                        Icon(Icons.Rounded.School, null, tint = childColor(child.colorKey))
                    }
                    Spacer(Modifier.height(10.dp))
                    val routines = vm.items.filter { it.childId == child.id && it.repeat == Repeat.WEEKLY }.take(3)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Icon(Icons.Rounded.Add, null)
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
private fun ChildScreen(vm: SchoolStuffViewModel, childId: String, onBack: () -> Unit, onAddThing: () -> Unit) {
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
        item { Header(child.name, "Your school dashboard. ♥", back = onBack, note = child.grade.ifBlank { "School year" }) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = childColor(child.colorKey).copy(alpha = .10f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChildBadge(child, 62)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(child.name, fontSize = 24.sp, color = Ink, fontWeight = FontWeight.Black)
                            Text(child.grade, color = Ink.copy(alpha = .58f))
                        }
                        IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, "Edit") }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(child.teacherName.ifBlank { "Add teacher information" }, color = Ink, fontWeight = FontWeight.SemiBold)
                    if (child.room.isNotBlank()) Text("Room ${child.room}", color = Ink.copy(alpha = .6f))
                    if (child.schoolName.isNotBlank()) Text(child.schoolName, color = Ink.copy(alpha = .6f))
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section("This Week", SchoolGreen) {
                    Column {
                        val next = vm.upcoming(7).filter { it.second.childId == childId }.take(6)
                        if (next.isEmpty()) Text("Nothing scheduled yet.", modifier = Modifier.padding(vertical = 12.dp))
                        next.forEach { (date, item) ->
                            SchoolRow(vm, item) {
                                Text(date.format(DateTimeFormatter.ofPattern("EEE")), color = Ink.copy(alpha = .6f), fontSize = 12.sp)
                            }
                            HorizontalDivider(color = Ink.copy(alpha = .07f))
                        }
                        TextButton(onClick = onAddThing) { Icon(Icons.Rounded.Add, null); Text("Add school thing") }
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section("Teacher & School", SchoolBlue) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        InfoLine(Icons.Rounded.School, "Teacher", child.teacherName.ifBlank { "Not added" })
                        InfoLine(Icons.Rounded.Email, "Email", child.teacherEmail.ifBlank { "Not added" })
                        InfoLine(Icons.Rounded.Call, "Classroom phone", child.classroomPhone.ifBlank { "Not added" })
                        InfoLine(Icons.Rounded.Home, "School", child.schoolName.ifBlank { "Not added" })
                        InfoLine(Icons.Rounded.Call, "School phone", child.schoolPhone.ifBlank { "Not added" })
                        if (child.specialNotes.isNotBlank()) Text("Note: ${child.specialNotes}", color = Ink.copy(alpha = .7f), fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section("Homework & Forms", SchoolYellow) {
                    Column {
                        val tasks = vm.items.filter { it.childId == childId && it.category in listOf(Category.HOMEWORK, Category.FORM_DUE, Category.BRING_ITEM) }
                        if (tasks.isEmpty()) Text("No homework or forms tracked yet.", modifier = Modifier.padding(vertical = 12.dp))
                        tasks.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = item.completed, onCheckedChange = { vm.toggleComplete(item.id) })
                                Box(Modifier.weight(1f)) { SchoolRow(vm, item) }
                            }
                        }
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section("Papers & Memories", SchoolPink) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DocumentType.all.forEach { type ->
                                FilterChip(selected = selectedDocType == type, onClick = { selectedDocType = type }, label = { Text(type) })
                            }
                        }
                        val docs = vm.documents.filter { it.childId == childId && it.type == selectedDocType }
                        docs.forEach { doc ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                Icon(documentIcon(doc.type), null, tint = SchoolBlue)
                                Spacer(Modifier.width(10.dp))
                                Text(doc.title, color = Ink, modifier = Modifier.weight(1f), maxLines = 2)
                                TextButton(onClick = { vm.deleteDocument(doc.id) }) { Text("Remove") }
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
        }
    }

    if (editing) {
        EditChildDialog(child, onDismiss = { editing = false }, onSave = { vm.updateChild(it); editing = false })
    }
}

@Composable
private fun InfoLine(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = SchoolBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label:", color = Ink.copy(alpha = .62f), fontSize = 13.sp, modifier = Modifier.width(112.dp))
        Text(value, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

private fun documentIcon(type: String): ImageVector = when (type) {
    DocumentType.REPORT_CARD -> Icons.Rounded.Star
    DocumentType.ARTWORK -> Icons.Rounded.Palette
    DocumentType.PHOTO -> Icons.Rounded.PhotoCamera
    else -> Icons.Rounded.Description
}

@Composable
private fun EditChildDialog(child: ChildProfile, onDismiss: () -> Unit, onSave: (ChildProfile) -> Unit) {
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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddThingScreen(vm: SchoolStuffViewModel, onBack: () -> Unit) {
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
        item { Header("Add School Thing", "Get it out of your brain and into the app. ♥", back = onBack, note = "Future you says thanks.") }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) }, leadingIcon = { Icon(categoryIcon(cat), null, modifier = Modifier.size(17.dp)) })
                    }
                }
                Text("Date", color = Ink, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { date = date.minusDays(1) }) { Text("−") }
                    Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), color = Ink, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { date = date.plusDays(1) }) { Text("+") }
                }
                Text("Repeat", color = Ink, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
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
                    shape = RoundedCornerShape(20.dp)
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
private fun CalendarScreen(vm: SchoolStuffViewModel) {
    val today = LocalDate.now()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Header("Calendar", "The week without the fridge blindness. ♥", note = "This week") }
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
                        dayItems.forEach { SchoolRow(vm, it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: SchoolStuffViewModel) {
    val context = LocalContext.current
    val repo = remember { CalendarProviderRepository(context) }
    var calendars by remember { mutableStateOf<List<DeviceCalendar>>(emptyList()) }
    var selected by remember { mutableStateOf(vm.selectedCalendarIds()) }
    var defaultId by remember { mutableStateOf(vm.defaultCalendarId()) }
    var message by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.READ_CALENDAR] == true) calendars = repo.queryCalendars()
    }

    LaunchedEffect(Unit) {
        if (repo.hasReadPermission()) calendars = repo.queryCalendars()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header("Settings", "Calendar connection and the boring useful bits. ♥", note = "No account required") }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section("Google / Android Calendar", SchoolBlue) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("School Stuff reads calendars already synced to this phone. You choose which ones it may import and where it writes School Stuff events.", color = Ink.copy(alpha = .68f), fontSize = 13.sp)
                        if (!repo.hasReadPermission() || !repo.hasWritePermission()) {
                            Button(onClick = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                            }) {
                                Icon(Icons.Rounded.CalendarMonth, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Connect Calendar")
                            }
                        } else {
                            calendars.forEach { cal ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Checkbox(
                                        checked = cal.id in selected,
                                        onCheckedChange = { checked ->
                                            selected = if (checked) selected + cal.id else selected - cal.id
                                            vm.saveSelectedCalendarIds(selected)
                                        }
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(cal.displayName, color = Ink, fontWeight = FontWeight.SemiBold)
                                        Text(cal.accountName, color = Ink.copy(alpha = .52f), fontSize = 11.sp)
                                    }
                                    if (cal.canWrite) {
                                        FilterChip(
                                            selected = defaultId == cal.id,
                                            onClick = {
                                                defaultId = cal.id
                                                vm.saveDefaultCalendarId(cal.id)
                                            },
                                            label = { Text(if (defaultId == cal.id) "Writes here" else "Use") }
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    val imported = vm.importCalendarEvents(repo.importInstances(selected))
                                    message = "$imported new calendar event${if (imported == 1) "" else "s"} imported."
                                }, enabled = selected.isNotEmpty()) { Text("Import") }
                                Button(onClick = {
                                    val target = defaultId
                                    if (target == null) {
                                        message = "Choose a calendar to write to first."
                                    } else {
                                        val count = vm.items.count { repo.upsertSchoolItem(target, it) }
                                        message = "$count School Stuff events synced."
                                    }
                                }, enabled = defaultId != null) { Text("Sync Out") }
                            }
                            if (message.isNotBlank()) Text(message, color = Ink.copy(alpha = .68f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section("About this build", SchoolGreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("School Stuff 0.1.0", color = Ink, fontWeight = FontWeight.Bold)
                        Text("Local-first prototype. No cloud account, subscription or AI scanning yet.", color = Ink.copy(alpha = .65f), fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Notifications, null, tint = SchoolGreen)
                            Spacer(Modifier.width(8.dp))
                            Text("Reminders use Android notifications and WorkManager.", color = Ink, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
