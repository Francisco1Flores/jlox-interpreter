package com.craftinginterpreters.lox;

import java.util.List;

abstract class Expr {
 static class Binary extends Expr {
    final Expr left;
    final  Token operator;
    final  Expr right;
public static Binary(Expr left, Token operator, Expr right) {
this.left = left;
this.Token = Token;
this.Expr = Expr;
    }
 static class Grouping extends Expr {
    final Expr expression;
public static Grouping(Expr expression) {
this.expression = expression;
    }
 static class Literal extends Expr {
    final Object value;
public static Literal(Object value) {
this.value = value;
    }
 static class Unary extends Expr {
    final Token operator;
    final  Expr right;
public static Unary(Token operator, Expr right) {
this.operator = operator;
this.Expr = Expr;
    }
}
