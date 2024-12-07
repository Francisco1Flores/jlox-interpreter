package interpreter.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {

    private final Expr.AnFunction declaration;
    private final Environment closure;
    private final String kind;
    private final String name;

    private final boolean isInitializer;

    public LoxFunction(String name,
                       String kind,
                       Expr.AnFunction declaration,
                       Environment closure,
                       boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.kind = (name == null ? "function" : kind);
        this.name = (name == null ? "anonymous" : name);
        this.isInitializer = isInitializer;
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
            if (isInitializer) {
                return closure.getAt(0, "this");
            }
            return returnValue.value;
        }
        if (isInitializer) {
            return closure.getAt(0, "this");
        }
        return null;
    }

    public LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(this.name, this.kind, declaration, environment, isInitializer);
    }

    public String getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return "<"+ kind + " " + name + ">";
    }
}
