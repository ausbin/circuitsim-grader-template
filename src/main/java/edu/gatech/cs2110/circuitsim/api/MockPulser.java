package edu.gatech.cs2110.circuitsim.api;

// TODO FIXME document
public abstract class MockPulser {
    protected InputPin mockPin;
    protected Subcircuit subcircuit;

    public MockPulser(InputPin mockPin, Subcircuit subcircuit) {
        this.mockPin = mockPin;
        this.subcircuit = subcircuit;
    }

    public InputPin getMockPin() {
        return mockPin;
    }

    public Subcircuit getSubcircuit() {
        return subcircuit;
    }

    protected void pulse() {
        mockPin.set(0b0);
        mockPin.set(0b1);
        mockPin.set(0b0);
    }
}
