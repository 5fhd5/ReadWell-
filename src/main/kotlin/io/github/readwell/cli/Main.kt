package io.github.readwell.cli

import io.github.readwell.domain.DomainException
import java.io.PrintWriter
import kotlin.system.exitProcess
import picocli.CommandLine
import picocli.CommandLine.ParameterException

fun main(args: Array<String>) {
    exitProcess(runApplication(args))
}

internal fun runApplication(
    args: Array<String>,
    root: RootCommand = RootCommand(),
    stdout: PrintWriter = PrintWriter(System.out, true),
    stderr: PrintWriter = PrintWriter(System.err, true),
): Int {
    val commandLine = CommandLine(root)
        .setOut(stdout)
        .setErr(stderr)
    commandLine.executionExceptionHandler = CommandLine.IExecutionExceptionHandler { exception, _, _ ->
        val domainException = exception as? DomainException
        if (domainException != null) {
            stderr.println("Error: ${domainException.message}")
            domainException.exitCode
        } else {
            stderr.println("Error: ${exception.message ?: exception::class.simpleName ?: "unexpected failure"}")
            1
        }
    }
    commandLine.parameterExceptionHandler = CommandLine.IParameterExceptionHandler { exception, _ ->
        stderr.println(exception.message)
        stderr.println("Try 'readwell --help' for usage.")
        2
    }
    @Suppress("SpreadOperator") // Picocli exposes its public execution API as a Java vararg.
    return try {
        commandLine.execute(*args)
    } catch (_: ParameterException) {
        2
    }
}
