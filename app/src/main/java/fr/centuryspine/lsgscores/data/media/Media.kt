package fr.centuryspine.lsgscores.data.media

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "media")
data class Media(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sessionId: Int,                  // Foreign key vers Session
    val uri: String,                     // URI du média (obligatoire)
    val comment: String? = null,         // Optionnel, commentaire sur ce média
    val dateAdded: LocalDateTime = LocalDateTime.now() // Date d'ajout (optionnel, utile pour tri/affichage)
)