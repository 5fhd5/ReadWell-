package io.github.readwell.cli

import io.github.readwell.domain.InMemoryRepository
import io.github.readwell.domain.ReadingListService
import io.github.readwell.domain.idSequence
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import picocli.CommandLine

class CliTest {
    @Test
    fun `parses add options and inherited data file`() {
        val root = RootCommand()
        val dataFile = Path.of("build", "test-data", "books.json")
        val commandLine = CommandLine(root)
        commandLine.parseArgs(
            "--data",
            dataFile.toString(),
            "add",
            "--title",
            "Dune",
            "--author",
            "Frank Herbert",
            "--year",
            "1965",
            "--pages",
            "412",
            "--priority",
            "HIGH",
            "--tag",
            "classic",
        )
        val add = commandLine.subcommands["add"]?.commandSpec?.userObject() as AddCommand

        assertEquals(dataFile, root.dataFile)
        assertEquals("Dune", add.title)
        assertEquals(412, add.pages)
        assertEquals(listOf("classic"), add.tags)
    }

    @Test
    fun `returns usage exit code for parsing errors`() {
        val result = execute(
            root = RootCommand(service()),
            "add",
            "--title",
            "Missing other options",
        )
        assertEquals(2, result.exitCode)
        assertTrue(result.stderr.contains("Missing required"))
    }

    @Test
    fun `returns usage error when enum cannot be parsed`() {
        val result = execute(
            root = RootCommand(service()),
            "list",
            "--status",
            "SOMEDAY",
        )
        assertEquals(2, result.exitCode)
        assertTrue(result.stderr.contains("expected one of"))
    }

    @Test
    fun `run without command prints usage error`() {
        val result = execute(root = RootCommand(service()))
        assertEquals(2, result.exitCode)
        assertTrue(result.stderr.contains("Specify a command"))
    }

    @Test
    fun `executes complete lifecycle through cli`() {
        val service = service()
        val root = RootCommand(service)
        val added = execute(
            root,
            "add",
            "--title",
            "The Left Hand of Darkness",
            "--author",
            "Ursula K. Le Guin",
            "--year",
            "1969",
            "--pages",
            "300",
            "--priority",
            "HIGH",
            "--tag",
            "science-fiction",
        )
        assertEquals(0, added.exitCode)
        val shortId = added.stdout.substringAfter("Added ").substringBefore(":")

        assertEquals(0, execute(root, "list", "--sort", "TITLE").exitCode)
        assertEquals(0, execute(root, "list", "--priority", "HIGH", "--status", "TO_READ").exitCode)
        assertEquals(0, execute(root, "search", "left").exitCode)
        assertEquals(0, execute(root, "show", shortId).exitCode)
        assertEquals(0, execute(root, "status", shortId, "--set", "READING").exitCode)
        assertEquals(0, execute(root, "progress", shortId, "--pages", "120").exitCode)
        assertEquals(0, execute(root, "status", shortId, "--set", "FINISHED", "--rating", "5").exitCode)
        assertTrue(execute(root, "stats").stdout.contains("FINISHED"))
        assertTrue(execute(root, "history", "--limit", "10").stdout.contains("STATUS_CHANGED"))
    }

    @Test
    fun `remove requires confirmation and emits domain exit codes`() {
        val root = RootCommand(service())
        val added = execute(
            root,
            "add",
            "--title",
            "Removable",
            "--author",
            "Author",
            "--year",
            "2020",
            "--pages",
            "100",
            "--priority",
            "LOW",
        )
        val shortId = added.stdout.substringAfter("Added ").substringBefore(":")

        val refused = execute(root, "remove", shortId)
        assertEquals(3, refused.exitCode)
        assertTrue(refused.stderr.contains("--yes"))
        assertEquals(0, execute(root, "remove", shortId, "--yes").exitCode)
        assertEquals(4, execute(root, "show", shortId).exitCode)
    }

    @Test
    fun `help and version are available`() {
        val root = RootCommand(service())
        assertTrue(execute(root, "--help").stdout.contains("Usage: readwell"))
        assertTrue(execute(root, "--version").stdout.contains("ReadWell 1.0.0"))
    }

    private fun service(): ReadingListService =
        ReadingListService(InMemoryRepository(), idFactory = idSequence("abcdef00"))

    private fun execute(root: RootCommand, vararg args: String): CommandResult {
        val stdout = StringWriter()
        val stderr = StringWriter()
        val exitCode = runApplication(
            args = args.toList().toTypedArray(),
            root = root,
            stdout = PrintWriter(stdout, true),
            stderr = PrintWriter(stderr, true),
        )
        return CommandResult(exitCode, stdout.toString(), stderr.toString())
    }

    private data class CommandResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
    )
}
