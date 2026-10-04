package com.pausespace.app

import com.pausespace.app.data.AppRule
import com.pausespace.app.data.Ritual
import com.pausespace.app.data.Rules
import com.pausespace.app.data.WordLines
import com.pausespace.app.data.Words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RulesTest {
    @Test
    fun allDayAlwaysInSchedule() {
        assertTrue(Rules.inSchedule(AppRule(allDay = true), 0))
        assertTrue(Rules.inSchedule(AppRule(allDay = true), 23 * 60 + 59))
    }

    @Test
    fun daytimeWindow() {
        val r = AppRule(allDay = false, startMin = 9 * 60, endMin = 17 * 60)
        assertFalse(Rules.inSchedule(r, 8 * 60 + 59))
        assertTrue(Rules.inSchedule(r, 9 * 60))
        assertTrue(Rules.inSchedule(r, 16 * 60 + 59))
        assertFalse(Rules.inSchedule(r, 17 * 60))
    }

    @Test
    fun windowWrappingMidnight() {
        val r = AppRule(allDay = false, startMin = 21 * 60, endMin = 7 * 60)
        assertTrue(Rules.inSchedule(r, 23 * 60))
        assertTrue(Rules.inSchedule(r, 3 * 60))
        assertFalse(Rules.inSchedule(r, 12 * 60))
    }

    @Test
    fun roundsGrowFromThirdTry() {
        val ritual = Ritual(rounds = 2)
        val rule = AppRule(grow = true)
        assertEquals(2, Rules.rounds(ritual, rule, 1))
        assertEquals(2, Rules.rounds(ritual, rule, 2))
        assertEquals(3, Rules.rounds(ritual, rule, 3))
        assertEquals(4, Rules.rounds(ritual, rule, 4))
        assertEquals(Rules.MAX_ROUNDS, Rules.rounds(ritual, rule, 40))
        assertEquals(2, Rules.rounds(ritual, rule.copy(grow = false), 10))
    }

    @Test
    fun linePicksSelectedOrCustom() {
        assertEquals(WordLines[2], Rules.line(Words(selected = 2)))
        assertEquals("Not now", Rules.line(Words(custom = " Not now ", useCustom = true)))
        // Blank custom text falls back to the selected preset.
        assertEquals(WordLines[0], Rules.line(Words(custom = "  ", useCustom = true)))
    }

    @Test
    fun shuffleDrawsFromPresetsAndCustom() {
        val words = Words(custom = "Mine", shuffle = true)
        val seen = (0 until 200).map { Rules.line(words, Random(it)) }.toSet()
        assertEquals((WordLines + "Mine").toSet(), seen)
    }

    @Test
    fun limitsNeverNegative() {
        assertEquals(0, Rules.skipsLeft(3, 5))
        assertEquals(2, Rules.skipsLeft(3, 1))
        assertEquals(0, Rules.opensLeft(AppRule(dailyOpens = 2), 4))
    }

    @Test
    fun formatting() {
        assertEquals("1h 42m", Rules.duration(102 * 60_000L))
        assertEquals("4h 02m", Rules.duration(242 * 60_000L))
        assertEquals("52m", Rules.duration(52 * 60_000L))
        assertEquals("8th", Rules.ordinal(8))
        assertEquals("22nd", Rules.ordinal(22))
        assertEquals("11th", Rules.ordinal(11))
        assertEquals(0.113f, Rules.stripFraction(7 * 60 + 42), 0.002f)
    }
}
