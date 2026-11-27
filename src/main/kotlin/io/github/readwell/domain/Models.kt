package io.github.readwell.domain

import kotlinx.serialization.Serializable

@Serializable
enum class Priority(val rank: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
}

@Serializable
enum class ReadingStatus {
    TO_READ,
    READING,
    FINISHED,
}

@Serializable
enum class ActivityKind {
    BOOK_ADDED,
    PROGRESS_RECORDED,
    STATUS_CHANGED,
    BOOK_REMOVED,
}

enum class BookSort {
    PRIORITY,
    TITLE,
    RECENT,
}

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val publicationYear: Int,
    val totalPages: Int,
    val priority: Priority,
    val status: ReadingStatus,
    val progressPages: Int,
    val rating: Int? = null,
    val tags: Set<String> = emptySet(),
    val addedAt: String,
    val updatedAt: String,
) {
    val progressPercent: Int
        get() = ((progressPages.toLong() * PERCENT) / totalPages).toInt()

    private companion object {
        const val PERCENT = 100L
    }
}

@Serializable
data class Activity(
    val occurredAt: String,
    val kind: ActivityKind,
    val bookId: String,
    val detail: String,
)

@Serializable
data class LibraryState(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val books: List<Book> = emptyList(),
    val activities: List<Activity> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class NewBook(
    val title: String,
    val author: String,
    val publicationYear: Int,
    val totalPages: Int,
    val priority: Priority,
    val tags: Set<String> = emptySet(),
)

data class BookFilter(
    val status: ReadingStatus? = null,
    val priority: Priority? = null,
    val sort: BookSort = BookSort.PRIORITY,
)

data class LibraryStatistics(
    val totalBooks: Int,
    val byStatus: Map<ReadingStatus, Int>,
    val averageRating: Double?,
    val totalPagesRead: Int,
    val topTags: List<Pair<String, Int>>,
)

