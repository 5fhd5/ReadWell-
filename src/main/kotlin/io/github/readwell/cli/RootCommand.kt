package io.github.readwell.cli

import io.github.readwell.domain.ReadingListService
import io.github.readwell.persistence.FileLibraryRepository
import java.nio.file.Path
import java.nio.file.Paths
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.ParameterException
import picocli.CommandLine.ScopeType
import picocli.CommandLine.Spec
import picocli.CommandLine.Model.CommandSpec

@Command(
    name = "readwell",
    mixinStandardHelpOptions = true,
    version = ["ReadWell 1.0.0"],
    description = ["A local, dependency-light reading-list manager."],
    subcommands = [
        AddCommand::class,
        ListCommand::class,
        SearchCommand::class,
        ShowCommand::class,
        ProgressCommand::class,
        StatusCommand::class,
        RemoveCommand::class,
        HistoryCommand::class,
        StatsCommand::class,
    ],
)
class RootCommand(
    internal val injectedService: ReadingListService? = null,
) : Runnable {
    @Spec
    lateinit var commandSpec: CommandSpec

    @Option(
        names = ["--data"],
        paramLabel = "FILE",
        scope = ScopeType.INHERIT,
        description = ["JSON data file (default: ~/.readwell/library.json)."],
    )
    var dataFile: Path? = null

    private var cachedService: ReadingListService? = null

    override fun run() {
        throw ParameterException(commandSpec.commandLine(), "Specify a command. Use --help to see commands.")
    }

    internal fun service(): ReadingListService =
        cachedService ?: (injectedService ?: ReadingListService(
            FileLibraryRepository(dataFile ?: defaultDataFile()),
        )).also { cachedService = it }

    private fun defaultDataFile(): Path =
        Paths.get(System.getProperty("user.home"), ".readwell", "library.json")
}

