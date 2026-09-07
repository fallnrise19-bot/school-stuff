package ca.creativepixels.schoolstuff

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.provider.CalendarContract
import android.provider.Settings
import android.provider.OpenableColumns
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Language
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ca.creativepixels.schoolstuff.calendar.CalendarProviderRepository
import ca.creativepixels.schoolstuff.data.Category
import ca.creativepixels.schoolstuff.data.ChildProfile
import ca.creativepixels.schoolstuff.data.DeviceCalendar
import ca.creativepixels.schoolstuff.data.DocumentType
import ca.creativepixels.schoolstuff.data.Reminder
import ca.creativepixels.schoolstuff.data.Repeat
import ca.creativepixels.schoolstuff.data.SchoolDocument
import ca.creativepixels.schoolstuff.data.SchoolItem
import ca.creativepixels.schoolstuff.data.TransportationInfo
import ca.creativepixels.schoolstuff.data.TransportationMode
import ca.creativepixels.schoolstuff.notifications.ReminderNotifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class MainTab(val english: String, val french: String, val icon: ImageVector) {
    HOME("Home", "Accueil", Icons.Rounded.Home),
    CALENDAR("Calendar", "Calendrier", Icons.Rounded.CalendarMonth),
    KIDS("Kids", "Enfants", Icons.Rounded.Groups),
    SETTINGS("Settings", "Paramètres", Icons.Rounded.Settings)
}

@Composable
fun SchoolStuffApp(
    vm: SchoolStuffViewModel,
    appLockEnabled: Boolean,
    onChangeAppLock: (Boolean, (Boolean, String) -> Unit) -> Unit,
    onTestAppLock: ((Boolean, String) -> Unit) -> Unit,
    appLanguage: String,
    onChangeLanguage: (String) -> Unit
) {
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
                                label = { Text(tr(item.english, item.french)) }
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
                    else -> SettingsScreen(vm, appLockEnabled, onChangeAppLock, onTestAppLock, appLanguage, onChangeLanguage)
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
            IconButton(onClick = back) { Icon(Icons.Rounded.ArrowBack, tr("Back", "Retour"), tint = Ink) }
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
private fun SchoolRow(
    vm: SchoolStuffViewModel,
    item: SchoolItem,
    trailing: @Composable (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
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
            val who = if (item.childId.isBlank()) tr("Family", "Famille") else vm.childName(item.childId)
            Text("$who • ${categoryLabel(item.category)}", color = Ink.copy(alpha = .55f), fontSize = 12.sp)
        }
        trailing?.invoke()
        if (onDelete != null) {
            DeleteSchoolItemButton(item = item, onDelete = onDelete)
        }
    }
}

@Composable
private fun DeleteSchoolItemButton(item: SchoolItem, onDelete: () -> Unit) {
    var confirmingDelete by remember(item.id) { mutableStateOf(false) }

    IconButton(onClick = { confirmingDelete = true }) {
        Icon(
            Icons.Rounded.Delete,
            contentDescription = tr("Remove ${item.title}", "Supprimer ${item.title}"),
            tint = SchoolRed.copy(alpha = .78f)
        )
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(tr("Remove ${item.title}?", "Supprimer ${item.title} ?")) },
            text = {
                Text(
                    if (item.repeat == Repeat.ONE_TIME) {
                        tr("This removes it from ParentBell.", "Cet élément sera supprimé de ParentBell.")
                    } else {
                        tr("This removes the item and all of its future repeats.", "Cet élément et toutes ses répétitions futures seront supprimés.")
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    confirmingDelete = false
                    onDelete()
                }) { Text(tr("Remove", "Supprimer")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(tr("Cancel", "Annuler")) }
            }
        )
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
                Text(tr("Everything school. One place.", "Toute l’école. Un seul endroit."), color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text(tr("The fridge has officially been demoted.", "Le frigo vient officiellement d’être rétrogradé."), color = Ink.copy(alpha = .62f), fontSize = 12.sp)
            }
            MockupArtImage(MockupAsset.BACKPACK, modifier = Modifier.size(104.dp), contentDescription = tr("School backpack", "Sac à dos scolaire"))
        }
    }
}

@Composable
private fun MockupMiniStrip() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        MockupArtImage(MockupAsset.BOOKS, Modifier.size(45.dp), tr("Books", "Livres"))
        MockupArtImage(MockupAsset.PIZZA, Modifier.size(45.dp), "Pizza")
        MockupArtImage(MockupAsset.SHIRT, Modifier.size(45.dp), tr("Spirit shirt", "Chandail thématique"))
        MockupArtImage(MockupAsset.SANDWICH, Modifier.size(45.dp), tr("Sub day", "Journée sous-marin"))
    }
}

@Composable
private fun PapersArtStrip() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        MockupArtImage(MockupAsset.PALETTE, Modifier.size(58.dp), tr("Artwork", "Œuvre d’art"))
        MockupArtImage(MockupAsset.CAMERA, Modifier.size(52.dp), "Photos")
        MockupArtImage(MockupAsset.DOCUMENT, Modifier.size(52.dp), tr("Forms", "Formulaires"))
        MockupArtImage(MockupAsset.STAR, Modifier.size(42.dp), tr("Memories", "Souvenirs"))
    }
}

@Composable
private fun HomeScreen(vm: SchoolStuffViewModel, onAdd: () -> Unit, onChild: (String) -> Unit) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    var selectedChild by remember { mutableStateOf<String?>(null) }
    var editingParentNotes by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header("ParentBell", tr("Little things. Big school days. ♥", "Petites choses. Grandes journées d’école. ♥"), note = tr("You got this!", "Vous êtes capable !")) }
        item { HomeMockupHero() }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = selectedChild == null, onClick = { selectedChild = null }, label = { Text(tr("All", "Tous")) })
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
                        MockupArtImage(MockupAsset.CAMERA, modifier = Modifier.size(52.dp), contentDescription = tr("Picture Day", "Journée photo"))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("Picture Day coming up", "La journée photo approche"), color = Ink, fontWeight = FontWeight.Bold)
                            Text(upcomingPicture.first.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), color = Ink.copy(alpha = .65f))
                        }
                    }
                }
            }
        }
        item {
            ParentNotesCard(notes = vm.parentNotes, onEdit = { editingParentNotes = true })
        }
        item {
            DaySection(tr("Today", "Aujourd’hui"), today, SoftBlue, vm, selectedChild)
        }
        item {
            DaySection(tr("Tomorrow", "Demain"), tomorrow, SoftGreen, vm, selectedChild)
        }
        item {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 50.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text(tr("Add School Thing", "Ajouter un élément scolaire"))
            }
        }
        item {
            Text(
                tr("Tap a child below for teacher info, homework and papers.", "Touchez un enfant ci-dessous pour voir l’enseignant, les devoirs et les documents."),
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

    if (editingParentNotes) {
        ParentNotesDialog(
            currentNotes = vm.parentNotes,
            onDismiss = { editingParentNotes = false },
            onSave = { notes ->
                vm.saveParentNotes(notes)
                editingParentNotes = false
            }
        )
    }
}

@Composable
private fun ParentNotesCard(notes: String, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = SoftYellow),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.MenuBook, null, tint = SchoolBlue, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Parent Notes", "Notes des parents"), color = Ink, fontWeight = FontWeight.Bold)
                Text(
                    notes.ifBlank { tr("A little notebook for the things parents need to remember.", "Un petit carnet pour les choses que les parents doivent retenir.") },
                    color = Ink.copy(alpha = .62f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onEdit) { Text(if (notes.isBlank()) tr("Add", "Ajouter") else tr("Edit", "Modifier")) }
        }
    }
}

@Composable
private fun ParentNotesDialog(currentNotes: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var notes by remember(currentNotes) { mutableStateOf(currentNotes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Parent Notes", "Notes des parents")) },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("Quick notes", "Notes rapides")) },
                minLines = 4,
                maxLines = 8
            )
        },
        confirmButton = { Button(onClick = { onSave(notes) }) { Text(tr("Save", "Enregistrer")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "Annuler")) } }
    )
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
        Section("$title  •  ${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}", tint) {
            Column {
                if (list.isEmpty()) {
                    Text(tr("Nothing on the list. Suspiciously peaceful.", "Rien sur la liste. C’est presque suspect."), color = Ink.copy(alpha = .6f), modifier = Modifier.padding(vertical = 14.dp))
                } else {
                    list.forEachIndexed { index, item ->
                        SchoolRow(vm, item, onDelete = { vm.deleteItem(item.id) })
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
        item { Header(tr("Kids", "Enfants"), tr("Different schedules. One parent brain. ♥", "Des horaires différents. Un seul cerveau de parent. ♥"), note = tr("Same kids.\nBrighter days.", "Les mêmes enfants.\nDes journées plus simples.")) }
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
                            Text(child.grade.ifBlank { tr("Grade not set", "Niveau non indiqué") }, color = Ink.copy(alpha = .58f))
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
                Text(tr("Add Child", "Ajouter un enfant"))
            }
        }
    }

    if (addDialog) {
        var name by remember { mutableStateOf("") }
        var grade by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addDialog = false },
            title = { Text(tr("Add Child", "Ajouter un enfant")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(tr("Name", "Nom")) }, singleLine = true)
                    OutlinedTextField(grade, { grade = it }, label = { Text(tr("Grade", "Niveau")) }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = { vm.addChild(name, grade); addDialog = false }, enabled = name.isNotBlank()) { Text(tr("Add", "Ajouter")) }
            },
            dismissButton = { TextButton(onClick = { addDialog = false }) { Text(tr("Cancel", "Annuler")) } }
        )
    }
}

@Composable
private fun WorkWeekStrip(weekdays: List<LocalDate>, today: LocalDate) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        weekdays.forEach { date ->
            val isToday = date == today
            Surface(
                modifier = Modifier.weight(1f),
                color = if (isToday) SchoolBlue else SoftBlue,
                shape = RoundedCornerShape(13.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("EEE")).trimEnd('.').uppercase(),
                        color = if (isToday) Color.White else Ink.copy(alpha = .62f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        date.dayOfMonth.toString(),
                        color = if (isToday) Color.White else Ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun ChildScreen(vm: SchoolStuffViewModel, childId: String, onBack: () -> Unit, onAddThing: () -> Unit) {
    val child = vm.child(childId) ?: return
    val context = LocalContext.current
    var selectedDocType by remember { mutableStateOf(DocumentType.FORM) }
    var uploadDocType by remember { mutableStateOf(DocumentType.FORM) }
    var choosingDocumentType by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var editingTransportation by remember { mutableStateOf(false) }

    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull() ?: tr("School file", "Fichier scolaire")
            vm.addDocument(SchoolDocument(childId = childId, title = name, type = uploadDocType, uri = uri.toString()))
            selectedDocType = uploadDocType
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header(child.name, tr("Your school dashboard. ♥", "Votre tableau de bord scolaire. ♥"), back = onBack, note = child.grade.ifBlank { tr("School year", "Année scolaire") }) }
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
                        IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, tr("Edit", "Modifier")) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(child.teacherName.ifBlank { tr("Add teacher information", "Ajouter les renseignements de l’enseignant") }, color = Ink, fontWeight = FontWeight.SemiBold)
                    if (child.room.isNotBlank()) Text("${tr("Room", "Salle")} ${child.room}", color = Ink.copy(alpha = .6f))
                    if (child.schoolName.isNotBlank()) Text(child.schoolName, color = Ink.copy(alpha = .6f))
                    Spacer(Modifier.height(10.dp))
                    MockupMiniStrip()
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section(tr("This Week", "Cette semaine"), SchoolGreen) {
                    Column {
                        val today = LocalDate.now()
                        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
                        val weekdays = (0L..4L).map { monday.plusDays(it) }
                        WorkWeekStrip(weekdays = weekdays, today = today)
                        Spacer(Modifier.height(5.dp))
                        val weekItems = weekdays.flatMap { date ->
                            vm.itemsFor(date)
                                .filter { it.childId == childId }
                                .map { date to it }
                        }
                        if (weekItems.isEmpty()) Text(tr("Nothing scheduled yet.", "Rien de prévu pour le moment."), modifier = Modifier.padding(vertical = 12.dp))
                        weekItems.forEach { (date, item) ->
                            SchoolRow(
                                vm = vm,
                                item = item,
                                trailing = {
                                    Text(
                                        date.format(DateTimeFormatter.ofPattern("EEE d")),
                                        color = Ink.copy(alpha = .6f),
                                        fontSize = 12.sp
                                    )
                                },
                                onDelete = { vm.deleteItem(item.id) }
                            )
                            HorizontalDivider(color = Ink.copy(alpha = .07f))
                        }
                        TextButton(onClick = onAddThing) { Icon(Icons.Rounded.Add, null); Text(tr("Add school thing", "Ajouter un élément scolaire")) }
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section(tr("Teacher & School", "Enseignant et école"), SchoolBlue) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        InfoLine(Icons.Rounded.School, tr("Teacher", "Enseignant(e)"), child.teacherName.ifBlank { tr("Not added", "Non ajouté") })
                        InfoLine(Icons.Rounded.Email, tr("Email", "Courriel"), child.teacherEmail.ifBlank { tr("Not added", "Non ajouté") })
                        InfoLine(Icons.Rounded.Call, tr("Classroom phone", "Téléphone de la classe"), child.classroomPhone.ifBlank { tr("Not added", "Non ajouté") })
                        InfoLine(Icons.Rounded.Home, tr("School", "École"), child.schoolName.ifBlank { tr("Not added", "Non ajouté") })
                        InfoLine(Icons.Rounded.Call, tr("School phone", "Téléphone de l’école"), child.schoolPhone.ifBlank { tr("Not added", "Non ajouté") })
                        if (child.specialNotes.isNotBlank()) Text("${tr("Note", "Note")} : ${child.specialNotes}", color = Ink.copy(alpha = .7f), fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            val info = vm.transportationFor(childId)
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section(tr("Transportation", "Transport"), SchoolGreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        if (info == null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.DirectionsBus, null, tint = SchoolBlue, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(tr("No transportation information added yet.", "Aucun renseignement de transport n’a été ajouté."), color = Ink.copy(alpha = .62f), modifier = Modifier.weight(1f))
                            }
                        } else if (info.mode == TransportationMode.PRIVATE) {
                            InfoLine(Icons.Rounded.DirectionsCar, tr("Type", "Type"), transportationModeLabel(TransportationMode.PRIVATE))
                            Text(
                                info.pickupInfo.ifBlank { tr("Add who is picking up, where and when.", "Ajoutez qui vient chercher l’enfant, où et quand.") },
                                color = Ink.copy(alpha = .7f),
                                fontSize = 13.sp
                            )
                        } else {
                            InfoLine(Icons.Rounded.DirectionsBus, tr("Bus number", "Numéro d’autobus"), info.busNumber.ifBlank { tr("Not added", "Non ajouté") })
                            InfoLine(Icons.Rounded.LocationOn, tr("Pickup point", "Point d’embarquement"), info.pickupPoint.ifBlank { tr("Not added", "Non ajouté") })
                            InfoLine(Icons.Rounded.Person, tr("Driver", "Chauffeur"), info.driverName.ifBlank { tr("Not added", "Non ajouté") })
                            if (info.pickupInfo.isNotBlank()) Text(info.pickupInfo, color = Ink.copy(alpha = .7f), fontSize = 13.sp)
                        }
                        TextButton(onClick = { editingTransportation = true }) {
                            Icon(Icons.Rounded.Edit, null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (info == null) tr("Add transportation", "Ajouter le transport") else tr("Edit transportation", "Modifier le transport"))
                        }
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section(tr("Homework & Forms", "Devoirs et formulaires"), SchoolYellow) {
                    Column {
                        val tasks = vm.items.filter { it.childId == childId && it.category in listOf(Category.HOMEWORK, Category.FORM_DUE, Category.BRING_ITEM) }
                        if (tasks.isEmpty()) Text(tr("No homework or forms tracked yet.", "Aucun devoir ni formulaire suivi."), modifier = Modifier.padding(vertical = 12.dp))
                        tasks.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = item.completed, onCheckedChange = { vm.toggleComplete(item.id) })
                                Box(Modifier.weight(1f)) {
                                    SchoolRow(vm, item, onDelete = { vm.deleteItem(item.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section(tr("Papers & Memories", "Documents et souvenirs"), SchoolPink) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PapersArtStrip()
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DocumentType.all.forEach { type ->
                                FilterChip(selected = selectedDocType == type, onClick = { selectedDocType = type }, label = { Text(documentTypeLabel(type)) })
                            }
                        }
                        val docs = vm.documents.filter { it.childId == childId && it.type == selectedDocType }
                        if (docs.isEmpty()) {
                            Text(
                                tr("Nothing saved in ${documentTypeLabel(selectedDocType).lowercase()} yet.", "Aucun élément enregistré dans ${documentTypeLabel(selectedDocType).lowercase()} pour le moment."),
                                color = Ink.copy(alpha = .58f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                        docs.forEach { doc ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openSchoolDocument(context, doc) },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(documentIcon(doc.type), null, tint = SchoolBlue)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(documentDisplayTitle(doc), color = Ink, fontWeight = FontWeight.Bold)
                                        Text(
                                            doc.title,
                                            color = Ink.copy(alpha = .55f),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    TextButton(onClick = { openSchoolDocument(context, doc) }) { Text(tr("Open", "Ouvrir")) }
                                    TextButton(onClick = { vm.deleteDocument(doc.id) }) { Text(tr("Remove", "Supprimer")) }
                                }
                            }
                        }
                        OutlinedButton(onClick = { choosingDocumentType = true }) {
                            Icon(Icons.Rounded.UploadFile, null)
                            Spacer(Modifier.width(6.dp))
                            Text(tr("Add Paper or Memory", "Ajouter un document ou un souvenir"))
                        }
                    }
                }
            }
        }
    }

    if (editing) {
        EditChildDialog(child, onDismiss = { editing = false }, onSave = { vm.updateChild(it); editing = false })
    }

    if (choosingDocumentType) {
        DocumentTypePickerDialog(
            onDismiss = { choosingDocumentType = false },
            onSelected = { type ->
                uploadDocType = type
                selectedDocType = type
                choosingDocumentType = false
                documentLauncher.launch(arrayOf("image/*", "application/pdf"))
            }
        )
    }

    if (editingTransportation) {
        TransportationDialog(
            child = child,
            current = vm.transportationFor(childId),
            onDismiss = { editingTransportation = false },
            onSave = { info ->
                vm.saveTransportation(info)
                editingTransportation = false
            }
        )
    }
}

@Composable
private fun TransportationDialog(
    child: ChildProfile,
    current: TransportationInfo?,
    onDismiss: () -> Unit,
    onSave: (TransportationInfo) -> Unit
) {
    var mode by remember(current) { mutableStateOf(current?.mode ?: TransportationMode.BUS) }
    var busNumber by remember(current) { mutableStateOf(current?.busNumber.orEmpty()) }
    var pickupPoint by remember(current) { mutableStateOf(current?.pickupPoint.orEmpty()) }
    var driverName by remember(current) { mutableStateOf(current?.driverName.orEmpty()) }
    var pickupInfo by remember(current) { mutableStateOf(current?.pickupInfo.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("${child.name}'s transportation", "Transport de ${child.name}")) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(tr("Transportation type", "Type de transport"), color = Ink, fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        TransportationMode.all.forEach { option ->
                            FilterChip(selected = mode == option, onClick = { mode = option }, label = { Text(transportationModeLabel(option)) })
                        }
                    }
                }
                if (mode == TransportationMode.BUS) {
                    item { OutlinedTextField(busNumber, { busNumber = it }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Bus number", "Numéro d’autobus")) }, singleLine = true) }
                    item { OutlinedTextField(pickupPoint, { pickupPoint = it }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Pickup point or stop", "Point d’embarquement ou arrêt")) }, singleLine = true) }
                    item { OutlinedTextField(driverName, { driverName = it }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Driver's name", "Nom du chauffeur")) }, singleLine = true) }
                    item { OutlinedTextField(pickupInfo, { pickupInfo = it }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Pickup time or extra notes", "Heure d’embarquement ou notes")) }, minLines = 2, maxLines = 4) }
                } else {
                    item {
                        OutlinedTextField(
                            pickupInfo,
                            { pickupInfo = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(tr("Who is picking up, where and when?", "Qui vient chercher l’enfant, où et quand ?")) },
                            minLines = 4,
                            maxLines = 7
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    TransportationInfo(
                        childId = child.id,
                        mode = mode,
                        busNumber = busNumber.trim(),
                        pickupPoint = pickupPoint.trim(),
                        driverName = driverName.trim(),
                        pickupInfo = pickupInfo.trim()
                    )
                )
            }) { Text(tr("Save", "Enregistrer")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "Annuler")) } }
    )
}

@Composable
private fun DocumentTypePickerDialog(onDismiss: () -> Unit, onSelected: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("What are you adding?", "Qu’ajoutez-vous ?")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                documentUploadChoices.forEach { type ->
                    OutlinedButton(onClick = { onSelected(type) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(documentIcon(type), null)
                        Spacer(Modifier.width(8.dp))
                        Text(documentTypeLabel(type), modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "Annuler")) } }
    )
}

private val documentUploadChoices = DocumentType.all

private fun documentDisplayTitle(document: SchoolDocument): String {
    val date = Instant.ofEpochMilli(document.addedAtMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    return "${documentTypeLabel(document.type)} • $date"
}

private fun openSchoolDocument(context: Context, document: SchoolDocument) {
    runCatching {
        val uri = Uri.parse(document.uri)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(viewIntent, tr("Open school file", "Ouvrir le fichier scolaire")))
    }.onFailure {
        Toast.makeText(context, tr("This file is no longer available. Try adding it again.", "Ce fichier n’est plus disponible. Essayez de l’ajouter de nouveau."), Toast.LENGTH_LONG).show()
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
    DocumentType.MEDICAL -> Icons.Rounded.Assignment
    DocumentType.ARTWORK -> Icons.Rounded.Palette
    DocumentType.PHOTO -> Icons.Rounded.PhotoCamera
    else -> Icons.Rounded.Description
}

@Composable
private fun ReminderTimeRow(label: String, hour: Int, minute: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onClick) {
            Text(LocalTime.of(hour, minute).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
        }
    }
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
        title = { Text(tr("${child.name}'s school info", "Renseignements scolaires de ${child.name}")) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                item { OutlinedTextField(grade, { grade = it }, label = { Text(tr("Grade", "Niveau")) }, singleLine = true) }
                item { OutlinedTextField(teacher, { teacher = it }, label = { Text(tr("Teacher", "Enseignant(e)")) }, singleLine = true) }
                item { OutlinedTextField(email, { email = it }, label = { Text(tr("Teacher email", "Courriel de l’enseignant")) }, singleLine = true) }
                item { OutlinedTextField(phone, { phone = it }, label = { Text(tr("Classroom phone", "Téléphone de la classe")) }, singleLine = true) }
                item { OutlinedTextField(room, { room = it }, label = { Text(tr("Room", "Salle")) }, singleLine = true) }
                item { OutlinedTextField(school, { school = it }, label = { Text(tr("School", "École")) }, singleLine = true) }
                item { OutlinedTextField(schoolPhone, { schoolPhone = it }, label = { Text(tr("School phone", "Téléphone de l’école")) }, singleLine = true) }
                item { OutlinedTextField(notes, { notes = it }, label = { Text(tr("Special notes", "Notes particulières")) }) }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(child.copy(grade = grade, teacherName = teacher, teacherEmail = email, classroomPhone = phone, room = room, schoolName = school, schoolPhone = schoolPhone, specialNotes = notes))
            }) { Text(tr("Save", "Enregistrer")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "Annuler")) } }
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
    var reminder by remember { mutableStateOf(vm.defaultReminder) }
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
        item { Header(tr("Add School Thing", "Ajouter un élément scolaire"), tr("Get it out of your brain and into the app. ♥", "Sortez-le de votre tête et mettez-le dans l’appli. ♥"), back = onBack, note = tr("Future you says thanks.", "Votre futur vous remercie.")) }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Who is this for?", "Pour qui ?"), color = Ink, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip(selected = childId.isBlank(), onClick = { childId = "" }, label = { Text(tr("Family", "Famille")) })
                    vm.children.forEach { child ->
                        FilterChip(selected = childId == child.id, onClick = { childId = child.id }, label = { Text(child.name) })
                    }
                }
                OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("What's the school thing?", "Quel est l’élément scolaire ?")) }, singleLine = true)
                Text(tr("Category", "Catégorie"), color = Ink, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (category == customCategoryOption) tr("Custom…", "Personnalisée…") else categoryLabel(category),
                        onValueChange = {},
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
                                text = { Text(categoryLabel(cat)) },
                                leadingIcon = { Icon(categoryIcon(cat), contentDescription = null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    category = cat
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(tr("Custom…", "Personnalisée…")) },
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
                        label = { Text(tr("Custom category", "Catégorie personnalisée")) },
                        placeholder = { Text(tr("e.g. Field Trip, Club, Fundraiser", "p. ex. Sortie, Club, Collecte de fonds")) },
                        singleLine = true
                    )
                }
                Text(tr("Emoji (optional)", "Émoji (facultatif)"), color = Ink, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = { showEmojiPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(if (selectedEmoji.isBlank()) "🙂" else selectedEmoji, fontSize = 25.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(if (selectedEmoji.isBlank()) tr("Choose an emoji", "Choisir un émoji") else tr("Change emoji", "Changer l’émoji"), modifier = Modifier.weight(1f))
                }
                Text(tr("Date", "Date"), color = Ink, fontWeight = FontWeight.Bold)
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
                            date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                            color = Ink,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(tr("Tap to choose a date", "Touchez pour choisir une date"), color = Ink.copy(alpha = .55f), fontSize = 12.sp)
                    }
                }
                Text(tr("Repeat", "Répétition"), color = Ink, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Repeat.all.forEach { value -> FilterChip(selected = repeat == value, onClick = { repeat = value }, label = { Text(repeatLabel(value)) }) }
                }
                Text(tr("Reminder", "Rappel"), color = Ink, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Reminder.all.forEach { value -> FilterChip(selected = reminder == value, onClick = { reminder = value }, label = { Text(reminderLabel(value)) }) }
                }
                OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth(), label = { Text(tr("Notes", "Notes")) })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = needsHome, onCheckedChange = { needsHome = it })
                    Text(tr("Needs item from home", "Objet requis de la maison"), color = Ink)
                }
                Button(
                    onClick = {
                        val finalCategory = if (category == customCategoryOption) customCategory.trim().ifBlank { "Other" } else category
                        vm.addItem(
                            SchoolItem(
                                childId = childId,
                                title = title.trim(),
                                category = finalCategory,
                                emoji = selectedEmoji.takeIf { it.isNotBlank() },
                                dateIso = date.toString(),
                                repeat = repeat,
                                reminder = reminder,
                                notes = notes.trim(),
                                needsItemFromHome = needsHome
                            )
                        )
                        onBack()
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null)
                    Spacer(Modifier.width(6.dp))
                    Text(tr("Save", "Enregistrer"))
                }
            }
        }
    }

    if (showEmojiPicker) {
        AlertDialog(
            onDismissRequest = { showEmojiPicker = false },
            title = { Text(tr("Choose an emoji", "Choisir un émoji")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        tr("Pick something that will make this school thing easy to spot.", "Choisissez quelque chose qui permettra de repérer facilement cet élément."),
                        color = Ink.copy(alpha = .65f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    emojiChoices.chunked(5).forEach { emojiRow ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
            confirmButton = { TextButton(onClick = { showEmojiPicker = false }) { Text(tr("Close", "Fermer")) } },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedEmoji = ""
                        showEmojiPicker = false
                    }
                ) { Text(tr("No emoji", "Aucun émoji")) }
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
                ) { Text(tr("Choose", "Choisir")) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(tr("Cancel", "Annuler")) } }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState, showModeToggle = true)
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
        item { Header(tr("Calendar", "Calendrier"), tr("The week without the fridge blindness. ♥", "La semaine sans l’angle mort du frigo. ♥"), note = tr("This week", "Cette semaine")) }
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
                        Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), color = Ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        dayItems.forEach { item ->
                            SchoolRow(vm, item, onDelete = { vm.deleteItem(item.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSettingsCard(
    title: String,
    summary: String,
    icon: ImageVector,
    accent: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(summary, color = Ink.copy(alpha = .58f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) tr("Collapse $title", "Réduire $title") else tr("Expand $title", "Développer $title"),
                tint = Ink.copy(alpha = .55f)
            )
        }
        if (expanded) {
            HorizontalDivider(color = Ink.copy(alpha = .08f))
            Box(Modifier.fillMaxWidth().padding(14.dp)) { content() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    vm: SchoolStuffViewModel,
    appLockEnabled: Boolean,
    onChangeAppLock: (Boolean, (Boolean, String) -> Unit) -> Unit,
    onTestAppLock: ((Boolean, String) -> Unit) -> Unit,
    appLanguage: String,
    onChangeLanguage: (String) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { CalendarProviderRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var calendarPermissionGranted by remember { mutableStateOf(repo.hasReadPermission()) }
    var calendars by remember { mutableStateOf<List<DeviceCalendar>>(emptyList()) }
    var calendarListRevision by remember { mutableIntStateOf(0) }
    var calendarListExpanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(vm.selectedCalendarIds()) }
    var defaultId by remember { mutableStateOf(vm.defaultCalendarId()) }
    var message by remember { mutableStateOf("") }
    var calendarSyncing by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(ReminderNotifications.areEnabled(context)) }
    var notificationMessage by remember { mutableStateOf("") }
    var defaultReminderMenuExpanded by remember { mutableStateOf(false) }
    var expandedSettingsSection by remember { mutableStateOf<String?>(null) }
    var securityMessage by remember { mutableStateOf("") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationsEnabled = ReminderNotifications.areEnabled(context)
        notificationMessage = if (notificationsEnabled) {
            vm.rescheduleAllReminders()
            tr("Notifications are ready.", "Les notifications sont prêtes.")
        } else {
            tr("Notifications are still blocked. You can allow them in your phone settings.", "Les notifications sont toujours bloquées. Vous pouvez les autoriser dans les paramètres du téléphone.")
        }
    }

    DisposableEffect(context, lifecycleOwner) {
        ReminderNotifications.ensureChannel(context)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = ReminderNotifications.areEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            !granted -> tr("Google Calendar is not connected.", "Google Agenda n’est pas connecté.")
            result.isEmpty() -> tr("Connected, but Android has not exposed any visible Google calendars yet.", "Connecté, mais Android n’a pas encore affiché de calendriers Google visibles.")
            else -> tr("Connected. ${result.size} calendar${if (result.size == 1) "" else "s"} available.", "Connecté. ${result.size} calendrier${if (result.size == 1) "" else "s"} disponible${if (result.size == 1) "" else "s"}.")
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
        message = tr("Calendar access is connected, but Android has not exposed the list yet. Keep this screen open and tap Refresh calendars.", "L’accès au calendrier est connecté, mais Android n’a pas encore affiché la liste. Gardez cet écran ouvert et touchez Actualiser les calendriers.")
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val readGranted = result[Manifest.permission.READ_CALENDAR] == true || repo.hasReadPermission()
        calendarPermissionGranted = readGranted
        if (readGranted) {
            message = tr("Calendar access granted. Loading calendars…", "Accès au calendrier accordé. Chargement des calendriers…")
            calendarSyncing = true
            scope.launch {
                refreshCalendarsWithRetry()
                calendarSyncing = false
            }
        } else {
            calendars = emptyList()
            calendarListRevision++
            message = tr("Calendar permission was not granted.", "L’autorisation d’accès au calendrier n’a pas été accordée.")
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
        item { Header(tr("Settings", "Paramètres"), tr("Everything useful, without the giant wall of controls. ♥", "Tout ce qui est utile, sans le mur géant d’options. ♥"), note = tr("Tap a section to open it", "Touchez une section pour l’ouvrir")) }
        item {
            CollapsibleSettingsCard(
                title = tr("Language", "Langue"),
                summary = if (appLanguage == AppLanguage.FRENCH) "Français" else "English",
                icon = Icons.Rounded.Language,
                accent = Color(0xFFC98A00),
                expanded = expandedSettingsSection == "language",
                onToggle = { expandedSettingsSection = if (expandedSettingsSection == "language") null else "language" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        tr("Choose the language used throughout ParentBell.", "Choisissez la langue utilisée dans ParentBell."),
                        color = Ink.copy(alpha = .68f),
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = appLanguage == AppLanguage.ENGLISH,
                            onClick = { onChangeLanguage(AppLanguage.ENGLISH) },
                            label = { Text("English") }
                        )
                        FilterChip(
                            selected = appLanguage == AppLanguage.FRENCH,
                            onClick = { onChangeLanguage(AppLanguage.FRENCH) },
                            label = { Text("Français") }
                        )
                    }
                }
            }
        }
        item {
            CollapsibleSettingsCard(
                title = "Google Calendar",
                summary = when {
                    !calendarPermissionGranted -> tr("Not connected", "Non connecté")
                    selected.isEmpty() -> tr("Connected · no calendars selected", "Connecté · aucun calendrier sélectionné")
                    else -> tr("Connected · ${selected.size} calendar${if (selected.size == 1) "" else "s"} selected", "Connecté · ${selected.size} calendrier${if (selected.size == 1) "" else "s"} sélectionné${if (selected.size == 1) "" else "s"}")
                },
                icon = Icons.Rounded.CalendarMonth,
                accent = Color(0xFF287AC8),
                expanded = expandedSettingsSection == "calendar",
                onToggle = { expandedSettingsSection = if (expandedSettingsSection == "calendar") null else "calendar" }
            ) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MockupArtImage(MockupAsset.BOOKS, Modifier.size(54.dp), "School calendar")
                            Spacer(Modifier.width(10.dp))
                            Text(
                                tr("ParentBell reads Google calendars already synced to this phone. Pick only the calendars you want to see.", "ParentBell lit les calendriers Google déjà synchronisés avec ce téléphone. Choisissez seulement ceux que vous voulez voir."),
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
                                Text(tr("Connect Calendar", "Connecter le calendrier"))
                            }
                        } else {
                            if (calendarSyncing) {
                                Text(tr("Loading calendars…", "Chargement des calendriers…"), color = Ink.copy(alpha = .62f), fontSize = 12.sp)
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
                                            tr("Calendars shown in ParentBell · ${selectedVisible.size} selected", "Calendriers affichés dans ParentBell · ${selectedVisible.size} sélectionné${if (selectedVisible.size == 1) "" else "s"}"),
                                            color = Ink,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (selectedVisible.isEmpty()) {
                                            Text(tr("Tap to choose calendars", "Touchez pour choisir les calendriers"), color = Ink.copy(alpha = .55f), fontSize = 11.sp)
                                        } else {
                                            selectedVisible.take(3).forEach { cal ->
                                                Text("${cal.displayName} · ${cal.accountName}", color = Ink.copy(alpha = .58f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            if (selectedVisible.size > 3) {
                                                Text(tr("+${selectedVisible.size - 3} more", "+${selectedVisible.size - 3} de plus"), color = SchoolBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                    Icon(
                                        if (calendarListExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        if (calendarListExpanded) tr("Collapse calendars", "Réduire les calendriers") else tr("Expand calendars", "Développer les calendriers"),
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
                                                    label = { Text(if (defaultId == cal.id) tr("Writes here", "Écriture ici") else tr("Use", "Utiliser")) }
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
                                }) { Text(tr("Refresh calendars", "Actualiser les calendriers")) }
                                OutlinedButton(onClick = {
                                    val imported = vm.importCalendarEvents(repo.importInstances(selected))
                                    message = tr("$imported new calendar event${if (imported == 1) "" else "s"} imported.", "$imported nouvel événement de calendrier importé${if (imported == 1) "" else "s"}.")
                                }, enabled = selected.isNotEmpty()) { Text(tr("Import", "Importer")) }
                            }
                            Button(onClick = {
                                val target = defaultId
                                if (target == null) {
                                    message = tr("Choose a selected calendar as the write destination first.", "Choisissez d’abord un calendrier sélectionné comme destination d’écriture.")
                                } else {
                                    val count = vm.items.count { repo.upsertSchoolItem(target, it) }
                                    message = tr("$count ParentBell events synced.", "$count événements ParentBell synchronisés.")
                                }
                            }, enabled = defaultId != null) { Text(tr("Sync ParentBell Out", "Synchroniser ParentBell vers Google")) }

                            if (message.isNotBlank()) Text(message, color = Ink.copy(alpha = .68f), fontSize = 12.sp)
                        }
                    }
            }
        }
        item {
            CollapsibleSettingsCard(
                title = tr("Notifications", "Notifications"),
                summary = if (notificationsEnabled) {
                    tr("On · night before ${LocalTime.of(vm.nightReminderHour, vm.nightReminderMinute).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))} · morning ${LocalTime.of(vm.morningReminderHour, vm.morningReminderMinute).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))}", "Activées · veille ${LocalTime.of(vm.nightReminderHour, vm.nightReminderMinute).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))} · matin ${LocalTime.of(vm.morningReminderHour, vm.morningReminderMinute).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))}")
                } else {
                    tr("Off · tap to set up reminders", "Désactivées · touchez pour configurer les rappels")
                },
                icon = Icons.Rounded.Notifications,
                accent = Color(0xFF2C9A60),
                expanded = expandedSettingsSection == "notifications",
                onToggle = { expandedSettingsSection = if (expandedSettingsSection == "notifications") null else "notifications" }
            ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Notifications, null, tint = if (notificationsEnabled) SchoolGreen else Ink.copy(alpha = .45f))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (notificationsEnabled) tr("Notifications allowed", "Notifications autorisées") else tr("Notifications are off", "Notifications désactivées"),
                                    color = Ink,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (notificationsEnabled) tr("ParentBell can send the reminders chosen on your items.", "ParentBell peut envoyer les rappels choisis pour vos éléments.")
                                    else tr("Allow notifications so school reminders can reach you.", "Autorisez les notifications pour recevoir les rappels scolaires."),
                                    color = Ink.copy(alpha = .62f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (!notificationsEnabled) {
                            Button(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationsEnabled = ReminderNotifications.areEnabled(context)
                                }
                            }) { Text(tr("Allow notifications", "Autoriser les notifications")) }
                        }

                        ReminderTimeRow(
                            label = tr("Night before", "La veille"),
                            hour = vm.nightReminderHour,
                            minute = vm.nightReminderMinute,
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute -> vm.setNightReminderTime(hour, minute) },
                                    vm.nightReminderHour,
                                    vm.nightReminderMinute,
                                    android.text.format.DateFormat.is24HourFormat(context)
                                ).show()
                            }
                        )
                        ReminderTimeRow(
                            label = tr("Morning of", "Le matin même"),
                            hour = vm.morningReminderHour,
                            minute = vm.morningReminderMinute,
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute -> vm.setMorningReminderTime(hour, minute) },
                                    vm.morningReminderHour,
                                    vm.morningReminderMinute,
                                    android.text.format.DateFormat.is24HourFormat(context)
                                ).show()
                            }
                        )

                        Text(tr("Default for new items", "Valeur par défaut des nouveaux éléments"), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        ExposedDropdownMenuBox(
                            expanded = defaultReminderMenuExpanded,
                            onExpandedChange = { defaultReminderMenuExpanded = !defaultReminderMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = reminderLabel(vm.defaultReminder),
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(defaultReminderMenuExpanded) },
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = defaultReminderMenuExpanded,
                                onDismissRequest = { defaultReminderMenuExpanded = false }
                            ) {
                                Reminder.all.forEach { choice ->
                                    DropdownMenuItem(
                                        text = { Text(reminderLabel(choice)) },
                                        onClick = {
                                            vm.updateDefaultReminder(choice)
                                            defaultReminderMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val shown = ReminderNotifications.showTest(context)
                                notificationsEnabled = ReminderNotifications.areEnabled(context)
                                notificationMessage = if (shown) tr("Test notification sent.", "Notification d’essai envoyée.") else tr("Allow notifications first.", "Autorisez d’abord les notifications.")
                            }) { Text(tr("Send test", "Envoyer un test")) }
                            OutlinedButton(onClick = {
                                ReminderNotifications.ensureChannel(context)
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }) { Text(tr("Phone settings", "Paramètres du téléphone")) }
                        }

                        if (notificationMessage.isNotBlank()) {
                            Text(notificationMessage, color = Ink.copy(alpha = .68f), fontSize = 12.sp)
                        }
                        Text(
                            tr("Android may deliver scheduled reminders a little after the chosen time to save battery.", "Android peut envoyer les rappels programmés un peu après l’heure choisie afin d’économiser la batterie."),
                            color = Ink.copy(alpha = .55f),
                            fontSize = 11.sp
                        )
                    }
            }
        }
        item {
            CollapsibleSettingsCard(
                title = tr("Security", "Sécurité"),
                summary = if (appLockEnabled) tr("App lock on · fingerprint, face, or phone lock", "Verrouillage activé · empreinte, visage ou téléphone") else tr("App lock off", "Verrouillage désactivé"),
                icon = Icons.Rounded.Lock,
                accent = Color(0xFFD45680),
                expanded = expandedSettingsSection == "security",
                onToggle = { expandedSettingsSection = if (expandedSettingsSection == "security") null else "security" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = SchoolBlue, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.Face, contentDescription = null, tint = SchoolPink, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("Fingerprint or face", "Empreinte ou visage"), color = Ink, fontWeight = FontWeight.Bold)
                            Text(tr("Android uses the biometric method enrolled on this phone, with PIN, pattern, or password as backup.", "Android utilise la méthode biométrique configurée sur ce téléphone, avec le NIP, le schéma ou le mot de passe en secours."), color = Ink.copy(alpha = .62f), fontSize = 12.sp)
                        }
                    }

                    Text(
                        tr("When enabled, ParentBell locks at launch and after it has been away for 30 seconds. Child information is hidden from the recent-apps preview.", "Lorsqu’il est activé, ParentBell se verrouille au démarrage et après 30 secondes en arrière-plan. Les renseignements des enfants sont masqués dans l’aperçu des applis récentes."),
                        color = Ink.copy(alpha = .66f),
                        fontSize = 12.sp
                    )

                    if (appLockEnabled) {
                        Button(onClick = {
                            onTestAppLock { _, message -> securityMessage = message }
                        }) { Text(tr("Test unlock", "Tester le déverrouillage")) }
                        OutlinedButton(onClick = {
                            onChangeAppLock(false) { _, message -> securityMessage = message }
                        }) { Text(tr("Turn off app lock", "Désactiver le verrouillage")) }
                    } else {
                        Button(onClick = {
                            onChangeAppLock(true) { _, message -> securityMessage = message }
                        }) {
                            Icon(Icons.Rounded.Lock, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(tr("Turn on app lock", "Activer le verrouillage"))
                        }
                    }

                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                    }) { Text(tr("Phone security settings", "Paramètres de sécurité du téléphone")) }

                    if (securityMessage.isNotBlank()) {
                        Text(securityMessage, color = Ink.copy(alpha = .7f), fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            CollapsibleSettingsCard(
                title = tr("About ParentBell", "À propos de ParentBell"),
                summary = tr("Version ${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}", "Version ${BuildConfig.VERSION_NAME} · compilation ${BuildConfig.VERSION_CODE}"),
                icon = Icons.Rounded.School,
                accent = Color(0xFF6557B8),
                expanded = expandedSettingsSection == "about",
                onToggle = { expandedSettingsSection = if (expandedSettingsSection == "about") null else "about" }
            ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(tr("ParentBell ${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}", "ParentBell ${BuildConfig.VERSION_NAME} · compilation ${BuildConfig.VERSION_CODE}"), color = Ink, fontWeight = FontWeight.Bold)
                        Text(tr("Compact settings, notification controls, and optional fingerprint or face app lock.", "Paramètres compacts, réglages des notifications et verrouillage facultatif par empreinte ou visage."), color = Ink.copy(alpha = .65f), fontSize = 13.sp)
                    }
            }
        }
    }
}
