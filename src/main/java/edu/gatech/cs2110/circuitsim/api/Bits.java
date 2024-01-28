package edu.gatech.cs2110.circuitsim.api;

import java.util.function.Supplier;

/**
 * Provides useful bit twiddling operations.
 */
class Bits {
    /**
     * Sign-extends a value.
     */
    static int sext(int orig, int bits) {
        // x << 32 == x if x is a java int, so exclude that case
        if (bits < 32 && (orig & (1 << (bits - 1))) != 0) {
            return orig | (-1 << bits);
        } else {
            return orig;
        }
    }

    /**
     * An IllegalStateException is thrown when at least one bit is floating.
     * But it just says "Invalid value," so throw a more human-friendly error
     * message for the sake of students' mental stability.
     */
    static <T> T betterFloatingErrorMessage(Supplier<T> x) {
        try {
            return x.get();
        } catch (IllegalStateException err) {
            if (err.getMessage().equals("Invalid value")) {
                throw new IllegalStateException(
                    "At least one output bit is floating (undefined, or blue " +
                    "in CircuitSim). Is the output pin connected to anything?", err);
            } else {
                throw err;
            }
        }
    }
}
