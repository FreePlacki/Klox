class Scanner(private val source: String) {
    private val tokens = mutableListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 0

    val keywords = mapOf(
        "and"    to TokenType.AND,
        "class"  to TokenType.CLASS,
        "else"   to TokenType.ELSE,
        "false"  to TokenType.FALSE,
        "for"    to TokenType.FOR,
        "fun"    to TokenType.FUN,
        "if"     to TokenType.IF,
        "nil"    to TokenType.NIL,
        "or"     to TokenType.OR,
        "print"  to TokenType.PRINT,
        "return" to TokenType.RETURN,
        "super"  to TokenType.SUPER,
        "this"   to TokenType.THIS,
        "true"   to TokenType.TRUE,
        "var"    to TokenType.VAR,
        "while"  to TokenType.WHILE
    )

    fun scanTokens(): MutableList<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens.add(Token(TokenType.EOF, "", null, line))
        return tokens
    }

    private fun isAtEnd() = current >= source.length

    private fun scanToken() {
        val c = advance()
        when (c) {
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)

            '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)

            '/' -> {
                if (match('/'))
                    while (peek() != '\n' && !isAtEnd())
                        advance()
                else if (match('*'))
                    blockComment()
                else
                    addToken(TokenType.SLASH)
            }

            ' ', '\r', '\t' -> return
            '\n' -> line++

            '"' -> string()

            else -> {
                if (c.isDigit()) number()
                else if (isAlpha(c)) identifier()
                else Klox.error(line, "Unexpected character.")
            }
        }
    }

    private fun advance() = source[current++]

    private fun peek(): Char =
        if (isAtEnd()) '\u0000'
        else source[current]

    private fun peekNext(): Char =
        if (current + 1 >= source.length) '\u0000'
        else source[current + 1]

    private fun match(c: Char): Boolean {
        if (isAtEnd() || source[current] != c)
            return false
        current++
        return true
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            Klox.error(line, "Unterminated string.")
            return
        }

        advance() // consume closing "
        addToken(TokenType.STRING,
            source.substring(start+1, current-1)) // trim ""
    }

    private fun number() {
        while(peek().isDigit())
            advance()
        if (peek() == '.' && peekNext().isDigit()) {
            advance()

            while (peek().isDigit()) advance()
        }

        addToken(TokenType.NUMBER,
            source.substring(start, current).toDouble())
    }

    private fun isAlpha(c: Char) =
        c in 'a'..'z' || c in 'A'..'Z' || c == '_'

    private fun identifier() {
        while (isAlpha(peek()) || peek().isDigit())
            advance()

        val type = keywords[source.substring(start, current)] ?: TokenType.IDENTIFIER
        addToken(type)
    }

    private fun blockComment() {
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                repeat(2) { advance() }
                return
            }
            if (advance() == '\n') line++
        }
    }


    private fun addToken(type: TokenType) = addToken(type, null)

    private fun addToken(type: TokenType, literal: Any?) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }
}
