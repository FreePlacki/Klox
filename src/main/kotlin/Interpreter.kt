class Interpreter {
    private val globals = Environment()
    private val locals = mutableMapOf<Expr, Int>()
    private var environment = globals
    private class BreakJump: RuntimeException()
    private class ContinueJump: RuntimeException()

    init {
        class Clock: KloxCallable {
            override fun arity() = 0
            override fun call(interpreter: Interpreter, arguments: List<Any?>) = System.currentTimeMillis() / 1000.0
            override fun toString() = "<native fn>"
        }
        globals.define("clock", Clock())
    }

    fun interpret(statements: MutableList<Stmt?>, isRepl: Boolean = false) {
        try {
            for (stmt in statements) {
                if (isRepl && stmt is Expression) {
                    println(stringify(evaluate(stmt.expression)))
                }
                execute(stmt)
            }
        } catch (error: RuntimeError) {
            Klox.runtimeError(error)
        }
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    private fun execute(statement: Stmt?) {
        when (statement) {
            is Print      -> execPrint(statement)
            is Expression -> execExpression(statement)
            is Function   -> execFunction(statement)
            is Var        -> execVariable(statement)
            is Block      -> execBlock(statement.statements, Environment(environment))
            is If         -> execIf(statement)
            is While      -> execWhile(statement)
            is Break      -> execBreak()
            is Continue   -> execContinue()
            is Return     -> execReturn(statement)
            null -> return
        }
    }

    private fun execExpression(stmt: Expression) {
        evaluate(stmt.expression)
    }

    private fun execFunction(stmt: Function) {
        val function = KloxFunction(stmt, environment)
        environment.define(stmt.name!!.lexeme, function)
    }

    private fun execIf(stmt: If) {
        if (isTruthy(evaluate(stmt.condition)))
            execute(stmt.thenBranch)
        else if (stmt.elseBranch != null)
            execute(stmt.elseBranch)
    }

    private fun execWhile(stmt: While) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body)
            } catch (_: BreakJump) {
                break
            } catch (_: ContinueJump) {}
        }
    }

    private fun execBreak() {
        throw BreakJump()
    }

    private fun execContinue() {
        throw ContinueJump()
    }

    private fun execPrint(stmt: Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    private fun execVariable(stmt: Var) {
        val value = if (stmt.initializer != null) evaluate(stmt.initializer) else null
        if (value is Function)
            environment.define(stmt.name.lexeme, KloxFunction(value, environment))
        else
            environment.define(stmt.name.lexeme, value)
    }

    fun execBlock(statements: List<Stmt?>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment
            for (stmt in statements)
                execute(stmt)
        } finally { // we want to restore this.environment even in case of an error
            this.environment = previous
        }
    }

    private fun execReturn(stmt: Return) {
        val value = if (stmt.expression != null) evaluate(stmt.expression) else null

        throw ReturnExc(value)
    }

    private fun evaluate(expr: Expr): Any? {
        return when (expr) {
            is Literal  -> evalLiteral(expr)
            is Unary    -> evalUnary(expr)
            is Binary   -> evalBinary(expr)
            is Grouping -> evalGrouping(expr)
            is Ternary  -> evalTernary(expr)
            is Variable -> evalVariable(expr)
            is Assign   -> evalAssign(expr)
            is Logical  -> evalLogical(expr)
            is Call     -> evalCall(expr)
            is FunctionBody -> evalFuncBody(expr)
        }
    }

    private fun evalLiteral(expr: Literal): Any? {
        return expr.value
    }

    private fun evalGrouping(expr: Grouping): Any? {
        return evaluate(expr.expression)
    }

    private fun evalUnary(expr: Unary): Any? {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.BANG  -> !isTruthy(right)
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                -1 * (right as Double)
            }
            else            -> "Not implemented"
        }
    }

    private fun evalVariable(expr: Variable): Any? {
        return lookUpVariable(expr.name, expr)
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        if (distance != null)
            return environment.getAt(distance, name.lexeme)
        return globals.get(name)
    }

    private fun evalAssign(expr: Assign): Any? {
        val value = evaluate(expr.value)

        val distance = locals[expr]
        if (distance != null)
            environment.assignAt(distance, expr.name, value)
        else
            environment.assign(expr.name, value)

        return value
    }

    private fun evalLogical(expr: Logical): Any? {
        val left = evaluate(expr.left)

        if (expr.operator.type == TokenType.OR)
            if (isTruthy(left)) return left
        else
            if (!isTruthy(left)) return left

        return evaluate(expr.right)
    }

    private fun evalCall(expr: Call): Any? {
        val callee = evaluate(expr.callee)
        val arguments = expr.arguments.map { evaluate(it) }

        if (callee !is KloxCallable)
            throw RuntimeError(expr.paren, "Can only call functions and classes.")
        if (arguments.size != callee.arity())
            throw RuntimeError(expr.paren, "Expected ${callee.arity()} arguments but got ${arguments.size}.")

        return callee.call(this, arguments)
    }

    private fun evalFuncBody(expr: FunctionBody): Any? {
        return Function(null, expr)
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun isTruthy(value: Any?): Boolean {
        return when (value) {
            null       -> false
            is Boolean -> value
            else       -> true
        }
    }

    private fun evalBinary(expr: Binary): Any? {
        val left =  evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) - (right as Double)
            }
            TokenType.PLUS -> {
                if (left is String) {
                    var rightStr = right.toString()
                    if (rightStr.endsWith(".0"))
                        rightStr = rightStr.substring(0, rightStr.length - 2)
                    left + rightStr
                }
                else if (left is Double && right is Double)
                    left + right
                else
                    throw RuntimeError(expr.operator, "Operands must be two strings or two numbers.")
            }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                if ((right as Double) == 0.0)
                        throw RuntimeError(expr.operator, "It looks like you tried division by 0. Yeah better don't try this at home.")
                (left as Double) / right
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) * (right as Double)
            }

            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) > (right as Double)
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) >= (right as Double)
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) < (right as Double)
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                (left as Double) <= (right as Double)
            }

            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            TokenType.BANG_EQUAL -> !isEqual(left, right)

            else -> "Not implemented"
        }
    }

    private fun evalTernary(expr: Ternary): Any? {
        val condition = evaluate(expr.expression)

        if (condition as Boolean)
            return evaluate(expr.thenBranch)
        return evaluate(expr.elseBranch)
    }

    private fun isEqual(a: Any? ,b: Any?): Boolean {
        if (a == null && b == null)
            return true
        if (a == null)
            return false

        return a.equals(b)
    }

    private fun stringify(value: Any?): String {
        return when (value) {
            null -> "nil"
            is Double -> {
                val str = value.toString()
                if (str.endsWith(".0"))
                    str.substring(0, str.length - 2)
                str
            }
            else -> value.toString()
        }
    }
}