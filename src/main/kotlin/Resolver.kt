class Resolver(private val interpreter: Interpreter) {
    private val scopes = ArrayDeque<MutableMap<String, Boolean>>()
    private var currentFunction = FunctionType.NONE
    private var insideLoop = false

    enum class FunctionType {
        FUNCTION,
        NONE
    }

    fun resolve(statements: List<Stmt?>) {
        for (stmt in statements)
            resolve(stmt)
    }

    private fun resolve(statement: Stmt?) {
        when (statement) {
            is Block      -> resBlock(statement)
            is Var        -> resVar(statement)
            is Function   -> resFunction(statement)
            is Expression -> resExpression(statement)
            is If         -> resIf(statement)
            is Print      -> resPrint(statement)
            is Return     -> resReturn(statement)
            is While      -> resWhile(statement)
            is Break      -> resBreak(statement)
            is Continue   -> resContinue(statement)
            null -> return
        }
    }

    private fun resContinue(stmt: Continue) {
        if (!insideLoop)
            Klox.error(stmt.keyword, "'continue' only allowed inside a loop.")
    }

    private fun resBreak(stmt: Break) {
        if (!insideLoop)
            Klox.error(stmt.keyword, "'break' only allowed inside a loop.")
    }

    private fun resolve(expression: Expr?) {
        when (expression) {
            is Variable     -> resVariable(expression)
            is Assign       -> resAssign(expression)
            is Binary       -> resBinary(expression)
            is Call         -> resCall(expression)
            is Grouping     -> resGrouping(expression)
            is Literal      -> resLiteral(expression)
            is Logical      -> resLogical(expression)
            is Unary        -> resUnary(expression)
            is Ternary      -> resTernary(expression)
            is FunctionBody -> resFuncBody(expression)
            null -> return
        }
    }

    private fun resFuncBody(expr: FunctionBody) {
        val enclosingFunc = currentFunction
        currentFunction = FunctionType.FUNCTION

        beginScope()
        for (param in expr.params) {
            declare(param)
            define(param)
        }
        resolve(expr.body)
        endScope()
        currentFunction = enclosingFunc
    }

    private fun resTernary(expr: Ternary) {
        resolve(expr.expression)
        resolve(expr.thenBranch)
        resolve(expr.elseBranch)
    }

    private fun resUnary(expr: Unary) {
        resolve(expr.right)
    }

    private fun resLogical(expr: Logical) {
        resolve(expr.right)
        resolve(expr.left)
    }

    private fun resLiteral(expr: Literal) {}

    private fun resGrouping(expr: Grouping) {
        resolve(expr.expression)
    }

    private fun resCall(expr: Call) {
        resolve(expr.callee)
        for (arg in expr.arguments)
            resolve(arg)
    }

    private fun resBinary(expr: Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    private fun resBlock(stmt: Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    private fun resVar(stmt: Var) {
        declare(stmt.name)
        if (stmt.initializer != null)
            resolve(stmt.initializer)
        define(stmt.name)
    }

    private fun resVariable(expr: Variable) {
        if (!scopes.isEmpty() && scopes.last()[expr.name.lexeme] == false) {
            Klox.error(expr.name, "Can't read variable in its own initializer.")
        }

        resolveLocal(expr, expr.name)
    }

    private fun resAssign(expr: Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for ((i, v) in scopes.reversed().withIndex()) {
            if (v.containsKey(name.lexeme)) {
                interpreter.resolve(expr, i)
                return
            }
        }
    }

    private fun resFunction(stmt: Function) {
        declare(stmt.name!!)
        define(stmt.name)

        val enclosingFunc = currentFunction
        currentFunction = FunctionType.FUNCTION

        beginScope()
        for (param in stmt.funBody.params) {
            declare(param)
            define(param)
        }
        resolve(stmt.funBody.body)
        endScope()
        currentFunction = enclosingFunc
    }

    private fun resExpression(stmt: Expression) {
        resolve(stmt.expression)
    }

    private fun resPrint(stmt: Print) {
        resolve(stmt.expression)
    }

    private fun resReturn(stmt: Return) {
        if (currentFunction == FunctionType.NONE)
            Klox.error(stmt.keyword, "Can't return from top-level.")
        if (stmt.expression != null)
            resolve(stmt.expression)
    }

    private fun resIf(stmt: If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null)
            resolve(stmt.elseBranch)
    }

    private fun resWhile(stmt: While) {
        val prevInsideLoop = insideLoop
        insideLoop = true

        resolve(stmt.condition)
        resolve(stmt.body)

        insideLoop = prevInsideLoop
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return

        if (scopes.last().containsKey(name.lexeme))
            Klox.error(name, "Variable with this name already exists in this scope.")

        val scope = scopes.last()
        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (!scopes.isEmpty()) {
            scopes.last()[name.lexeme] = true
        }
    }

    private fun beginScope() = scopes.addLast(mutableMapOf())

    private fun endScope() = scopes.removeLast()

}