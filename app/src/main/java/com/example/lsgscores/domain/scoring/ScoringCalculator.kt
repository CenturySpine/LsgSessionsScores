package com.example.lsgscores.domain.scoring

interface ScoringCalculator {
    fun calculateScores(strokes: Map<Long, Int>): Map<Long, Int>
}

