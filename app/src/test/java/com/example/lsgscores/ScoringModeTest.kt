package com.example.lsgscores

import com.example.lsgscores.domain.scoring.ClassicScoringCalculator
import com.example.lsgscores.domain.scoring.FirstThreeElseTwoScoringCalculator
import com.example.lsgscores.domain.scoring.SinglePointScoringCalculator
import com.example.lsgscores.domain.scoring.TwoOneScoringCalculator


import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringCalculatorTest {

    @Test
    fun `ClassicScoringCalculator returns strokes as scores`() {
        val calculator = ClassicScoringCalculator()
        val strokes = mapOf("Alice" to 4, "Bob" to 3, "Clara" to 5)
        val expected = mapOf("Alice" to 4, "Bob" to 3, "Clara" to 5)

        val result = calculator.calculateScores(strokes)

        assertEquals(expected, result)
    }

    @Test
    fun `SinglePointScoringCalculator gives 1 point to sole winner`() {
        val calculator = SinglePointScoringCalculator()
        val strokes = mapOf("Alice" to 2, "Bob" to 5, "Clara" to 4)
        val expected = mapOf("Alice" to 1, "Bob" to 0, "Clara" to 0)

        val result = calculator.calculateScores(strokes)

        assertEquals(expected, result)
    }

    @Test
    fun `SinglePointScoringCalculator gives 0 points when tie for lowest score`() {
        val calculator = SinglePointScoringCalculator()
        val strokes = mapOf("Alice" to 3, "Bob" to 3, "Clara" to 4)
        val expected = mapOf("Alice" to 0, "Bob" to 0, "Clara" to 0)

        val result = calculator.calculateScores(strokes)

        assertEquals(expected, result)
    }

    @Test
    fun `FirstThreeElseTwoScoringCalculator gives 3 to solo leader and 0 to others`() {
        val calculator = FirstThreeElseTwoScoringCalculator()
        val strokes = mapOf("Alice" to 2, "Bob" to 4, "Clara" to 5)
        val expected = mapOf("Alice" to 3, "Bob" to 0, "Clara" to 0)

        val result = calculator.calculateScores(strokes)
        assertEquals(expected, result)
    }

    @Test
    fun `FirstThreeElseTwoScoringCalculator gives 2 to tied leaders and 0 to others`() {
        val calculator = FirstThreeElseTwoScoringCalculator()
        val strokes = mapOf("Alice" to 3, "Bob" to 3, "Clara" to 5)
        val expected = mapOf("Alice" to 2, "Bob" to 2, "Clara" to 0)

        val result = calculator.calculateScores(strokes)
        assertEquals(expected, result)
    }

    @Test
    fun `TwoOneScoringCalculator gives 2 to solo first, 1 to solo second`() {
        val calculator = TwoOneScoringCalculator()
        val strokes = mapOf("Alice" to 2, "Bob" to 4, "Clara" to 3)
        val expected = mapOf("Alice" to 2, "Bob" to 0, "Clara" to 1)

        val result = calculator.calculateScores(strokes)
        assertEquals(expected, result)
    }

    @Test
    fun `TwoOneScoringCalculator gives 1 to each of two tied firsts and 1 to solo second`() {
        val calculator = TwoOneScoringCalculator()
        val strokes = mapOf("Alice" to 3, "Bob" to 3, "Clara" to 4)
        val expected = mapOf("Alice" to 1, "Bob" to 1, "Clara" to 1)

        val result = calculator.calculateScores(strokes)
        assertEquals(expected, result)
    }

    @Test
    fun `TwoOneScoringCalculator gives 0 to all if three or more tied first`() {
        val calculator = TwoOneScoringCalculator()
        val strokes = mapOf("Alice" to 2, "Bob" to 2, "Clara" to 2, "David" to 3)
        val expected = mapOf("Alice" to 0, "Bob" to 0, "Clara" to 0, "David" to 0)

        val result = calculator.calculateScores(strokes)
        assertEquals(expected, result)
    }

    @Test
    fun `TwoOneScoringCalculator gives no second place if tie for second`() {
        val calculator = TwoOneScoringCalculator()
        val strokes = mapOf("Alice" to 2, "Bob" to 3, "Clara" to 3)
        val expected = mapOf("Alice" to 2, "Bob" to 0, "Clara" to 0)

        val result = calculator.calculateScores(strokes)
        assertEquals(expected, result)
    }

}
