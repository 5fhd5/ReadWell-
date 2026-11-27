package io.github.readwell.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectionsTest {
    @Test
    fun `priority shelf orders high first and titles within a tier`() {
        val shelf = PriorityShelf(
            listOf(
                testBook("1", "Zebra", priority = Priority.HIGH),
                testBook("2", "alpha", priority = Priority.HIGH),
                testBook("3", "Middle", priority = Priority.LOW),
                testBook("4", "Beta", priority = Priority.MEDIUM),
            ),
        )
        assertEquals(listOf("alpha", "Zebra", "Beta", "Middle"), shelf.ordered().map(Book::title))

        val extra = testBook("5", "Gamma", priority = Priority.MEDIUM)
        shelf.add(extra)
        assertEquals("Beta", shelf.ordered()[2].title)
        assertTrue(shelf.remove(extra))
        assertFalse(shelf.remove(extra))
    }

    @Test
    fun `title trie returns case-insensitive prefix matches`() {
        val trie = TitleTrie(
            listOf(
                testBook("1", "Dune"),
                testBook("2", "Dune Messiah"),
                testBook("3", "Neuromancer"),
            ),
        )
        assertEquals(setOf("1", "2"), trie.withPrefix("DU"))
        assertEquals(emptySet(), trie.withPrefix(""))
        assertEquals(emptySet(), trie.withPrefix("x"))
    }

    @Test
    fun `activity ring evicts oldest entries after capacity`() {
        val activities = (1..5).map {
            Activity("2026-01-01T00:00:0${it}Z", ActivityKind.BOOK_ADDED, "book$it", "event $it")
        }
        val ring = ActivityRing(activities.take(2), capacity = 3)
        activities.drop(2).forEach(ring::add)

        assertEquals(listOf("book5", "book4", "book3"), ring.newestFirst().map(Activity::bookId))
        assertEquals(listOf("book3", "book4", "book5"), ring.oldestFirst().map(Activity::bookId))
    }
}

