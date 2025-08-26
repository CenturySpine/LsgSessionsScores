package fr.centuryspine.lsgscores.domain.scoring

object ScoringCalculatorFactory {
    fun getCalculatorById(scoringModeId: Int): ScoringCalculator =
        when (scoringModeId) {
            1 -> ClassicScoringCalculator()
            2 -> SinglePointScoringCalculator()
            3 -> FirstThreeElseTwoScoringCalculator()
            4 -> TwoOneScoringCalculator()
            else -> throw IllegalArgumentException("Unknown scoring mode ID: $scoringModeId")
        }
}