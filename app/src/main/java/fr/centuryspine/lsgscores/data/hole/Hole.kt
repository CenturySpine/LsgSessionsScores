package fr.centuryspine.lsgscores.data.hole

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import fr.centuryspine.lsgscores.data.gamezone.GameZone
import kotlinx.serialization.Serializable

@Entity(
    tableName = "holes",
        indices = [Index(value = ["gameZoneId"])]
)
@Serializable
data class Hole(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gameZoneId: Long,
    val description: String?,
    val distance: Int?,
    val par: Int,
    val startPhotoUri: String?,
    val endPhotoUri: String?
)
