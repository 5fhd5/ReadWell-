package io.github.readwell.cli

import io.github.readwell.domain.Activity
import io.github.readwell.domain.Book
import io.github.readwell.domain.LibraryStatistics
import io.github.readwell.domain.ReadingStatus

object TextFormat {
    fun bookTable(books: List<Book>): String {
        if (books.isEmpty()) {
            return "No books match."
        }
        val rows = books.map(::bookRow)
        return buildString {
            appendLine("ID        PRIORITY  STATUS    PROGRESS  TITLE - AUTHOR")
            rows.forEach(::appendLine)
            append("${books.size} book${if (books.size == 1) "" else "s"} shown.")
        }
    }

    fun bookDetails(book: Book): String = buildString {
        appendLine("ID:         ${book.id}")
        appendLine("Title:      ${book.title}")
        appendLine("Author:     ${book.author}")
        appendLine("Published:  ${book.publicationYear}")
        appendLine("Pages:      ${book.progressPages}/${book.totalPages} (${book.progressPercent}%)")
        appendLine("Priority:   ${book.priority}")
        appendLine("Status:     ${book.status}")
        appendLine("Rating:     ${book.rating?.let { "$it/5" } ?: "-"}")
        appendLine("Tags:       ${book.tags.sorted().joinToString(", ").ifEmpty { "-" }}")
        appendLine("Added:      ${book.addedAt}")
        append("Updated:    ${book.updatedAt}")
    }

    fun history(activities: List<Activity>): String {
        if (activities.isEmpty()) {
            return "No activity yet."
        }
        return activities.joinToString("\n") { activity ->
            "${activity.occurredAt}  ${activity.kind}  ${activity.detail}"
        }
    }

    fun statistics(statistics: LibraryStatistics): String = buildString {
        appendLine("Books:       ${statistics.totalBooks}")
        ReadingStatus.entries.forEach { status ->
            appendLine("${status.name.padEnd(12)} ${statistics.byStatus[status] ?: 0}")
        }
        appendLine("Pages read:  ${statistics.totalPagesRead}")
        appendLine("Avg rating:  ${statistics.averageRating?.format(2) ?: "-"}")
        append(
            "Top tags:    ${
                statistics.topTags.joinToString(", ") { (tag, count) -> "#$tag ($count)" }.ifEmpty { "-" }
            }",
        )
    }

    private fun bookRow(book: Book): String {
        val id = book.id.take(SHORT_ID_LENGTH)
        val progress = "${book.progressPercent}%"
        return "${id.padEnd(10)}${book.priority.name.padEnd(10)}${book.status.name.padEnd(10)}${
            progress.padEnd(10)
        }${book.title} - ${book.author}"
    }

    private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)

    private const val SHORT_ID_LENGTH = 8
}

