package ca.creativepixels.schoolstuff

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private data class PreviewChild(val name: String, val grade: String, val color: Color)
private data class PreviewItem(val title: String, val child: String, val date: LocalDate, val notes: String, val art: MockupAsset)

/** A separate, read-only example family. This screen never receives the real data ViewModel. */
@Composable
fun ParentBellPreviewScreen(page: String, onSubscription: () -> Unit) {
    val today = remember { LocalDate.now() }
    val children = listOf(
        PreviewChild("Alex", tr("Grade 3", "3e année"), SchoolBlue),
        PreviewChild("Sam", tr("Kindergarten", "Maternelle"), SchoolGreen)
    )
    val examples = listOf(
        PreviewItem(tr("Library books", "Livres de bibliothèque"), "Alex", today,
            tr("Bring the two library books in the backpack. Reminder: 7:30 a.m.", "Mettre les deux livres de bibliothèque dans le sac à dos. Rappel : 7 h 30."), MockupAsset.BOOKS),
        PreviewItem(tr("Permission form", "Formulaire d’autorisation"), "Sam", today,
            tr("Sign the class trip form and return it to the teacher.", "Signer le formulaire de sortie et le remettre à l’enseignante."), MockupAsset.DOCUMENT),
        PreviewItem(tr("Gym day", "Journée de sport"), "Alex", today.plusDays(1),
            tr("Pack indoor running shoes and a water bottle.", "Préparer les chaussures de sport d’intérieur et une bouteille d’eau."), MockupAsset.SHOES),
        PreviewItem(tr("Picture day", "Journée photo"), "Sam", today.plusDays(4),
            tr("Class pictures at school. Choose an outfit the night before.", "Photos de classe à l’école. Choisir une tenue la veille."), MockupAsset.CAMERA)
    )
    var childPage by remember(page) { mutableStateOf<PreviewChild?>(null) }
    var selectedChild by remember(page) { mutableStateOf<String?>(null) }
    var details by remember(page) { mutableStateOf<PreviewItem?>(null) }
    var month by remember(page) { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember(page) { mutableStateOf(today) }
    val locale = if (tr("en", "fr") == "fr") Locale.CANADA_FRENCH else Locale.CANADA
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    BackHandler(enabled = childPage != null) { childPage = null }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(color = SoftYellow, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("Explore ParentBell", "Découvrez ParentBell"), color = Ink, fontWeight = FontWeight.Bold)
                    Text(tr("Preview with example information. Subscribe to add your own family.", "Aperçu avec des exemples. Abonnez-vous pour ajouter votre famille."), color = Ink.copy(alpha = .72f), fontSize = 13.sp)
                    OutlinedButton(onClick = onSubscription, modifier = Modifier.fillMaxWidth()) {
                        Text(tr("View subscription", "Voir l’abonnement"))
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (childPage != null) {
                    IconButton(onClick = { childPage = null }) {
                        Icon(Icons.Rounded.ArrowBack, tr("Back", "Retour"), tint = Ink)
                    }
                }
                Column {
                    Text(childPage?.name ?: when (page) {
                        "CALENDAR" -> tr("Calendar", "Calendrier")
                        "KIDS" -> tr("Kids", "Enfants")
                        else -> "ParentBell"
                    }, color = Ink, fontWeight = FontWeight.Black, fontSize = 26.sp)
                    Text(childPage?.grade ?: tr("Little things. Big school days. ♥", "Petites choses. Grandes journées d’école. ♥"), color = Ink.copy(alpha = .65f))
                }
            }
        }
        when {
            childPage != null -> {
                val child = childPage!!
                item {
                    PreviewSection(tr("School & teachers", "École et enseignants"), SoftBlue) {
                        Text(tr("Maple Grove School", "École du Bosquet"), color = Ink, fontWeight = FontWeight.Bold)
                        PreviewText(tr("Teacher: Ms. Taylor · Homeroom", "Enseignante : Mme Taylor · Classe principale"))
                        PreviewText(tr("Keep teacher contact details and subjects together.", "Gardez les coordonnées des enseignants et leurs matières au même endroit."))
                    }
                }
                item {
                    PreviewSection(tr("Bus details", "Transport scolaire"), SoftGreen) {
                        PreviewText(tr("Bus 24 · Oak Street stop", "Autobus 24 · Arrêt rue du Chêne"))
                        PreviewText(tr("Pickup 8:10 a.m. · Drop-off 3:35 p.m.", "Départ à 8 h 10 · Retour à 15 h 35"))
                    }
                }
                item {
                    PreviewSection(tr("School things", "Éléments scolaires"), SoftYellow) {
                        examples.filter { it.child == child.name }.forEach { event ->
                            PreviewSchoolRow(event, { details = event }, dateFormat)
                        }
                    }
                }
                item {
                    PreviewSection(tr("Absence at a glance", "Absences en un coup d’œil"), SoftPink) {
                        PreviewText(tr("This school year: 2 days", "Cette année scolaire : 2 jours"))
                        PreviewText(tr("Sick: 1 · Appointment: 1", "Maladie : 1 · Rendez-vous : 1"))
                    }
                }
                item {
                    PreviewSection(tr("Papers & memories", "Documents et souvenirs"), SoftBlue) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MockupArtImage(MockupAsset.FOLDER, Modifier.size(44.dp))
                            PreviewText(tr("Class trip letter · First-day photo", "Lettre de sortie · Photo de rentrée"))
                        }
                    }
                }
            }
            page == "CALENDAR" -> {
                item {
                    PreviewSection(tr("School calendar", "Calendrier scolaire"), SoftBlue) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { month = month.minusMonths(1); selectedDate = month.atDay(1) }) {
                                Icon(Icons.Rounded.ArrowBack, tr("Previous month", "Mois précédent"), tint = Ink)
                            }
                            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), color = Ink,
                                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { month = month.plusMonths(1); selectedDate = month.atDay(1) }) {
                                Icon(Icons.Rounded.ArrowForward, tr("Next month", "Mois suivant"), tint = Ink)
                            }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            (1..7).forEach { day ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(java.time.DayOfWeek.of(day).getDisplayName(TextStyle.NARROW, locale), color = Ink.copy(alpha = .6f), fontSize = 12.sp)
                                }
                            }
                        }
                        val offset = month.atDay(1).dayOfWeek.value - 1
                        val cellCount = ((offset + month.lengthOfMonth() + 6) / 7) * 7
                        (0 until cellCount step 7).forEach { start ->
                            Row(Modifier.fillMaxWidth()) {
                                (start until start + 7).forEach { cell ->
                                    val number = cell - offset + 1
                                    if (number in 1..month.lengthOfMonth()) {
                                        val date = month.atDay(number)
                                        Column(Modifier.weight(1f).heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (date == selectedDate) SoftBlue else Color.Transparent)
                                            .clickable { selectedDate = date }.padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(number.toString(), color = Ink, fontWeight = if (date == selectedDate) FontWeight.Bold else FontWeight.Normal)
                                            Spacer(Modifier.height(3.dp))
                                            Box(Modifier.size(5.dp).clip(CircleShape).background(if (examples.any { it.date == date }) SchoolBlue else Color.Transparent))
                                        }
                                    } else Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        PreviewText(tr("Tap a date to see its school items. Dots mark days with something planned.", "Touchez une date pour voir ses éléments scolaires. Les points indiquent les journées avec un événement."))
                    }
                }
                item {
                    PreviewSection(selectedDate.format(dateFormat), SoftGreen) {
                        val events = examples.filter { it.date == selectedDate }
                        if (events.isEmpty()) PreviewText(tr("Nothing planned for this example day.", "Rien de prévu pour cette journée d’exemple."))
                        events.forEach { event -> PreviewSchoolRow(event, { details = event }, dateFormat) }
                    }
                }
            }
            page == "KIDS" -> {
                children.forEach { child -> item { PreviewChildCard(child) { childPage = child } } }
                item { PreviewText(tr("Tap an example child to explore teachers, transportation, absences and school papers.", "Touchez un enfant d’exemple pour découvrir les enseignants, le transport, les absences et les documents.")) }
            }
            else -> {
                item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = selectedChild == null, onClick = { selectedChild = null }, label = { Text(tr("All", "Tous")) })
                        children.forEach { child -> FilterChip(selected = selectedChild == child.name,
                            onClick = { selectedChild = child.name }, label = { Text(child.name) }) }
                    }
                }
                item {
                    PreviewSection(tr("Parent Notes", "Notes des parents"), SoftYellow) {
                        PreviewText(tr("Ask about the class trip at pickup. Pack spare mittens for Sam.", "Poser une question sur la sortie au retour. Préparer des mitaines de rechange pour Sam."))
                    }
                }
                listOf(today to tr("Today", "Aujourd’hui"), today.plusDays(1) to tr("Tomorrow", "Demain")).forEach { (date, label) ->
                    item {
                        PreviewSection("$label · ${date.format(dateFormat)}", if (date == today) SoftBlue else SoftGreen) {
                            val events = examples.filter { it.date == date && (selectedChild == null || it.child == selectedChild) }
                            if (events.isEmpty()) PreviewText(tr("Nothing on the list. Suspiciously peaceful.", "Rien sur la liste. C’est presque suspect."))
                            events.forEach { event -> PreviewSchoolRow(event, { details = event }, dateFormat) }
                        }
                    }
                }
                children.forEach { child -> item { PreviewChildCard(child) { childPage = child } } }
            }
        }
        item {
            Button(onClick = onSubscription, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Start with your family", "Commencez avec votre famille"))
            }
        }
    }
    details?.let { event ->
        AlertDialog(onDismissRequest = { details = null },
            title = { Text(event.title) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MockupArtImage(event.art, Modifier.size(60.dp))
                Text("${event.child} · ${event.date.format(dateFormat)}")
                Text(event.notes)
                Text(tr("Example item. With a subscription, add your own dates, notes and reminders.", "Élément d’exemple. Avec un abonnement, ajoutez vos dates, vos notes et vos rappels."), fontSize = 13.sp)
            } },
            confirmButton = { TextButton(onClick = { details = null }) { Text(tr("Close", "Fermer")) } })
    }
}

@Composable
private fun PreviewSection(title: String, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Text(title, color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth().background(color).padding(horizontal = 16.dp, vertical = 10.dp))
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun PreviewText(text: String) {
    Text(text, color = Ink.copy(alpha = .7f), fontSize = 14.sp)
}

@Composable
private fun PreviewSchoolRow(item: PreviewItem, onClick: () -> Unit, dateFormat: DateTimeFormatter) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        MockupArtImage(item.art, Modifier.size(40.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.title, color = Ink, fontWeight = FontWeight.SemiBold)
            Text("${item.child} · ${item.date.format(dateFormat)}", color = Ink.copy(alpha = .6f), fontSize = 12.sp)
            Text(item.notes, color = Ink.copy(alpha = .72f), fontSize = 12.sp)
        }
        Icon(Icons.Rounded.ArrowForward, null, tint = SchoolBlue, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PreviewChildCard(child: PreviewChild, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = child.color.copy(alpha = .12f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(child.color.copy(alpha = .25f)), contentAlignment = Alignment.Center) {
                Text(child.name.take(1), color = Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(child.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text(child.grade, color = Ink.copy(alpha = .65f))
            }
            Icon(Icons.Rounded.ArrowForward, tr("Open example profile", "Ouvrir le profil d’exemple"), tint = SchoolBlue)
        }
    }
}
