package interpreter.lox;

import java.util.List;

abstract class Stmt {
    interface Visitor<R> {
        R visitVarStmt(Var stmt);
        R visitExpressionStmt(Expression stmt);
        R visitFunctionStmt(Function stmt);
        R visitIfStmt(If stmt);
        R visitWhileStmt(While stmt);
        R visitPrintStmt(Print stmt);
        R visitBlockStmt(Block stmt);
        R visitReturnStmt(Return stmt);
        R visitBreakStmt(Break stmt);
    }

    abstract <R> R accept(Visitor<R> visitor);

    static class Var extends Stmt {
        final Token name;
        final Expr initializer;

        public Var(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }
    }
    static class Expression extends Stmt {
        final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }
    static class Function extends Stmt {
        final Token name;
        final Expr function;

        public Function(Token name, Expr function) {
            this.name = name;
            this.function = function;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }
    }
    static class If extends Stmt {
        final Expr condition;
        final Stmt thenBranch;
        final Stmt elseBranch;

        public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }
    }
    static class While extends Stmt {
        final Expr condition;
        final Stmt body;

        public While(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitWhileStmt(this);
        }
    }
    static class Print extends Stmt {
        final Expr expression;

        public Print(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }
    }
    static class Block extends Stmt {
        final List<Stmt> statements;

        public Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }
    }
    static class Return extends Stmt {
        final Token keyWord;
        final Expr value;

        public Return(Token keyWord, Expr value) {
            this.keyWord = keyWord;
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitReturnStmt(this);
        }
    }
    static class Break extends Stmt {
        final Token name;

        public Break(Token name) {
            this.name = name;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBreakStmt(this);
        }
    }
}
