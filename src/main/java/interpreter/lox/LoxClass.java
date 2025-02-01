package interpreter.lox;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LoxClass extends LoxInstance implements LoxCallable {
    private final LoxClass superClass;
    public final String name;
    private final Map<String, LoxFunction> methods;

    public LoxClass(String name, LoxClass superClass, Map<String, LoxFunction> methods) {
        super(null);
        this.superClass = superClass;
        this.name = name;
        this.methods = methods;
        if (!allStaticMethods(methods)) {
            this.setKlass(new LoxClass(name + " meta", superClass, staticMethods(methods)));
        } else {
            this.setKlass(this);
        }
    }

    public LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        if (superClass != null) {
            return superClass.findMethod(name);
        }

        return null;
    }

    public LoxInstance getMetaClass() {
        return this.getKlass();
    }

    @Override
    public String toString() {
        return "<class " + name + ">";
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }

    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer == null) {
            return 0;
        }
        return initializer.arity();
    }

    private boolean allStaticMethods(Map<String, LoxFunction> methods) {
        for (var e : methods.entrySet()) {
            if (e.getValue().getKind().equals("method")) {
                return false;
            }
        }
        return true;
    }

    private Map<String, LoxFunction> staticMethods(Map<String, LoxFunction> methods) {
        return methods.entrySet().stream()
                .filter(e -> e.getValue().getKind().equals("static method"))
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          Map.Entry::getValue));

    }

    private Map<String, LoxFunction> extractNonStatic(Map<String, LoxFunction> methods) {
        return methods.entrySet().stream()
                .filter(e -> e.getValue().getKind().equals("method"))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue));
    }
}
