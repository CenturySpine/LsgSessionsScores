package fr.centuryspine.lsgscores.data.session

import kotlinx.coroutines.flow.Flow

interface PlayedHoleScoreDao {
    suspend fun insert(score: PlayedHoleScore): Long

    // Upsert by (playedholeid, teamid): create if not exists, otherwise update; last write wins
    suspend fun upsert(score: PlayedHoleScore): Long

    fun getScoresForPlayedHole(playedHoleId: Long): Flow<List<PlayedHoleScore>>

    suspend fun getAll(): List<PlayedHoleScore>

    suspend fun deleteScoresForPlayedHoles(playedHoleIds: List<Long>)
}