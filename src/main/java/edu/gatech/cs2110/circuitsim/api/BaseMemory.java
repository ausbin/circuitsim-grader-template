package edu.gatech.cs2110.circuitsim.api;

import java.io.InputStream;
import java.util.Scanner;

public abstract class BaseMemory {
    public abstract void store(int address, int value);

    /**
     * Loads a stream of a dat file into this component's memory. Format
     * is the same as the .dat files saved in the CircuitSim memory
     * editor window.
     */
    public void load(InputStream stream) {
        Scanner scanner = new Scanner(stream);

        int address = 0;
        while (scanner.hasNext()) {
            String word = scanner.next();

            if (word.contains("-")) {
                // Roi's world-famous patented run-length encoding
                // `x-y', where x is the number of occurrences and y is a
                // hex word
                String[] pieces = word.split("-");
                if (pieces.length != 2) {
                    throw new IllegalArgumentException("invalid run-length encoded word " + word);
                }
                int length = Integer.parseInt(pieces[0]);
                int value = Integer.parseInt(pieces[1], 16);

                for (int i = 0; i < length; i++) {
                    store(address++, value);
                }
            } else {
                int value = Integer.parseInt(word, 16);
                store(address++, value);
            }
        }
    }
}
