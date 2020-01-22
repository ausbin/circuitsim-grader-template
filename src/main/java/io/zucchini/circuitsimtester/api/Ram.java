package edu.gatech.cs2110.circuitsim.api;

import com.ra4king.circuitsim.simulator.components.memory.RAM;

/**
 * Wraps a CircuitSim RAM component.
 *
 * @author Austin Adams
 */
public class Ram extends BaseMemory {
    private RAM ram;
    private Subcircuit subcircuit;

    /**
     * Creates a new Ram which wraps the provided {@code RAM}
     * component and which lives in the provided {@code Subcircuit}.
     *
     * @param ram {@code RAM} component to wrap
     * @param subcircuit where this pin lives
     */
    public Ram(RAM ram, Subcircuit subcircuit) {
        this.ram = ram;
        this.subcircuit = subcircuit;
    }

    @Override
    public void store(int address, int value) {
        ram.store(subcircuit.getCircuitState(), address, value);
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
    public RAM getRAM() {
        return ram;
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
