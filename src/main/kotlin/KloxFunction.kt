class KloxFunction(private val declaration: Function, val closure: Environment): KloxCallable {
    override fun arity() = declaration.params.size

    override fun toString() = "<fn ${declaration.name.lexeme}>"

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        declaration.params.forEachIndexed { i, param ->
            environment.define(param.lexeme, arguments[i])
        }

        try {
            interpreter.execBlock(declaration.body, environment)
        } catch (returnValue: ReturnExc) {
            return returnValue.value
        }
        return null
    }
}