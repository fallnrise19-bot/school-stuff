package ca.creativepixels.schoolstuff

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        BackHandler(enabled = addingThing || childPage != null) {
            when {
                addingThing -> addingThing = false
                childPage != null -> childPage = null
            }
        }

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
        val itemEmoji = item.emoji.orEmpty()
        if (itemEmoji.isNotBlank()) {
            Surface(color = SoftYellow, shape = RoundedCornerShape(12.dp)) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Text(itemEmoji, fontSize = 23.sp)
                }
            }
        } else {
            MockupArtImage(categoryArt(item), modifier = Modifier.size(36.dp), contentDescription = item.category)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${vm.childName(item.childId)} • ${item.category}", color = Ink.copy(alpha = .55f), fontSize = 12.sp)
        }
        trailing?.invoke()
    }
}

private fun categoryArt(item: SchoolItem): MockupAsset = when {
    item.title.contains("picture", ignoreCase = true) -> MockupAsset.CAMERA
    item.category == Category.LIBRARY -> MockupAsset.BOOKS
    item.category == Category.GYM -> MockupAsset.SHOES
    item.category == Category.SPIRIT_DAY -> MockupAsset.SHIRT
    item.category == Category.FOOD_DAY && item.title.contains("sub", ignoreCase = true) -> MockupAsset.SANDWICH
    item.category == Category.FOOD_DAY -> MockupAsset.PIZZA
    item.category == Category.HOMEWORK || item.category == Category.BRING_ITEM -> MockupAsset.FOLDER
    item.category == Category.FORM_DUE -> MockupAsset.DOCUMENT
    else -> MockupAsset.STAR
}

@Composable
private fun HomeMockupHero() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SoftBlue)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Everything school. One place.", color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text("The fridge has officially been demoted.", color = Ink.copy(alpha = .62f), fontSize = 12.sp)
            }
            MockupArtImage(MockupAsset.BACKPACK, modifier = Modifier.size(104.dp), contentDescription = "School backpack")
        }
    }
}

@Composable
private fun MockupMiniStrip() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        MockupArtImage(MockupAsset.BOOKS, Modifier.size(45.dp), "Books")
        MockupArtImage(MockupAsset.PIZZA, Modifier.size(45.dp), "Pizza")
        MockupArtImage(MockupAsset.SHIRT, Modifier.size(45.dp), "Spirit shirt")
        MockupArtImage(MockupAsset.SANDWICH, Modifier.size(45.dp), "Sub day")
    }
}

@Composable
private fun PapersArtStrip() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        MockupArtImage(MockupAsset.PALETTE, Modifier.size(58.dp), "Artwork")
        MockupArtImage(MockupAsset.CAMERA, Modifier.size(52.dp), "Photos")
        MockupArtImage(MockupAsset.DOCUMENT, Modifier.size(52.dp), "Forms")
        MockupArtImage(MockupAsset.STAR, Modifier.size(42.dp), "Memories")
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
        item { HomeMockupHero() }
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
                        MockupArtImage(MockupAsset.CAMERA, modifier = Modifier.size(52.dp), contentDescription = "Picture Day")
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
    var previewDocument by remember { mutableStateOf<SchoolDocument?>(null) }
    var galleryOpen by remember { mutableStateOf(false) }

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
                    Spacer(Modifier.height(10.dp))
                    MockupMiniStrip()
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
                Section("Transportation", SchoolGreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        val hasTransportInfo = listOf(
                            child.transportationType,
                            child.busNumber,
                            child.transportDriverName,
                            child.transportLicensePlate,
                            child.pickupInfo,
                            child.dropOffInfo,
                            child.transportationNotes
                        ).any { it.isNotBlank() }

                        if (!hasTransportInfo) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.DirectionsBus, null, tint = SchoolGreen)
                                Spacer(Modifier.width(8.dp))
                                Text("No transportation details added yet.", color = Ink.copy(alpha = .62f))
                            }
                        } else {
                            if (child.transportationType.isNotBlank()) InfoLine(Icons.Rounded.DirectionsBus, "Type", child.transportationType)
                            if (child.busNumber.isNotBlank()) InfoLine(Icons.Rounded.DirectionsBus, "Bus / route", child.busNumber)
                            if (child.transportDriverName.isNotBlank()) InfoLine(Icons.Rounded.Person, "Driver", child.transportDriverName)
                            if (child.transportLicensePlate.isNotBlank()) InfoLine(Icons.Rounded.DirectionsBus, "Plate", child.transportLicensePlate)
                            if (child.pickupInfo.isNotBlank()) InfoLine(Icons.Rounded.Home, "Pickup", child.pickupInfo)
                            if (child.dropOffInfo.isNotBlank()) InfoLine(Icons.Rounded.School, "Drop-off", child.dropOffInfo)
                            if (child.transportationNotes.isNotBlank()) {
                                Text("Note: ${child.transportationNotes}", color = Ink.copy(alpha = .7f), fontSize = 13.sp)
                            }
                        }
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
        }
    }


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

    previewDocument?.let { doc ->
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
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(doc.uri))
                            .size(1600)
                            .crossfade(false)
                            .build(),
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
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(doc.uri))
                            .size(320)
                            .crossfade(false)
                            .build(),
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
    var transportationType by remember { mutableStateOf(child.transportationType) }
    var busNumber by remember { mutableStateOf(child.busNumber) }
    var transportDriverName by remember { mutableStateOf(child.transportDriverName) }
    var transportLicensePlate by remember { mutableStateOf(child.transportLicensePlate) }
    var pickupInfo by remember { mutableStateOf(child.pickupInfo) }
    var dropOffInfo by remember { mutableStateOf(child.dropOffInfo) }
    var transportationNotes by remember { mutableStateOf(child.transportationNotes) }
    val transportationTypes = listOf("School Bus", "Parent / Caregiver", "Walk", "Other")

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
                item {
                    Text("Transportation", color = Ink, fontWeight = FontWeight.Bold)
                }
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = transportationType.isBlank(),
                            onClick = { transportationType = "" },
                            label = { Text("Not set") }
                        )
                        transportationTypes.forEach { type ->
                            FilterChip(
                                selected = transportationType == type,
                                onClick = { transportationType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                }
                if (transportationType == "School Bus") {
                    item { OutlinedTextField(busNumber, { busNumber = it }, label = { Text("Bus / route number") }, singleLine = true) }
                    item { OutlinedTextField(transportDriverName, { transportDriverName = it }, label = { Text("Driver name") }, singleLine = true) }
                    item { OutlinedTextField(transportLicensePlate, { transportLicensePlate = it }, label = { Text("Licence plate") }, singleLine = true) }
                }
                if (transportationType == "Parent / Caregiver" || transportationType == "Other") {
                    item { OutlinedTextField(transportDriverName, { transportDriverName = it }, label = { Text("Driver / caregiver") }, singleLine = true) }
                    item { OutlinedTextField(transportLicensePlate, { transportLicensePlate = it }, label = { Text("Vehicle / licence plate") }, singleLine = true) }
                }
                if (transportationType.isNotBlank()) {
                    item { OutlinedTextField(pickupInfo, { pickupInfo = it }, label = { Text("Pickup info") }, placeholder = { Text("Time, stop, location…") }) }
                    item { OutlinedTextField(dropOffInfo, { dropOffInfo = it }, label = { Text("Drop-off info") }, placeholder = { Text("Time, stop, location…") }) }
                    item { OutlinedTextField(transportationNotes, { transportationNotes = it }, label = { Text("Transportation notes") }) }
                }
                item { OutlinedTextField(notes, { notes = it }, label = { Text("Special notes") }) }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(child.copy(
                    grade = grade,
                    teacherName = teacher,
                    teacherEmail = email,
                    classroomPhone = phone,
                    room = room,
                    schoolName = school,
                    schoolPhone = schoolPhone,
                    transportationType = transportationType,
                    busNumber = busNumber,
                    transportDriverName = transportDriverName,
                    transportLicensePlate = transportLicensePlate,
                    pickupInfo = pickupInfo,
                    dropOffInfo = dropOffInfo,
                    transportationNotes = transportationNotes,
                    specialNotes = notes
                ))
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
    var showDatePicker by remember { mutableStateOf(false) }
    val customCategoryOption = "__CUSTOM__"
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var customCategory by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val emojiChoices = listOf(
        "🎒", "📚", "✏️", "📝", "📄",
        "🍕", "🥪", "🍎", "🥛", "🧁",
        "⚽", "🏀", "🏃", "👟", "🏊",
        "🎨", "🎭", "🎵", "📷", "⭐",
        "🚌", "🏫", "🔬", "💻", "🧪",
        "👕", "🎉", "📌", "⏰", "❤️"
    )

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
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (category == customCategoryOption) "Custom…" else category,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        Category.all.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                leadingIcon = { Icon(categoryIcon(cat), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    category = cat
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Custom…") },
                            leadingIcon = { Text("✨", fontSize = 20.sp) },
                            onClick = {
                                category = customCategoryOption
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
                if (category == customCategoryOption) {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Custom category") },
                        placeholder = { Text("e.g. Field Trip, Club, Fundraiser") },
                        singleLine = true
                    )
                }
                Text("Emoji (optional)", color = Ink, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = { showEmojiPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(if (selectedEmoji.isBlank()) "🙂" else selectedEmoji, fontSize = 25.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (selectedEmoji.isBlank()) "Choose an emoji" else "Change emoji",
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Date", color = Ink, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        Text(
                            date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                            color = Ink,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Tap to choose a date", color = Ink.copy(alpha = .55f), fontSize = 12.sp)
                    }
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
                        val finalCategory = if (category == customCategoryOption) customCategory.trim().ifBlank { "Other" } else category
                        vm.addItem(SchoolItem(childId = childId, title = title.trim(), category = finalCategory, emoji = selectedEmoji.takeIf { it.isNotBlank() }, dateIso = date.toString(), repeat = repeat, reminder = reminder, notes = notes.trim(), needsItemFromHome = needsHome))
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

    if (showEmojiPicker) {
        AlertDialog(
            onDismissRequest = { showEmojiPicker = false },
            title = { Text("Choose an emoji") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Pick something that will make this school thing easy to spot.",
                        color = Ink.copy(alpha = .65f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    emojiChoices.chunked(5).forEach { emojiRow ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            emojiRow.forEach { emoji ->
                                TextButton(
                                    onClick = {
                                        selectedEmoji = emoji
                                        showEmojiPicker = false
                                    },
                                    modifier = Modifier.size(46.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(emoji, fontSize = 25.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmojiPicker = false }) { Text("Close") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedEmoji = ""
                        showEmojiPicker = false
                    }
                ) { Text("No emoji") }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = date
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )

        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            date = java.time.Instant
                                .ofEpochMilli(millis)
                                .atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("Choose") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            androidx.compose.material3.DatePicker(
                state = datePickerState,
                showModeToggle = true
            )
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
    val scope = rememberCoroutineScope()
    var calendarPermissionGranted by remember { mutableStateOf(repo.hasReadPermission()) }
    var calendars by remember { mutableStateOf<List<DeviceCalendar>>(emptyList()) }
    var calendarListRevision by remember { mutableIntStateOf(0) }
    var calendarListExpanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(vm.selectedCalendarIds()) }
    var defaultId by remember { mutableStateOf(vm.defaultCalendarId()) }
    var message by remember { mutableStateOf("") }
    var calendarSyncing by remember { mutableStateOf(false) }

    suspend fun refreshCalendarsOnce(): List<DeviceCalendar> {
        val granted = repo.hasReadPermission()
        calendarPermissionGranted = granted
        val result = if (granted) {
            withContext(Dispatchers.IO) {
                repo.queryCalendars()
                    .filter { it.accountType == "com.google" }
                    .sortedWith(compareBy<DeviceCalendar> { it.accountName.lowercase() }.thenBy { it.displayName.lowercase() })
            }
        } else emptyList()
        calendars = result.toList()
        calendarListRevision++
        message = when {
            !granted -> "Google Calendar is not connected."
            result.isEmpty() -> "Connected, but Android has not exposed any visible Google calendars yet."
            else -> "Connected. ${result.size} calendar${if (result.size == 1) "" else "s"} available."
        }
        return result
    }

    suspend fun refreshCalendarsWithRetry() {
        if (!repo.hasReadPermission()) {
            refreshCalendarsOnce()
            return
        }
        val waits = listOf(0L, 200L, 400L, 700L, 1000L, 1500L, 2000L, 2500L)
        for (wait in waits) {
            if (wait > 0) delay(wait)
            if (refreshCalendarsOnce().isNotEmpty()) return
        }
        message = "Calendar access is connected, but Android has not exposed the list yet. Keep this screen open and tap Refresh calendars."
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val readGranted = result[Manifest.permission.READ_CALENDAR] == true || repo.hasReadPermission()
        calendarPermissionGranted = readGranted
        if (readGranted) {
            message = "Calendar access granted. Loading calendars…"
            calendarSyncing = true
            scope.launch {
                refreshCalendarsWithRetry()
                calendarSyncing = false
            }
        } else {
            calendars = emptyList()
            calendarListRevision++
            message = "Calendar permission was not granted."
        }
    }

    // Directive fix: this observable permission state + repeated reads keeps the Settings
    // screen live while Android publishes Calendar Provider rows after permission/resume.
    LaunchedEffect(calendarPermissionGranted) {
        if (calendarPermissionGranted) {
            repeat(12) { attempt ->
                val result = refreshCalendarsOnce()
                if (result.isNotEmpty()) return@LaunchedEffect
                delay(if (attempt < 4) 400L else 900L)
            }
        }
    }

    // Directive also listens to the Calendar Provider itself, so a late sync redraws
    // the list without making the user leave Settings and come back.
    DisposableEffect(calendarPermissionGranted) {
        if (!calendarPermissionGranted) return@DisposableEffect onDispose { }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                scope.launch { refreshCalendarsWithRetry() }
            }
        }
        context.contentResolver.registerContentObserver(
            CalendarContract.Calendars.CONTENT_URI,
            true,
            observer
        )
        onDispose {
            runCatching { context.contentResolver.unregisterContentObserver(observer) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header("Settings", "Calendar connection and the boring useful bits. ♥", note = "No account required") }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section("Google Calendar", SchoolBlue) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MockupArtImage(MockupAsset.BOOKS, Modifier.size(54.dp), "School calendar")
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "School Stuff reads Google calendars already synced to this phone. Pick only the calendars you want to see.",
                                color = Ink.copy(alpha = .68f),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (!calendarPermissionGranted || !repo.hasWritePermission()) {
                            Button(onClick = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                            }) {
                                Icon(Icons.Rounded.CalendarMonth, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Connect Calendar")
                            }
                        } else {
                            if (calendarSyncing) {
                                Text("Loading calendars…", color = Ink.copy(alpha = .62f), fontSize = 12.sp)
                            }

                            val selectedVisible = calendars.filter { it.id in selected }
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { calendarListExpanded = !calendarListExpanded },
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Calendars shown in School Stuff · ${selectedVisible.size} selected",
                                            color = Ink,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (selectedVisible.isEmpty()) {
                                            Text("Tap to choose calendars", color = Ink.copy(alpha = .55f), fontSize = 11.sp)
                                        } else {
                                            selectedVisible.take(3).forEach { cal ->
                                                Text("${cal.displayName} · ${cal.accountName}", color = Ink.copy(alpha = .58f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            if (selectedVisible.size > 3) {
                                                Text("+${selectedVisible.size - 3} more", color = SchoolBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                    Icon(
                                        if (calendarListExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        if (calendarListExpanded) "Collapse calendars" else "Expand calendars",
                                        tint = Ink.copy(alpha = .62f)
                                    )
                                }
                            }

                            if (calendarListExpanded) {
                                key(calendarListRevision) {
                                    calendars.forEach { cal ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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
                                                Text(cal.accountName, color = Ink.copy(alpha = .52f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            if (cal.canWrite && cal.id in selected) {
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
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    calendarSyncing = true
                                    scope.launch {
                                        refreshCalendarsWithRetry()
                                        calendarSyncing = false
                                    }
                                }) { Text("Refresh calendars") }
                                OutlinedButton(onClick = {
                                    val imported = vm.importCalendarEvents(repo.importInstances(selected))
                                    message = "$imported new calendar event${if (imported == 1) "" else "s"} imported."
                                }, enabled = selected.isNotEmpty()) { Text("Import") }
                            }
                            Button(onClick = {
                                val target = defaultId
                                if (target == null) {
                                    message = "Choose a selected calendar as the write destination first."
                                } else {
                                    val count = vm.items.count { repo.upsertSchoolItem(target, it) }
                                    message = "$count School Stuff events synced."
                                }
                            }, enabled = defaultId != null) { Text("Sync School Stuff Out") }

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
                        Text("School Stuff ${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}", color = Ink, fontWeight = FontWeight.Bold)
                        Text("Verified build: Directive-style live calendar refresh + the actual illustrated prototype artwork.", color = Ink.copy(alpha = .65f), fontSize = 13.sp)
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
