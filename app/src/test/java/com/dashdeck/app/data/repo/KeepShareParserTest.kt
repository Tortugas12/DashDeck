package com.dashdeck.app.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepShareParserTest {

    @Test
    fun parsesMarkdownAndUnicodeCheckboxesWithExplicitSubject() {
        val shared = """
            - [ ] Buy oat milk
            - [x] Send sprint recap
            ☐ Review Googlebook OS layout
            ☑ Ship Phase 2 tests
        """.trimIndent()

        val parsed = KeepShareParser.parse(
            subject = "Weekly Errands",
            sharedText = shared,
            idGenerator = { "id_$it" },
        )

        assertEquals("Weekly Errands", parsed.title)
        assertEquals("", parsed.body)
        assertEquals(4, parsed.checklist.size)
        assertEquals("Buy oat milk", parsed.checklist[0].text)
        assertFalse(parsed.checklist[0].isChecked)
        assertEquals("Send sprint recap", parsed.checklist[1].text)
        assertTrue(parsed.checklist[1].isChecked)
        assertEquals("Review Googlebook OS layout", parsed.checklist[2].text)
        assertFalse(parsed.checklist[2].isChecked)
        assertEquals("Ship Phase 2 tests", parsed.checklist[3].text)
        assertTrue(parsed.checklist[3].isChecked)
    }

    @Test
    fun derivesTitleFromFirstLineWhenSubjectIsMissing() {
        val shared = """
            Release Notes
            First paragraph of details.
            Second line of details.
        """.trimIndent()

        val parsed = KeepShareParser.parse(subject = null, sharedText = shared)
        assertEquals("Release Notes", parsed.title)
        assertEquals("First paragraph of details.\nSecond line of details.", parsed.body)
        assertTrue(parsed.checklist.isEmpty())
    }

    @Test
    fun mixedNoteSeparatesTitleBodyAndChecklist() {
        val shared = """
            Trip Prep
            Pack light for the flight.
            [ ] Passport
            [X] USB-C charger
            - Adapters
        """.trimIndent()

        val parsed = KeepShareParser.parse(
            subject = null,
            sharedText = shared,
            idGenerator = { "item_$it" },
        )

        assertEquals("Trip Prep", parsed.title)
        assertEquals("Pack light for the flight.", parsed.body)
        assertEquals(3, parsed.checklist.size)
        assertEquals(listOf("Passport", "USB-C charger", "Adapters"), parsed.checklist.map { it.text })
        assertEquals(listOf(false, true, false), parsed.checklist.map { it.isChecked })
    }
}
