# TODO
# TODO use link below to write custom explanations for failures
# https://docs.pytest.org/en/7.1.x/how-to/assert.html

import pexpect
import tempfile
import re

# markers
# loop
# cond

import pytest

CLOX_TYPE = 'clox'
JLOX_TYPE = 'jlox'

BINS = {
    CLOX_TYPE : '/home/kang/workspace/lox/clox/clox',
    JLOX_TYPE : '/home/kang/workspace/lox/jlox/jlox',
}

@pytest.fixture(params=[CLOX_TYPE, JLOX_TYPE])
#@pytest.fixture(params=[JLOX_TYPE])
#@pytest.fixture(params=[CLOX_TYPE])
def lox_type(request):
    return request.param


def runLox(source, lox_type):
    with tempfile.NamedTemporaryFile(mode='w') as f:
        f.write(source)
        f.flush()
        bin_path = BINS[lox_type]
        output = pexpect.run(f'{bin_path} {f.name}')
        return output.decode('ascii')


def is_start(s):
    return s.startswith(">>>")


def is_continuation(s):
    return s.startswith("...")


def is_expected(s):
    return not (is_start(s) or is_continuation(s))


def strip_prefix(s, prefix):
    return s.removeprefix(prefix)


def remove_leading_spaces(l: list[str]):
    return [s.strip() for s in l]


def runDocTest(loxType, docTest: str):
    script = ""
    expected = ""
    lines = remove_leading_spaces(docTest.splitlines(True))
    index = 0
    while index < len(lines):
        line = lines[index]
        index += 1
        if is_start(line):
            script = line.removeprefix(">>>") + "\n"
            expected = ""
            while index < len(lines) and is_continuation(lines[index]):
                script += lines[index].removeprefix("...") + "\n"
                index += 1
        while index < len(lines) and is_expected(lines[index]):
            expected += lines[index] + '\n'
            index += 1
        runAndCompare(loxType, script, expected)

def normalize(s: str):
    return "\n".join(s.splitlines()).rstrip()

def runAndCompare(lox_type, script, expected):
    with open('script.lox', 'w') as f:
        f.write(script)
    actual = runLox(script, lox_type)
    if expected and not expected.isspace():
        assert normalize(actual) == normalize(expected)

def runAndComparePattern(lox_type, script, expectedPattern):
    actual = runLox(script, lox_type)
    if expectedPattern:
        assert bool(re.match(expectedPattern, normalize(actual)))

def pattern(msg):
    return r"(?s).*" + msg + "(.*)"

def contains(msg):
    return r"(?s).*" + msg + "(.*)"

def error_pattern(msg):
    return r"(?s).*Error(.*)" + msg + "(.*)"

def runtime_error_pattern(msg):
    return r"(?s).*RuntimeError(.*)" + msg + "(.*)"

#
# Tests
#

def test_unary_expr(lox_type):
    runDocTest(lox_type, """
        >>> var a = 1;
        ... print -a;
        -1
        >>> var b = false;
        ... print !b;
        true
        """)

def test_get_expr_err(lox_type):
    runAndComparePattern(lox_type, """
        "some string".property;
        """,
        error_pattern("Only instances have properties"))

def test_set_expr_err(lox_type):
    runAndComparePattern(lox_type, """
        "some string".property = true;
        """,
        error_pattern("Only instances have fields"))


def test_check_num_err(lox_type):
    runAndComparePattern(lox_type, """
        false < 1;
        """,
        runtime_error_pattern("Operands must be a number"))
    runAndComparePattern(lox_type, """
        -false;
        """,
        runtime_error_pattern("Operand must be a number"))

def test_short_circuit(lox_type):
    runDocTest(lox_type, """
        >>> fun sideEffect() {
        ...     print "sideEffect";
        ...     return true;
        ... }
        ... print false and sideEffect();
        false
        """)

def test_and_or(lox_type):
    # kinda hard to test this without functions, so, do something that 
    # would cause a runtime error
    runDocTest(lox_type, """
        >>> print false and (a == true);
        false
        >>> var a = false;
        ... print true and (a == true);
        false
        """)
    runDocTest(lox_type, """
        >>> print true or (a == true);
        true
        >>> var a = true;
        ... print false or (a == true);
        true
        """)

def test_binary_exprs(lox_type):
    runDocTest(lox_type, """
        >>> print 0 > 1; // greater
        false
        >>> print 1 > 1;
        false
        >>> print 2 > 1;
        true
        """)
    runDocTest(lox_type, """
        >>> print 0 >= 1; // greater-equal
        false
        >>> print 1 >= 1;
        true
        >>> print 2 >= 1;
        true
        """)
    runDocTest(lox_type, """
        >>> print 0 < 1; // less
        true
        >>> print 1 < 1; // less
        false
        >>> print 2 < 1;
        false
        """)
    runDocTest(lox_type, """
        >>> print 0 <= 1;
        true
        >>> print 1 <= 1;
        true
        >>> print 2 <= 1;
        false
        """)
    runDocTest(lox_type, """
        >>> print "abc" == "def"; //  equal-equal
        false
        >>> print "abc" == "abc";
        true
        >>> print 0 == 1;
        false
        >>> print 1 == 1;
        true
        """)
    runDocTest(lox_type, """
        >>> print "abc" != "def"; //  bang-equal
        true
        >>> print "abc" != "abc";
        false
        >>> print 1 != 0;
        true
        >>> print 1 != 1;
        false
        """)
#   runDocTest(lox_type, """
#       >>> print 6 / 2; // div
#       3
#       >>> // adding strings to nums
#       >>> var num = 6;
#       ... print "your number is " + num;
#       your number is 6
#       >>> var num = 6;
#       ... print num + " is your number";
#       6 is your number
#       """)

def test_scoping(lox_type):
    runDocTest(lox_type, """
        >>> var a = "global a";
        ... var b = "global b";
        ... var c = "global c";
        ... {
        ...     var a = "outer a";
        ...     var b = "inner b";
        ...     {
        ...         var a = "inner a";
        ...         print a;
        ...         print b;
        ...         print c;
        ...     }
        ...     print a;
        ...     print b;
        ...     print c;
        ... }
        ... print a;
        ... print b;
        ... print c;
        inner a
        inner b
        global c
        outer a
        inner b
        global c
        global a
        global b
        global c
        """)

def test_grouping_expr(lox_type):
    runDocTest(lox_type, """
            >>> var a = 5;
            ... print ((a + 2) * 3);
            21
            """)

@pytest.mark.loop
def test_while(lox_type):
    runDocTest(lox_type, """
        >>> var a = 2;
        ... while (a < 15) {
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
        """)


@pytest.mark.loop
def test_for(lox_type):
    runDocTest(lox_type, """
        >>> for (var i = 0; i < 5; i = i + 1) { print i; }
        0
        1
        2
        3
        4
        """)


@pytest.mark.cond
def test_or(lox_type):
    runDocTest(lox_type, """
            >>> print "hi" or 2;
            hi
            >>> print nil or "yes";
            yes
            """)

@pytest.mark.cond
def test_and(lox_type):
    # kinda hard to test this without functions, so, do something that 
    # would cause a runtime error
    runDocTest(lox_type, """
        >>> print false and "no";
        false
        >>> print true and "yes";
        yes
        """)

# commenting out because i have a better test for ifs that does not use while
#@pytest.mark.cond
#def test_if(lox_type):
#    runDocTest(lox_type, """
#        >>> var a = 0;
#        ... while (a < 5) {
#        ...     if (a < 3) {
#        ...         print "a is less than 3";
#        ...     } else {
#        ...         print "a is not less than 3";
#        ...     }
#        ... a = a + 1;
#        ... }
#        a is less than 3
#        a is less than 3
#        a is less than 3
#        a is not less than 3
#        a is not less than 3
#        """)


def test_rec_fun(lox_type):
    runDocTest(lox_type, """
        >>> fun count(n) {
        ...     if (n > 1) count(n - 1);
        ...     print n;
        ... }
        ... count(3);
        1
        2
        3
        """)

@pytest.mark.func
def test_mutual_recursive_func(lox_type):
    runDocTest(lox_type, """
        >>> fun isOdd(n) {
        ...   if (n == 0) return false;
        ...   return isEven(n - 1);
        ... }
        ... fun isEven(n) {
        ...   if (n == 0) return true;
        ...   return isOdd(n - 1);
        ... }
        ... print isOdd(3);
        ... print isEven(6);
        true
        true
        """)

@pytest.mark.func
def test_fib_fun(lox_type):
    runDocTest(lox_type, """
        >>> fun fib(n) {
        ...     if (n <= 1) return n;
        ...     return fib(n-2) + fib(n-1);
        ... }
        ... for (var i = 0; i < 7; i = i + 1) {
        ...     print fib(i);
        ... }
        0
        1
        1
        2
        3
        5
        8
        """)


def test_closure(lox_type):
    runDocTest(lox_type, """
        >>> fun makeCounter() {
        ...   var i = 0;
        ...   fun count() {
        ...     i = i + 1;
        ...     print i;
        ...   }
        ...   return count;
        ... }
        ... var counter = makeCounter();
        ... counter();
        ... counter();
        1
        2
        """)


def test_closure_bug(lox_type):
    runDocTest(lox_type, """
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
        """)


@pytest.mark.loop
def test_for_initializer(lox_type):
    runDocTest(lox_type, """
        >>> // no initializer
        >>> var i = 0;
        ... for (; i < 3; i = i + 1) {
        ...   print i;
        ... }
        0
        1
        2
        >>> // expr statement
        >>> var j = 5;
        ... for (j = 0; j < 3; j = j + 1) {
        ...   print j;
        ... }
        0
        1
        2
        >>> // var declaration
        >>> for (var k = 0; k < 3; k = k + 1) { 
        ...   print k;
        ... }
        0
        1
        2
        """)


def test_fun_to_str(lox_type):
    runDocTest(lox_type, """
        >>> fun square(n) {
        ...   return n*n;
        ... }
        ... print square;
        <fn square>
        """)

@pytest.mark.classes
def test_print_class(lox_type):
    runDocTest(lox_type, """
        >>> class Bagel {}
        ... print Bagel;
        <class Bagel>
        >>> class Bagel {}
        ... print Bagel();
        <class Bagel instance>
        """)


@pytest.mark.classes
def test_basic_class(lox_type):
    runDocTest(lox_type, """
        >>> class Bagel {}
        ... var bagel = Bagel();
        ... print Bagel;
        <class Bagel>
        >>> class Bagel {}
        ... var bagel = Bagel();
        ... print bagel;
        <class Bagel instance>
        >>> class Bagel {}
        ... var bagel = Bagel();
        ... bagel.topping = "cream cheese";
        ... print bagel.topping;
        cream cheese
        """)


@pytest.mark.classes
def test_class_method(lox_type):
    # normal
    runDocTest(lox_type, """
        >>> class Bacon {
        ...   eat() {
        ...     print "Crunch";
        ...   }
        ... }
        ... Bacon().eat();
        Crunch
        """)
    # normal with params
    runDocTest(lox_type, """
        >>> class Scone {
        ...   topping(first, second) {
        ...     print "scone with " + first + " and " + second;
        ...   }
        ... }
        ... var scone = Scone();
        ... scone.topping("berries", "cream");
        scone with berries and cream
        """)
    # put method into object
    runDocTest(lox_type, """
        >>> class Box {}
        ... fun notMethod(argument) {
        ...   print "called function with " + argument;
        ... }
        ... var box = Box();
        ... box.function = notMethod;
        ... box.function("argument");
        called function with argument
        """)
    # separate instance and method
    runDocTest(lox_type, """
        >>> class Person {
        ...   sayName() {
        ...     print this.name;
        ...   }
        ... }
        ...
        ... var jane = Person();
        ... jane.name = "Jane";
        ... var method = jane.sayName;
        ... method();
        Jane
        """)

@pytest.mark.classes
def test_class_this(lox_type):
    runDocTest(lox_type, """
        >>> class Cake {
        ...   taste() {
        ...     var adjective = "delicious";
        ...     print "The " + this.flavor + " cake is " + adjective + "!";
        ...   }
        ... }
        ... var cake = Cake();
        ... cake.flavor = "Chocolate";
        ... cake.taste();
        The Chocolate cake is delicious!
        """)
    # nested function declaration - this
    runDocTest(lox_type, """
        >>> class Nested {
        ...   method() {
        ...     fun function() {
        ...       print this;
        ...     }
        ... 
        ...     function();
        ...   }
        ... }
        ... Nested().method();
        <class Nested instance>
        """)


@pytest.mark.classes
def test_class_closure(lox_type):
    runDocTest(lox_type, """
            >>> class Egotist {
            ...   speak() {
            ...     print this;
            ...   }
            ... }
            ... var method = Egotist().speak;
            ... method();
            <class Egotist instance>
            """)


@pytest.mark.classes
def test_class_initializer(lox_type):
    # no params
    runDocTest(lox_type, """
        >>> class Foo {
        ...   init() {
        ...     this.value = "bar";
        ...     print "inside init";
        ...   }
        ... }
        ... var foo = Foo();
        ... print foo.value;
        inside init
        bar
        """)
    # params
    runDocTest(lox_type, """
        >>> class Brunch {
        ...   init(food, drink) {
        ...     this.food = food;
        ...     this.drink = drink;
        ...     print "inside init";
        ...   }
        ...   eat() {
        ...     print "Eat " + this.food + " and drink " + this.drink;
        ...   }
        ... }
        ... var brunch = Brunch("eggs", "coffee");
        ... brunch.eat();
        inside init
        Eat eggs and drink coffee
        """)

@pytest.mark.classes
def test_class_initializer_err(lox_type):
    # calling init with params when it is not defined
    runAndComparePattern(lox_type,"""
        class Brunch {}
        var brunch = Brunch("eggs", "coffee");
        """, pattern("Expected 0 arguments but got 2."))
    # returning value from initializer
    runAndComparePattern(lox_type,"""
        class Brunch {
            init() {
              return "just coffee";
            }
        }
        """, error_pattern("Can't return a value from an initializer."))

# this test is the same as one of the items in test_class_method
@pytest.mark.classes
def test_class_invoke(lox_type):
    runDocTest(lox_type, """
        >>> class Oops {
        ...   init() {
        ...     fun f() {
        ...       print "not a method";
        ...     }
        ... 
        ...     this.field = f;
        ...   }
        ... }
        ... 
        ... var oops = Oops();
        ... oops.field();
        not a method
        """)


@pytest.mark.classes
def test_inheritance(lox_type):
    runDocTest(lox_type, """
        >>> class Doughnut {
        ...   cook() {
        ...     print "Fry until golden brown.";
        ...   }
        ... }
        ... class BostonCream < Doughnut {
        ...   cook() {
        ...     super.cook();
        ...     print "Pipe full of custard and coat with chocolate.";
        ...   }
        ... }
        ... BostonCream().cook();
        Fry until golden brown.
        Pipe full of custard and coat with chocolate.
        """)


@pytest.mark.classes
def test_inheritance_super(lox_type):
    runDocTest(lox_type, """
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
        """)


def test_env_enclosing_scope(lox_type):
    runDocTest(lox_type, """
            >>> var global = "outside";
            ... {
            ...     var local = "inside";
            ...     print global + local;
            ... } 
            outsideinside
            """);

def test_eval_ordering(lox_type):
    runDocTest(lox_type, """
            >>> fun echo(n) {
            ...   print n;
            ...   return n;
            ... } 
            ...
            ... print echo(echo(1) + echo(2)) + echo(echo(4) + echo(5));
            1
            2
            3
            4
            5
            9
            12
            """)

def test_variables(lox_type):
    runDocTest(lox_type, """
        >>> var breakfast = "cigarettes";
        ... var beverage = "coffee";
        ... breakfast = "cigarettes with " + beverage;
        ... print breakfast;
        cigarettes with coffee
        """)

def test_local_vars_1(lox_type):
    runDocTest(lox_type, """
        >>> {
        ...     var a = "outer";
        ...     print a;
        ...     {
        ...         var a = "inner";
        ...         print a;
        ...     }
        ... }
        outer
        inner
        """)

@pytest.mark.func
def test_return_err(lox_type):
    runAndComparePattern(lox_type, """
        return "at top level";
        """, error_pattern("Can't return from top-level code"))

@pytest.mark.classes
def test_this_err(lox_type):
    # this outside class
    runAndComparePattern(lox_type, """
        print this;
        """, error_pattern("Can't use 'this' outside of a class."))
    # this outside class
    runAndComparePattern(lox_type, """
        fun notMethod() { print this; }
        """, error_pattern("Can't use 'this' outside of a class."))

@pytest.mark.func
def test_native_clock(lox_type):
    runDocTest(lox_type, """
        >>> var start = clock();
        ... var now = start;
        ... while (now - start < 0.5) {
        ...   now = clock();
        ... }
        ... var elapsed = now - start;
        ... print (elapsed > 0.4) and (elapsed < 0.6);
        true
        """);

@pytest.mark.func
def test_native_arity_err(lox_type):
    runAndComparePattern(lox_type, """
        clock(999);
        """,
        pattern("Expected 0 arguments but got 1"))
    runAndComparePattern(lox_type, """
        floor();
        """,
        pattern("Expected 1 arguments but got 0"))
    runAndComparePattern(lox_type, """
        random(1, 2, "aa");
        """,
        pattern("Expected 0 arguments but got 3"))

@pytest.mark.func
def test_simple_call(lox_type):
    runDocTest(lox_type, """
        >>> fun sayHi(name) {
        ...   print "Hi " + name;
        ... }
        ... sayHi("Homer");
        Hi Homer
        """);

@pytest.mark.func
def test_call_non_function(lox_type):
    runAndComparePattern(lox_type, """
        "not a function"();
        """, pattern("Can only call functions and classes"))

@pytest.mark.func
def test_call_arity_err(lox_type):
    runAndComparePattern(lox_type, """
        fun square(n) { return n*n; }
        square();
        """, pattern("Expected 1 arguments but got 0"))

def test_global_vars_1(lox_type):
    # from 21.3 Reading Variables
    runDocTest(lox_type, """
        >>> var beverage = "cafe au lait";
        ... var breakfast = "beignets with " + beverage;
        ... print breakfast;
        beignets with cafe au lait
        """)

def test_global_vars_2(lox_type):
    # from 21.4 Assignment
    runAndComparePattern(lox_type,"""
        var a = 1;
        var b = 2;
        var c = 3;
        var d = 4;
        a * b = c + d;
        """, error_pattern("Invalid assignment target."))

def test_global_vars_3(lox_type):
    # from 21.4 Assignment
    runDocTest(lox_type, """
        >>> var breakfast = "beignets";
        ... var beverage = "cafe au lait";
        ... breakfast = "beignets with " + beverage;
        ... print breakfast;
        beignets with cafe au lait
        """)

def test_local_vars_2(lox_type):
    runAndComparePattern(lox_type,"""
        {
            var a = "first";
            var a = "second";
        }
        """, error_pattern("Already a variable with this name in this scope"))

def test_local_vars_3(lox_type):
    runAndComparePattern(lox_type,"""
        {
            var a = "outer";
            {
                var a = a;
            }
        }
        """, error_pattern("Can't read local variable in its own initializer"))

def generate_block_with_locals(num_locals):
    script = "";
    script += "{\n"
    for i in range(0, num_locals):
        script += f'var local_{i:03d} = {i};\n'
    script += "}"
    return script

max_num_locals = 256

def test_local_vars_max(lox_type):
    script = generate_block_with_locals(max_num_locals)
    runAndComparePattern(lox_type, script, '.*')

# this is only in clox
def test_local_vars_too_many():
    lox_type = CLOX_TYPE
    script = generate_block_with_locals(max_num_locals + 1)
    runAndComparePattern(lox_type, script, error_pattern("Too many local variables in function."))

@pytest.mark.cond
def test_jumping_if_else(lox_type):
    # if-else (then branch)
    runDocTest(lox_type, """
        >>> var a = 0;
        ... print a;
        ... if (a < 1) {
        ...    print "a is less than 1";
        ... } else {
        ...    print "a is greater than 1";
        ... }
        0
        a is less than 1
        """)
    # if-else (else branch)
    runDocTest(lox_type, """
        >>> var a = 2;
        ... print a;
        ... if (a < 1) {
        ...    print "a is less than 1";
        ... } else {
        ...    print "a is greater than 1";
        ... }
        2
        a is greater than 1
        """)
    # if-no else (then branch)
    runDocTest(lox_type, """
        >>> var a = -1;
        ... print a;
        ... if (a < 0) {
        ...    print "a is less than zero";
        ... }
        -1
        a is less than zero
        """)
    # if-no else (else branch)
    runDocTest(lox_type, """
        >>> var a = 1;
        ... print a;
        ... if (a < 0) {
        ...    print "a is less than zero";
        ... }
        1
        """)

@pytest.mark.func
def test_function_decl(lox_type):
    runDocTest(lox_type, """
        >>> fun areWeHavingItYet() {
        ... print "yes we are";
        ... }
        ... print areWeHavingItYet;
        <fn areWeHavingItYet>
        """)

@pytest.mark.func
def test_function_args(lox_type):
    runDocTest(lox_type, """
        >>> fun sum(a, b, c) {
        ...   return a + b + c;
        ... }
        ... print 4 + sum(5, 6, 7);
        22
        """)

# only in clox for now
@pytest.mark.func
def test_stack_trace():
    lox_type = CLOX_TYPE
    runDocTest(lox_type, """
        >>> fun a() { b(); }
        ... fun b() { c(); }
        ... fun c() {
        ...    c("too", "many");
        ... }
        ...
        ... a();
        Expected 0 arguments but got 2.
        [line 4] in c()
        [line 2] in b()
        [line 1] in a()
        [line 7] in script
        """)

@pytest.mark.func
def test_no_return(lox_type):
    runDocTest(lox_type, """
        >>> fun noReturn() {
        ...   print "Do stuff";
        ... }
        ... print noReturn();
        Do stuff
        nil
        """)

@pytest.mark.closure
def test_closure(lox_type):
    runDocTest(lox_type, """
        >>> var x = "global";
        ... fun outer() {
        ...   var x = "inner";
        ...   fun inner() {
        ...     print x;
        ...   }
        ...   inner();
        ... }
        ... outer();
        inner
        """)
    runDocTest(lox_type, """
        >>> fun outer() {
        ...   var x = "outside";
        ...   fun inner() {
        ...     print x;
        ...   }
        ...   inner();
        ... }
        ... outer();
        outside
        """)

@pytest.mark.closure
def test_closure_diff_values(lox_type):
    runDocTest(lox_type, """
        >>> fun makeClosure(value) {
        ...   fun closure() {
        ...     print value;
        ...   }
        ...   return closure;
        ... }
        ...
        ... var doughnut = makeClosure("doughnut");
        ... var bagel = makeClosure("bagel");
        ... doughnut();
        ... bagel();
        doughnut
        bagel
        """)

@pytest.mark.closure
def test_closure_up_value(lox_type):
    runDocTest(lox_type, """
        >>> fun outer() {
        ...   var x = 1;
        ...   x = 2;
        ...   fun inner() {
        ...     print x;
        ...   }
        ...   inner();
        ... }
        ... outer();
        2
        """)

@pytest.mark.closure
def test_closure_devious(lox_type):
    runDocTest(lox_type, """
        >>> fun outer() {
        ...   var x = "value";
        ...   fun middle() {
        ...     fun inner() {
        ...       print x;
        ...     }
        ... 
        ...     print "create inner closure";
        ...     return inner;
        ...   }
        ... 
        ...   print "return from outer";
        ...   return middle;
        ... }
        ... var mid = outer();
        ... var in  = mid();
        ... in();
        return from outer
        create inner closure
        value
        """)

@pytest.mark.closure
def test_closure_simple(lox_type):
    runDocTest(lox_type, """
        >>> fun outer() {
        ...   var x = "outside";
        ...   fun inner() {
        ...     print x;
        ...   } 
        ...   inner();
        ... }
        ... outer();
        outside
        """)


@pytest.mark.closure
def test_closed_upvalues(lox_type):
    runDocTest(lox_type, """
        >>> fun outer() {
        ...   var x = "outside";
        ...   fun inner() {
        ...     print x;
        ...   } 
        ...   return inner;
        ... }
        ... 
        ... var closure = outer();
        ... closure();
        outside
        """)

@pytest.mark.closure
def test_closure_var_or_value(lox_type):
    runDocTest(lox_type, """
        >>> var globalSet;
        ... var globalGet;
        ... 
        ... fun main() {
        ...   var a = "initial";
        ... 
        ...   fun set() { a = "updated"; }
        ...   fun get() { print a; }
        ... 
        ...   globalSet = set;
        ...   globalGet = get;
        ... }
        ... 
        ... main();
        ... globalSet();
        ... globalGet();
        updated
        """)

# should add a test for the sibling upvalue scenario mentioned in 
# ch 25.4.2 closing upvalues
#@pytest.mark.closure
#def test_upvalue_is_ref_to_var(lox_type):
#    runDocTest(lox_type, """
#        >>> {
#        ...   var a = 1;
#        ...   fun f() {
#        ...     print a;
#        ...   }
#        ...   var b = 2;
#        ...   fun g() {
#        ...     print b;
#        ...   }
#        ...   var c = 3;
#        ...   fun h() {
#        ...     print c;
#        ...   }
#        ... }
#    """)

@pytest.mark.closure
def test_upvalue_is_ref_to_var(lox_type):
    runDocTest(lox_type, """
        >>> fun outer() {
        ...   var x = "before";
        ...   fun inner() {
        ...     x = "assigned";
        ...   }
        ...   inner();
        ...   print x;
        ... }
        ... outer();
        assigned
    """)

@pytest.mark.skip(reason="no way of currently testing this")
# from 25.2.2 flattening upvalues
def test_closure_disasm():
    expected = """
    0004    9 OP_CLOSURE          2 <fn inner>
    0006      |                     upvalue 0
    0008      |                     local 1
    0010      |                     upvalue 1
    0012      |                     local 2
    """
    script = """
        fun outer() {
          var a = 1;
          var b = 2;
          fun middle() {
            var c = 3;
            var d = 4;
            fun inner() {
              print a + c + b + d;
            }
          }
        }
    """
    runAndComparePattern(lox_type, script, contains(expected))

if __name__ == "__main__":
    runLox("1 == 0;")
    runLox('print "abc";')
