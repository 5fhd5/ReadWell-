package io.github.readwell.domain

import java.util.Locale

object BookValidation {
    private const val MAX_TITLE_LENGTH = 120
    private const val MAX_AUTHOR_LENGTH = 80
    private const val MIN_YEAR = 1000
    private const val MAX_FUTURE_YEARS = 1
    private const val MIN_PAGES = 1
    private const val MAX_PAGES = 20_000
    private const val MAX_TAGS = 8
    private const val MAX_TAG_LENGTH = 24
    private const val MIN_RATING = 1
    private const val MAX_RATING = 5
    private val whitespace = Regex("\\s+")
    private val controlCharacter = Regex("[\\p{Cntrl}]")
    private val tagPattern = Regex("[a-z0-9][a-z0-9+.#-]*")

    fun normalizeTitle(raw: String): String {
        val title = normalizeText(raw)
        if (title.isEmpty()) {
            throw ValidationException("Title cannot be blank.")
        }
        if (title.length > MAX_TITLE_LENGTH) {
            throw ValidationException("Title must be at most $MAX_TITLE_LENGTH characters.")
        }
        if (controlCharacter.containsMatchIn(title)) {
            throw ValidationException("Title cannot contain control characters.")
        }
        return title
    }

    fun normalizeAuthor(raw: String): String {
        val author = normalizeText(raw)
        if (author.isEmpty()) {
            throw ValidationException("Author cannot be blank.")
        }
        if (author.length > MAX_AUTHOR_LENGTH) {
            throw ValidationException("Author must be at most $MAX_AUTHOR_LENGTH characters.")
        }
        if (controlCharacter.containsMatchIn(author)) {
            throw ValidationException("Author cannot contain control characters.")
        }
        return author
    }

    fun validatePublicationYear(year: Int, currentYear: Int): Int {
        if (year !in MIN_YEAR..currentYear + MAX_FUTURE_YEARS) {
            throw ValidationException(
                "Publication year must be between $MIN_YEAR and ${currentYear + MAX_FUTURE_YEARS}.",
            )
        }
        return year
    }

    fun validatePages(pages: Int): Int {
        if (pages !in MIN_PAGES..MAX_PAGES) {
            throw ValidationException("Page count must be between $MIN_PAGES and $MAX_PAGES.")
        }
        return pages
    }

    fun validateProgress(progressPages: Int, totalPages: Int): Int {
        if (progressPages !in 0..totalPages) {
            throw ValidationException("Progress must be between 0 and $totalPages pages.")
        }
        return progressPages
    }

    fun validateRating(rating: Int?): Int? {
        if (rating != null && rating !in MIN_RATING..MAX_RATING) {
            throw ValidationException("Rating must be between $MIN_RATING and $MAX_RATING.")
        }
        return rating
    }

    fun normalizeTags(rawTags: Collection<String>): Set<String> {
        val tags = rawTags
            .map { it.trim().removePrefix("#").lowercase(Locale.ROOT) }
            .filter(String::isNotEmpty)
            .toSet()
        if (tags.size > MAX_TAGS) {
            throw ValidationException("A book can have at most $MAX_TAGS distinct tags.")
        }
        val invalid = tags.firstOrNull { it.length > MAX_TAG_LENGTH || !tagPattern.matches(it) }
        if (invalid != null) {
            throw ValidationException(
                "Invalid tag '$invalid'. Use letters, digits, +, ., #, or - (maximum $MAX_TAG_LENGTH characters).",
            )
        }
        return tags
    }

    private fun normalizeText(raw: String): String = raw.trim().replace(whitespace, " ")
}

