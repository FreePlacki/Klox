import java.io.File
import kotlin.system.exitProcess

object Klox {
    private val interpreter = Interpreter()

    var hadError = false
    var hadRuntimeError = false

    fun runFile(path: String) {
        val file = File(path)
        if (!file.exists()) {
            println("Cannot find file: $path")
            exitProcess(66)
        }
        run(file.readText())

        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
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
        val tokens = Scanner(source).scanTokens()
        val statements = Parser(tokens).parse()
        if (statements.isEmpty()) {
            hadError = true
            return
        }
        if (hadError) return

        interpreter.interpret(statements)
    }

    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    private fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error$where: $message")
    }

    fun error(token: Token, message: String) {
        report(token.line,
            if (token.type == TokenType.EOF) " at end"
            else " at '${token.lexeme}'",
            message
        )
    }

    fun runtimeError(error: RuntimeError) {
        System.err.println("[line ${error.token.line}] ${error.message}")
        hadRuntimeError = true
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