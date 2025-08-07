package com.example.lsgscores.domain.scoring

class SinglePointScoringCalculator : ScoringCalculator {
    override fun calculateScores(strokes: Map<Long, Int>): Map<Long, Int> {
        if (strokes.isEmpty()) return emptyMap()
        val minScore = strokes.values.minOrNull()!!
        val leaders = strokes.filterValues { it == minScore }
        return if (leaders.size == 1) {
            strokes.mapValues { (player, _) -> if (leaders.containsKey(player)) 1 else 0 }
        } else {
            strokes.mapValues { 0 }
        }
    }
}



