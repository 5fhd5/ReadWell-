package io.github.readwell.domain

import io.github.readwell.persistence.LibraryRepository
import java.time.Clock
import java.time.Instant
import java.util.EnumMap
import java.util.Locale
import java.util.UUID

class ReadingListService(
    private val repository: LibraryRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    private var books: LinkedHashMap<String, Book>
    private var activity: ActivityRing
    private var shelf: PriorityShelf
    private var titleIndex: TitleTrie

    init {
        val state = repository.load()
        books = LinkedHashMap(state.books.associateBy(Book::id))
        activity = ActivityRing(state.activities)
        shelf = PriorityShelf()
        titleIndex = TitleTrie()
        rebuildIndexes()
    }

    @Synchronized
    fun add(command: NewBook): Book {
        val title = BookValidation.normalizeTitle(command.title)
        val author = BookValidation.normalizeAuthor(command.author)
        val year = BookValidation.validatePublicationYear(
            command.publicationYear,
            clock.instant().atZone(clock.zone).year,
        )
        val pages = BookValidation.validatePages(command.totalPages)
        val tags = BookValidation.normalizeTags(command.tags)
        val duplicate = books.values.any {
            it.title.equals(title, ignoreCase = true) && it.author.equals(author, ignoreCase = true)
        }
        if (duplicate) {
            throw DuplicateBookException(title, author)
        }

        val now = Instant.now(clock).toString()
        val book = Book(
            id = idFactory(),
            title = title,
            author = author,
            publicationYear = year,
            totalPages = pages,
            priority = command.priority,
            status = ReadingStatus.TO_READ,
            progressPages = 0,
            tags = tags,
            addedAt = now,
            updatedAt = now,
        )
        commit(
            books.values + book,
            Activity(now, ActivityKind.BOOK_ADDED, book.id, "Added '$title' by $author"),
        )
        return book
    }

    @Synchronized
    fun list(filter: BookFilter = BookFilter()): List<Book> {
        val ordered = when (filter.sort) {
            BookSort.PRIORITY -> shelf.ordered()
            BookSort.TITLE -> books.values.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            BookSort.RECENT -> books.values.sortedWith(
                compareByDescending<Book> { it.updatedAt }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
            )
        }
        return ordered.filter { book ->
            (filter.status == null || book.status == filter.status) &&
                (filter.priority == null || book.priority == filter.priority)
        }
    }

    @Synchronized
    fun search(rawQuery: String): List<Book> {
        val query = normalizeQuery(rawQuery)
        val titleMatches = titleIndex.withPrefix(query)
        val allMatches = books.values.filter { book ->
            book.id in titleMatches ||
                book.title.contains(query, ignoreCase = true) ||
                book.author.contains(query, ignoreCase = true) ||
                book.tags.any { it.contains(query, ignoreCase = true) }
        }
        return allMatches.sortedWith(
            compareByDescending<Book> { it.priority.rank }
                .thenBy(String.CASE_INSENSITIVE_ORDER, Book::title),
        )
    }

    @Synchronized
    fun get(rawQuery: String): Book {
        val query = normalizeQuery(rawQuery)
        val exact = books[query.lowercase(Locale.ROOT)]
        if (exact != null) {
            return exact
        }
        val matches = books.values.filter { it.id.startsWith(query, ignoreCase = true) }
        return when (matches.size) {
            0 -> throw BookNotFoundException(query)
            1 -> matches.single()
            else -> throw AmbiguousBookException(query, matches.size)
        }
    }

    @Synchronized
    fun recordProgress(rawQuery: String, pages: Int): Book {
        val book = get(rawQuery)
        if (book.status != ReadingStatus.READING) {
            throw InvalidTransitionException(
                "Progress can only be recorded for a book in READING state. Current state: ${book.status}.",
            )
        }
        val validatedPages = BookValidation.validateProgress(pages, book.totalPages)
        if (validatedPages == book.progressPages) {
            throw ValidationException("Progress is already $validatedPages pages.")
        }
        val updated = book.copy(progressPages = validatedPages, updatedAt = Instant.now(clock).toString())
        commit(
            books.values.replacing(updated),
            Activity(
                updated.updatedAt,
                ActivityKind.PROGRESS_RECORDED,
                book.id,
                "Progress on '${book.title}' set to $validatedPages/${book.totalPages} pages",
            ),
        )
        return updated
    }

    @Synchronized
    fun changeStatus(rawQuery: String, target: ReadingStatus, rating: Int? = null): Book {
        val book = get(rawQuery)
        val validatedRating = BookValidation.validateRating(rating)
        if (target != ReadingStatus.FINISHED && validatedRating != null) {
            throw ValidationException("A rating can only be supplied when marking a book FINISHED.")
        }
        validateTransition(book.status, target)
        val progress = when (target) {
            ReadingStatus.TO_READ -> 0
            ReadingStatus.READING -> if (book.status == ReadingStatus.FINISHED) 0 else book.progressPages
            ReadingStatus.FINISHED -> book.totalPages
        }
        val newRating = when (target) {
            ReadingStatus.FINISHED -> validatedRating
            ReadingStatus.READING, ReadingStatus.TO_READ -> null
        }
        val updated = book.copy(
            status = target,
            progressPages = progress,
            rating = newRating,
            updatedAt = Instant.now(clock).toString(),
        )
        commit(
            books.values.replacing(updated),
            Activity(
                updated.updatedAt,
                ActivityKind.STATUS_CHANGED,
                book.id,
                "Status for '${book.title}' changed from ${book.status} to $target",
            ),
        )
        return updated
    }

    @Synchronized
    fun remove(rawQuery: String): Book {
        val book = get(rawQuery)
        val now = Instant.now(clock).toString()
        commit(
            books.values.filterNot { it.id == book.id },
            Activity(now, ActivityKind.BOOK_REMOVED, book.id, "Removed '${book.title}' by ${book.author}"),
        )
        return book
    }

    @Synchronized
    fun history(limit: Int = DEFAULT_HISTORY_LIMIT): List<Activity> {
        if (limit !in 1..ActivityRing.DEFAULT_CAPACITY) {
            throw ValidationException("History limit must be between 1 and ${ActivityRing.DEFAULT_CAPACITY}.")
        }
        return activity.newestFirst().take(limit)
    }

    @Synchronized
    fun statistics(): LibraryStatistics {
        val byStatus = EnumMap<ReadingStatus, Int>(ReadingStatus::class.java).apply {
            ReadingStatus.entries.forEach { put(it, 0) }
        }
        books.values.forEach { book ->
            byStatus.compute(book.status) { _, count -> (count ?: 0) + 1 }
        }
        val ratings = books.values.mapNotNull(Book::rating)
        val tagCounts = books.values
            .flatMap(Book::tags)
            .groupingBy { it }
            .eachCount()
        return LibraryStatistics(
            totalBooks = books.size,
            byStatus = byStatus,
            averageRating = ratings.takeIf { it.isNotEmpty() }?.average(),
            totalPagesRead = books.values.sumOf(Book::progressPages),
            topTags = tagCounts.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(TOP_TAG_LIMIT)
                .map { it.key to it.value },
        )
    }

    private fun validateTransition(from: ReadingStatus, to: ReadingStatus) {
        if (from == to) {
            throw InvalidTransitionException("Book is already in $to state.")
        }
        val allowed = when (from) {
            ReadingStatus.TO_READ -> setOf(ReadingStatus.READING, ReadingStatus.FINISHED)
            ReadingStatus.READING -> setOf(ReadingStatus.TO_READ, ReadingStatus.FINISHED)
            ReadingStatus.FINISHED -> setOf(ReadingStatus.READING)
        }
        if (to !in allowed) {
            throw InvalidTransitionException("Cannot change status from $from to $to.")
        }
    }

    private fun commit(updatedBooks: Collection<Book>, event: Activity) {
        val updatedActivity = ActivityRing(activity.oldestFirst())
        updatedActivity.add(event)
        val newState = LibraryState(
            books = updatedBooks.sortedBy(Book::id),
            activities = updatedActivity.oldestFirst(),
        )
        repository.save(newState)
        books = LinkedHashMap(updatedBooks.associateBy(Book::id))
        activity = updatedActivity
        rebuildIndexes()
    }

    private fun rebuildIndexes() {
        shelf = PriorityShelf(books.values)
        titleIndex = TitleTrie(books.values)
    }

    private fun normalizeQuery(rawQuery: String): String {
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            throw ValidationException("Search or lookup value cannot be blank.")
        }
        if (query.length > MAX_QUERY_LENGTH) {
            throw ValidationException("Search or lookup value must be at most $MAX_QUERY_LENGTH characters.")
        }
        return query
    }

    private fun Collection<Book>.replacing(updated: Book): List<Book> =
        map { if (it.id == updated.id) updated else it }

    companion object {
        const val DEFAULT_HISTORY_LIMIT = 50
        private const val MAX_QUERY_LENGTH = 80
        private const val TOP_TAG_LIMIT = 5
    }
}

