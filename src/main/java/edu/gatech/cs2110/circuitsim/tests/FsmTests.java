package edu.gatech.cs2110.circuitsim.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import edu.gatech.cs2110.circuitsim.api.InputPin;
import edu.gatech.cs2110.circuitsim.api.MockRegister;
import edu.gatech.cs2110.circuitsim.api.OutputPin;
import edu.gatech.cs2110.circuitsim.api.SubcircuitComponent;
import edu.gatech.cs2110.circuitsim.api.SubcircuitTest;
import edu.gatech.cs2110.circuitsim.extension.BasesConverter;
import edu.gatech.cs2110.circuitsim.extension.CircuitSimExtension;

@DisplayName("Finite State Machine")
@ExtendWith(CircuitSimExtension.class)
@SubcircuitTest(file="fsm.sim", subcircuit="fsm")
public class FsmTests {
    @SubcircuitComponent(bits=1)
    private InputPin g;

    @SubcircuitComponent(bits=1)
    private InputPin clk;

    @SubcircuitComponent(bits=1)
    private InputPin rst;

    @SubcircuitComponent(bits=1)
    private InputPin en;

    @SubcircuitComponent(bits=1)
    private OutputPin a;

    @SubcircuitComponent(bits=2, onlyInstance=true)
    private MockRegister stateReg;

    @DisplayName("Clock is connected")
    @Test
    public void clockConnected() {
        clk.set(1);
        assertEquals(1, stateReg.getClk().get(), "clk pin on state register");
        clk.set(0);
        assertEquals(0, stateReg.getClk().get(), "clk pin on state register");
    }

    @DisplayName("Reset is connected")
    @Test
    public void resetConnected() {
        rst.set(1);
        assertEquals(1, stateReg.getRst().get(), "reset pin on state register");
        rst.set(0);
        assertEquals(0, stateReg.getRst().get(), "reset pin on state register");
    }

    @DisplayName("Enable is connected")
    @Test
    public void enableConnected() {
        en.set(1);
        assertEquals(1, stateReg.getEn().get(), "enable pin on state register");
        en.set(0);
        assertEquals(0, stateReg.getEn().get(), "enable pin on state register");
    }

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
}
