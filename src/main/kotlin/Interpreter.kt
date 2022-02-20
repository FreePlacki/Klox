class Interpreter {
    fun interpret(expr: Expr) {
        try {
            val value = evaluate(expr)
            println(stringify(value))
        } catch (error: RuntimeError) {
            Klox.runtimeError(error)
        }
    }


    private fun evaluate(expr: Expr): Any? {
        return when (expr) {
            is Literal  -> evalLiteral(expr)
            is Unary    -> evalUnary(expr)
            is Binary   -> evalBinary(expr)
            is Grouping -> evalGrouping(expr)
            is Ternary  -> evalTernary(expr)
        }
    }

    fun evalLiteral(expr: Literal): Any? {
        return expr.value
    }

    fun evalGrouping(expr: Grouping): Any? {
        return evaluate(expr.expression)
    }

    fun evalUnary(expr: Unary): Any? {
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

    fun evalBinary(expr: Binary): Any? {
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

    fun evalTernary(expr: Ternary): Any? {
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