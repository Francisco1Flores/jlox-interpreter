package interpreter.lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private LoxClass klass;

    private Map<String, Object> fields = new HashMap<>();

    public LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    public Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        LoxFunction method = klass.findMethod(name.lexeme);
        if (method != null) {
            return method.bind(this);
        }
        if (klass.name.contains("meta")) {
            throw new RuntimeError(name, "Only can call static methods from classes.");
        }

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    public void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    public void setKlass(LoxClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return "<" + klass.name + " instance>";
    }
}