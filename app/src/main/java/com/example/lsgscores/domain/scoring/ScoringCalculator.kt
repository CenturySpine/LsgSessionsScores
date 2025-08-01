package com.example.lsgscores.domain.scoring

interface ScoringCalculator {
    fun calculateScores(strokes: Map<String, Int>): Map<String, Int>
}

