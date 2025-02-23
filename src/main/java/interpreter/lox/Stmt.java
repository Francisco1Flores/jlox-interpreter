package interpreter.lox;

import java.util.List;

abstract class Stmt {
    interface Visitor<R> {
        R visitClassStmt(Class stmt);
        R visitVarStmt(Var stmt);
        R visitExpressionStmt(Expression stmt);
        R visitFunctionStmt(Function stmt);
        R visitIfStmt(If stmt);
        R visitWhileStmt(While stmt);
        R visitPrintStmt(Print stmt);
        R visitBlockStmt(Block stmt);
        R visitReturnStmt(Return stmt);
        R visitBreakStmt(Break stmt);
        R visitImportStmt(Import stmt);
    }

    abstract <R> R accept(Visitor<R> visitor);

    static class Class extends Stmt {
        final Token name;
        final Expr.Variable superclass ;
        final List<Stmt.Function> methods;

        public Class(Token name, Expr.Variable superclass , List<Stmt.Function> methods) {
            this.name = name;
            this.superclass = superclass;
            this.methods = methods;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitClassStmt(this);
        }
    }
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
        final String kind;

        public Function(Token name, Expr function, String kind) {
            this.name = name;
            this.function = function;
            this.kind = kind;
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
    static class Import extends Stmt {
        final Token name;
        final Token alias;

        public Import(Token name, Token alias) {
            this.name = name;
            this.alias = alias;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitImportStmt(this);
        }
    }
}
