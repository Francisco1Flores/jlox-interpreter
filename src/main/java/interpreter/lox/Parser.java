package interpreter.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static interpreter.lox.TokenType.*;

public class Parser {

    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    private boolean isBreakAvailable = false;
    private boolean isInsideParen = false;

    private boolean isFunction = false;


    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(CLASS)) {
                return classDeclaration();
            }
            if (match(FUN)) {
                isFunction = true;
                if (peek().type == IDENTIFIER) {
                    return funDeclaration("function");
                } else {
                    return statement();
                }
            }
            if (match(VAR)) {
                return varDeclaration();
            }
            if (match(IMPORT)) {
                return importDeclaration();
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");

        Expr.Variable superClass = null;
        if (match(LESS)) {
            consume(IDENTIFIER, "Expect superclass name after '<'.");
            superClass =  new Expr.Variable(previous());
        }


        consume(LEFT_BRACE, "Expect '{' before class body.");
        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            String kind = match(CLASS) ? "static method" : "method";
            methods.add((Stmt.Function)funDeclaration(kind));
        }
        consume(RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, superClass, methods);
    }

    private Stmt funDeclaration(String kind) {
        Token name = consume(IDENTIFIER, "Expect identifier after 'fun'.");
        consume(LEFT_PAREN, "Expect '(' after '" + name.lexeme +"'.");

        Expr function = function();
        return new Stmt.Function(name, function, kind);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect identifier after 'var'.");
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt importDeclaration() {
        Token moduleFileName = consume(STRING, "Expect identifier after 'import'.");
        Token alias = null;
        if (match(AS)) {
            alias = consume(IDENTIFIER, "Expect identifier after 'as'.");
        }
        consume(SEMICOLON, "Expect ';' after module import.");

        return new Stmt.Import(moduleFileName, alias);
    }

    private Stmt statement() {
        if (match(PRINT)) {
            return printStatement();
        }
        if (match(LEFT_BRACE)) {
            return new Stmt.Block(block());
        }
        if (match(IF)) {
            return ifStatement();
        }
        if (match(WHILE)) {
            return whileStatement();
        }
        if (match(FOR)) {
            return forStatement();
        }
        if (match(RETURN)) {
            return returnStmt();
        }
        if (match(BREAK)) {
            if (!isBreakAvailable) {
                throw error(previous(), "break statement must be inside a loop structure.");
            }
            return breakStatement();
        }
        return exprStatement();
    }

    private Stmt exprStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'if'.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN,"Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN,"Expect ')' after condition.");
        isBreakAvailable = true;
        Stmt body = statement();
        isBreakAvailable = false;
        return new Stmt.While(condition, body);
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = exprStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after for loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        isBreakAvailable = true;
        Stmt body = statement();
        isBreakAvailable = false;

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(
                    body,
                    new Stmt.Expression(increment)));
        }
        if (condition == null) {
            condition = new Expr.Literal(true);
        }

        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt returnStmt() {
        Token keyWord = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyWord, value);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block");
        return statements;
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt breakStatement() {
        Token name = previous();
        consume(SEMICOLON, "Expect ';' after 'break'.");

        return new Stmt.Break(name);
    }

    private Expr expression() {
        Expr expr = assignment();

        while (!isInsideParen && match(COMMA)) {
            expr = assignment();
        }
        return expr;
    }

    private Expr assignment() {
        Expr expr = ternary();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get) expr;
                return new Expr.Set(get.object, get.name, value);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }


    private Expr ternary() {
        Expr expr = logicOr();
        while (match(QUESTION_MARK)) {
            Expr left = expression();
            consume(COLON, "Expect ':' after expression.");
            Expr right = expression();
            return new Expr.Ternary(expr, left, right);
        }
        return expr;
    }

    private Expr logicOr() {
        Expr expr = logicAnd();
        while (match(OR)) {
            Token operator = previous();
            Expr rightHand = logicAnd();
            expr = new Expr.Logical(expr, operator, rightHand);
        }
        return expr;
    }

    private Expr logicAnd() {
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr rightHand = equality();
            expr = new Expr.Logical(expr, operator, rightHand);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(PLUS, MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(STAR, SLASH, PERCENT)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            }else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);

            } else {
                break;
            }
        }
        return expr;
    }

    private Expr primary() {
        if (match(FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(NIL)) {
            return new Expr.Literal(null);
        }
        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(SUPER)) {
            Token keyword = previous();
            consume(DOT, "Expect '.' after 'super'.");
            Token method = consume(IDENTIFIER, "Expect superclass method name.");
            return new Expr.Super(keyword, method);
        }
        if (match(THIS)) {
            return new Expr.This(previous());
        }
        if (match(FUN) || isFunction) {
            consume(LEFT_PAREN,"Expect '(' after 'fun'.");
            return function();
        }
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(QUESTION_MARK)) {
            Lox.error(peek(), "Expect expression before ? operator.");
            return new Expr.Literal(null);
        }
        throw error(peek(), "Expect expression.");
    }

    private Expr function() {
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters");
        consume(LEFT_BRACE, "Expect '{' before function  body.");
        isFunction = false;
        List<Stmt> body = block();
        return new Expr.AnFunction(parameters, body);
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            isInsideParen = true;
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        isInsideParen = false;
        return new Expr.Call(callee, paren, arguments);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) {
                return;
            }
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }

    // helper methods
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token advance() {
        if (!isAtEnd()) {
            return tokens.get(current++);
        }
        return previous();
    }

    private Token next() {
        if (!isAtEnd()) {
            return tokens.get(current + 1);
        }
        return previous();
    }

    private Token previous(){
        return tokens.get(current - 1);
    }
}
