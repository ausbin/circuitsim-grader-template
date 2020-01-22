package edu.gatech.cs2110.circuitsim.api;

import java.util.function.BooleanSupplier;

// TODO FIXME document
public class Clock extends MockPulser {
    public Clock(InputPin mockPin, Subcircuit subcircuit) {
        super(mockPin, subcircuit);
    }

    public void tick() {
        pulse();
    }

    public long tickUntil(long maxCycleCount, BooleanSupplier stopWhen) {
        long ticks = 0;
        while (!stopWhen.getAsBoolean()) {
            if (ticks > maxCycleCount) {
                throw new IllegalStateException(
                    "Ticked clock more than " + maxCycleCount + " times without finishing. " +
                    "Please check for errors in your logic");
            }

            tick();
        }
        return ticks;
    }
}
