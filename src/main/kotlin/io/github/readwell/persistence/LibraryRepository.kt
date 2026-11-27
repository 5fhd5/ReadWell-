package io.github.readwell.persistence

import io.github.readwell.domain.LibraryState

interface LibraryRepository {
    fun load(): LibraryState

    fun save(state: LibraryState)
}

