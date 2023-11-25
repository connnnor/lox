package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    static boolean hadError = false;

    static final int ARG_ERR = 64;
    static final int SCAN_ERR = 65;

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

        if (hadError) {
            System.exit(SCAN_ERR);
        }
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

        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void report(int line, String where, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("[line ").append(line).append("]");
        sb.append(" ");
        sb.append("Error").append(where).append(": ").append(message);
        System.err.println(sb.toString());
        hadError = true;
    }
}