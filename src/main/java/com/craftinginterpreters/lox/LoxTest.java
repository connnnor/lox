package com.craftinginterpreters.lox;

import org.junit.jupiter.api.Assertions;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

class LoxTest {
    public class TestScript {
        final String script;
        final String expected;

        public TestScript(String script, String expected) {
            this.script = script;
            this.expected = expected;
        }
    }

    /** remove prefix (if it exists) from string. */
    private static String removePrefix(String s, String prefix) {
        if (s != null && prefix != null && s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    private static boolean isScript(String s) {
        return s.startsWith("> ");
    }

    private static boolean isExpectedVal(String s) {
        return !isScript(s);
    }

    /**
     * Test runner for scripts.
     *
     * Redirect system out into variable.
     *
     * @param script Lox source text
     * @param expected expected system output
     */
    private String runner(String script) {
        PrintStream old = System.out;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(os));
            Lox.run(script);
            System.out.flush();
            return os.toString();
        } finally {
            // restore System.out
            System.setOut(old);
        }
    }

    /**
     * Run script and compare actual output to expected
     * @param script
     * @param expected
     */
    private void runAndCompare(String script, String expected) {
        final String actual = runner(script);
        final String msg = "running test script\n" + script;
        if (expected != null) {
            Assertions.assertEquals(expected.trim(), actual.trim(), msg);
        }
    }

    private void runAndCompare(TestScript test) {
        runAndCompare(test.script, test.expected);
    }

    // FIXME. currently limited because these are run as whole scripts
    // so ... env is not persisted like in repl
    private void runDocTestLine(String docTest) {
        String[] lines = docTest.split("\n");
        int index = 0;
        while (index < lines.length) {
            String script = "";
            String line = lines[index++];
            Assertions.assertTrue(
                    isScript(line),
                    "expected script line to start with '> '. Found '" + line + "'.");
            // extract script starting with '> '
            if (isScript(line)) {
                script = removePrefix(line, "> ");
            }
            String expected = "";
            // extract expected value
            line = lines[index];
            if (index < lines.length && isExpectedVal(line)) {
                expected = line;
                index++;
            }
            runAndCompare(script, expected);
        }
    }

    @org.junit.jupiter.api.Test
    void printTest() {
        runDocTestLine("""
                > print "one";
                one
                > print true;
                true
                > print 2 + 1;
                3
                """);
    }

    @org.junit.jupiter.api.Test
    void varTest() {
        runDocTestLine("""
                > var a = 1;
                > var b = 2;
                > print a + b;
                3
                """);
    }

    @org.junit.jupiter.api.Test
    void scopingTest() {
        final String script= """
                var a = "global a";
                var b = "global b";
                var c = "global c";
                {
                    var a = "outer a";
                    var b = "inner b";
                    {
                        var a = "inner a";
                        print a;
                        print b;
                        print c;
                    }
                    print a;
                    print b;
                    print c;
                }
                print a;
                print b;
                print c;
                """;
        final String expected = """
            inner a
            inner b
            global c
            outer a
            inner b
            global c
            global a
            global b
            global c
            """;
        runAndCompare(script, expected);
    }

    @org.junit.jupiter.api.Test
    void whileTest() {
        final String script= """
            var a = 2;
            while (a < 15) {
                print a;
                a = a + 2;
            }
            """;

        final String expected = """
                    2
                    4
                    6
                    8
                    10
                    12
                    14
                    """;
        runAndCompare(script, expected);
    }

    @org.junit.jupiter.api.Test
    void forTest() {
        final String script= """
            for (var i = 0; i < 5; i = i + 1) { print i; }
            """;

        final String expected = """
                    0
                    1
                    2
                    3
                    4
                    """;
        runAndCompare(script, expected);
    }

    @org.junit.jupiter.api.Test
    void orTest() {
        runDocTestLine("""
                > print "hi" or 2;
                hi
                > print nil or "yes";
                yes
                """);
    }

    @org.junit.jupiter.api.Test
    void ifTest() {
        final String script= """
            var a = 0;
            while (a < 5) {
                if (a < 3) {
                    print "a is less than 3";
                } else {
                    print "a is not less than 3";
                }
            a = a + 1;
            }
            """;

        final String expected = """
                    a is less than 3
                    a is less than 3
                    a is less than 3
                    a is not less than 3
                    a is not less than 3
                    """;
        runAndCompare(script, expected);
    }

}