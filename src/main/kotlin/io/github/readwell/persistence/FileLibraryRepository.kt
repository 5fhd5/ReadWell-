package io.github.readwell.persistence

import io.github.readwell.domain.LibraryState
import io.github.readwell.domain.PersistenceException
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FileLibraryRepository(
    private val dataFile: Path,
    private val json: Json = DEFAULT_JSON,
) : LibraryRepository {
    override fun load(): LibraryState {
        if (Files.notExists(dataFile)) {
            return LibraryState()
        }
        return try {
            val contents = Files.readString(dataFile)
            val state = json.decodeFromString<LibraryState>(contents)
            if (state.schemaVersion != LibraryState.CURRENT_SCHEMA_VERSION) {
                throw PersistenceException(
                    "Unsupported data schema ${state.schemaVersion}; expected ${LibraryState.CURRENT_SCHEMA_VERSION}.",
                )
            }
            state
        } catch (exception: PersistenceException) {
            throw exception
        } catch (exception: IOException) {
            throw PersistenceException("Could not read data file '$dataFile': ${exception.message}", exception)
        } catch (exception: SerializationException) {
            throw PersistenceException("Data file '$dataFile' is not valid ReadWell JSON.", exception)
        }
    }

    override fun save(state: LibraryState) {
        val parent = dataFile.toAbsolutePath().parent
        val temporaryFile = dataFile.resolveSibling("${dataFile.fileName}.tmp")
        try {
            Files.createDirectories(parent)
            Files.writeString(temporaryFile, json.encodeToString(state))
            moveIntoPlace(temporaryFile)
        } catch (exception: IOException) {
            throw PersistenceException("Could not write data file '$dataFile': ${exception.message}", exception)
        } finally {
            Files.deleteIfExists(temporaryFile)
        }
    }

    private fun moveIntoPlace(temporaryFile: Path) {
        try {
            Files.move(
                temporaryFile,
                dataFile,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        private val DEFAULT_JSON = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = false
        }
    }
}

