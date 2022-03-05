sealed class Stmt

class Expression(val expression: Expr): Stmt()
class Function(val name: Token, val params: List<Token>, val body: List<Stmt?>): Stmt()
class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?): Stmt()
class While(val condition: Expr, val body: Stmt): Stmt()
object Break: Stmt()
object Continue: Stmt()
class Print(val expression: Expr): Stmt()
class Return(val keyword: Token, val expression: Expr?): Stmt()
class Var(val name: Token, val initializer: Expr?): Stmt()
class Block(val statements: List<Stmt?>): Stmt()