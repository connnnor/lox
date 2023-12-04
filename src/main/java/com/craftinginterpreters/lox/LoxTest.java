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
        Assertions.assertEquals(expected, actual,msg);
    }

    private void runAndCompare(TestScript test) {
        runAndCompare(test.script, test.expected);
    }

    @org.junit.jupiter.api.Test
    void printTest() {
        final String script = """
                print "one";
                print true;
                print 2 + 1;
                """;
        final String expected = """
                one
                true
                3
                """;
        runAndCompare(script, expected);
    }
}