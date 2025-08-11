package com.example.lsgscores.domain.scoring


class TwoOneScoringCalculator : ScoringCalculator {
    override fun calculateScores(strokes: Map<Long, Int>): Map<Long, Int> {
        if (strokes.isEmpty()) return emptyMap()

        // Sort players by increasing score
        val sortedScores = strokes.entries.sortedBy { it.value }
        val minScore = sortedScores.first().value
        val leaders = sortedScores.filter { it.value == minScore }.map { it.key }

        return when (leaders.size) {
            1 -> {
                // Single leader: 2 points, check for a unique second place
                val nextScores = sortedScores.filter { it.value > minScore }
                val secondScore = nextScores.firstOrNull()?.value
                val seconds = nextScores.filter { it.value == secondScore }.map { it.key }
                val secondPoint =
                    if (seconds.size == 1) mapOf(seconds[0] to 1) else emptyMap()
                strokes.mapValues { (player, _) ->
                    when (player) {
                        leaders[0] -> 2
                        in secondPoint -> 1
                        else -> 0
                    }
                }
            }
            2 -> {
                // Two leaders: each gets 1 point, check for a unique second place
                val nextScores = sortedScores.filter { it.value > minScore }
                val secondScore = nextScores.firstOrNull()?.value
                val seconds = nextScores.filter { it.value == secondScore }.map { it.key }
                val secondPoint =
                    if (seconds.size == 1) mapOf(seconds[0] to 1) else emptyMap()
                strokes.mapValues { (player, _) ->
                    when (player) {
                        in leaders -> 1
                        in secondPoint -> 1
                        else -> 0
                    }
                }
            }
            else -> {
                // Three or more tied for first: nobody scores
                strokes.mapValues { 0 }
            }
        }
    }
}
