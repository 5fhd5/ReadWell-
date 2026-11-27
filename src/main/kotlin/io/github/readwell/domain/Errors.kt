package io.github.readwell.domain

sealed class DomainException(
    message: String,
    val exitCode: Int,
) : RuntimeException(message)

class ValidationException(message: String) : DomainException(message, 3)

class BookNotFoundException(query: String) :
    DomainException("No book matches '$query'. Run 'readwell list' to see available IDs.", 4)

class AmbiguousBookException(query: String, count: Int) :
    DomainException("ID prefix '$query' matches $count books. Use at least four characters.", 4)

class DuplicateBookException(title: String, author: String) :
    DomainException("'$title' by $author is already in the reading list.", 5)

class InvalidTransitionException(message: String) : DomainException(message, 6)

class PersistenceException(message: String, cause: Throwable? = null) :
    DomainException(message, 7) {
    init {
        cause?.let(::initCause)
    }
}

