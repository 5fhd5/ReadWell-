package io.github.readwell.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BookValidationTest {
    @Test
    fun `normalizes title and author whitespace`() {
        assertEquals("The Left Hand", BookValidation.normalizeTitle("  The   Left  Hand  "))
        assertEquals("Ursula K. Le Guin", BookValidation.normalizeAuthor(" Ursula  K.  Le Guin "))
    }

    @Test
    fun `rejects blank and oversized text`() {
        assertFailsWith<ValidationException> { BookValidation.normalizeTitle(" ") }
        assertFailsWith<ValidationException> { BookValidation.normalizeAuthor("\t") }
        assertFailsWith<ValidationException> { BookValidation.normalizeTitle("x".repeat(121)) }
        assertFailsWith<ValidationException> { BookValidation.normalizeAuthor("x".repeat(81)) }
    }

    @Test
    fun `rejects control characters`() {
        assertFailsWith<ValidationException> { BookValidation.normalizeTitle("Good\u0000Book") }
        assertFailsWith<ValidationException> { BookValidation.normalizeAuthor("A\u0007uthor") }
    }

    @Test
    fun `validates publication year inclusive boundaries`() {
        assertEquals(1000, BookValidation.validatePublicationYear(1000, 2026))
        assertEquals(2027, BookValidation.validatePublicationYear(2027, 2026))
        assertFailsWith<ValidationException> { BookValidation.validatePublicationYear(999, 2026) }
        assertFailsWith<ValidationException> { BookValidation.validatePublicationYear(2028, 2026) }
    }

    @Test
    fun `validates page bounds and progress`() {
        assertEquals(1, BookValidation.validatePages(1))
        assertEquals(20_000, BookValidation.validatePages(20_000))
        assertFailsWith<ValidationException> { BookValidation.validatePages(0) }
        assertFailsWith<ValidationException> { BookValidation.validatePages(20_001) }
        assertEquals(42, BookValidation.validateProgress(42, 100))
        assertFailsWith<ValidationException> { BookValidation.validateProgress(101, 100) }
        assertFailsWith<ValidationException> { BookValidation.validateProgress(-1, 100) }
    }

    @Test
    fun `validates optional rating`() {
        assertNull(BookValidation.validateRating(null))
        assertEquals(5, BookValidation.validateRating(5))
        assertFailsWith<ValidationException> { BookValidation.validateRating(0) }
        assertFailsWith<ValidationException> { BookValidation.validateRating(6) }
    }

    @Test
    fun `normalizes tags and detects invalid values`() {
        val tags = BookValidation.normalizeTags(listOf(" #SciFi ", "sci-fi", "SCIFI"))
        assertEquals(setOf("scifi", "sci-fi"), tags)
        assertFailsWith<ValidationException> { BookValidation.normalizeTags(listOf("bad tag")) }
        assertFailsWith<ValidationException> { BookValidation.normalizeTags(listOf("x".repeat(25))) }
        assertFailsWith<ValidationException> {
            BookValidation.normalizeTags((1..9).map { "tag$it" })
        }
    }
}

