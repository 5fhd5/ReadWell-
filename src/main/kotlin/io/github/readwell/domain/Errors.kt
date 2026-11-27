package io.github.readwell.domain

sealed class DomainException(
    message: String,
    val exitCode: Int,
) : RuntimeException(message)

class ValidationException(message: String) : DomainException(message, ExitCodes.VALIDATION)

class BookNotFoundException(query: String) :
    DomainException("No book matches '$query'. Run 'readwell list' to see available IDs.", ExitCodes.NOT_FOUND)

class AmbiguousBookException(query: String, count: Int) :
    DomainException("ID prefix '$query' matches $count books. Use at least four characters.", ExitCodes.NOT_FOUND)

class DuplicateBookException(title: String, author: String) :
    DomainException("'$title' by $author is already in the reading list.", ExitCodes.CONFLICT)

class InvalidTransitionException(message: String) : DomainException(message, ExitCodes.INVALID_STATE)

class PersistenceException(message: String, cause: Throwable? = null) :
    DomainException(message, ExitCodes.PERSISTENCE) {
    init {
        cause?.let(::initCause)
    }
}

object ExitCodes {
    const val USAGE = 2
    const val VALIDATION = 3
    const val NOT_FOUND = 4
    const val CONFLICT = 5
    const val INVALID_STATE = 6
    const val PERSISTENCE = 7
}

