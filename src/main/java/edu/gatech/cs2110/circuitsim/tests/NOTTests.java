package edu.gatech.cs2110.circuitsim.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import edu.gatech.cs2110.circuitsim.api.InputPin;
import edu.gatech.cs2110.circuitsim.api.OutputPin;
import edu.gatech.cs2110.circuitsim.api.Restrictor;
import edu.gatech.cs2110.circuitsim.api.Subcircuit;
import edu.gatech.cs2110.circuitsim.api.SubcircuitComponent;
import edu.gatech.cs2110.circuitsim.api.SubcircuitTest;
import edu.gatech.cs2110.circuitsim.extension.CircuitSimExtension;

@DisplayName("NOT Gate")
@ExtendWith(CircuitSimExtension.class)
@SubcircuitTest(file="not.sim", subcircuit="not",
                restrictors={NOTTests.Restrictions.class})
public class NOTTests {
    public static class Restrictions extends Restrictor {
        @Override
        public void validate(Subcircuit subcircuit) throws AssertionError {
            whitelistComponents(subcircuit, "Transistor");

            // Check if they abused
            assertEquals(2, subcircuit.getPinCount(), "Total number of Pin components");
        }
    }

    @SubcircuitComponent(bits=1)
    private InputPin in;

    @SubcircuitComponent(bits=1)
    private OutputPin out;

    @ParameterizedTest(name="NOT in:{0} → out:{1}")
    @CsvSource({
     /* in | out */
        "0,  1",
        "1,  0",
    })
    public void xor(int inIn, int outOut) {
        in.set(inIn);
        assertEquals(outOut, out.get(), "out");
    }
}
