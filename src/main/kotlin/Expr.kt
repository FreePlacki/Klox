sealed class Expr

class Literal(val value: Any): Expr()
class Logical(val left: Expr, val operator: Token, val right: Expr): Expr()
class Unary(val operator: Token, val right: Expr): Expr()
class Binary(val left: Expr, val operator: Token, val right: Expr): Expr()
class Ternary(val expression: Expr, val thenBranch: Expr, val elseBranch: Expr): Expr()
class Grouping(val expression: Expr): Expr()
class Variable(val name: Token): Expr()
class Assign(val name: Token, val value: Expr): Expr()
class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>): Expr()
class FunctionBody(val params: List<Token>, val body: List<Stmt?>): Expr()
