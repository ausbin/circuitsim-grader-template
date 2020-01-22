package edu.gatech.cs2110.circuitsim.api;

import com.ra4king.circuitsim.simulator.components.memory.ROM;

/**
 * Wraps a CircuitSim RAM component.
 *
 * @author Austin Adams
 */
public class Rom extends BaseMemory {
    private ROM rom;
    private Subcircuit subcircuit;

    /**
     * Creates a new Ram which wraps the provided {@code RAM}
     * component and which lives in the provided {@code Subcircuit}.
     *
     * @param rom {@code RAM} component to wrap
     * @param subcircuit where this pin lives
     */
    public Rom(ROM rom, Subcircuit subcircuit) {
        this.rom = rom;
        this.subcircuit = subcircuit;
    }

    @Override
    public void store(int address, int value) {
        rom.getMemory()[address] = value;
        subcircuit.getCircuit().forEachState(state -> rom.valueChanged(state, null, 0));
    }

    /**
     * Returns the internal CircuitSim {@code RAM} component this
     * object wraps.
     * <p>
     * <b>This exposes an internal CircuitSim API. Do not use unless you
     *    know what you are doing.</b>
     *
     * @return the CircuitSim {@code RAM} component wrapped by this
     *         object.
     */
    public ROM getROM() {
        return rom;
    }

    /**
     * Returns the {@link Subcircuit}, a wrapper around a CircuitSim
     * {@code CircuitBoard} where this Pin lives.
     *
     * @return the {@link Subcircuit} where this pin lives
     */
    public Subcircuit getSubcircuit() {
        return subcircuit;
    }
}
