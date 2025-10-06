package fr.centuryspine.lsgscores.data.player

import kotlinx.coroutines.flow.Flow

interface PlayerDao {
    fun getPlayersByCityId(cityId: Long): Flow<List<Player>>

    suspend fun getPlayersByCityIdList(cityId: Long): List<Player>

    suspend fun getAll(): List<Player>

    suspend fun getById(id: Long): Player?

    fun insert(player: Player): Long

    fun update(player: Player)

    fun delete(player: Player)
}
