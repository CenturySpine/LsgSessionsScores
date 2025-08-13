package com.example.lsgscores.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase data models that match the database schema
 * These will be used for API communication and then converted to domain entities
 */

@Serializable
data class PlayerDto(
    val id: Long = 0,
    val name: String,
    @SerialName("photo_url")
    val photoUrl: String? = null
)

@Serializable
data class HoleDto(
    val id: Long = 0,
    val name: String,
    @SerialName("geo_zone")
    val geoZone: String,
    val description: String? = null,
    val constraints: String? = null,
    val distance: Int? = null,
    val par: Int,
    @SerialName("start_name")
    val startName: String,
    @SerialName("start_photo_uri")
    val startPhotoUri: String? = null,
    @SerialName("end_name")
    val endName: String,
    @SerialName("end_photo_uri")
    val endPhotoUri: String? = null
)

@Serializable
data class SessionDto(
    val id: Long = 0,
    val name: String,
    @SerialName("date_time")
    val dateTime: String, // ISO string format for Supabase
    @SerialName("end_date_time")
    val endDateTime: String? = null,
    @SerialName("session_type")
    val sessionType: String,
    @SerialName("scoring_mode_id")
    val scoringModeId: Int,
    val comment: String? = null,
    @SerialName("is_ongoing")
    val isOngoing: Boolean = false
)

@Serializable
data class TeamDto(
    val id: Long = 0,
    @SerialName("session_id")
    val sessionId: Long,
    @SerialName("player1_id")
    val player1Id: Long,
    @SerialName("player2_id")
    val player2Id: Long? = null
)

@Serializable
data class PlayedHoleDto(
    val id: Long = 0,
    @SerialName("session_id")
    val sessionId: Long,
    @SerialName("hole_id")
    val holeId: Long,
    @SerialName("game_mode_id")
    val gameModeId: Int,
    val position: Int
)

@Serializable
data class PlayedHoleScoreDto(
    val id: Long = 0,
    @SerialName("played_hole_id")
    val playedHoleId: Long,
    @SerialName("team_id")
    val teamId: Long,
    val strokes: Int
)

@Serializable
data class MediaDto(
    val id: Long = 0,
    @SerialName("session_id")
    val sessionId: Long,
    val uri: String,
    val comment: String? = null,
    @SerialName("date_added")
    val dateAdded: String // ISO string format
)