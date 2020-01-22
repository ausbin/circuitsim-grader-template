package edu.gatech.cs2110.circuitsim.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import edu.gatech.cs2110.circuitsim.api.InputPin;
import edu.gatech.cs2110.circuitsim.api.OutputPin;
import edu.gatech.cs2110.circuitsim.api.SubcircuitComponent;
import edu.gatech.cs2110.circuitsim.api.SubcircuitTest;
import edu.gatech.cs2110.circuitsim.extension.CircuitSimExtension;

@DisplayName("1-Bit Adder")
@ExtendWith(CircuitSimExtension.class)
@SubcircuitTest(file="adder.sim", subcircuit="1-bit adder")
public class AdderTests {
    @SubcircuitComponent(bits = 1)
    private InputPin a;

    @SubcircuitComponent(bits = 1)
    private InputPin b;

    @SubcircuitComponent(bits = 1)
    private InputPin cin;

    @SubcircuitComponent(bits = 1)
    private OutputPin sum;

    @SubcircuitComponent(bits = 1)
    private OutputPin cout;

    @ParameterizedTest(name="a:{0}, b:{1}, cin:{2} → cout:{3} + sum:{4}")
    @CsvSource({
      /* a  b cin | cout sum */
        "0, 0, 0,    0,   0",
        "0, 0, 1,    0,   1",
        "0, 1, 0,    0,   1",
        "0, 1, 1,    1,   0",
        "1, 0, 0,    0,   1",
        "1, 0, 1,    1,   0",
        "1, 1, 0,    1,   0",
        "1, 1, 1,    1,   1"
    })
    public void oneBitAdder(int aIn, int bIn, int cinIn,
                            int coutOut, int sumOut) {
        a.set(aIn);
        b.set(bIn);
        cin.set(cinIn);
        assertEquals(coutOut, cout.get(), "cout");
        assertEquals(sumOut, sum.get(), "sum");
    }
}
