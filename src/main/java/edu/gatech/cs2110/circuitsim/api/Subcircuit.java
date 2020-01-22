package edu.gatech.cs2110.circuitsim.api;

import static com.ra4king.circuitsim.simulator.components.memory.Register.PORT_IN;
import static com.ra4king.circuitsim.simulator.components.memory.Register.PORT_ENABLE;
import static com.ra4king.circuitsim.simulator.components.memory.Register.PORT_CLK;
import static com.ra4king.circuitsim.simulator.components.memory.Register.PORT_ZERO;
import static com.ra4king.circuitsim.simulator.components.memory.Register.PORT_OUT;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.ra4king.circuitsim.gui.*;
import com.ra4king.circuitsim.gui.Properties;
import com.ra4king.circuitsim.gui.peers.memory.RAMPeer;
import com.ra4king.circuitsim.gui.peers.memory.ROMPeer;
import com.ra4king.circuitsim.gui.peers.wiring.ClockPeer;
import com.ra4king.circuitsim.gui.peers.wiring.Tunnel;
import com.ra4king.circuitsim.simulator.components.memory.RAM;
import javafx.util.Pair;

import com.ra4king.circuitsim.gui.peers.SubcircuitPeer;
import com.ra4king.circuitsim.gui.peers.memory.RegisterPeer;
import com.ra4king.circuitsim.gui.peers.wiring.PinPeer;
import com.ra4king.circuitsim.simulator.Circuit;
import com.ra4king.circuitsim.simulator.CircuitState;
import com.ra4king.circuitsim.simulator.Component;
import com.ra4king.circuitsim.simulator.components.wiring.Pin;
import com.ra4king.circuitsim.simulator.Port;
import com.ra4king.circuitsim.simulator.Simulator;

/**
 * Represents and wraps the subcircuit to test.
 * <p>
 * Holds a CircuitSim {@code CircuitBoard} (also known as a
 * "subcircuit") and the {@code CircuitSim} instance used to simulate
 * it. Your tests shouldn't need to touch this, since it mainly provides
 * methods for loading a subcircuit from a file and poking at its
 * internal state to find components to test — all things {@link
 * edu.gatech.cs2110.circuitsim.extension.CircuitSimExtension} handles
 * for you.
 */
public class Subcircuit {
    private String name;
    private CircuitSim circuitSim;
    private SubcircuitState state;
    // Used for lookups of components by name
    private ComponentNameInfo componentNameInfo;

    private Subcircuit(
            String name, CircuitSim circuitSim, SubcircuitState state, ComponentNameInfo componentNameInfo) {
        this.name = name;
        this.circuitSim = circuitSim;
        this.state = state;
        this.componentNameInfo = componentNameInfo;
    }

    private Subcircuit(String name, CircuitSim circuitSim, SubcircuitState state) {
        this(name, circuitSim, state, ComponentNameInfo.fromCircuitSim(circuitSim));
    }

    /**
     * Returns the name of this subcircuit as written in the test file.
     *
     * @return a {@code String} holding the subcircuit name. Not
     * normalized, so represents exactly what the test file specified.
     * @see #fromPath(String, String) for information on subcircuit name normalization
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the {@code CircuitSim} instance simulating the circuit.
     * <p>
     * <b>This exposes an internal CircuitSim API. Do not use unless you
     *    know what you are doing.</b>
     *
     * @return the {@code CircuitSim} instance used for simulation
     */
    public CircuitSim getCircuitSim() {
        return circuitSim;
    }

    /**
     * Returns the CircuitSim {@code Simulator} running the circuit.
     * <p>
     * <b>This exposes an internal CircuitSim API. Do not use unless you
     *    know what you are doing.</b>
     *
     * @return the {@code Simulator} instance used for simulation
     */
    public Simulator getSimulator() {
        return getCircuitSim().getSimulator();
    }

    /**
     * Returns the CircuitSim {@code CircuitBoard} for this subcircuit.
     * This is a GUI-side thing.
     * <p>
     * <b>This exposes an internal CircuitSim API. Do not use unless you
     *    know what you are doing.</b>
     *
     * @return the {@code CircuitBoard} of this subcircuit
     */
    public CircuitBoard getCircuitBoard() {
        return getCircuitManager().getCircuitBoard();
    }

    public CircuitManager getCircuitManager() {
        return state.circuitManager;
    }

    /**
     * Returns the CircuitSim {@code Circuit} for this subcircuit.
     * This is a simulation-side thing.
     * <p>
     * <b>This exposes an internal CircuitSim API. Do not use unless you
     *    know what you are doing.</b>
     *
     * @return the {@code Circuit} of this subcircuit
     */
    public Circuit getCircuit() {
        return getCircuitBoard().getCircuit();
    }

    /**
     * Returns the CircuitSim {@code CircuitState} of this subcircuit.
     * <p>
     * <b>This exposes an internal CircuitSim API. Do not use unless you
     *    know what you are doing.</b>
     *
     * @return the {@code CircuitState} of top level state of this subcircuit
     */
    public CircuitState getCircuitState() {
        return state.circuitState;
    }

    // Don't hate me for `throws Exception', Roi made me do it
    /**
     * Loads the subcircuit named {@code subcircuitName} from the {@code
     * .sim} file {@code simFilePath}.
     * <p>
     * Note that for the sake of students' stress levels, {@code
     * subcircuitName} is normalized to a lowercase alphanumeric string
     * before searching the {@code .sim} file for it, as are the names of
     * subcircuits in the file. So {@code "1-bit adder!"} will match
     * {@code "1 Bit Adder"}, {@code "1bit adder"}, {@code "1bitadder"},
     * {@code "1 B  I T A D DD E R"}, and so on.
     *
     * @param  simFilePath    path to the subcircuit. Usually relative, like
     * {@code "adder.sim"}
     * @param  subcircuitName name of the subcircuit. Normalized as
     *                        described above before lookup.
     * @return a new {@code Subcircuit} simulating the requested
     *         subcircuit
     * @throws Exception      specified by {@code CircuitSim.loadCircuits()}
                              in violation of all good taste on Earth
     */
    public static Subcircuit fromPath(String simFilePath, String subcircuitName) throws Exception {
        File circuitFile = new File(simFilePath);

        // In theory, zucchini should guarantee this for us, but stuff
        // happens
        // TODO: Race condition R I P
        if (!circuitFile.canRead()) {
            throw new IllegalArgumentException(
                String.format("Cannot read circuit file `%s'", simFilePath));
        }

        CircuitSim circuitSim = new CircuitSim(false);
        circuitSim.loadCircuits(circuitFile);

        return new Subcircuit(subcircuitName, circuitSim, lookupSubcircuit(circuitSim, subcircuitName));
    }

    // Create another version of this instance, except with a different
    // CircuitManager
    private Subcircuit withSubcircuitState(SubcircuitState state) {
        // Don't create an unnecessary new instance
        return Objects.equals(this.state, state)
               ? this
               : new Subcircuit(name, circuitSim, state, componentNameInfo);
    }

    private static String canonicalName(String name) {
        return name.toLowerCase().replaceAll("[^0-9a-z]+", "");
    }

    private static SubcircuitState lookupSubcircuit(CircuitSim circuitSim, String subcircuitName) {
        String canonicalSubcircuitName = canonicalName(subcircuitName);
        Map<String, CircuitManager> managers = circuitSim.getCircuitManagers();

        List<CircuitManager> matchingCircuits = managers.entrySet().stream()
            .filter(entry -> canonicalName(entry.getKey()).equals(canonicalSubcircuitName))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());

        if (matchingCircuits.size() == 0) {
            throw new IllegalArgumentException(String.format(
                "No subcircuits match the name `%s'. " +
                "Please double-check the names of all your subcircuits.",
                subcircuitName));
        } else if (matchingCircuits.size() > 1) {
            throw new IllegalArgumentException(String.format(
                "More than one subcircuit has the name `%s'. " +
                "Can't continue deterministically.",
                subcircuitName));
        }

        CircuitManager manager = matchingCircuits.get(0);
        return new SubcircuitState(manager, manager.getCircuit().getTopLevelState());
    }

    /**
     * Resets the simulation for this subcircuit.
     * <p>
     * The same thing as clicking Simulation → Reset Simulation in
     * CircuitSim.
     */
    public void resetSimulation() {
        circuitSim.getSimulator().reset();
    }

    /**
     * Returns the number of Input Pin or Output Pin components in this
     * circuit. Does not include subcircuits.
     *
     * @return the total pin count of this subcircuit
     */
    public int getPinCount() {
        Map<String, Integer> pinCounts = lookupComponentCounts(
            Arrays.asList("Input Pin", "Output Pin"), false, false);
        int totalPins = pinCounts.getOrDefault("Input Pin", 0) +
                        pinCounts.getOrDefault("Output Pin", 0);
        return totalPins;
    }

    /**
     * Finds if components with the given names or in the given component
     * categories exist in this subcircuit.
     *
     * @param  componentNames an iterable of component names, component
     *                        category names, or some mixture
     * @param  inverse true if you want to invert the search, else false
     * @param  recursive true if you want to search subcircuits, else false
     * @return a map of distinct component names matching the given
     *         criteria to their number occurrences
     */
    public Map<String, Integer> lookupComponentCounts(
            Collection<String> componentNames,
            boolean inverse,
            boolean recursive) {
        return lookupComponentPeers(componentNames, inverse, recursive)
               .entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().size()));
    }

    // TODO: delete me????
    private Map<String, Set<Component>> lookupComponents(
            Collection<String> componentNames,
            boolean inverse,
            boolean recursive) {
        return lookupComponentPeers(componentNames, inverse, recursive)
               .entrySet().stream().collect(Collectors.toMap(
                   Map.Entry::getKey,
                   entry -> entry.getValue().stream().map(ComponentPeer::getComponent)
                                 .collect(Collectors.toSet())));
    }


    private Map<String, Set<ComponentPeer<?>>> lookupComponentPeers(
            Collection<String> componentNames,
            boolean inverse,
            boolean recursive) {
        // Split the set of names into a set of component names and a
        // set of category names
        Set<String> components = new HashSet<>(componentNames);
        Set<String> goalCategories = new HashSet<>(components);
        goalCategories.retainAll(componentNameInfo.getCategoryNamesKnown());
        Set<String> goalComponents = new HashSet<>(components);
        goalComponents.retainAll(componentNameInfo.getComponentNamesKnown());

        // Look for bogus entries in the list passed by the programmer
        components.removeAll(goalCategories);
        components.removeAll(goalComponents);
        if (!components.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                "Unknown component/category names: %s",
                String.join(", ", components)));
        }

        Map<String, Set<ComponentPeer<?>>> matchingComponents = new HashMap<>();
        // Run a depth-first search through the simulation DAG starting
        // at this subcircuit. (Setting revisit=false makes this a DFS)
        walk(recursive, false, (circuitBoard, component) -> {
            Pair<String, String> name = componentNameInfo.getPeerCategoryAndName(component.getClass());
            boolean match = goalCategories.contains(name.getKey()) ||
                            goalComponents.contains(name.getValue());

            if (match ^ inverse) {
                matchingComponents.computeIfAbsent(
                    name.getValue(),
                    k -> new HashSet<>()).add(component);
            }
        });

        return matchingComponents;
    }

    // TODO FIXME update docs
    // pinLabel == null means only pin
    /**
     * Finds a Pin component labelled {@code pinLabel} in this
     * subcircuit and returns a wrapper around it.
     * <p>
     * To make sure the tester is deterministic, this method requires
     * the subcircuit contain exactly one pin with a matching label.
     * The algorithm checks this before verifying if a match has the
     * right bit size or direction (input/output).
     * <p>
     * Also for the sake of students' stress levels, {@code pinLabel} is
     * normalized as described for {@link #lookupSubcircuit(CircuitSim,String)}
     * before lookup.
     *
     * @param  pinLabel     the label of the pin
     * @param  wantInputPin whether the pin found should be input or output
     * @param  wantBits     how many bits the pin found should have
     * @return either an {@link InputPin} or {@link OutputPin} depending
     *         on {@code wantInputPin}
     * @throws IllegalArgumentException if the subcircuit does not
     *                                  contain exactly one matching pin
     * @see InputPin
     * @see OutputPin
     */
    public BasePin lookupPin(String pinLabel, boolean wantInputPin,
                             int wantBits, boolean recursive) {
        // TODO: InputPin.get() does not exist, but if it does later, this
        //       should actually be supported
        if (recursive && wantInputPin) {
            throw new IllegalArgumentException(
                "Can't recursively locate an input pin; consider how " +
                "changing its values would affect parent circuits");
        }

        Pair<SubcircuitState, PinPeer> match = lookupComponent(
            pinLabel, wantBits, recursive, PinPeer.class);
        SubcircuitState matchingState = match.getKey();
        PinPeer matchingPin = match.getValue();

        // Use their labels for error messages to be less confusing
        String theirPinLabel = matchingPin.getProperties().getValue(Properties.LABEL);

        if (matchingPin.isInput() != wantInputPin) {
            throw new IllegalArgumentException(String.format(
                "Subcircuit `%s' has %s pin labelled `%s', but expected it to be an %s pin instead",
                matchingState.circuitManager.getCircuitBoard().getCircuit().getName(),
                matchingPin.isInput()? "input" : "output",
                theirPinLabel,
                wantInputPin? "input" : "output"));
        }

        Subcircuit subcircuit = withSubcircuitState(matchingState);
        BasePin pinWrapper = wantInputPin? new InputPin(matchingPin.getComponent(),
                                                        subcircuit)
                                         : new OutputPin(matchingPin.getComponent(),
                                                         subcircuit);
        return pinWrapper;
    }

    // FIXME TODO document me
    // regLabel == null means only register
    public Register lookupRegister(String regLabel, int wantBits, boolean recursive) {
        Pair<SubcircuitState, RegisterPeer> match =
                lookupComponent(regLabel, wantBits, recursive, RegisterPeer.class);
        SubcircuitState matchingState = match.getKey();
        com.ra4king.circuitsim.simulator.components.memory.Register matchingRegister =
                match.getValue().getComponent();

        Subcircuit subcircuit = withSubcircuitState(matchingState);
        return new Register(matchingRegister, subcircuit);
    }

    public enum MemoryType {
        ROM,
        RAM
    }

    public BaseMemory lookupMemory(String label, int wantBits, boolean recursive, MemoryType type) {
        switch (type) {
            case ROM:
                Pair<SubcircuitState, ROMPeer> romMatch =
                    lookupComponent(label, wantBits, recursive, ROMPeer.class);
                return new Rom(romMatch.getValue().getComponent(), withSubcircuitState(romMatch.getKey()));
            case RAM:
                Pair<SubcircuitState, RAMPeer> ramMatch =
                    lookupComponent(label, wantBits, recursive, RAMPeer.class);
                return new Ram(ramMatch.getValue().getComponent(), withSubcircuitState(ramMatch.getKey()));
            default:
                throw new IllegalArgumentException("unknown memory type " + type.name());
        }
    }

    // TODO: support recursive
    public OutputPin snitchTunnel(String label, int wantBits) {
        String canonicalLabel = canonicalName(label);
        // Hack used because variables referenced in Lambdas must be
        // effectively final: https://stackoverflow.com/a/32523185/321301
        Tunnel[] tunnelFound = {null};

        walk(false, false, (state, peer) -> {
            if (peer instanceof Tunnel &&
                    canonicalName(peer.getProperties().getValue(Properties.LABEL)).equals(canonicalName(label))) {
                int actualBits = peer.getProperties().getValue(Properties.BITSIZE);
                if (actualBits != wantBits) {
                    throw new IllegalArgumentException(String.format(
                        "Tunnel `%s' in subcircuit `%s' should have %d bits, not %d",
                        label, state.circuitManager.getCircuitBoard().getCircuit().getName(),
                        wantBits, actualBits));
                }
                tunnelFound[0] = (Tunnel) peer;
            }
        });

        Tunnel tunnel = tunnelFound[0];
        if (tunnel == null) {
            throw new IllegalArgumentException(String.format(
                "No tunnel `%s' found in subcircuit `%s'", label, getCircuit().getName()));
        }

        Port.Link tunnelLink = tunnel.getComponent().getPort(0).getLink();
        Pin snitchPin = addSnitchPinToLink(tunnelLink, false);
        return new OutputPin(snitchPin, this);
    }

    // label == null means look for the only component
    private <T extends ComponentPeer<?>> Pair<SubcircuitState, T> lookupComponent(
            String label, int wantBits, boolean recursive, Class<T> peerClass) {

        String componentName = componentNameInfo.getPeerCategoryAndName(peerClass).getValue();
        String canonicalLabel = (label == null)? null : canonicalName(label);

        List<Pair<SubcircuitState, T>> matches = new LinkedList<>();
        // If we're doing recursive, we need to revisit to make sure we
        // find duplicates
        walk(recursive, true, (state, componentPeer) -> {
            if (peerClass.isInstance(componentPeer) &&
                    (label == null || canonicalLabel.equals(canonicalName(componentPeer.getProperties()
                                                                                       .getValue(Properties.LABEL))))) {
                matches.add(new Pair<>(state, peerClass.cast(componentPeer)));
            }
        });

        String searchCriteria = (label == null)? ""
                                               : String.format(" labelled `%s'", label);

        if (matches.size() > 1) {
            throw new IllegalArgumentException(String.format(
                "Subcircuit `%s'%s contains %d %ss%s, expected 1",
                getCircuitBoard().getCircuit().getName(),
                recursive? " (and its children)" : "",
                matches.size(), componentName, searchCriteria));
        } else if (matches.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                "Subcircuit `%s'%s contains no %ss%s!",
                getCircuitBoard().getCircuit().getName(),
                recursive? " (and its children)" : "",
                componentName, searchCriteria));
        }

        Pair<SubcircuitState, T> matchingPair = matches.get(0);
        if (wantBits > 0) {
            SubcircuitState matchingState = matchingPair.getKey();
            T matchingPeer = matchingPair.getValue();

            int actualBits = matchingPeer.getProperties().getValue(Properties.BITSIZE);
            if (actualBits != wantBits) {
                throw new IllegalArgumentException(String.format(
                        "Subcircuit `%s' has %s%s with %d bits, but expected %d bits",
                        matchingState.circuitManager.getCircuitBoard().getCircuit().getName(),
                        componentName, searchCriteria, actualBits, wantBits));
            }
        }

        return matchingPair;
    }

    // TODO FIXME update docs
    /**
     * Mocks the only register in this subcircuit by replacing it with
     * input and output pins. Useful for testing the combinational logic
     * in a sequential circuit with exactly one register. For more
     * details on the motivation behind {@link MockRegister}s, see
     * {@link MockRegister}.
     * <p>
     * Each register port will be disconnected from anything else and
     * replaced with a new Pin component reconnected to everything like
     * before.
     * <p>
     * Will blow up if this subcircuit does not contain exactly one
     * Register component. Often, this is what you want for state
     * machine subcircuits for example, and it does not require students
     * to label the register some arbitrary name buried deep in the
     * assignment PDF.
     *
     * @param  wantBits the bitsize of the register
     * @return a {@code MockRegister} which effectively acts a
     *         collection of input and output pins
     * @see    MockRegister
     */
    MockRegister mockRegister(com.ra4king.circuitsim.simulator.components.memory.Register reg) {
        //List<Register> registers = getCircuitBoard().getComponents().stream()
        //    .filter(component -> component instanceof RegisterPeer)
        //    .map(component -> ((RegisterPeer) component).getComponent())
        //    .collect(Collectors.toList());

        //if (registers.size() > 1) {
        //    throw new IllegalArgumentException(String.format(
        //        "Subcircuit `%s' contains %d registers, expected 1",
        //        getCircuitBoard().getCircuit().getName(), registers.size()));
        //} else if (registers.isEmpty()) {
        //    throw new IllegalArgumentException(String.format(
        //        "Subcircuit `%s' contains no registers!",
        //        getCircuitBoard().getCircuit().getName()));
        //}

        //Register reg = registers.get(0);

        //int actualBits = reg.getBitSize();
        //if (actualBits != wantBits) {
        //    throw new IllegalArgumentException(String.format(
        //        "Subcircuit `%s' has register with %d bits, but expected %d bits",
        //        getCircuitBoard().getCircuit().getName(), actualBits, wantBits));
        //}

        //com.ra4king.circuitsim.simulator.components.memory.Register reg = regWrapper.getRegister();

        InputPin q = new InputPin(substitutePin(reg.getPort(PORT_OUT), true), this);
        OutputPin d = new OutputPin(substitutePin(reg.getPort(PORT_IN), false), this);
        OutputPin en = new OutputPin(substitutePin(reg.getPort(PORT_ENABLE), false), this);
        OutputPin clk = new OutputPin(substitutePin(reg.getPort(PORT_CLK), false), this);
        OutputPin rst = new OutputPin(substitutePin(reg.getPort(PORT_ZERO), false), this);

        return new MockRegister(q, d, en, clk, rst, this);
    }

    public enum PulserType {
        CLOCK,
        BUTTON
    }

    public MockPulser mockPulser(String label, int wantBits, boolean recursive, PulserType type) {
        SubcircuitState matchingState;
        ComponentPeer<?> matchingComponent;

        switch (type) {
            case CLOCK:
                Pair<SubcircuitState, ClockPeer> clockMatch = lookupComponent(label, wantBits, recursive, ClockPeer.class);
                matchingState = clockMatch.getKey();
                matchingComponent = clockMatch.getValue();
                break;
            case BUTTON:
                Pair<SubcircuitState, com.ra4king.circuitsim.gui.peers.io.Button> buttonMatch =
                    lookupComponent(label, wantBits, recursive, com.ra4king.circuitsim.gui.peers.io.Button.class);
                matchingState = buttonMatch.getKey();
                matchingComponent = buttonMatch.getValue();
                break;
            default:
                throw new IllegalArgumentException("unknown pulser type " + type.name());
        }

        Subcircuit subcircuit = withSubcircuitState(matchingState);
        Port onlyPort = matchingComponent.getComponent().getPort(0);
        InputPin mockPin = new InputPin(subcircuit.substitutePin(onlyPort, true), subcircuit);
        return (type == PulserType.BUTTON)? new Button(mockPin, subcircuit)
                                          : new Clock(mockPin, subcircuit);
    }

    private Port.Link makeOrphanPort(Port port) {
        Port.Link link = port.getLink();
        port.unlinkPort(port);
        return link;
    }

    private int getMaxX() {
        return (int) Math.ceil(state.circuitManager.getCanvas().getWidth() / GuiUtils.BLOCK_SIZE);
    }

    private int getMaxY() {
        return (int) Math.ceil(state.circuitManager.getCanvas().getHeight() / GuiUtils.BLOCK_SIZE);
    }

    private Pin substitutePin(Port port, boolean isInput) {
        return addSnitchPinToLink(makeOrphanPort(port), isInput);
    }

    private Pin addSnitchPinToLink(Port.Link link, boolean isInput) {
        PinPeer mockPinPeer = new PinPeer(new Properties(
            new Properties.Property<>(Properties.BITSIZE, link.getBitSize()),
            new Properties.Property<>(PinPeer.IS_INPUT, isInput)
        ), getMaxX(), getMaxY());

        // Find a valid place to drop it
        do {
            mockPinPeer.setX(mockPinPeer.getX() + 32);
            mockPinPeer.setY(mockPinPeer.getY() + 32);
        } while (!getCircuitBoard().isValidLocation(mockPinPeer));

        getCircuitBoard().addComponent(mockPinPeer);

        Pin mockPin = mockPinPeer.getComponent();
        Port newPort = mockPin.getPort(Pin.PORT);
        // Now reconnect all the old stuff
        link.linkPort(newPort);

        return mockPin;
    }

    private void walk(boolean recursive, boolean revisit, BiConsumer<SubcircuitState, ComponentPeer<?>> consumer) {
        Set<String> visitedSubcircuits = revisit? Collections.emptySet()
                                                : new HashSet<>();
        walk(visitedSubcircuits, state, recursive, revisit, consumer);
    }

    private void walk(Set<String> visitedSubcircuits,
                      SubcircuitState state,
                      boolean recursive,
                      boolean revisit,
                      BiConsumer<SubcircuitState, ComponentPeer<?>> consumer) {
        for (ComponentPeer<?> component : state.circuitManager.getCircuitBoard().getComponents()) {
            boolean isSubcircuit = component instanceof SubcircuitPeer;

            if (isSubcircuit && recursive) {
                SubcircuitPeer subcircuitPeer = (SubcircuitPeer) component;
                String subcircuitName = subcircuitPeer.getProperties()
                                                      .getProperty(SubcircuitPeer.SUBCIRCUIT)
                                                      .getStringValue();
                if (!visitedSubcircuits.contains(subcircuitName)) {
                    if (!revisit)
                        visitedSubcircuits.add(subcircuitName);

                    CircuitManager childCircuitManager = subcircuitPeer.getProperties()
                                                                       .getValue(SubcircuitPeer.SUBCIRCUIT);
                    CircuitState childCircuitState = subcircuitPeer.getComponent()
                                                                   .getSubcircuitState(state.circuitState);
                    SubcircuitState childState = new SubcircuitState(childCircuitManager, childCircuitState);
                    walk(visitedSubcircuits, childState, recursive, revisit, consumer);
                }
            } else if (!isSubcircuit) {
                consumer.accept(state, component);
            }
        }
    }

    private static class SubcircuitState {
        private CircuitManager circuitManager;
        private CircuitState circuitState;

        private SubcircuitState(CircuitManager circuitManager, CircuitState circuitState) {
            this.circuitManager = circuitManager;
            this.circuitState = circuitState;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SubcircuitState that = (SubcircuitState) o;
            return Objects.equals(circuitManager, that.circuitManager) &&
                    Objects.equals(circuitState, that.circuitState);
        }
    }

    private static class ComponentNameInfo {
        private Set<String> categoryNamesKnown;
        private Set<String> componentNamesKnown;
        private Map<Class<? extends ComponentPeer<?>>,
                Pair<String, String>> componentClassNames;

        private ComponentNameInfo(Set<String> categoryNamesKnown,
                                  Set<String> componentNamesKnown,
                                  Map<Class<? extends ComponentPeer<?>>, Pair<String, String>> componentClassNames) {
            this.categoryNamesKnown = categoryNamesKnown;
            this.componentNamesKnown = componentNamesKnown;
            this.componentClassNames = componentClassNames;
        }

        // Find the names of CircuitSim components and component categories
        public static ComponentNameInfo fromCircuitSim(CircuitSim circuitSim) {
            Set<String> categoryNamesKnown = new HashSet<>();
            Set<String> componentNamesKnown = new HashSet<>();
            Map<Class<? extends ComponentPeer<?>>,
                    Pair<String, String>> componentClassNames = new HashMap<>();

            circuitSim.getComponentManager().forEach((ComponentManager.ComponentLauncherInfo info) -> {
                categoryNamesKnown.add(info.name.getKey());
                componentNamesKnown.add(info.name.getValue());
                componentClassNames.put(info.clazz, info.name);
            });

            return new ComponentNameInfo(categoryNamesKnown, componentNamesKnown, componentClassNames);
        }

        public Pair<String, String> getPeerCategoryAndName(Class<? extends ComponentPeer> peerClass) {
            Pair<String, String> name = componentClassNames.get(peerClass);
            if (name == null) {
                throw new IllegalStateException(String.format("Unknown component %s", peerClass));
            }
            return name;
        }

        public Set<String> getCategoryNamesKnown() {
            return Collections.unmodifiableSet(categoryNamesKnown);
        }

        public Set<String> getComponentNamesKnown() {
            return Collections.unmodifiableSet(componentNamesKnown);
        }
    }
}
