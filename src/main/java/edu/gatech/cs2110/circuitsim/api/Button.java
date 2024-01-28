package edu.gatech.cs2110.circuitsim.api;

// TODO FIXME document
public class Button extends MockPulser {
    public Button(InputPin mockPin, Subcircuit subcircuit) {
        super(mockPin, subcircuit);
    }

    public void press() {
        pulse();
    }
}
