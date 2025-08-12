package com.example.lsgscores.domain.scoring

class ClassicScoringCalculator : ScoringCalculator {
    override fun calculateScores(strokes: Map<String, Int>): Map<String, Int> {
        return strokes // No transformation, returns the same mapping
    }
}



