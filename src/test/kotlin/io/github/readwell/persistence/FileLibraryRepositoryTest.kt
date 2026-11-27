package io.github.readwell.persistence

import io.github.readwell.domain.Activity
import io.github.readwell.domain.ActivityKind
import io.github.readwell.domain.LibraryState
import io.github.readwell.domain.PersistenceException
import io.github.readwell.domain.testBook
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileLibraryRepositoryTest {
    @Test
    fun `missing file starts with empty state`() {
        val repository = FileLibraryRepository(newPath("missing"))
        assertEquals(LibraryState(), repository.load())
    }

    @Test
    fun `saves and reloads state without temporary file`() {
        val path = newPath("saved").resolveSibling("${System.nanoTime()}-nested/library.json")
        val repository = FileLibraryRepository(path)
        val state = LibraryState(
            books = listOf(testBook("book-1", "Persisted")),
            activities = listOf(
                Activity("2026-01-01T00:00:00Z", ActivityKind.BOOK_ADDED, "book-1", "Added"),
            ),
        )

        repository.save(state)

        assertTrue(Files.exists(path))
        assertFalse(Files.exists(path.resolveSibling("library.json.tmp")))
        assertEquals(state, repository.load())
    }

    @Test
    fun `rejects invalid JSON with actionable error`() {
        val path = newPath("invalid")
        Files.writeString(path, "{not-json")
        val repository = FileLibraryRepository(path)

        val error = assertFailsWith<PersistenceException> { repository.load() }
        assertTrue(error.message.orEmpty().contains("not valid ReadWell JSON"))
    }

    @Test
    fun `rejects unsupported schema versions`() {
        val path = newPath("schema")
        val repository = FileLibraryRepository(path)
        repository.save(LibraryState(schemaVersion = 99))

        val error = assertFailsWith<PersistenceException> { repository.load() }
        assertTrue(error.message.orEmpty().contains("Unsupported data schema"))
    }

    private fun newPath(label: String): Path {
        val directory = Path.of("build", "test-data", "persistence")
        Files.createDirectories(directory)
        return directory.resolve("$label-${System.nanoTime()}.json")
    }
}

