package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    static final int ARG_ERR = 64;
    static final int SCAN_ERR = 65;
    static final int RUNTIME_ERR = 70;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: ./jlox [script]");
            System.exit(ARG_ERR);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate error in exit code
        if (hadError) { System.exit(SCAN_ERR); }
        if (hadRuntimeError) { System.exit(RUNTIME_ERR); }
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line  = reader.readLine();
            if (line == null) { break; }
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (hadError) { return; }

//        System.out.println(new AstPrinter().print(expression));
        interpreter.interpret(statements);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void report(int line, String where, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("[line ").append(line).append("]");
        sb.append(" ");
        sb.append("Error").append(where).append(": ").append(message);
        System.err.println(sb.toString());
        hadError = true;
    }

    public static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line" + error.token.line + "]");
        hadRuntimeError = true;
    }
}