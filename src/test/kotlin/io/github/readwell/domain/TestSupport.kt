package io.github.readwell.domain

import io.github.readwell.persistence.LibraryRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

internal fun testBook(
    id: String,
    title: String,
    author: String = "Test Author",
    priority: Priority = Priority.MEDIUM,
    status: ReadingStatus = ReadingStatus.TO_READ,
    progressPages: Int = 0,
    totalPages: Int = 300,
    rating: Int? = null,
    tags: Set<String> = emptySet(),
    addedAt: String = "2026-01-01T00:00:00Z",
    updatedAt: String = addedAt,
): Book = Book(
    id = id,
    title = title,
    author = author,
    publicationYear = 2020,
    totalPages = totalPages,
    priority = priority,
    status = status,
    progressPages = progressPages,
    rating = rating,
    tags = tags,
    addedAt = addedAt,
    updatedAt = updatedAt,
)

internal class InMemoryRepository(
    var state: LibraryState = LibraryState(),
) : LibraryRepository {
    var saveCount: Int = 0
    var failOnSave: Boolean = false

    override fun load(): LibraryState = state

    override fun save(state: LibraryState) {
        if (failOnSave) {
            throw PersistenceException("disk full")
        }
        this.state = state
        saveCount += 1
    }
}

internal class MutableClock(
    var current: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)

    override fun instant(): Instant = current

    fun advanceSeconds(seconds: Long) {
        current = current.plusSeconds(seconds)
    }
}

internal fun idSequence(prefix: String = "book"): () -> String {
    var nextId = 1
    return { "$prefix-${nextId++}" }
}

