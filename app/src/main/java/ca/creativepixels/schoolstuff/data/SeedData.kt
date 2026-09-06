package ca.creativepixels.schoolstuff.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object SeedData {
    fun children(): List<ChildProfile> = listOf(
        ChildProfile(
            id = "oliver",
            name = "Oliver",
            grade = "3rd Grade",
            colorKey = "blue",
            teacherName = "Mrs. Bennett",
            teacherEmail = "mbennett@school.edu",
            classroomPhone = "555-0142",
            room = "12",
            schoolName = "Maple Grove Public School",
            schoolPhone = "555-0100",
            attendancePhone = "555-0101",
            schoolAddress = "123 Learning Lane",
            specialNotes = "Library books due Tuesdays"
        ),
        ChildProfile(
            id = "logan",
            name = "Logan",
            grade = "5th Grade",
            colorKey = "green",
            schoolName = "Maple Grove Public School",
            schoolPhone = "555-0100",
            attendancePhone = "555-0101",
            schoolAddress = "123 Learning Lane",
            specialNotes = "Gym shoes stay at school"
        ),
        ChildProfile(
            id = "chloe",
            name = "Chloe",
            grade = "1st Grade",
            colorKey = "pink",
            schoolName = "Maple Grove Public School",
            schoolPhone = "555-0100",
            attendancePhone = "555-0101",
            schoolAddress = "123 Learning Lane"
        )
    )

    fun items(today: LocalDate = LocalDate.now()): List<SchoolItem> {
        fun next(day: DayOfWeek): LocalDate = today.with(TemporalAdjusters.nextOrSame(day))
        return listOf(
            SchoolItem(childId = "oliver", title = "Library Day", category = Category.LIBRARY, dateIso = next(DayOfWeek.TUESDAY).toString(), repeat = Repeat.WEEKLY),
            SchoolItem(childId = "oliver", title = "Pizza Day", category = Category.FOOD_DAY, dateIso = next(DayOfWeek.WEDNESDAY).toString(), repeat = Repeat.WEEKLY),
            SchoolItem(childId = "oliver", title = "Wear Blue for Spirit Day", category = Category.SPIRIT_DAY, dateIso = next(DayOfWeek.FRIDAY).toString(), repeat = Repeat.WEEKLY),
            SchoolItem(childId = "logan", title = "Homework Folder", category = Category.HOMEWORK, dateIso = next(DayOfWeek.MONDAY).toString(), repeat = Repeat.WEEKLY),
            SchoolItem(childId = "logan", title = "Gym Day", category = Category.GYM, dateIso = next(DayOfWeek.TUESDAY).toString(), repeat = Repeat.WEEKLY),
            SchoolItem(childId = "logan", title = "Subs Day", category = Category.FOOD_DAY, dateIso = next(DayOfWeek.THURSDAY).toString(), repeat = Repeat.WEEKLY),
            SchoolItem(childId = "chloe", title = "Library Day", category = Category.LIBRARY, dateIso = next(DayOfWeek.MONDAY).toString(), repeat = Repeat.WEEKLY),
            SchoolItem(childId = "chloe", title = "Pizza Day", category = Category.FOOD_DAY, dateIso = next(DayOfWeek.WEDNESDAY).toString(), repeat = Repeat.WEEKLY),
            SchoolItem(childId = "chloe", title = "Gym Day", category = Category.GYM, dateIso = next(DayOfWeek.THURSDAY).toString(), repeat = Repeat.WEEKLY),
            SchoolItem(childId = "", title = "Picture Day", category = Category.SCHOOL_EVENT, dateIso = next(DayOfWeek.FRIDAY).toString(), reminder = Reminder.BOTH, notes = "Wear something picture-ready")
        )
    }
}
