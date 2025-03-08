# lox

My repository for working through [Crafting Interpreters](https://craftinginterpreters.com) that implements a scripting language lox.

jlox is the java implementation. clox is the c implementation.

## Building

Building `clox`:

```sh
> cd clox
> ./clox ray.lox
 mvn install
```

Building `jlox`:

```sh
> cd jlox
> mvn install
```

## Running

To open an interactive interpeter, just execute `clox` or `jlox`.
To execute a file specify the file as an arg like:

```sh
# building clox
> cd clox
> ./clox ray.lox
> cd ../
# building jlox
> cd jlox
> mvn install
```

## Running Tests

Each test specifies a piece of lox source code and the expected output on stdout,
then runs it with both jlox (java) and clox (c) implementations.

To execute tests first setup a venv and install pytest, then you can run them like:

```sh
> cd tests
> pytest tests.py
```

Here is a test for conditional and branching instructions. The lines starting with `>>>` or `...` are executed by the lox interpreter,
then the following lines state the expected output.

Here is an example for calculating fibonacci numbers to test recursion:

```python
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
```

# Changes from Book

TODO

# lox-src

I wrote some lox source files to see how it felt performing actual programming tasks in
lox:

* `ray.lox`: ray tracing program. i translated the existing code from [Ray Tracing in One Weekend](https://raytracing.github.io/books/RayTracingInOneWeekend.html). Execute `clox ray.lox > image.ppm` to generate output file with resulting image.
