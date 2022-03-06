class KloxFunction(private val declaration: Function, private val closure: Environment): KloxCallable {
    override fun arity() = declaration.funBody.params.size

    override fun toString() = "<fn ${declaration.name?.lexeme}>"

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        declaration.funBody.params.forEachIndexed { i, param ->
            if (arguments[i] is Function)
                environment.define(param.lexeme, KloxFunction(arguments[i] as Function, closure))
            else
                environment.define(param.lexeme, arguments[i])
        }

        try {
            interpreter.execBlock(declaration.funBody.body, environment)
        } catch (returnValue: ReturnExc) {
            return returnValue.value
        }
        return null
    }
}