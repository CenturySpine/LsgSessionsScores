package fr.centuryspine.lsgscores.domain.scoring

interface ScoringCalculator {
    fun calculateScores(strokes: Map<Long, Int>): Map<Long, Int>
}

