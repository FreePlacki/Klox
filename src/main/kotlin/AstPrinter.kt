object AstPrinter {
   fun print(expr: Expr): String {
       return when (expr) {
           is Literal -> expr.value.toString()
           is Binary -> "(${expr.operator} ${print(expr.left)} ${print(expr.right)})"
           is Unary -> "(${expr.operator} ${print(expr.right)})"
           is Grouping -> "(group ${print(expr.expression)})"
           is Ternary -> "(${expr.expression} ? ${expr.thenBranch} : ${expr.elseBranch})"
           is Variable -> "(var ${expr.name})"
           is Assign -> "(assign ${expr.name} = ${expr.value})"
           is Logical -> "(${expr.left} ${expr.operator} ${expr.right})"
       }
   }
}

fun main() {
    val expr: Expr = Binary(
        Unary(
            Token(TokenType.MINUS, "-", null, 1),
            Literal(123)
        ),
        Token(TokenType.STAR, "*", null, 1),
        Grouping(
            Literal(45.67)
        )
    )

    println(AstPrinter.print(expr))
}
