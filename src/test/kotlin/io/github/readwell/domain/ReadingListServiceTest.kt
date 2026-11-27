package io.github.readwell.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadingListServiceTest {
    private val clock = MutableClock()

    @Test
    fun `adds normalized book and records activity`() {
        val repository = InMemoryRepository()
        val service = service(repository)
        val book = service.add(
            NewBook(
                title = "  The   Hobbit ",
                author = " J. R. R.  Tolkien ",
                publicationYear = 1937,
                totalPages = 310,
                priority = Priority.HIGH,
                tags = setOf("#Fantasy"),
            ),
        )

        assertEquals("The Hobbit", book.title)
        assertEquals("J. R. R. Tolkien", book.author)
        assertEquals(setOf("fantasy"), book.tags)
        assertEquals(ReadingStatus.TO_READ, book.status)
        assertEquals(1, repository.saveCount)
        assertEquals(ActivityKind.BOOK_ADDED, service.history().single().kind)
    }

    @Test
    fun `rejects duplicate title and author ignoring case`() {
        val service = service()
        val command = NewBook("Dune", "Frank Herbert", 1965, 412, Priority.HIGH)
        service.add(command)

        assertFailsWith<DuplicateBookException> {
            service.add(command.copy(title = "DUNE", author = "frank herbert"))
        }
    }

    @Test
    fun `rejects invalid input before persistence`() {
        val repository = InMemoryRepository()
        val service = service(repository)
        assertFailsWith<ValidationException> {
            service.add(NewBook("", "Author", 2020, 100, Priority.LOW))
        }
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `lists by priority and filters both dimensions`() {
        val service = service()
        add(service, "Low Book", Priority.LOW)
        add(service, "High Book", Priority.HIGH)
        add(service, "Medium Book", Priority.MEDIUM)

        assertEquals(
            listOf("High Book", "Medium Book", "Low Book"),
            service.list().map(Book::title),
        )
        assertEquals(
            listOf("Low Book"),
            service.list(BookFilter(priority = Priority.LOW)).map(Book::title),
        )
        assertTrue(service.list(BookFilter(status = ReadingStatus.FINISHED)).isEmpty())
        assertEquals(
            listOf("High Book", "Low Book", "Medium Book"),
            service.list(BookFilter(sort = BookSort.TITLE)).map(Book::title),
        )
    }

    @Test
    fun `sorts recent updates newest first`() {
        val service = service()
        add(service, "Older", Priority.LOW)
        clock.advanceSeconds(10)
        add(service, "Newer", Priority.LOW)

        assertEquals(listOf("Newer", "Older"), service.list(BookFilter(sort = BookSort.RECENT)).map(Book::title))
    }

    @Test
    fun `searches title prefixes authors and tags`() {
        val service = service()
        add(service, "Dune", Priority.HIGH, author = "Frank Herbert", tags = setOf("classic"))
        add(service, "Neuromancer", Priority.MEDIUM, author = "William Gibson", tags = setOf("cyberpunk"))

        assertEquals(listOf("Dune"), service.search("DU").map(Book::title))
        assertEquals(listOf("Neuromancer"), service.search("gibson").map(Book::title))
        assertEquals(listOf("Neuromancer"), service.search("cyber").map(Book::title))
    }

    @Test
    fun `resolves exact and unique prefix ids and reports lookup errors`() {
        val first = testBook("abc123", "First")
        val second = testBook("abc456", "Second")
        val service = ReadingListService(
            InMemoryRepository(LibraryState(books = listOf(first, second))),
            clock,
            idSequence(),
        )

        assertEquals(first, service.get("abc123"))
        assertEquals(second, service.get("abc4"))
        assertFailsWith<AmbiguousBookException> { service.get("abc") }
        assertFailsWith<BookNotFoundException> { service.get("zzz") }
        assertFailsWith<ValidationException> { service.get(" ") }
    }

    @Test
    fun `records progress for reading book only`() {
        val repository = InMemoryRepository()
        val service = service(repository)
        val book = add(service, "Progress Book", Priority.HIGH)

        service.changeStatus(book.id, ReadingStatus.READING)
        val updated = service.recordProgress(book.id, 100)
        assertEquals(100, updated.progressPages)
        assertEquals(33, updated.progressPercent)
        assertFailsWith<ValidationException> { service.recordProgress(book.id, 100) }
        assertFailsWith<ValidationException> { service.recordProgress(book.id, 301) }
        assertFailsWith<InvalidTransitionException> {
            service.changeStatus(book.id, ReadingStatus.TO_READ).let {
                service.recordProgress(book.id, 1)
            }
        }
    }

    @Test
    fun `enforces status lifecycle and rating rules`() {
        val service = service()
        val book = add(service, "Status Book", Priority.MEDIUM)

        assertFailsWith<InvalidTransitionException> {
            service.changeStatus(book.id, ReadingStatus.TO_READ)
        }
        assertFailsWith<ValidationException> {
            service.changeStatus(book.id, ReadingStatus.READING, rating = 5)
        }
        val reading = service.changeStatus(book.id, ReadingStatus.READING)
        assertEquals(0, reading.progressPages)
        val finished = service.changeStatus(book.id, ReadingStatus.FINISHED, rating = 5)
        assertEquals(300, finished.progressPages)
        assertEquals(5, finished.rating)
        val reread = service.changeStatus(book.id, ReadingStatus.READING)
        assertEquals(0, reread.progressPages)
        assertNull(reread.rating)
    }

    @Test
    fun `allows marking to-read book finished and validates rating`() {
        val service = service()
        val book = add(service, "Direct Finish", Priority.LOW)
        assertFailsWith<ValidationException> {
            service.changeStatus(book.id, ReadingStatus.FINISHED, rating = 6)
        }
        assertEquals(ReadingStatus.FINISHED, service.changeStatus(book.id, ReadingStatus.FINISHED).status)
    }

    @Test
    fun `removes book and can recreate it`() {
        val service = service()
        val book = add(service, "Temporary", Priority.LOW)
        val removed = service.remove(book.id)
        assertEquals(book, removed)
        assertTrue(service.list().isEmpty())
        assertEquals(ActivityKind.BOOK_REMOVED, service.history().first().kind)
        add(service, "Temporary", Priority.LOW)
        assertEquals(1, service.list().size)
    }

    @Test
    fun `computes statistics and limits history`() {
        val service = service()
        val first = add(service, "One", Priority.HIGH, tags = setOf("fiction", "classic"))
        add(service, "Two", Priority.LOW, tags = setOf("fiction"))
        service.changeStatus(first.id, ReadingStatus.FINISHED, rating = 4)

        val stats = service.statistics()
        assertEquals(2, stats.totalBooks)
        assertEquals(1, stats.byStatus[ReadingStatus.FINISHED])
        assertEquals(1, stats.byStatus[ReadingStatus.TO_READ])
        assertEquals(4.0, stats.averageRating)
        assertEquals(300, stats.totalPagesRead)
        assertEquals("fiction" to 2, stats.topTags.first())
        assertEquals(1, service.history(1).size)
        assertFailsWith<ValidationException> { service.history(0) }
        assertFailsWith<ValidationException> { service.history(201) }
    }

    @Test
    fun `does not mutate memory when persistence fails`() {
        val repository = InMemoryRepository()
        val service = service(repository)
        repository.failOnSave = true
        assertFailsWith<PersistenceException> {
            service.add(NewBook("Unsaved", "Author", 2020, 100, Priority.HIGH))
        }
        assertTrue(service.list().isEmpty())
    }

    @Test
    fun `reloads all books and activity from repository`() {
        val repository = InMemoryRepository()
        val firstService = service(repository)
        val book = add(firstService, "Persisted", Priority.HIGH)

        val secondService = service(repository)
        assertEquals(book, secondService.get(book.id))
        assertEquals(ActivityKind.BOOK_ADDED, secondService.history().single().kind)
    }

    private fun service(repository: InMemoryRepository = InMemoryRepository()): ReadingListService =
        ReadingListService(repository, clock, idSequence())

    private fun add(
        service: ReadingListService,
        title: String,
        priority: Priority,
        author: String = "Test Author",
        tags: Set<String> = emptySet(),
    ): Book = service.add(
        NewBook(
            title = title,
            author = author,
            publicationYear = 2020,
            totalPages = 300,
            priority = priority,
            tags = tags,
        ),
    )

}

