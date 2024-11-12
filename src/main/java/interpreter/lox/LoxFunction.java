package interpreter.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {

    private final Expr.AnFunction declaration;
    private final Environment closure;
    private final String name;

    public LoxFunction(String name, Expr.AnFunction declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
        this.name = (name == null ? "anonymous" : name);
    }

    @Override
    public int arity() {
        return declaration.parameters.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.parameters.size(); i++) {
            environment.define(declaration.parameters.get(i).lexeme, arguments.get(i));
        }
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + name + ">";
    }
}
