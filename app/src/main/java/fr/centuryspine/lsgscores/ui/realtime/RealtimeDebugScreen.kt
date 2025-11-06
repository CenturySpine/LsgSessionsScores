package fr.centuryspine.lsgscores.ui.realtime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.animation.AnimatedVisibility
import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
import io.github.jan.supabase.postgrest.query.Order

// Model de session existant dans le projet
import fr.centuryspine.lsgscores.data.session.Session
import fr.centuryspine.lsgscores.data.session.PlayedHole
import fr.centuryspine.lsgscores.data.session.PlayedHoleScore
import fr.centuryspine.lsgscores.data.hole.Hole
import fr.centuryspine.lsgscores.data.player.Player
import fr.centuryspine.lsgscores.data.session.Team
import fr.centuryspine.lsgscores.data.gamezone.GameZone

import io.github.jan.supabase.annotations.SupabaseExperimental
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@SupabaseExperimental
@Composable
fun RealtimeDebugScreen(
    supabase: SupabaseClient
) {
    // Flow de toutes les sessions en temps réel (clé primaire pour le cache)
    val flow: Flow<List<Session>> = remember(supabase) {
        supabase.from("sessions").selectAsFlow(Session::id)
    }


    // Realtime flow des trous joués (played_holes)
    val playedHolesFlow: Flow<List<PlayedHole>> = remember(supabase) {
        supabase.from("played_holes").selectAsFlow(PlayedHole::id)
    }

    
    // Realtime flow des scores par trou joué (played_hole_scores)
    val playedHoleScoresFlow: Flow<List<PlayedHoleScore>> = remember(supabase) {
        supabase.from("played_hole_scores").selectAsFlow(PlayedHoleScore::id)
    }


    // Liste d'affichage des logs sur l'écran
    val logs = remember { mutableStateListOf<String>() }
    // Cache local: id du trou -> nom du trou
    val holes = remember { mutableStateMapOf<Long, Hole>() }
    // Cache local: id de la zone -> zone
    val gameZones = remember { mutableStateMapOf<Long, GameZone>() }
    // Cache local: id des teams -> team
    val sessionTeams = remember { mutableStateMapOf<Long, Team>() }
    // Cache local: id joueur -> Player
    val teamsPlayers = remember { mutableStateMapOf<Long, Player>() }
    // Cache local: id du played_hole -> PlayedHole (pour retrouver le trou depuis un score)
    val playedHolesById = remember { mutableStateMapOf<Long, PlayedHole>() }
    // Panneau d'explications (collapsed par défaut)
    var helpExpanded by rememberSaveable { mutableStateOf(false) }
    // Session active pour laquelle le cache de trous est chargé
    var activeSessionId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Utilitaire: horodatage HH:mm:ss
    fun now() = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    fun appendLog(line: String) {
        logs.add(line)
        Log.d("RealtimeDebug", line)
    }

    fun teamDisplayName(teamId: Long): String {
        val t = sessionTeams[teamId] ?: return "?$teamId"
        val n1 = teamsPlayers[t.player1Id]?.name ?: "?${t.player1Id}"
        val n2 = t.player2Id?.let { pid -> teamsPlayers[pid]?.name ?: "?${pid}" }
        return if (n2 != null) "[${n1}, ${n2}]" else "[${n1}]"
    }
    
    // Factorisé: nettoyage des caches pour la session active (avec option de réinitialisation de l'ID de session)
    fun clearCachesForActiveSession(reason: String, resetSessionId: Boolean = true) {
        val sid = activeSessionId
        holes.clear()
        sessionTeams.clear()
        teamsPlayers.clear()
        gameZones.clear()
        playedHolesById.clear()
        appendLog("[${now()}] [Cache] Caches vidés (session ${sid} ${reason})")
        if (resetSessionId) {
            activeSessionId = null
        }
    }

    // Diff simple entre les émissions successives pour détecter:
    // 1) suppressions de sessions
    // 2) passage isOngoing de true -> false
    LaunchedEffect(Unit) {
        var previousById: Map<Long, Session>? = null
        flow.collect { currentList ->
            // Filtrer les sessions pour ne garder que celles du user authentifié
            val currentUserId = supabase.auth.currentSessionOrNull()?.user?.id
            val filtered = if (currentUserId != null) {
                currentList.filter { it.userId == currentUserId }
            } else {
                emptyList()
            }
            val currentById = filtered.associateBy { it.id }

            val prev = previousById
            if (prev != null) {

                // 1) Créations: ids présents maintenant mais absents avant
                val insertedIds = currentById.keys.minus(prev.keys)
                if (insertedIds.isNotEmpty()) {
                    appendLog("[${now()}] [Realtime] Nouvelle session créée: id=${insertedIds.joinToString()}")
                    // Charge le cache statique des trous pour la dernière session créée
                    val newId = insertedIds.maxOrNull()
                    val newSession = newId?.let { currentById[it] }
                    if (newSession != null) {
                        activeSessionId = newSession.id
                        try {
                            // Nettoyage centralisé des caches avant chargement d'une nouvelle session
                            clearCachesForActiveSession("réinitialisée pour nouvelle session", resetSessionId = false)

                            // Trous de la zone de jeu
                            val holeList = supabase.postgrest["holes"].select {
                                filter { eq("gamezoneid", newSession.gameZoneId) }
                                order("name", Order.ASCENDING)
                            }.decodeList<Hole>()
                            holeList.forEach { h -> holes[h.id] = h }

                            // Équipes de la session
                            val sessionTeamsList = supabase.postgrest["teams"].select {
                                filter { eq("sessionid", activeSessionId!!) }
                                order("id", Order.ASCENDING)
                            }.decodeList<Team>()
                            sessionTeamsList.forEach { t -> sessionTeams[t.id] = t }

                            // Joueurs appartenant aux équipes de la session
                            val teamPlayerIds = sessionTeamsList
                                .flatMap { t -> listOfNotNull(t.player1Id, t.player2Id) }
                                .distinct()

                            val playersList = if (teamPlayerIds.isNotEmpty()) {
                                supabase.postgrest["players"].select {
                                    order("name", Order.ASCENDING)
                                }.decodeList<Player>().filter { it.id in teamPlayerIds }
                            } else {
                                emptyList()
                            }
                            playersList.forEach { p -> teamsPlayers[p.id] = p }

                            appendLog("[${now()}] [Cache] ${sessionTeamsList.size} equipes chargées pour la session ${newSession.id}")

                            // Affichage des joueurs par équipe au format [nom_joueur1, nom_joueur2]
                            val teamPairs = sessionTeamsList.joinToString(separator = " | ") { t ->
                                val n1 = playersList.firstOrNull { it.id == t.player1Id }?.name ?: "?${t.player1Id}"
                                val n2 = t.player2Id?.let { pid -> playersList.firstOrNull { it.id == pid }?.name ?: "?${pid}" } ?: "—"
                                "[${n1}, ${n2}]"
                            }
                            if (teamPairs.isNotBlank()) {
                                appendLog("[${now()}] [Cache] Équipes/joueurs: $teamPairs")
                            }
                            appendLog("[${now()}] [Cache] ${playersList.size} joueurs chargés pour la session ${newSession.id}")

                            // Zone de jeu: récupérer le nom et l'afficher
                            val zone = supabase.postgrest["game_zones"].select {
                                filter { eq("id", newSession.gameZoneId) }
                            }.decodeList<GameZone>().firstOrNull()
                            zone?.let { z -> gameZones[z.id] = z }
                            val zoneName = zone?.name ?: "(inconnue)"
                            appendLog("[${now()}] [Cache] ${holeList.size} trous chargés pour la session ${newSession.id} (gameZoneId=${newSession.gameZoneId}, zone='${zoneName}')")
                        } catch (t: Throwable) {
                            appendLog("[${now()}] [Erreur] Chargement des trous échoué: ${t.message}")
                        }
                    }
                }

                // 2) Suppressions: ids présents avant mais plus maintenant
                val deletedIds = prev.keys.minus(currentById.keys)
                if (deletedIds.isNotEmpty()) {
                    appendLog("[${now()}] [Realtime] Session supprimée: id=${deletedIds.joinToString()}")
                    if (activeSessionId != null && deletedIds.contains(activeSessionId)) {
                        clearCachesForActiveSession("supprimée")
                    }
                }

                // 3) isOngoing: true -> false
                val endedIds = prev.keys.intersect(currentById.keys)
                    .mapNotNull { id ->
                        val before = prev[id]
                        val after = currentById[id]
                        if (before != null && after != null && before.isOngoing && !after.isOngoing) id else null
                    }
                if (endedIds.isNotEmpty()) {
                    appendLog("[${now()}] [Realtime] Session terminée (isOngoing=false): id=${endedIds.joinToString()}")
                    if (activeSessionId != null && endedIds.contains(activeSessionId)) {
                        clearCachesForActiveSession("terminée")
                    }
                }
            } else {
                // Première émission: on ne log rien pour éviter un bruit initial
                appendLog("[${now()}] Abonnement Realtime actif — en attente d'événements…")
            }

            previousById = currentById
        }
    }

    // Diff pour détecter INSERT/DELETE dans 'played_holes' et logguer le nom du trou
    LaunchedEffect(Unit) {
        var previousById: Map<Long, PlayedHole>? = null
        playedHolesFlow.collect { currentList ->
            val currentById = currentList.associateBy { it.id }
            val prev = previousById

            if (prev != null) {
                val insertedIds = currentById.keys.minus(prev.keys)
                insertedIds.forEach { id ->
                    val ph = currentById[id]
                    if (ph != null) {
                        val holeName =  holes[ph.holeId]?.name ?: "Trou ${'$'}{ph.holeId}"
                        appendLog("[${now()}] [Realtime] played_holes INSERT — nom du trou='${holeName}'")
                    }
                }

                val deletedIds = prev.keys.minus(currentById.keys)
                deletedIds.forEach { id ->
                    val before = prev[id]
                    val holeName = before?.let { holes[it.holeId]?.name ?: "Trou ${'$'}{it.holeId}" } ?: "(inconnu)"
                    appendLog("[${now()}] [Realtime] played_holes DELETE — nom du trou='${holeName}'")
                }
            } else {
                logs.add("[${now()}] Abonnement Realtime 'played_holes' actif — en attente d'événements…")
            }

            // Garder le cache des played_holes en phase avec la dernière émission
            playedHolesById.clear()
            currentList.forEach { ph -> playedHolesById[ph.id] = ph }

            previousById = currentById
        }
    }

    // Collecte et traçage des mises à jour de scores d'équipe sur les trous joués
    LaunchedEffect(Unit) {
        var previousScoresById: Map<Long, PlayedHoleScore>? = null
        playedHoleScoresFlow.collect { currentList ->
            val currentById = currentList.associateBy { it.id }
            val prev = previousScoresById

            if (prev != null) {
                // INSERTS
                val insertedIds = currentById.keys.minus(prev.keys)
                insertedIds.forEach { id ->
                    val s = currentById[id] ?: return@forEach
                    val holeName = playedHolesById[s.playedHoleId]?.let { ph ->
                        holes[ph.holeId]?.name ?: "Trou ${'$'}{ph.holeId}"
                    } ?: "(trou inconnu)"
                    val teamName = teamDisplayName(s.teamId)
                    appendLog("[${now()}] [Realtime] played_hole_scores INSERT — équipe=${teamName}, trou='${holeName}', coups=${s.strokes}")
                }

                // UPDATES (même id, champs modifiés)
                val commonIds = prev.keys.intersect(currentById.keys)
                commonIds.forEach { id ->
                    val before = prev[id]
                    val after = currentById[id]
                    if (before != null && after != null) {
                        val teamChanged = before.teamId != after.teamId
                        val strokesChanged = before.strokes != after.strokes
                        if (teamChanged || strokesChanged) {
                            val holeName = playedHolesById[after.playedHoleId]?.let { ph ->
                                holes[ph.holeId]?.name ?: "Trou ${'$'}{ph.holeId}"
                            } ?: "(trou inconnu)"
                            val teamName = teamDisplayName(after.teamId)
                            val detail = when {
                                teamChanged && strokesChanged -> "équipe & coups mis à jour"
                                teamChanged -> "équipe mise à jour"
                                else -> "coups mis à jour"
                            }
                            appendLog("[${now()}] [Realtime] played_hole_scores UPDATE — ${detail} — équipe=${teamName}, trou='${holeName}', coups=${after.strokes}")
                        }
                    }
                }

                // DELETES (optionnel, utile pour debug)
                val deletedIds = prev.keys.minus(currentById.keys)
                if (deletedIds.isNotEmpty()) {
                    deletedIds.forEach { id ->
                        val before = prev[id]
                        if (before != null) {
                            val holeName = playedHolesById[before.playedHoleId]?.let { ph ->
                                holes[ph.holeId]?.name ?: "Trou ${'$'}{ph.holeId}"
                            } ?: "(trou inconnu)"
                            val teamName = teamDisplayName(before.teamId)
                            appendLog("[${now()}] [Realtime] played_hole_scores DELETE — équipe=${teamName}, trou='${holeName}'")
                        }
                    }
                }
            } else {
                appendLog("[${now()}] Abonnement Realtime 'played_hole_scores' actif — en attente d'événements…")
            }

            previousScoresById = currentById
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Realtime Debug — Sessions",
            style = MaterialTheme.typography.headlineSmall
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { helpExpanded = !helpExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val indicator = if (helpExpanded) "▼" else "▶"
                Text(
                    text = "$indicator Explications (toucher pour ${if (helpExpanded) "replier" else "déplier"})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            AnimatedVisibility(visible = helpExpanded) {
                Text(
                    text = "Cette page affiche des logs en direct lors des événements suivants:\n" +
                            "[sessions]\n" +
                            "• Création d'une session (INSERT)\n" +
                            "• Suppression d'une session\n" +
                            "• Passage de isOngoing à false\n\n" +
                            "[played_holes]\n" +
                            "• INSERT — affiche le nom du trou\n" +
                            "• DELETE — affiche le nom du trou\n\n" +
                            "Notes:\n" +
                            "• Les explications sont repliables (panneau par défaut replié).\n" +
                            "• Cet écran (aide + logs) est totalement scrollable.\n" +
                            "• Les mêmes lignes sont envoyées dans Logcat (tag RealtimeDebug).\n" +
                            "• Noms des trous: chargés une fois à la création de la session à partir de sa game zone, puis mis en cache local.\n" +
                            "  Le cache est vidé lorsque la session est supprimée ou terminée.\n\n" +
                            "Étapes de test:\n" +
                            "1) Ouvre cette page (l'abonnement démarre automatiquement).\n" +
                            "2) Crée une nouvelle session → 'Nouvelle session créée' + 'Cache X trous chargés'.\n" +
                            "3) Ajoute/Supprime un trou joué → 'played_holes INSERT/DELETE — nom du trou=…'.\n" +
                            "4) Mets fin/supprime la session → 'Session terminée/supprimée' + 'Cache trous vidé'.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = "Logs Realtime:",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                logs.asReversed().forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
