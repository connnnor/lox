package com.craftinginterpreters.lox;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

class LoxTests {
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

    private static boolean isStart(String s) {
        return s.startsWith(">>>");
    }

    private static boolean isContinuation(String s) {
        return s.startsWith("...");
    }

    private static boolean isExpected(String s) {
        return !(isStart(s) || isContinuation(s));
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

    private void runDocTest(String docTest) {
        String[] lines = docTest.split("\n");
        int index = 0;
        String script = "";
        String expected = "";
        while (index < lines.length) {
            String line = lines[index++];
            if (isStart(line)) {
                script = removePrefix(line, ">>>") + "\n";
                expected = "";
                while (index < lines.length && isContinuation(lines[index])) {
                    script += removePrefix(lines[index],"...") + "\n";
                    index++;
                }
            }
            while (index < lines.length && isExpected(lines[index])) {
                expected += lines[index] + "\n";
                index++;
            }
            runAndCompare(script, expected);
        }
    }

    @Test
    void printTest() {
        runDocTest("""
                >>> print "one";
                one
                >>> print true;
                true
                >>> print 2 + 1;
                3
                """);
    }

    @Test
    void varTest() {
        runDocTest("""
                >>> var a = 1;
                >>> var b = 2;
                >>> print a + b;
                3
                """);
    }

    @Test
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

    @Test
    void whileTest() {
        runDocTest("""
            >>> var a = 2;
            >>> while (a < 15) {
            ...     print a;
            ...     a = a + 2;
            ... }
            2
            4
            6
            8
            10
            12
            14
            """);
    }

    @Test
    void forTest() {
        runDocTest("""
            >>> for (var i = 0; i < 5; i = i + 1) { print i; }
            0
            1
            2
            3
            4
            """);
    }

    @Test
    void orTest() {
        runDocTest("""
                >>> print "hi" or 2;
                hi
                >>> print nil or "yes";
                yes
                """);
    }

    @Test
    void ifTest() {
        runDocTest("""
            >>> var a = 0;
            >>> while (a < 5) {
            ...     if (a < 3) {
            ...         print "a is less than 3";
            ...     } else {
            ...         print "a is not less than 3";
            ...     }
            ... a = a + 1;
            ... }
            a is less than 3
            a is less than 3
            a is less than 3
            a is not less than 3
            a is not less than 3
            """);
    }

    @Test
    void recFunTest() {
        runDocTest("""
            >>> fun count(n) {
            ...     if (n > 1) count(n - 1);
            ...     print n;
            ... }
            >>> count(3);
            1
            2
            3
            """);
    }

    @Test
    void fibFunTest() {
        runDocTest("""
            >>> fun count(n) {
            ...   if (n > 1) count(n - 1);
            ...   print n;
            ... }
            >>> fun fib(n) {
            ...     if (n <= 1) return n;
            ...     return fib(n-2) + fib(n-1);
            ... }
            >>> for (var i = 0; i < 7; i = i + 1) {
            ...     print fib(i);
            ... }
            0
            1
            1
            2
            3
            5
            8
            """);
    }

    @Test
    void closureTest() {
        runDocTest("""
            >>> fun makeCounter() {
            ...   var i = 0;
            ...   fun count() {
            ...     i = i + 1;
            ...     print i;
            ...   }
            ...   return count;
            ... }
            >>> var counter = makeCounter();
            >>> counter();
            1
            >>> counter();
            2
            """);
    }

    @Test
    void closureBugTest() {
        runDocTest("""
            >>> var a = "global";
            ... {
            ...   fun showA() {
            ...     print a;
            ...   }
            ...   showA();
            ...   var a = "block";
            ...   showA();
            ... }
            global
            global
            """);
    }
}