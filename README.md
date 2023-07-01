CircuitSim Autograder Template
==============================

**Note: A more up-to-date version of this tool is maintained by the Georgia
Tech CS 2110 TAs here: <https://github.com/zucchini/circuitsim-tester>. If you
are a CS 2110 Student or TA, you should consider using that library instead of
this template.**

Javadoc for API: <https://ausbin.github.io/circuitsim-grader-template/>

This repository aims to bridge the gap between the nitty-gritty details
of the [CircuitSim][1] API and high-level tests on combinational logic
in a CircuitSim file. Until now, we (the CS 2110 staff) handled
CircuitSim homeworks with one of the following strategies:

 1. Don't provide a tester because they were scary
 2. Copy and paste some old tests, often missing error handling
    because the tons of duplicate code and intertwined CircuitSim
    implementation details made the tests hard to follow

So I have two goals in this template:

 1. Make CircuitSim autograders easy to write, for students too if they
    want
 2. Insulate tests from the verboseness of the CircuitSim API by
    wrapping the CircuitSim API, and pass these wrappers to test classes
    using dependency injection

Getting Started
---------------

To make your own CircuitSim autograder:

 1. Clone this repository, probably running `rm -r README.md docs/ *.sim`
    and writing a new `README.md`
 2. Edit the project name in `gradle.properties`
 3. Write some tests in `src/main/java/edu/gatech/cs2110/circuitsim/tests/`
 4. Test with `./gradlew run` and generate `build/libs/hwX-tester.jar`
    with `./gradlew jar`

### Writing Tests

#### Testing basic combinational logic

Suppose we had a Toy ALU like the following that we wanted to test:

![Screenshot of `toy-alu.sim`](https://i.imgur.com/tnemxwg.png)

You can find the full test at
`src/main/java/edu/gatech/cs2110/circuitsim/tests/ToyALU.java`, but
a brief walkthrough follows if you just want the highlights.

Let's start with the boilerplate test class:

```java
@DisplayName("Toy ALU")
@ExtendWith(CircuitSimExtension.class)
@SubcircuitTest(file="toy-alu.sim", subcircuit="ALU")
public class ToyALUTests {
    @SubcircuitPin(bits=4)
    private InputPin a;

    @SubcircuitPin(bits=4)
    private InputPin b;

    @SubcircuitPin(bits=2)
    private InputPin sel;

    @SubcircuitPin(bits=4)
    private OutputPin out;
}
```

`@DisplayName("Toy ALU")` lets students will see the name `Toy ALU` on
test failures (if you don't specify it, they will just see the ugly
fully-qualified class name instead), and
`@ExtendWith(CircuitSimExtension.class)` tells JUnit to run some custom
CircuitSim setup code first which loads up the subcircuit we want and
injects pins in it into the test class instance. Indeed,
`@SubcircuitTest(file="toy-alu.sim", subcircuit="ALU")` tells the
CircuitSim JUnit extension to load `toy-alu.sim` and then open its
subcircuit matching the name `ALU`. As described [in the API
documentation][2], this subcircuit name (as well as Pin labels later)
will be normalized into a lowercase alphanumeric string. That way,
`1-bit ALU` matches `1bit alu`, `1 bit ALU`, and so on.

The Pin fields are pretty self-explanatory: the extension looks up Pin
components in the subcircuit based on the name of fields annotated with
`@SubcircuitPin`. [If you want to match on a different label, say
`label="something else"`][3].

Let's start with testing the last operation, Pass A, since it's the easiest:

```java
    @DisplayName("pass A")
    @Test
    public void passA() {
        a.set(0);
        sel.set(0b11);
        assertEquals(0, out.get(), "out");

        a.set(0b1011);
        sel.set(0b11);
        assertEquals(0b1011, out.get(), "out");
    }
```

Well, that's pretty easy. But what if you need to test more than two
cases? That's where JUnit 5 parameterized tests come in:

```java
    @ParameterizedTest(name="a:{0}, b:{1}, sel:00 (a xor b) → out:{2}")
    @CsvSource({
        /*  a      b    |  out */
        "0b1111, 0b0000, 0b1111",
        "0b0000, 0b1111, 0b1111",
        "0b1111, 0b1111, 0b0000",
        "0b1011, 0b0010, 0b1001",
    })
    public void xor(@ConvertWith(BasesConverter.class) int aIn,
                    @ConvertWith(BasesConverter.class) int bIn,
                    @ConvertWith(BasesConverter.class) int outOut) {
        a.set(aIn);
        b.set(bIn);
        sel.set(0b00);
        assertEquals(outOut, out.get(), "out");
    }
```

JUnit will run this test once for every row in the CSV. Unlike a simple
for loop in a test, if one fails, it keeps going. The
`@ConvertWith(BasesConverter.class)` parses Strings to ints based on the
prefix. So `0x` is hex and `0b` is binary, and anything else is decimal.

You can also source your parameterized test from a method as follows:

```java
    @ParameterizedTest(name="a:{0}, b:{1}, sel:01 (a + b) → out:{2}")
    @MethodSource
    public void add(int aIn, int bIn,
                    int outOut) {
        a.set(aIn);
        b.set(bIn);
        sel.set(0b01);
        assertEquals(outOut, out.get(), "out");
    }

    public static Stream<Arguments> add() {
        List<Arguments> args = new LinkedList<>();

        for (int a = 0; a < (1 << 4); a++) {
            for (int b = 0; b < (1 << 4); b++) {
                args.add(Arguments.of(a, b, (a + b) % (1 << 4)));
            }
        }

        return args.stream();
    }
```

This allows for a much easier way to do an exhaustive test than a CSV!
Note that JUnit locates the second (**static**!) method because it has
the same name as the test.

You can see the finished product in
`src/main/java/edu/gatech/cs2110/circuitsim/tests/ToyALU.java`.

#### Restrictors (Banning Components)

Suppose in the previous example, we wanted students to build the XOR
themselves, without using a XOR gate component. Then we can change the
test as follows:

```java
@DisplayName("Toy ALU")
@ExtendWith(CircuitSimExtension.class)
@SubcircuitTest(file="toy-alu.sim", subcircuit="ALU",
                restrictors={ToyALUTests.BannedGates.class})
public class ToyALUTests {
    public static class BannedGates extends Restrictor {
        @Override
        public void validate(Subcircuit subcircuit) throws AssertionError {
            blacklistComponents(subcircuit, "XOR");
        }
    }
    // ...
```

That is, we can provide `restrictors` to `@SubcircuitTest`. The fact
that it's a class is useful for when you have a bunch of subcircuits
with the same restrictions. In such a case, you can reduce code
duplication by simply referencing the same `Restrictor` class in all of
the tests.

Consult [the Restrictor documentation][8] for more information on
restrictions. There is also support for whitelists.

#### Testing sequential logic

Combinational logic is easy enough, but what about a state machine? Consider the following:

![fsm](https://i.imgur.com/cnGx4Y9.png)

<!---
# dot -Tpng fsm.dot -Nfontname=Roboto -Nfontsize=10 -Efontname=Roboto -Efontsize=10 -o fsm.png

digraph {
    size="8,8"
    dpi=200
    //ordering=out
    rankdir="LR"

    "" [shape=none]
    s0 [shape=circle, label="s0\na = 1"]
    s1 [shape=circle, label="s1\na = 0"]

    ""  -> s0
    s0  -> s0 [label="g'"]
    s0  -> s1 [label="g"]
    s1  -> s1 [label="g"]
    s1  -> s0 [label="g'"]
}

# vim:set ft=dot:
--->

which corresponds to this one-hot circuit:

![screenshot of `fsm.sim`](https://i.imgur.com/sbVb2hv.png)

The register here is kind of a pain, because we want to test the
combinational logic on each side of it. Luckily, we can use a feature in
the tester which replaces the register with some Pin components before
running the tests, (*very*) roughly like this:

![screenshot of `fsm.sim` after mocking the register](https://i.imgur.com/DfxqtrP.png)

The syntax for this is similar to pins:

```java
@DisplayName("Finite State Machine")
@ExtendWith(CircuitSimExtension.class)
@SubcircuitTest(file="fsm.sim", subcircuit="fsm")
public class FsmTests {
    @SubcircuitPin(bits=1)
    private InputPin g;

    @SubcircuitPin(bits=1)
    private InputPin clk;

    @SubcircuitPin(bits=1)
    private InputPin rst;

    @SubcircuitPin(bits=1)
    private InputPin en;

    @SubcircuitPin(bits=1)
    private OutputPin a;

    @SubcircuitRegister(bits=2, onlyRegister=true)
    private MockRegister stateReg;
}
```

So we specify the input/output pins we want as usual, but then also
require there to be exactly 1 2-bit register in the subcircuit: the
state register.

Testing the output `a` is fairly simple, so let's start with that:

```java
    @DisplayName("output a")
    @Test
    public void outputA() {
        stateReg.getQ().set(0b00);
        assertEquals(0, a.get(), "output a in state 00");
        stateReg.getQ().set(0b01);
        assertEquals(1, a.get(), "output a in state 01");
        stateReg.getQ().set(0b10);
        assertEquals(0, a.get(), "output a in state 10");
    }
```

[`MockRegister.getQ()`][4] returns an `InputPin` that the tester placed
where the out port of the register used to be, so we can set it to an
arbitrary value just like any other input pin.

[`MockRegister.getD()`][4], on the other hand, is an `OutputPin`:

```java
    @ParameterizedTest(name="state:{0}, g:{1} → next state:{2}")
    @CsvSource({
        /* state  g | next state */
        "   0b00, 0,        0b01",
        "   0b00, 1,        0b01",
        "   0b01, 0,        0b01",
        "   0b01, 1,        0b10",
        "   0b10, 0,        0b01",
        "   0b10, 1,        0b10",
    })
    public void transition(@ConvertWith(BasesConverter.class) int stateIn,
                           int gIn,
                           @ConvertWith(BasesConverter.class) int nextStateOut) {
        stateReg.getQ().set(stateIn);
        g.set(gIn);
        assertEquals(nextStateOut, stateReg.getD().get(), "next state");
    }
```

You can see the finished product at
`src/main/java/edu/gatech/cs2110/circuitsim/tests/FsmTests.java`.


Caveats
-------

Because CircuitSim is a JavaFX application and this tester simply runs
CircuitSim, this tester tries to attach to a display — like an X11
`$DISPLAY` on GNU/Linux. If you want to run this grader in a headless
environment, you can get around this by standing up a dummy X11 server.

For GNU/Linux, [zucchini][6] installs `xf86-video-dummy` and then uses
the following `run_graphical.sh`:

```bash
#!/bin/bash
cat >xorg.conf <<'EOF'
# This xorg configuration file is meant to be used by xpra
# to start a dummy X11 server.
# For details, please see:
# https://xpra.org/Xdummy.html
Section "ServerFlags"
  Option "DontVTSwitch" "true"
  Option "AllowMouseOpenFail" "true"
  Option "PciForceNone" "true"
  Option "AutoEnableDevices" "false"
  Option "AutoAddDevices" "false"
EndSection
Section "Device"
  Identifier "dummy_videocard"
  Driver "dummy"
  Option "ConstantDPI" "true"
  VideoRam 192000
EndSection
Section "Monitor"
  Identifier "dummy_monitor"
  HorizSync   5.0 - 1000.0
  VertRefresh 5.0 - 200.0
  Modeline "1024x768" 18.71 1024 1056 1120 1152 768 786 789 807
EndSection
Section "Screen"
  Identifier "dummy_screen"
  Device "dummy_videocard"
  Monitor "dummy_monitor"
  DefaultDepth 24
  SubSection "Display"
    Viewport 0 0
    Depth 24
    Modes "1024x768"
    Virtual 1024 768
  EndSubSection
EndSection
EOF
/usr/lib/xorg/Xorg -noreset -logfile ./xorg.log -config ./xorg.conf :69 \
    >/dev/null 2>&1 &
xorg_pid=$!
export DISPLAY=:69
"$@"
exitcode=$?
kill "$xorg_pid" || {
    printf 'did not kill Xorg!\n' >&2
    exit 1
}
exit $exitcode
```

which allows you to say `./run_graphical.sh java -jar hwX-tester.jar` on
a headless system.


Zucchini Support
----------------

A [zucchini][6] backend exists, [`CircuitSimGrader`][7], which reads the
JSON printed to stdout for

    java -jar hwX-tester.jar --zucchini FsmTests

given some test `FsmTests`. Each subcircuit you want to test (aka each
test class) should correspond to a different assignment component, and
each test method is a part of that component. Here's a `zucchini.yml`
for the FSM example above:

```yaml
name: Homework X
author: Austin Adams
components:
- name: Finite State Machine
  weight: 1
  backend: CircuitSimGrader
  backend-options:
    grader-jar: hwX-tester.jar
    test-class: FsmTests
  files: [fsm.sim]
  grading-files: [hwX-tester.jar]
  parts:
  - {test: clockConnected,  weight: 1}
  - {test: resetConnected,  weight: 1}
  - {test: enableConnected, weight: 1}
  - {test: outputA,         weight: 5}
  - {test: transition,      weight: 10}
```

Note that since `hwX-tester.jar` is in `grading-files`, it'll need to be
in `grading-files/` in the zucchini assignment repository.

[1]: https://github.com/ra4king/CircuitSim
[2]: https://ausbin.github.io/circuitsim-grader-template/edu/gatech/cs2110/circuitsim/api/Subcircuit.html#fromPath(java.lang.String,java.lang.String)
[3]: https://ausbin.github.io/circuitsim-grader-template/edu/gatech/cs2110/circuitsim/api/SubcircuitPin.html
[4]: https://ausbin.github.io/circuitsim-grader-template/edu/gatech/cs2110/circuitsim/api/MockRegister.html#getQ()
[5]: https://ausbin.github.io/circuitsim-grader-template/edu/gatech/cs2110/circuitsim/api/MockRegister.html#getD()
[6]: https://github.com/zucchini/zucchini
[7]: https://github.com/zucchini/zucchini/blob/master/zucchini/graders/circuitsim_grader.py
[8]: https://ausbin.github.io/circuitsim-grader-template/edu/gatech/cs2110/circuitsim/api/Restrictor.html
