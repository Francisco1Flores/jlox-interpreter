package interpreter.lox;

import java.util.List;

abstract class Stmt {
    interface Visitor<R> {
        R visitVarStmt(Var stmt);
        R visitExpressionStmt(Expression stmt);
        R visitIfStmt(If stmt);
        R visitWhileStmt(While stmt);
        R visitPrintStmt(Print stmt);
        R visitBlockStmt(Block stmt);
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
}
