import java.io.File
import kotlin.system.exitProcess

object Klox {
    var hadError = false

    fun runFile(path: String) {
        val file = File(path)
        if (!file.exists()) {
            println("Cannot find file: $path")
            exitProcess(66)
        }
        run(file.readText())

        if (hadError) exitProcess(65)
    }

    fun runPrompt() {
        println("Klox REPL [ctrl+D to quit]")
        while (true) {
            print("> ")
            val line: String = readLine() ?: break
            run(line)
            hadError = false
        }
    }

    private fun run(source: String) {
        println(Scanner(source).scanTokens().joinToString("\n"))
    }

    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error$where: $message")
    }
}

fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Usage: klox [script]")
        exitProcess(64)
    } else if (args.size == 1) {
        Klox.runFile(args.first())
    } else {
        Klox.runPrompt()
    }
}