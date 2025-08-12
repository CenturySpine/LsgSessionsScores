package com.example.lsgscores.data

import com.example.lsgscores.data.dateTimeFormatter
import com.example.lsgscores.data.hole.Hole
import com.example.lsgscores.data.hole.HolePoint
import com.example.lsgscores.data.media.Media
import com.example.lsgscores.data.player.Player
import com.example.lsgscores.data.session.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Extension functions to convert between Supabase DTOs and domain entities
 */

// DateTime conversion helpers
private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun LocalDateTime.toSupabaseString(): String = this.format(dateTimeFormatter)
fun String.toLocalDateTime(): LocalDateTime = LocalDateTime.parse(this, dateTimeFormatter)

// Player conversions
fun PlayerDto.toDomainModel(): Player = Player(
    id = id,
    name = name,
    photoUri = photoUrl
)

fun Player.toDto(): PlayerDto = PlayerDto(
    id = id,
    name = name,
    photoUrl = photoUri
)

// Hole conversions
fun HoleDto.toDomainModel(): Hole = Hole(
    id = id,
    name = name,
    geoZone = geoZone,
    description = description,
    constraints = constraints,
    distance = distance,
    par = par,
    start = HolePoint(name = startName, photoUri = startPhotoUri),
    end = HolePoint(name = endName, photoUri = endPhotoUri)
)

fun Hole.toDto(): HoleDto = HoleDto(
    id = id,
    name = name,
    geoZone = geoZone,
    description = description,
    constraints = constraints,
    distance = distance,
    par = par,
    startName = start.name,
    startPhotoUri = start.photoUri,
    endName = end.name,
    endPhotoUri = end.photoUri
)

// Session conversions
fun SessionDto.toDomainModel(): Session = Session(
    id = id,
    name = name,
    dateTime = dateTime.toLocalDateTime(),
    endDateTime = endDateTime?.toLocalDateTime(),
    sessionType = SessionType.valueOf(sessionType),
    scoringModeId = scoringModeId,
    comment = comment,
    isOngoing = isOngoing
)

fun Session.toDto(): SessionDto = SessionDto(
    id = id,
    name = name,
    dateTime = dateTime.toSupabaseString(),
    endDateTime = endDateTime?.toSupabaseString(),
    sessionType = sessionType.name,
    scoringModeId = scoringModeId,
    comment = comment,
    isOngoing = isOngoing
)

// Team conversions
fun TeamDto.toDomainModel(): Team = Team(
    id = id,
    sessionId = sessionId,
    player1Id = player1Id,
    player2Id = player2Id
)

fun Team.toDto(): TeamDto = TeamDto(
    id = id,
    sessionId = sessionId,
    player1Id = player1Id,
    player2Id = player2Id
)

// PlayedHole conversions
fun PlayedHoleDto.toDomainModel(): PlayedHole = PlayedHole(
    id = id,
    sessionId = sessionId,
    holeId = holeId,
    gameModeId = gameModeId,
    position = position
)

fun PlayedHole.toDto(): PlayedHoleDto = PlayedHoleDto(
    id = id,
    sessionId = sessionId,
    holeId = holeId,
    gameModeId = gameModeId,
    position = position
)

// PlayedHoleScore conversions
fun PlayedHoleScoreDto.toDomainModel(): PlayedHoleScore = PlayedHoleScore(
    id = id,
    playedHoleId = playedHoleId,
    teamId = teamId,
    strokes = strokes
)

fun PlayedHoleScore.toDto(): PlayedHoleScoreDto = PlayedHoleScoreDto(
    id = id,
    playedHoleId = playedHoleId,
    teamId = teamId,
    strokes = strokes
)

// Media conversions
fun MediaDto.toDomainModel(): Media = Media(
    id = id.toLong(), // Convert from Long to Int for existing entity
    sessionId = sessionId.toLong(), // Convert from Long to Int for existing entity
    uri = uri,
    comment = comment,
    dateAdded = dateAdded.toLocalDateTime()
)

fun Media.toDto(): MediaDto = MediaDto(
    id = id.toLong(), // Convert from Int to Long for Supabase
    sessionId = sessionId.toLong(), // Convert from Int to Long for Supabase
    uri = uri,
    comment = comment,
    dateAdded = dateAdded.toSupabaseString()
)