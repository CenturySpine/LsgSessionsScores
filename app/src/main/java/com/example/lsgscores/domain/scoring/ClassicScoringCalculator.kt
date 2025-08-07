package com.example.lsgscores.domain.scoring

class ClassicScoringCalculator : ScoringCalculator {
    override fun calculateScores(strokes: Map<Long, Int>): Map<Long, Int> {
        return strokes // No transformation, returns the same mapping
    }
}



