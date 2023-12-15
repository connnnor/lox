package com.craftinginterpreters.lox;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

    private static boolean isStart(String s) {
        return s.startsWith(">>>");
    }

    private static boolean isContinuation(String s) {
        return s.startsWith("...");
    }

    private static boolean isExpected(String s) {
        return !(isStart(s) || isContinuation(s));
    }

    @BeforeEach
    void init() {
        Lox.hadError = false;
    }

    /**
     * Test runner for scripts.
     * Redirect system out into variable.
     *
     * @param script Lox source text
     */
    private String runner(String script) {
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(os));
            System.setErr(new PrintStream(os));
            Lox.run(script);
            System.out.flush();
            System.err.flush();
            return os.toString();
        } finally {
            // restore System.out
            System.setOut(oldOut);
            System.setErr(oldErr);
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

    private void runAndComparePattern(String script, String expected) {
        final String actual = runner(script);
        if (expected != null) {
            Assertions.assertTrue(actual.strip().matches(expected),
                    "Actual string '" + actual +
                    "' does not match expected '" + expected + "'.");
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

    private String errorPattern(String msg) {
        return "(?s).*Error(.*)" + msg + "(.*)";
    }

    private String runtimeErrorPattern(String msg) {
        return "(?s).*RuntimeError(.*)" + msg + "(.*)";
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
    void unaryExprTest() {
        runDocTest("""
                >>> var a = 1;
                ... print -a;
                -1
                >>> var b = false;
                ... print !b;
                true
                """);
    }

    @Test
    void testCheckNumErrTest() {
        runAndComparePattern("""
                false < 1;
                """,
                runtimeErrorPattern("Operands must be a number"));
        Lox.hadError = false;
        runAndComparePattern("""
                -false;
                """,
                runtimeErrorPattern("Operand must be a number"));
    }

    @Test
    void shortCircuitTest() {
        runDocTest("""
                >>> fun sideEffect() {
                ...     print "sideEffect";
                ...     return true;
                ... }
                ... print false and sideEffect();
                false
                """);
    }


    @Test
    void binaryExprTest() {
        runDocTest("""
                >>> print 2 > 1; // greater
                true
                >>> print 1 > 2;
                false
                >>> print 2 >= 2; // greater-equal
                true
                >>> print 1 >= 2;
                false
                >>> print 2 < 1; // less
                false
                >>> print 1 < 2;
                true
                >>> print 1 <= 1; // less-equal
                true
                >>> print 2 <= 1;
                false
                >>> print "abc" == "def"; //  equal-equal
                false
                >>> print "abc" == "abc";
                true
                >>> print "abc" != "def"; //  bang-equal
                true
                >>> print "abc" != "abc";
                false
                >>> print 6 / 2; // div
                3
                >>> // adding strings to nums
                >>> var num = 6;
                ... print "your number is " + num;
                your number is 6
                >>> var num = 6;
                ... print num + " is your number";
                6 is your number
                """);
    }

    @Test
    void binaryExprErrTest() {
        runAndComparePattern("""
                false + 3;
                """,
                "(?s).*Error(.*)Operands must be two numbers or two strings(.*)");
        Lox.hadError = false;
    }

    @Test
    void groupingExprTest() {
        runDocTest("""
                >>> var a = 5;
                ... print ((a + 2) * 3);
                21
                """);
    }

    @Test
    void getExprErrTest() {
        runAndComparePattern("""
                "some string".property;
                """,
                "(?s).*Error(.*)Only instances have properties.(.*)");
    }

    @Test
    void setExprErrTest() {
        runAndComparePattern("""
                "some string".property = true;
                """,
                "(?s).*Error(.*)Only instances have fields.(.*)");
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
    void mutualRecursiveFuncTest() {
        runDocTest("""
            >>> fun isOdd(n) {
            ...   if (n == 0) return false;
            ...   return isEven(n - 1);
            ... }
            >>> fun isEven(n) {
            ...   if (n == 0) return true;
            ...   return isOdd(n - 1);
            ... }
            >>> print isOdd(3);
            true
            >>> print isEven(6);
            true
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

    @Test
    void localVarInitializerTest() {
        final String script = """
            var a = "outer";
            {
              var a = a;
            }
            """;

        runAndComparePattern(script, "(.*)Error(.*)Can't read local variable in its own initializer(.*)");
    }

    @Test
    void redefinitionErrTest() {
        final String script = """
            fun bad() {
              var a = "first";
              var a = "second";
            }
            """;

        runAndComparePattern(script, "(.*)Error(.*)Already a variable with this name(.*)");
    }

    @Test
    void returnErrTest() {
        final String script = """
            return "at top level";
            """;

        runAndComparePattern(script, "(.*)Error(.*)Can't return from top-level code(.*)");
    }

    // note: (?s) is needed for matching line breaks with '.' in regex
    @Test
    void callNonFunctionErrTest() {
        final String script = """
            "not a function"();
            """;
        runAndComparePattern(script, "(?s).*RuntimeError(.*)Can only call functions and classes(.*)");
    }

    @Test
    void callArityErrTest() {
        final String script = """
            fun square(n) { return n*n; }
            square();
            """;
        runAndComparePattern(script, runtimeErrorPattern("Expected 1 arguments but got 0"));
    }

    @Test
    void callNumArgsErrTest() {
        // function declaration
        StringBuilder sb = new StringBuilder();
        sb.append("fun tooMuchFun(");
        final int numArgs = 256;
        for (int i = 0; i < numArgs; i++) {
            sb.append("arg" + i + ",");
        }
        sb.deleteCharAt(sb.length() - 1); // remove last comma
        sb.append(") { print \"hi\"; }");
        String script = sb.toString();
        runAndComparePattern(script, errorPattern("Can't have more than 255 parameters."));

        Lox.hadError = false;

        // function call
        sb = new StringBuilder();
        sb.append("tooMuchFun(");
        for (int i = 0; i < numArgs; i++) {
            sb.append("arg" + i + ",");
        }
        sb.deleteCharAt(sb.length() - 1); // remove last comma
        sb.append(");");
        script = sb.toString();
        runAndComparePattern(script, errorPattern("Can't have more than 255 arguments."));
    }

    @Test
    void forInitializerTest() {
        runDocTest("""
            >>> var i = 0;
            ... for (; i < 3; i = i + 1) { // no initializer
            ...   print i;
            ... }
            0
            1
            2
            >>> var j = 5;
            >>> for (j = 0; j < 3; j = j + 1) { // expr statement
            ...   print j;
            ... }
            0
            1
            2
            >>> for (var k = 0; k < 3; k = k + 1) { // var declaration
            ...   print k;
            ... }
            0
            1
            2
            """);
    }

    @Test
    void primaryExprErrTest() {
        runAndComparePattern("""
                var a = @;
                """, errorPattern("Expect expression"));
    }

    @Test
    void unmatchedParenErrTest() {
        runAndComparePattern("""
                var a = (1 + 2;
                """, errorPattern("Expect '\\)' after expression."));
    }

    @Test
    void invalidAssignmentTest() {
        final String script = """
            var a = 1;
            var b = 2;
            var c = 3;
            a + b = c;
            """;
        runAndComparePattern(script, "(?s).*Error(.*)Invalid assignment target(.*)");
    }

    @Test
    void functionStrTest() {
        runDocTest("""
            >>> fun square(n) {
            ...   return n*n;
            ... }
            ... print square;
            <fn square>
            """);
    }

    @Test
    void basicClassTest() {
        runDocTest("""
            >>> class Bagel {}
            >>> var bagel = Bagel();
            >>> print Bagel;
            <class Bagel>
            >>> print bagel;
            <class Bagel instance>
            >>> bagel.topping = "cream cheese";
            >>> print bagel.topping;
            cream cheese
            """);
    }

    @Test
    void classUndefinedPropertyErrTest() {
        runAndComparePattern("""
            class Bagel {}
            var bagel = Bagel();
            print bagel.toasted;
            """, "(?s).*Error(.*)Undefined property 'toasted'(.*)");
    }

    @Test
    void classMethodTest() {
        // normal
        runDocTest("""
            >>> class Bacon {
            ...   eat() {
            ...     print "Crunch";
            ...   }
            ... }
            >>> Bacon().eat();
            Crunch
            """);
        // put method into object
        runDocTest("""
            >>> class Box {}
            >>> fun notMethod(argument) {
            ...   print "called function with " + argument;
            ... }
            >>> var box = Box();
            >>> box.function = notMethod;
            >>> box.function("argument");
            called function with argument
            """);
    }

    @Test
    void classThisTest() {
        runDocTest("""
            >>> class Cake {
            ...   taste() {
            ...     var adjective = "delicious";
            ...     print "The " + this.flavor + " cake is " + adjective + "!";
            ...   }
            ... }
            >>> var cake = Cake();
            >>> cake.flavor = "Chocolate";
            >>> cake.taste();
            The Chocolate cake is delicious!
            """);
    }

    @Test
    void classClosureTest() {
        runDocTest("""
                >>> class Egotist {
                ...   speak() {
                ...     print this;
                ...   }
                ... }
                >>> var method = Egotist().speak;
                >>> method();
                <class Egotist instance>
                """);
    }

    @Test
    void classThisErrTest() {
        runAndComparePattern("""
                print this;
                """,
                "(?s).*Error(.*)Can't use 'this' outside of a class(.*)");
        Lox.hadError = false;
        runAndComparePattern("""
                fun notAMethod() {
                  print this;
                }
                """,
                "(?s).*Error(.*)Can't use 'this' outside of a class(.*)");
    }

    @Test
    void classInitializerTest() {
        runDocTest("""
            >>> class Foo {
            ...   init() {
            ...     this.value = "bar";
            ...   }
            ... }
            ... var foo = Foo();
            ... print foo.value;
            bar
            """);
    }

    @Test
    void classInitializerErrTest() {
        final String script = """
            class Foo {
              init() {
                return "bad";
              }
            }
            """;
        runAndComparePattern(script, "(?s).*Error(.*)Can't return a value from an initializer(.*)");
    }

    @Test
    void builtInsTest() {
        runDocTest("""
            >>> var now = clock();
            >>> print clock;
            <native fn>
            """);
        runDocTest("""
            >>> var num = 49.34;
            ... print floor(num);
            49
            >>> print floor;
            <native fn>
            """);
    }

    @Test
    void floorArgErrTest() {
        runAndComparePattern("""
                floor("123.5");
                """,
                runtimeErrorPattern("floor argument must be double"));
    }

    @Test
    void inheritanceSelfErrTest() {
        runAndComparePattern("""
                class Bad < Bad {}
                """,
                errorPattern("A class can't inherit from itself"));
    }

    @Test
    void inheritanceParserErrTest() {
        runAndComparePattern("""
                class Bad < ! {}
                """,
                errorPattern("Expect superclass name"));
    }

    @Test
    void inheritanceNotClassErrTest() {
        runAndComparePattern("""
                var NotAClass = "a string";
                class Bad < NotAClass {}
                """,
                errorPattern("Superclass must be a class"));
    }

    @Test
    void inheritanceTest() {
        runDocTest("""
            >>> class Doughnut {
            ...   cook() {
            ...     print "Fry until golden brown.";
            ...   }
            ... }
            >>> class BostonCream < Doughnut {
            ...   cook() {
            ...     super.cook();
            ...     print "Pipe full of custard and coat with chocolate.";
            ...   }
            ... }
            >>> BostonCream().cook();
            Fry until golden brown.
            Pipe full of custard and coat with chocolate.
            """);
    }

    @Test
    void inheritanceSuperTest() {
        runDocTest("""
            >>> class A {
            ...   method() {
            ...     print "A method";
            ...   }
            ... }
            ... class B < A{
            ...   method() {
            ...     print "B method";
            ...   }
            ...   test() {
            ...     super.method();
            ...   }
            ... }
            ... class C < B {}
            ... C().test();
            A method
            """);
    }

    @Test
    void inheritanceSuperErrTest() {
        runAndComparePattern("""
                super.cook();
                """,
                errorPattern("Can't use 'super' outside of a class"));
        Lox.hadError = false;
        runAndComparePattern("""
                class Eclair {
                    cook() {
                        super.cook();
                        print "Pipe full of creme patisserie";
                    }
                }
                """,
                errorPattern("Can't use 'super' in a class with no superclass"));
        Lox.hadError = false;
        runAndComparePattern("""
                class A {}
                class B < A {
                    method() {
                        super.method();
                    }
                }
                B().method();
                """,
                runtimeErrorPattern("Undefined property 'method'"));
    }
}