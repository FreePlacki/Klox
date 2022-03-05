class Parser(private val tokens: List<Token>) {
    private class ParseError: RuntimeException()

    private var current = 0
    private var loopLevel = 0 // used for break and continue

    fun parse(): MutableList<Stmt?> {
        val statements = mutableListOf<Stmt?>()

        while (tokens[current].type != TokenType.EOF) {
            statements.add(declaration())
        }

        return statements
    }

    private fun declaration(): Stmt? {
        try {
            if (match(TokenType.VAR)) return varDeclaration()
            if (match(TokenType.FUN)) return function("function")
            return statement()
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun statement(): Stmt {
        return when {
            match(TokenType.IF)         -> ifStatement()
            match(TokenType.WHILE)      -> whileStatement()
            match(TokenType.FOR)        -> forStatement()
            match(TokenType.BREAK)      -> breakStatement()
            match(TokenType.CONTINUE)   -> continueStatement()
            match(TokenType.PRINT)      -> printStatement()
            match(TokenType.RETURN)     -> returnStatement()
            match(TokenType.LEFT_BRACE) -> Block(block())
            else                        -> expressionStatement()
        }
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'if' condition.")

        val thenBranch = statement()
        val elseBranch = if (match(TokenType.ELSE)) statement() else null

        return If(condition, thenBranch, elseBranch)
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'if' condition.")
        try {
            loopLevel++
            val body = statement()
            return While(condition, body)
        } finally {
            loopLevel--
        }
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")

        val initializer = if (match(TokenType.SEMICOLON)) null
                        else if (match(TokenType.VAR)) varDeclaration()
                        else expressionStatement()
        val condition = if (tokens[current].type == TokenType.SEMICOLON) null
                        else expression()
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.")
        val increment = if (tokens[current].type == TokenType.SEMICOLON) null
                        else expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after clauses.")

        try {
            loopLevel++
            var body = statement()

            if (increment != null) {
                body = Block(listOf(
                    body,
                    Expression(increment)
                ))
            }

            body = While(condition ?: Literal(true), body)

            if (initializer != null)
                body = Block(listOf(initializer, body))

            return body
        } finally {
            loopLevel--
        }
    }

    private fun breakStatement(): Stmt {
        if (loopLevel < 1)
            throw error(tokens[current - 1], "'break' only allowed inside a loop.")
        consume(TokenType.SEMICOLON, "Expect ';' after 'break' statement.")
        return Break
    }

    private fun continueStatement(): Stmt {
        if (loopLevel < 1)
            throw error(tokens[current - 1], "'continue' only allowed inside a loop.")
        consume(TokenType.SEMICOLON, "Expect ';' after 'continue' statement.")
        return Continue
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
        val initializer = if (match(TokenType.EQUAL)) expression() else null

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")

        return Var(name, initializer)
    }

    private fun function(kind: String): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")

        val parameters = mutableListOf<Token>()
        if (tokens[current].type != TokenType.RIGHT_PAREN) {
            do {
                if (parameters.size >= 255)
                    error(tokens[current + 1], "Can't have more than 255 arguments :(.")

                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")

        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()
        return Function(name, parameters, body)
    }

    private fun printStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Print(expr)
    }

    private fun returnStatement(): Stmt {
        val keyword = tokens[current - 1]
        val expr = if (tokens[current].type != TokenType.SEMICOLON) expression() else null

        consume(TokenType.SEMICOLON, "Expect ';' after return value.")
        return Return(keyword, expr)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Expression(expr)
    }

    private fun block(): List<Stmt?> {
        val statements = mutableListOf<Stmt?>()

        while (tokens[current].type != TokenType.EOF && tokens[current].type != TokenType.RIGHT_BRACE) {
            statements.add(declaration())
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun expression(): Expr = assignment()

    private fun assignment(): Expr {
        val expr = ternary()

        if (match(TokenType.EQUAL)) {
            val equals = tokens[current - 1]
            val value = assignment()

            if (expr is Variable) {
                return Assign(expr.name, value)
            }

            error(equals, "Invalid assignment target.")
        }

        return expr
    }

    private fun ternary(): Expr {
        var expr = or()

        if (match(TokenType.QUESTION_MARK)) {
            val thenBranch = expression()
            consume(TokenType.COLON, "Expect ':' after then branch of ternary expression.")
            val elseBranch = ternary()
            expr = Ternary(expr, thenBranch, elseBranch)
        }

        return expr
    }

    private fun or(): Expr {
        val expr = and()

        while (match(TokenType.OR)) {
            val operator = tokens[current - 1]
            val right = or()
            return Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expr {
        val expr = equality()

        while (match(TokenType.AND)) {
            val operator = tokens[current - 1]
            val right = equality()
            return Logical(expr, operator, right)
        }

        return expr
    }

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

        return call()
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (match(TokenType.LEFT_PAREN))
                expr = finishCall(expr)
            else
                break
        }

        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        while (tokens[current].type != TokenType.RIGHT_PAREN) {
            do {
                if (arguments.size >= 255)
                    error(tokens[current + 1], "Can't have more than 255 arguments :(.")
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }

        val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")

        return Call(callee, paren, arguments)
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

        if (match(TokenType.QUESTION_MARK)) {
            throw error(tokens[current - 1], "Missing left-hand condition of a ternary operator.")
        }

        if (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL, TokenType.GREATER,
                TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL,
                TokenType.PLUS, TokenType.STRING)) {
            throw error(tokens[current - 1], "Missing left-hand operand.")
        }

        if (match(TokenType.IDENTIFIER)) {
            return Variable(tokens[current - 1])
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