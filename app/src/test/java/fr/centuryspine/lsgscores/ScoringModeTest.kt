package fr.centuryspine.lsgscores

import fr.centuryspine.lsgscores.domain.scoring.ClassicScoringCalculator
import fr.centuryspine.lsgscores.domain.scoring.FirstThreeElseTwoScoringCalculator
import fr.centuryspine.lsgscores.domain.scoring.SinglePointScoringCalculator
import fr.centuryspine.lsgscores.domain.scoring.TwoOneScoringCalculator


import org.junit.Assert.assertEquals
import org.junit.Test


class ScoringCalculatorTest {

    @Test
    fun `ClassicScoringCalculator returns strokes as scores`() {
        val calculator = ClassicScoringCalculator()
        val strokes = mapOf(1L to 4, 2L to 3, 3L to 5)
        val expected = mapOf(1L to 4, 2L to 3, 3L to 5)

        val result = calculator.calculateScores(strokes)

        assertEquals(expected, result)
    }

    @Test
    fun `SinglePointScoringCalculator gives 1 point to sole winner`() {
        val calculator = SinglePointScoringCalculator()
        val strokes = mapOf(1L to 2, 2L to 5, 3L to 4)
        val expected = mapOf(1L to 1, 2L to 0, 3L to 0)

        val result = calculator.calculateScores(strokes)

        assertEquals(expected, result)
    }

    @Test
    fun `SinglePointScoringCalculator gives 0 points when tie for lowest score`() {
        val calculator = SinglePointScoringCalculator()
        val strokes = mapOf(1L to 3, 2L to 3, 3L to 4)
        val expected = mapOf(1L to 0, 2L to 0, 3L to 0)

        val result = calculator.calculateScores(strokes)

        assertEquals(expected, result)
    }

    @Test
    fun `FirstThreeElseTwoScoringCalculator gives 3 to solo leader and 0 to others`() {
        val calculator = FirstThreeElseTwoScoringCalculator()
        val strokes = mapOf(1L to 2, 2L to 4, 3L to 5)
        val expected = mapOf(1L to 3, 2L to 0, 3L to 0)

        val result = calculator.calculateScores(strokes)
        assertEquals(expected, result)
    }

    @Test
    fun `FirstThreeElseTwoScoringCalculator gives 2 to tied leaders and 0 to others`() {
        val calculator = FirstThreeElseTwoScoringCalculator()
        val strokes = mapOf(1L to 3, 2L to 3, 3L to 5)
        val expected = mapOf(1L to 2, 2L to 2, 3L to 0)

        val result = calculator.calculateScores(strokes)
        assertEquals(expected, result)
    }

    @Test
    fun `TwoOneScoringCalculator gives 2 to solo first, 1 to solo second`() {
        val calculator = TwoOneScoringCalculator()
        val strokes = mapOf(1L to 2, 2L to 4, 3L to 3)
        val expected = mapOf(1L to 2, 2L to 0, 3L to 1)

        val result = calculator.calculateScores(strokes)
        assertEquals(expected, result)
    }

    @Test
    fun `TwoOneScoringCalculator gives 1 to each of two tied firsts and 1 to solo second`() {
        val calculator = TwoOneScoringCalculator()
        val strokes = mapOf(1L to 3, 2L to 3, 3L to 4)
        val expected = mapOf(1L to 1, 2L to 1, 3L to 1)

        val result = calculator.calculateScores(strokes)
        assertEquals(expected, result)
    }

    @Test
    fun `TwoOneScoringCalculator gives 0 to all if three or more tied first`() {
        val calculator = TwoOneScoringCalculator()
        val strokes = mapOf(1L to 2, 2L to 2, 3L to 2, 4L to 3)
        val expected = mapOf(1L to 0, 2L to 0, 3L to 0, 4L to 0)

        val result = calculator.calculateScores(strokes)
        assertEquals(expected, result)
    }

    @Test
    fun `TwoOneScoringCalculator gives no second place if tie for second`() {
        val calculator = TwoOneScoringCalculator()
        val strokes = mapOf(1L to 2, 2L to 3, 3L to 3)
        val expected = mapOf(1L to 2, 2L to 0, 3L to 0)

        val result = calculator.calculateScores(strokes)
        assertEquals(expected, result)
    }

}
