package fr.centuryspine.lsgscores.data.hole

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import fr.centuryspine.lsgscores.data.gamezone.GameZone
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Entity(
    tableName = "holes",
        indices = [Index(value = ["gameZoneId"])]
)
@Serializable
data class Hole(
    @PrimaryKey(autoGenerate = true)
    @SerialName("id")
    val id: Long = 0,
    @SerialName("name")
    val name: String,
    @SerialName("gamezoneid")
    val gameZoneId: Long,
    @SerialName("description")
    val description: String?,
    @SerialName("distance")
    val distance: Int?,
    @SerialName("par")
    val par: Int,
    @SerialName("startphotouri")
    val startPhotoUri: String?,
    @SerialName("endphotouri")
    val endPhotoUri: String?
)
