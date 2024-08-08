package interpreter.lox;

import java.util.*;
import static interpreter.lox.TokenType.*;

public class Scanner {

    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static Map<String, TokenType> keyWords;

    static {
        keyWords = new HashMap<>();
        keyWords.put("and",    AND);
        keyWords.put("class",  CLASS);
        keyWords.put("else",   ELSE);
        keyWords.put("false",  FALSE);
        keyWords.put("for",    FOR);
        keyWords.put("fun",    FUN);
        keyWords.put("if",     IF);
        keyWords.put("nil",    NIL);
        keyWords.put("or",     OR);
        keyWords.put("print",  PRINT);
        keyWords.put("return", RETURN);
        keyWords.put("super",  SUPER);
        keyWords.put("this",   THIS);
        keyWords.put("true",   TRUE);
        keyWords.put("var",    VAR);
        keyWords.put("while",  WHILE);
    }

    public Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '?': addToken(QUESTION_MARK); break;
            case ':': addToken(COLON); break;
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '/':
                // this checks if there is a / or * after first / to discard comments
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else if (match('*')) {
                    while (peek() != '*' && !match('/') && !isAtEnd()) {
                        advance();
                    }
                    advance(2);
                } else {
                    addToken(SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n': line++; break;
            case '"' : string(); break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character: " + c + ".");
                }
                break;
        }
    }

    private char advance() {
        return source.charAt(current++);
    }

    private char advance(int positions) {
        current += positions;
        return source.charAt(current - positions);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String lexeme = source.substring(start, current);
        tokens.add(new Token(type, lexeme, literal, line));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }
        // Put current after the " that close the string
        advance();

        // Save the string in a token without the "
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private void number() {
        while (isDigit(peek())) {
            advance();
        }
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) {
                advance();
            }
        }
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        String value = source.substring(start, current);
        TokenType type = keyWords.get(value);
        if (type == null) {
            type = IDENTIFIER;
        }
       addToken(type);
    }

    private boolean match(char c) {
        if (isAtEnd()) return false;
        if (source.charAt(current ) != c) return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <='9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
       return isDigit(c) || isAlpha(c);
    }
}
