package interpreter.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();

    private Environment environment = globals;

    private final Map<Expr, Integer> locals = new HashMap<>();

    private boolean stopLoop = false;

    public Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });

        globals.define("read", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Scanner scanner = new Scanner(System.in);
                return scanner.nextLine();
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });

        globals.define("readNumber", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Scanner scanner = new Scanner(System.in);
                double number;
                try {
                    number = scanner.nextDouble();
                } catch (Exception e) {
                    throw new RuntimeError(null, "Cannot convert input to a number.");
                }
                return number;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    public List<Object> interpretModule(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
        return List.of(globals, locals);
    }


    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case EQUAL_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return isEqual(left, right);
            case BANG_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return !isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String || right instanceof String) {
                    return  left.toString() +  right.toString();
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case PERCENT:
                checkNumberOperands(expr.operator, left, right);
                return (double) left % (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double)right == 0f) {
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }
                return (double) left / (double) right;
        }

        return null;
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object leftHand = evaluate(expr.leftHand);
        if (isTruthy(leftHand)) {
            return evaluate(expr.left);
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) {
                return left;
            }
        } else {
            if (!isTruthy(left)) {
                return left;
            }
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = locals.get(expr);
        LoxClass superClass = (LoxClass) environment.getAt(distance, "super");
        LoxInstance object = (LoxInstance) environment.getAt(distance - 1, "this");

        LoxFunction method = superClass.findMethod(expr.method.lexeme);
        if (method == null) {
            throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
        }
        return method.bind(object);
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitAnFunctionExpr(Expr.AnFunction expr) {
        return new LoxFunction(null, null, expr,environment, false);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren,"Can only call functions and classes.");
        }
        LoxCallable function = (LoxCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren,
                    "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }

        Object result;
        try {
            result = function.call(this, arguments);
        } catch (Exception e) {
            throw new RuntimeError(expr.paren, e.getMessage());
        }
        return result;
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);

        if (object instanceof LoxModule mod) {
            return mod.get(expr.name);
        }

        if (object instanceof LoxClass klass) {
            return klass.getMetaClass().get(expr.name);
        }

        if (object instanceof LoxInstance instance) {
            return instance.get(expr.name);
        }
        throw new RuntimeError(expr.name, "Only instances have properties.");
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
        //return environment.get(expr.name);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        Object result = evaluate(stmt.expression);
        if (Lox.promptmode) {
            System.out.println(result);
        }
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(
                stmt.name.lexeme,
                stmt.kind,
                (Expr.AnFunction)stmt.function,
                environment,
                false);

        if (stmt.name.lexeme != null) {
            environment.define(stmt.name.lexeme, function);
        }
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }
        throw new Return(value);
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        stopLoop = true;
        return null;
    }

    @Override
    public Void visitImportStmt(Stmt.Import stmt) {
        String name = (stmt.alias == null) ?
                stmt.name.lexeme.substring(1,stmt.name.lexeme.lastIndexOf('.'))
                : stmt.alias.lexeme;
        environment.define(name, new LoxModule(stmt.name));
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
            if (stopLoop) {
                break;
            }
        }
        stopLoop = false;
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {

        Object superClass = null;
        if (stmt.superclass != null) {
            superClass = evaluate(stmt.superclass);
            if (!(superClass instanceof LoxClass)) {
                throw new RuntimeError(stmt.superclass.name, "Superclass must be a class");
            }
        }

        environment.define(stmt.name.lexeme, null);

        if (stmt.superclass != null) {
            environment = new Environment(environment);
            environment.define("super", superClass);
        }

        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(
                        method.name.lexeme,
                        method.kind,
                        (Expr.AnFunction) method.function,
                        environment,
                        method.name.lexeme.equals("init")
            );
            if (methods.containsKey(method.name.lexeme)) {
                throw new RuntimeError(method.name, "Methods must have different names.");
            }
            methods.put(method.name.lexeme, function);
        }

        LoxClass klass = new LoxClass(stmt.name.lexeme,(LoxClass) superClass, methods);

        if (superClass != null) {
            environment = environment.enclosing;
        }

        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        if (environment.variableExist(stmt.name)) {
            throw new RuntimeError(stmt.name,"Variable '"+ stmt.name.lexeme +"' already exist in scope.");
        }
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        //environment.assign(expr.name, value);
        return value;
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    public void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
                if (stopLoop) {
                    break;
                }
            }
        } finally {
            this.environment = previous;
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    public void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    private String stringify(Object value) {
        if (value == null) {
            return "nil";
        }
        if (value instanceof Double) {
            String text = value.toString();
            if (text.endsWith(".0")) {
                return text.substring(0, text.length()-2);
            }
            return text;
        }
        return value.toString();
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
       if (object == null) {
           return false;
       }
       if (object instanceof Boolean) {
           return (boolean) object;
       }
       return true;
    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null) {
            return false;
        }
        return left.equals(right);
    }

}
