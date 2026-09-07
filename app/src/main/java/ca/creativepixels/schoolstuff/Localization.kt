package ca.creativepixels.schoolstuff

import android.content.Context
import android.content.res.Configuration
import ca.creativepixels.schoolstuff.data.Category
import ca.creativepixels.schoolstuff.data.DocumentType
import ca.creativepixels.schoolstuff.data.Reminder
import ca.creativepixels.schoolstuff.data.Repeat
import ca.creativepixels.schoolstuff.data.TransportationMode
import java.util.Locale

object AppLanguage {
    const val ENGLISH = "en"
    const val FRENCH = "fr"

    fun wrap(context: Context): Context {
        val language = context.getSharedPreferences("school_stuff_store", Context.MODE_PRIVATE)
            .getString("app_language", ENGLISH)
            .orEmpty()
            .takeIf { it == ENGLISH || it == FRENCH }
            ?: ENGLISH
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
}

fun tr(english: String, french: String): String =
    if (Locale.getDefault().language == AppLanguage.FRENCH) french else english

fun trFor(language: String, english: String, french: String): String =
    if (language == AppLanguage.FRENCH) french else english

fun categoryLabel(value: String): String = when (value) {
    Category.FOOD_DAY -> tr("Food Day", "Journée repas")
    Category.FORM_DUE -> tr("Form Due", "Formulaire à remettre")
    Category.SPIRIT_DAY -> tr("Spirit Day", "Journée thématique")
    Category.BRING_ITEM -> tr("Bring Item", "Objet à apporter")
    Category.LIBRARY -> tr("Library", "Bibliothèque")
    Category.GYM -> tr("Gym", "Éducation physique")
    Category.HOMEWORK -> tr("Homework", "Devoirs")
    Category.SCHOOL_EVENT -> tr("School Event", "Événement scolaire")
    else -> value
}

fun repeatLabel(value: String): String = when (value) {
    Repeat.ONE_TIME -> tr("One-time", "Une fois")
    Repeat.WEEKLY -> tr("Weekly", "Chaque semaine")
    Repeat.MONTHLY -> tr("Monthly", "Chaque mois")
    else -> value
}

fun reminderLabel(value: String): String = when (value) {
    Reminder.NONE -> tr("None", "Aucun")
    Reminder.NIGHT_BEFORE -> tr("Night Before", "La veille")
    Reminder.MORNING_OF -> tr("Morning Of", "Le matin même")
    Reminder.BOTH -> tr("Both", "Les deux")
    else -> value
}

fun documentTypeLabel(value: String): String = when (value) {
    DocumentType.FORM -> tr("School form", "Formulaire scolaire")
    DocumentType.REPORT_CARD -> tr("Report card", "Bulletin scolaire")
    DocumentType.MEDICAL -> tr("Medical / ICP paper", "Document médical / PCI")
    DocumentType.ARTWORK -> tr("Artwork", "Œuvre d’art")
    DocumentType.PHOTO -> tr("Photo", "Photo")
    else -> tr("School file", "Fichier scolaire")
}

fun transportationModeLabel(value: String): String = when (value) {
    TransportationMode.BUS -> tr("Bus", "Autobus")
    TransportationMode.PRIVATE -> tr("Private transportation", "Transport privé")
    else -> value
}
