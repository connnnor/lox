package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;
import static javax.management.Query.and;
import static javax.management.Query.eq;

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
            if (match(FUN)) { return function("function"); }
            if (match(VAR)) { return varDeclaration(); }
           return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Stmt statement() {
        if (match(FOR)) {        return forStatement(); }
        if (match(IF)) {         return ifStatement(); }
        if (match(PRINT)) {      return printStatement(); }
        if (match(RETURN)) {     return returnStatement(); }
        if (match(WHILE)) {      return whileStatement(); }
        if (match(LEFT_BRACE)) { return new Stmt.Block(block()); }
        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value;");
        return new Stmt.Return(keyword, value);
    }

    // desugar for loop into while loop
    private Stmt forStatement() {
        consume(LEFT_PAREN, "expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");
        Stmt body = statement();

        // exec increment after body in each iteration
        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)));
        }

        if (condition == null) { condition = new Expr.Literal(true); }
        body = new Stmt.While(condition, body);
        // run initializer once before loop
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after while condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
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
        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
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