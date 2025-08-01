package com.example.lsgscores.domain.scoring

class FirstThreeElseTwoScoringCalculator : ScoringCalculator {
    override fun calculateScores(strokes: Map<String, Int>): Map<String, Int> {
        if (strokes.isEmpty()) return emptyMap()
        val minScore = strokes.values.minOrNull()!!
        val leaders = strokes.filterValues { it == minScore }
        return when (leaders.size) {
            1 -> strokes.mapValues { (player, _) -> if (leaders.containsKey(player)) 3 else 0 }
            else -> strokes.mapValues { (player, _) -> if (leaders.containsKey(player)) 2 else 0 }
        }
    }
}