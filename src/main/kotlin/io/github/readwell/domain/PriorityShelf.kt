package io.github.readwell.domain

import java.util.EnumMap
import java.util.TreeMap

/**
 * A priority-tiered shelf. Priorities are visited high-to-low and books within
 * each tier are kept in case-insensitive title order.
 */
class PriorityShelf(initialBooks: Collection<Book> = emptyList()) {
    private val tiers =
        EnumMap<Priority, TreeMap<String, MutableList<Book>>>(Priority::class.java).apply {
            Priority.entries.forEach { put(it, TreeMap(String.CASE_INSENSITIVE_ORDER)) }
        }

    init {
        initialBooks.forEach(::add)
    }

    fun add(book: Book) {
        tiers.getValue(book.priority)
            .getOrPut(book.title) { mutableListOf() }
            .add(book)
    }

    fun remove(book: Book): Boolean =
        tiers.getValue(book.priority)[book.title]?.let { books ->
            books.removeIf { it.id == book.id }
            if (books.isEmpty()) {
                tiers.getValue(book.priority).remove(book.title)
            }
            true
        } ?: false

    fun ordered(): List<Book> = buildList {
        Priority.entries.sortedByDescending(Priority::rank).forEach { priority ->
            tiers.getValue(priority).values.forEach { addAll(it) }
        }
    }
}

