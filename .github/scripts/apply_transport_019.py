from pathlib import Path

path = Path('app/src/main/java/ca/creativepixels/schoolstuff/SchoolStuffApp.kt')
text = path.read_text()


def require(value: str, label: str) -> None:
    if value not in text:
        raise SystemExit(f'Missing patch anchor: {label}')

# Icon import.
icon_anchor = 'import androidx.compose.material.icons.rounded.DirectionsRun\n'
if 'import androidx.compose.material.icons.rounded.DirectionsBus\n' not in text:
    require(icon_anchor, 'DirectionsRun import')
    text = text.replace(
        icon_anchor,
        'import androidx.compose.material.icons.rounded.DirectionsBus\n' + icon_anchor,
        1,
    )

# Add a Transportation card after Teacher & School.
teacher_block_end = '''        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Section("Homework & Forms", SchoolYellow) {
'''
require(teacher_block_end, 'Homework section start')
transport_section = '''        item {
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
'''
text = text.replace(teacher_block_end, transport_section + teacher_block_end, 1)

# Add edit state.
notes_state = '    var notes by remember { mutableStateOf(child.specialNotes) }\n'
require(notes_state, 'child notes state')
transport_state = '''    var transportationType by remember { mutableStateOf(child.transportationType) }
    var busNumber by remember { mutableStateOf(child.busNumber) }
    var transportDriverName by remember { mutableStateOf(child.transportDriverName) }
    var transportLicensePlate by remember { mutableStateOf(child.transportLicensePlate) }
    var pickupInfo by remember { mutableStateOf(child.pickupInfo) }
    var dropOffInfo by remember { mutableStateOf(child.dropOffInfo) }
    var transportationNotes by remember { mutableStateOf(child.transportationNotes) }
    val transportationTypes = listOf("School Bus", "Parent / Caregiver", "Walk", "Other")
'''
text = text.replace(notes_state, notes_state + transport_state, 1)

# Add transportation editor fields before Special notes.
special_notes_field = '                item { OutlinedTextField(notes, { notes = it }, label = { Text("Special notes") }) }\n'
require(special_notes_field, 'Special notes field')
transport_editor = '''                item {
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
'''
text = text.replace(special_notes_field, transport_editor + special_notes_field, 1)

# Save the transport fields with the child profile.
old_copy = 'onSave(child.copy(grade = grade, teacherName = teacher, teacherEmail = email, classroomPhone = phone, room = room, schoolName = school, schoolPhone = schoolPhone, specialNotes = notes))'
require(old_copy, 'child copy save')
new_copy = '''onSave(child.copy(
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
                ))'''
text = text.replace(old_copy, new_copy, 1)

path.write_text(text)
print('Transportation fields and UI patched.')
