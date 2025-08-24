package com.example.lsgscores.viewmodel

import com.example.lsgscores.data.session.SessionType
import java.time.LocalDateTime

data class SessionDraft(
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val sessionType: SessionType = SessionType.INDIVIDUAL,
    val scoringModeId: Int = 1,
    val gameZoneId: Long = 1L, // New field for GameZone
    val comment: String? = null
)