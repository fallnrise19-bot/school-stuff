package ca.creativepixels.schoolstuff

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.creativepixels.schoolstuff.billing.SubscriptionState

@Composable
fun SubscriptionScreen(
    state: SubscriptionState,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
    onManage: () -> Unit,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, tr("Back", "Retour"), tint = Ink)
                    }
                }
                Column {
                    Text(tr("Subscription", "Abonnement"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 25.sp)
                    Text(tr("One plan for all of ParentBell.", "Un abonnement pour tout ParentBell."), color = Ink.copy(alpha = .68f))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.Notifications, null, tint = SchoolBlue, modifier = Modifier.size(36.dp))
                    Text("ParentBell", color = Ink, fontWeight = FontWeight.Black, fontSize = 28.sp)
                    Text(tr("Keep school life in one place", "La vie scolaire au même endroit"), color = Ink, fontWeight = FontWeight.SemiBold)
                    if (state.price != null) {
                        Text(tr("${state.price} / month", "${state.price} / mois"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    } else {
                        Text(tr("Monthly plan", "Abonnement mensuel"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 23.sp)
                        Text(tr("Refresh subscription status to load your local price from Google Play.", "Actualisez l’état de l’abonnement pour charger le prix dans votre devise depuis Google Play."), color = Ink.copy(alpha = .68f), fontSize = 13.sp)
                    }
                    Text(state.summary, color = Ink, fontWeight = FontWeight.SemiBold)
                    if (state.active) {
                        Text(
                            if (state.autoRenewing) tr("Your subscription renews through Google Play.", "Votre abonnement se renouvelle par Google Play.")
                            else tr("Renewal is off. Access continues for your remaining paid period.", "Le renouvellement est désactivé. L’accès continue jusqu’à la fin de la période payée."),
                            color = Ink.copy(alpha = .7f), fontSize = 13.sp
                        )
                        Button(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                            Text(tr("Manage subscription", "Gérer l’abonnement"))
                        }
                    } else {
                        Button(onClick = onSubscribe, enabled = state.canSubscribe, modifier = Modifier.fillMaxWidth()) {
                            Text(when {
                                state.purchasing -> tr("Opening Google Play…", "Ouverture de Google Play…")
                                state.pending -> tr("Payment pending", "Paiement en attente")
                                else -> tr("Subscribe monthly", "S’abonner au mois")
                            })
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text(tr("Everything included", "Tout est inclus"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                listOf(
                    tr("All your children’s profiles, teachers and bus details", "Les profils de vos enfants, les enseignants et le transport scolaire"),
                    tr("School dates, homework, forms and items to bring", "Les dates scolaires, les devoirs, les formulaires et les objets à apporter"),
                    tr("Reminders, parent notes and absence tracking", "Les rappels, les notes des parents et le suivi des absences"),
                    tr("Papers, memories and Google Calendar connection", "Les documents, les souvenirs et la connexion à Google Agenda")
                ).forEach { benefit ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = SchoolBlue, modifier = Modifier.size(21.dp))
                        Text(benefit, color = Ink, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.message.isNotBlank()) {
                    Text(state.message, color = Ink, fontSize = 14.sp)
                }
                OutlinedButton(onClick = onRestore, enabled = !state.checking && !state.purchasing, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Restore purchases", "Restaurer les achats"))
                }
                if (!state.active) {
                    OutlinedButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                        Text(tr("Manage or cancel in Google Play", "Gérer ou annuler dans Google Play"))
                    }
                }
                TextButton(onClick = onRefresh, enabled = !state.checking && !state.purchasing, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Refresh subscription status", "Actualiser l’état de l’abonnement"))
                }
                HorizontalDivider()
                Text(tr(
                    "Billed monthly through your Google Play account. Renews automatically unless cancelled. Manage or cancel in Google Play; cancellation stops the next renewal, and access continues until the end of your paid period. No free trial is included in this plan.",
                    "Facturation mensuelle par votre compte Google Play. Renouvellement automatique sauf annulation. Gérez ou annulez dans Google Play; l’annulation arrête le prochain renouvellement et l’accès continue jusqu’à la fin de la période payée. Aucun essai gratuit n’est inclus."
                ), color = Ink.copy(alpha = .68f), fontSize = 13.sp)
                Text(tr(
                    "ParentBell stores school information on this device. A subscription does not add cloud backup or family sharing. Cancelling never deletes your saved information. Google Play checks your subscription when you return to the app; verified access is available offline for up to 24 hours.",
                    "ParentBell enregistre les renseignements scolaires sur cet appareil. L’abonnement n’ajoute ni sauvegarde infonuagique ni partage familial. L’annulation ne supprime jamais vos renseignements enregistrés. Google Play vérifie l’abonnement au retour dans l’appli; l’accès vérifié est disponible hors ligne pendant un maximum de 24 heures."
                ), color = Ink.copy(alpha = .68f), fontSize = 13.sp)
            }
        }
    }
}
