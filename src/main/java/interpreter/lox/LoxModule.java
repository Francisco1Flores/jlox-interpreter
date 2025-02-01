package interpreter.lox;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class LoxModule {
    Environment environment;
    Map<Expr, Integer> locals;

    public LoxModule(Token name) {
        List<Object> env;
        try {
            env = Lox.runModuleFile(name.literal.toString());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeError(name, "Error accessing module '" + name.literal + "'");
        }
        environment = (Environment) env.get(0);
        locals = (Map<Expr, Integer>) env.get(1);
    }

    public Object get(Token name) {
        return environment.get(name);
    }
}
