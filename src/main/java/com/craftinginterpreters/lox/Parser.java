package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Grammar Rules are described in table.
 * Each rule is a method in this class. Eac rule must match expressions
 * at that precedence level or higher.
 * <p><br>
 * <br>Rules Table:
 * <pre>{@code
 * expression  → equality ;
 * equality    → comparison ( ( "!=" | "==" ) comparison)* ;
 * comparison  → term ( ( "<" | ">" | "<=" | ">=" ) term)* ;
 * term        → factor ( ( "+"  | "-"  | "*" | "/" ) factor)* ;
 * factor      → unary ( ( "/" | "*") unary)* ;
 * unary       → ( "!" | "-") unary
 *             | primary
 * primary     → NUMBER | STRING | "true" | "false" | "nil"
 *             | "(" expression ")" ;
 * }</pre>
 *
 * <p><br>
 * <table border="1">
 *   <tr> <th>Notation</th>     <th>Code Repr</th> </tr>
 *   <tr> <td>Terminal</td>     <td>Code to match & consume a token</td> </tr>
 *   <tr> <td>Non-Terminal</td> <td>Call to that rule's function</td> </tr>
 *   <tr> <td>|</td>            <td>if or switch statement</td> </tr>
 *   <tr> <td>* or +</td>       <td>while or for loop</td> </tr>
 *   <tr> <td>?</td>            <td>if statement</td> </tr>
 * </table>
 */
public class Parser {
    private static class ParseError extends RuntimeException {};
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Stmt declaration() {
        try {
           if (match(VAR)) { return varDeclaration(); }
           return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Expr expression() {
        return equality();
    }
    
    private Stmt statement() {
        if (match(PRINT)) { return printStatement(); }
        return expressionStatement();
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initalizer = null;
        if (match(EQUAL)) {
            initalizer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initalizer);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    /**
     *
     * <pre>{@code
     * expression  → equality ;
     * equality    → comparison ( ( "!=" | "==" ) comparison)* ;
     * }</pre>
     * @return
     */
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }


    /**
     * comparison rule.
     *
     * <pre>{@code
     * comparison  → term ( ( "<" | ">" | "<=" | ">=" ) term)* ;
     * }</pre>
     * @return
     */
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * terminal rule.
     *
     * <pre>{@code
     * term        → factor ( ( "+"  | "-"  | "*" | "/" ) factor)* ;
     * }</pre>
     * @return
     */
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     *
     * <pre>{@code
     * factor      → unary ( ( "/" | "*") unary)* ;
     * }</pre>
     * @return
     */
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * unary rule.
     * Note: this rule is a little different
     *
     * <pre>{@code
     * unary       → ( "!" | "-") unary
     *             | primary
     * }</pre>
     * @return
     */
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    /**
     * primary rule.
     *
     * primary     → NUMBER | STRING | "true" | "false" | "nil"
     *             | "(" expression ")" ;
     * <pre>{@code
     * }</pre>
     * @return
     */
    private Expr primary() {
        if (match(FALSE)) { return new Expr.Literal(false); }
        if (match(TRUE)) { return new Expr.Literal(true); }
        if (match(NIL)) { return new Expr.Literal(null); }

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    /**
     * Check to see if the current token has any of the given types.
     * If so, consume the tokens and return true. Otherwise, return false
     * and leave the current token alone.
     *
     * @param types the types to check
     * <pre>{@code
     * expression  → equality ;
     * }</pre>
     * @return true if current token matches any of given types
     */
    private boolean match(TokenType ... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Check to see if the next token is of expected type. If so, consume it.
     * If not, then we've hit an error
     *
     * @param type the type to check
     * @param message error message to display
     * @return consumed token
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) { return advance(); };
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) { return; }

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }

    }

    private Token advance() {
        if (!isAtEnd()) { current++; }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    /** Return true if the current token is of given type. */
    private boolean check(TokenType type) {
        if (isAtEnd()) { return false; }
        return peek().type == type;
    }
}