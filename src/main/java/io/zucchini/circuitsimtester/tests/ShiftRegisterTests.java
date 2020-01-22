package edu.gatech.cs2110.circuitsim.tests;

import edu.gatech.cs2110.circuitsim.api.*;
import edu.gatech.cs2110.circuitsim.extension.CircuitSimExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static edu.gatech.cs2110.circuitsim.api.SubcircuitComponent.Type.TUNNEL;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Shift Register")
@ExtendWith(CircuitSimExtension.class)
@SubcircuitTest(file="shift-register.sim", subcircuit="main")
public class ShiftRegisterTests {

    @SubcircuitComponent(bits = 16)
    private InputPin input;

    @SubcircuitComponent
    private Clock clk;

    @SubcircuitComponent
    private Button rst;

    @SubcircuitComponent(bits = 16)
    private Register r0;
    @SubcircuitComponent(bits = 16)
    private Register r1;
    @SubcircuitComponent(bits = 16, recursiveSearch = true)
    private Register r2;
    @SubcircuitComponent(bits = 16)
    private Register r3;

    @SubcircuitComponent(bits = 16, type = TUNNEL)
    private OutputPin end;

    @BeforeEach
    public void reset() {
        rst.press();
    }

    @DisplayName("stupid test")
    @Test
    public void testShiftRegister() {
        input.set(0xf0f0);
        assertEquals(0x0000, r0.getQ(), "r0 cycle 0");
        assertEquals(0x0000, r1.getQ(), "r1 cycle 0");
        assertEquals(0x0000, r2.getQ(), "r2 cycle 0");
        assertEquals(0x0000, r3.getQ(), "r3 cycle 0");
        assertEquals(0x0000, end.get(), "end cycle 0");

        clk.tick();
        input.set(0x0f0f);
        assertEquals(0xf0f0, r0.getQ(), "r0 cycle 1");
        assertEquals(0x0000, r1.getQ(), "r1 cycle 1");
        assertEquals(0x0000, r2.getQ(), "r2 cycle 1");
        assertEquals(0x0000, r3.getQ(), "r3 cycle 1");
        assertEquals(0x0000, end.get(), "end cycle 1");

        clk.tick();
        input.set(0x0ff0);
        assertEquals(0x0f0f, r0.getQ(), "r0 cycle 2");
        assertEquals(0xf0f0, r1.getQ(), "r1 cycle 2");
        assertEquals(0x0000, r2.getQ(), "r2 cycle 2");
        assertEquals(0x0000, r3.getQ(), "r3 cycle 2");
        assertEquals(0x0000, end.get(), "end cycle 2");

        clk.tick();
        input.set(0xff00);
        assertEquals(0x0ff0, r0.getQ(), "r0 cycle 3");
        assertEquals(0x0f0f, r1.getQ(), "r1 cycle 3");
        assertEquals(0xf0f0, r2.getQ(), "r2 cycle 3");
        assertEquals(0x0000, r3.getQ(), "r3 cycle 3");
        assertEquals(0x0000, end.get(), "end cycle 3");

        clk.tick();
        input.set(0xbeef);
        assertEquals(0xff00, r0.getQ(), "r0 cycle 4");
        assertEquals(0x0ff0, r1.getQ(), "r1 cycle 4");
        assertEquals(0x0f0f, r2.getQ(), "r2 cycle 4");
        assertEquals(0xf0f0, r3.getQ(), "r3 cycle 4");
        assertEquals(0xf0f0, end.get(), "end cycle 4");

        clk.tick();
        assertEquals(0xbeef, r0.getQ(), "r0 cycle 5");
        assertEquals(0xff00, r1.getQ(), "r1 cycle 5");
        assertEquals(0x0ff0, r2.getQ(), "r2 cycle 5");
        assertEquals(0x0f0f, r3.getQ(), "r3 cycle 5");
        assertEquals(0x0f0f, end.get(), "end cycle 5");
    }

    @DisplayName("spicy test")
    @Test
    public void testShiftRegisterWithTickUntil() {
        input.set(0xdead);
        clk.tickUntil(16, () -> end.get() == 0xdead);

        assertEquals(0xdead, r0.getQ(), "r0");
        assertEquals(0xdead, r1.getQ(), "r1");
        assertEquals(0xdead, r2.getQ(), "r2");
        assertEquals(0xdead, r3.getQ(), "r3");
        assertEquals(0xdead, end.get(), "r3");
    }
}
