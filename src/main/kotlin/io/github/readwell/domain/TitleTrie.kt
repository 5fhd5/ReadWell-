package io.github.readwell.domain

import java.util.Locale

/**
 * Prefix index for titles. Every node keeps the complete set of book IDs below
 * it, which makes prefix lookup proportional to the prefix length plus matches.
 */
class TitleTrie(books: Collection<Book> = emptyList()) {
    private val root = Node()

    init {
        books.forEach(::insert)
    }

    fun insert(book: Book) {
        var node = root
        val key = normalize(book.title)
        for (character in key) {
            node = node.children.getOrPut(character, ::Node)
            node.matches.add(book.id)
        }
    }

    fun withPrefix(rawPrefix: String): Set<String> {
        val prefix = normalize(rawPrefix)
        if (prefix.isEmpty()) {
            return emptySet()
        }
        var node = root
        for (character in prefix) {
            node = node.children[character] ?: return emptySet()
        }
        return node.matches.toSet()
    }

    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)

    private class Node {
        val children: MutableMap<Char, Node> = HashMap()
        val matches: MutableSet<String> = linkedSetOf()
    }
}

