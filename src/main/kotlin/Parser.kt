class Parser(private val tokens: List<Token>) {
    private class ParseError: RuntimeException()

    private var current = 0

    fun parse(): Expr? {
        return try {
            expression()
        } catch (e: ParseError) {
            null
        }
    }

    fun expression(): Expr = equality()

    private fun equality(): Expr {
        var expr = comparison()

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = tokens[current - 1]
            val right = comparison()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val operator = tokens[current - 1]
            val right = term()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = tokens[current - 1]
            val right = factor()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(TokenType.STAR, TokenType.SLASH)) {
            val operator = tokens[current - 1]
            val right = unary()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = tokens[current - 1]
            val right = unary()
            return Unary(operator, right)
        }
        return primary()
    }

    private fun primary(): Expr {
        if (match(TokenType.TRUE)) return Literal("true")
        if (match(TokenType.FALSE)) return Literal("false")
        if (match(TokenType.NIL)) return Literal("nil")

        if (match(TokenType.STRING, TokenType.NUMBER))
            return Literal(tokens[current - 1].literal!!)

        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
            return Grouping(expr)
        }

        throw error(tokens[current], "Expect expression.")
    }

    private fun consume(type: TokenType, message: String): Token {
        if (type == tokens[current].type)
            return advance()

        throw error(tokens[current], message)
    }

    private fun error(token: Token, message: String): ParseError {
        Klox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()

        while (tokens[current].type != TokenType.EOF) {
            if (tokens[current - 1].type == TokenType.SEMICOLON)
                return

            if (tokens[current].type in listOf(
                    TokenType.CLASS,
                    TokenType.FUN,
                    TokenType.VAR,
                    TokenType.FOR,
                    TokenType.IF,
                    TokenType.WHILE,
                    TokenType.PRINT,
                    TokenType.RETURN))
                return

            advance()
        }
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (tokens[current].type != TokenType.EOF
                && tokens[current].type == type) {
                advance()
                return true
            }
        }

        return false
    }

    private fun advance(): Token {
        if (tokens[current].type != TokenType.EOF)
            current++
        return tokens[current - 1]
    }
}