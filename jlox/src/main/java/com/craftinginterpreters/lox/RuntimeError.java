package com.craftinginterpreters.lox;

public class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        super("RuntimeError: " + message);
        this.token = token;
    }

    RuntimeError(String message) {
        this(null, message);
    }

    String getTokenMsg() {
        return token != null ? "\n[line" + token.line + "]" : "";
    }
}
