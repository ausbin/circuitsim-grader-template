package edu.gatech.cs2110.circuitsim.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import edu.gatech.cs2110.circuitsim.api.InputPin;
import edu.gatech.cs2110.circuitsim.api.OutputPin;
import edu.gatech.cs2110.circuitsim.api.Restrictor;
import edu.gatech.cs2110.circuitsim.api.Subcircuit;
import edu.gatech.cs2110.circuitsim.api.SubcircuitComponent;
import edu.gatech.cs2110.circuitsim.api.SubcircuitTest;
import edu.gatech.cs2110.circuitsim.extension.CircuitSimExtension;
import edu.gatech.cs2110.circuitsim.extension.BasesConverter;

@DisplayName("Toy ALU")
@ExtendWith(CircuitSimExtension.class)
@SubcircuitTest(file="toy-alu.sim", subcircuit="ALU",
                restrictors={ToyALUTests.BannedGates.class})
public class ToyALUTests {
    public static class BannedGates extends Restrictor {
        @Override
        public void validate(Subcircuit subcircuit) throws AssertionError {
            blacklistComponents(subcircuit, "XOR");

            Map<String, Integer> counts = subcircuit.lookupComponentCounts(
                Arrays.asList("Adder", "Mux"), false, true);
            assertTrue(counts.getOrDefault("Adder", 0) <= 2, "must contain at most 2 adders");
            assertTrue(counts.getOrDefault("Mux", 0) <= 1, "must contain at most 1 mux");
        }
    }

    @SubcircuitComponent(bits=4)
    private InputPin a;

    @SubcircuitComponent(bits=4)
    private InputPin b;

    @SubcircuitComponent(bits=2)
    private InputPin sel;

    @SubcircuitComponent(bits=4)
    private OutputPin out;

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

    @ParameterizedTest(name="a:{0}, b:{1}, sel:10 (a - b) → out:{2}")
    @CsvFileSource(resources="/tests/subtract.csv")
    public void subtract(@ConvertWith(BasesConverter.class) int aIn,
                         @ConvertWith(BasesConverter.class) int bIn,
                         @ConvertWith(BasesConverter.class) int outOut) {
        a.set(aIn);
        b.set(bIn);
        sel.set(0b10);
        assertEquals(outOut, out.get(), "out");
    }
}
