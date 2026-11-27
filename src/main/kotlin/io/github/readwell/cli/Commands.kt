package io.github.readwell.cli

import io.github.readwell.domain.BookFilter
import io.github.readwell.domain.BookSort
import io.github.readwell.domain.NewBook
import io.github.readwell.domain.Priority
import io.github.readwell.domain.ReadingListService
import io.github.readwell.domain.ReadingStatus
import io.github.readwell.domain.ValidationException
import java.util.concurrent.Callable
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.ParentCommand
import picocli.CommandLine.Parameters
import picocli.CommandLine.Spec
import picocli.CommandLine.Model.CommandSpec

private const val QUERY_DESCRIPTION = "Full ID or unique ID prefix."

@Command(name = "add", description = ["Add a book to the reading list."], mixinStandardHelpOptions = true)
class AddCommand : Callable<Int> {
    @ParentCommand
    lateinit var root: RootCommand

    @Spec
    lateinit var spec: CommandSpec

    @Option(names = ["--title"], required = true, paramLabel = "TEXT")
    lateinit var title: String

    @Option(names = ["--author"], required = true, paramLabel = "TEXT")
    lateinit var author: String

    @Option(names = ["--year"], required = true, paramLabel = "YEAR")
    var publicationYear: Int = 0

    @Option(names = ["--pages"], required = true, paramLabel = "COUNT")
    var pages: Int = 0

    @Option(names = ["--priority"], required = true, paramLabel = "LOW|MEDIUM|HIGH")
    lateinit var priority: Priority

    @Option(names = ["--tag"], paramLabel = "TAG")
    var tags: MutableList<String> = mutableListOf()

    override fun call(): Int {
        val book = root.service().add(
            NewBook(
                title = title,
                author = author,
                publicationYear = publicationYear,
                totalPages = pages,
                priority = priority,
                tags = tags.toSet(),
            ),
        )
        spec.commandLine().out.println("Added ${book.id.take(8)}: ${book.title}")
        return 0
    }
}

@Command(name = "list", description = ["List books by priority, title, or recency."], mixinStandardHelpOptions = true)
class ListCommand : Callable<Int> {
    @ParentCommand
    lateinit var root: RootCommand

    @Spec
    lateinit var spec: CommandSpec

    @Option(names = ["--status"], paramLabel = "STATUS")
    var status: ReadingStatus? = null

    @Option(names = ["--priority"], paramLabel = "PRIORITY")
    var priority: Priority? = null

    @Option(names = ["--sort"], paramLabel = "ORDER", defaultValue = "PRIORITY")
    var sort: BookSort = BookSort.PRIORITY

    override fun call(): Int {
        val books = root.service().list(BookFilter(status, priority, sort))
        spec.commandLine().out.println(TextFormat.bookTable(books))
        return 0
    }
}

@Command(name = "search", description = ["Search titles by prefix and authors or tags by text."], mixinStandardHelpOptions = true)
class SearchCommand : Callable<Int> {
    @ParentCommand
    lateinit var root: RootCommand

    @Spec
    lateinit var spec: CommandSpec

    @Parameters(index = "0", paramLabel = "QUERY", description = ["Text to find."])
    lateinit var query: String

    override fun call(): Int {
        val books = root.service().search(query)
        spec.commandLine().out.println(TextFormat.bookTable(books))
        return 0
    }
}

@Command(name = "show", description = ["Show all details for one book."], mixinStandardHelpOptions = true)
class ShowCommand : Callable<Int> {
    @ParentCommand
    lateinit var root: RootCommand

    @Spec
    lateinit var spec: CommandSpec

    @Parameters(index = "0", paramLabel = "ID", description = [QUERY_DESCRIPTION])
    lateinit var id: String

    override fun call(): Int {
        spec.commandLine().out.println(TextFormat.bookDetails(root.service().get(id)))
        return 0
    }
}

@Command(name = "progress", description = ["Record current-page progress for a READING book."], mixinStandardHelpOptions = true)
class ProgressCommand : Callable<Int> {
    @ParentCommand
    lateinit var root: RootCommand

    @Spec
    lateinit var spec: CommandSpec

    @Parameters(index = "0", paramLabel = "ID", description = [QUERY_DESCRIPTION])
    lateinit var id: String

    @Option(names = ["--pages"], required = true, paramLabel = "COUNT")
    var pages: Int = 0

    override fun call(): Int {
        val book = root.service().recordProgress(id, pages)
        spec.commandLine().out.println("Progress: ${book.progressPages}/${book.totalPages} (${book.progressPercent}%)")
        return 0
    }
}

@Command(name = "status", description = ["Move a book through its reading lifecycle."], mixinStandardHelpOptions = true)
class StatusCommand : Callable<Int> {
    @ParentCommand
    lateinit var root: RootCommand

    @Spec
    lateinit var spec: CommandSpec

    @Parameters(index = "0", paramLabel = "ID", description = [QUERY_DESCRIPTION])
    lateinit var id: String

    @Option(names = ["--set"], required = true, paramLabel = "STATUS")
    lateinit var status: ReadingStatus

    @Option(names = ["--rating"], paramLabel = "1-5")
    var rating: Int? = null

    override fun call(): Int {
        val book = root.service().changeStatus(id, status, rating)
        spec.commandLine().out.println("${book.title}: ${book.status}")
        return 0
    }
}

@Command(name = "remove", description = ["Remove a book after explicit confirmation."], mixinStandardHelpOptions = true)
class RemoveCommand : Callable<Int> {
    @ParentCommand
    lateinit var root: RootCommand

    @Spec
    lateinit var spec: CommandSpec

    @Parameters(index = "0", paramLabel = "ID", description = [QUERY_DESCRIPTION])
    lateinit var id: String

    @Option(names = ["--yes"], description = ["Confirm permanent removal."])
    var confirmed: Boolean = false

    override fun call(): Int {
        if (!confirmed) {
            throw ValidationException("Refusing to remove a book without --yes.")
        }
        val book = root.service().remove(id)
        spec.commandLine().out.println("Removed: ${book.title}")
        return 0
    }
}

@Command(name = "history", description = ["Show recent activity, newest first."], mixinStandardHelpOptions = true)
class HistoryCommand : Callable<Int> {
    @ParentCommand
    lateinit var root: RootCommand

    @Spec
    lateinit var spec: CommandSpec

    @Option(
        names = ["--limit"],
        paramLabel = "COUNT",
        defaultValue = "${ReadingListService.DEFAULT_HISTORY_LIMIT}",
    )
    var limit: Int = ReadingListService.DEFAULT_HISTORY_LIMIT

    override fun call(): Int {
        spec.commandLine().out.println(TextFormat.history(root.service().history(limit)))
        return 0
    }
}

@Command(name = "stats", description = ["Summarize the reading list."], mixinStandardHelpOptions = true)
class StatsCommand : Callable<Int> {
    @ParentCommand
    lateinit var root: RootCommand

    @Spec
    lateinit var spec: CommandSpec

    override fun call(): Int {
        spec.commandLine().out.println(TextFormat.statistics(root.service().statistics()))
        return 0
    }
}

