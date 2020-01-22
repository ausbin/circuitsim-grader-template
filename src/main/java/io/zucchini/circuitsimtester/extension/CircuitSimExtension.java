package io.zucchini.circuitsimtester.extension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.gatech.cs2110.circuitsim.api.*;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.Extension;

import io.zucchini.circuitsimtester.api.BasePin;
import io.zucchini.circuitsimtester.api.InputPin;
import io.zucchini.circuitsimtester.api.MockRegister;
import io.zucchini.circuitsimtester.api.Restrictor;
import io.zucchini.circuitsimtester.api.Subcircuit;
import io.zucchini.circuitsimtester.api.SubcircuitPin;
import io.zucchini.circuitsimtester.api.SubcircuitRegister;
import io.zucchini.circuitsimtester.api.SubcircuitTest;
import static edu.gatech.cs2110.circuitsim.api.Subcircuit.MemoryType.RAM;
import static edu.gatech.cs2110.circuitsim.api.Subcircuit.MemoryType.ROM;
import static edu.gatech.cs2110.circuitsim.api.Subcircuit.PulserType.BUTTON;
import static edu.gatech.cs2110.circuitsim.api.Subcircuit.PulserType.CLOCK;
import static edu.gatech.cs2110.circuitsim.api.SubcircuitComponent.Type.TUNNEL;

/**
 * Extends JUnit to understand testing CircuitSim subcircuits.
 * <p>
 * To use: annotate a JUnit test class with {@code  @ExtendWith(CircuitSimExtension.class)}
 *
 * @see <a href="https://github.com/ausbin/circuitsim-grader-template/blob/master/README.md">The README with examples</a>
 */
public class CircuitSimExtension implements Extension, BeforeAllCallback, BeforeEachCallback {
    private boolean resetSimulationBetween;
    private Subcircuit subcircuit;
    private List<FieldInjection> fieldInjections;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        SubcircuitTest subcircuitAnnotation = testClass.getAnnotation(SubcircuitTest.class);

        if (subcircuitAnnotation == null) {
            throw new IllegalArgumentException(
                "The CircuitSim extension requires annotating the test class with @SubcircuitTest");
        }

        resetSimulationBetween = subcircuitAnnotation.resetSimulationBetween();
        subcircuit = Subcircuit.fromPath(subcircuitAnnotation.file(),
                                         subcircuitAnnotation.subcircuit());

        for (Class<? extends Restrictor> restrictor : subcircuitAnnotation.restrictors()) {
            runRestrictor(subcircuit, restrictor);
        }

        fieldInjections = generateFieldInjections(testClass);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        // Reset simulator before each test
        if (resetSimulationBetween) {
            subcircuit.resetSimulation();
        }

        // Do simple dependency injection
        for (FieldInjection fieldInjection : fieldInjections) {
            fieldInjection.inject(extensionContext.getRequiredTestInstance());
        }
    }

    private void runRestrictor(
            Subcircuit subcircuit, Class<? extends Restrictor> restrictorClass)
            throws AssertionError {
        Restrictor restrictor;
        // Lord Gosling, thank you for this boilerplate, amen
        try {
            restrictor = restrictorClass.getConstructor().newInstance();
        } catch (NoSuchMethodException err) {
            throw new IllegalStateException(String.format(
                "restrictor class %s needs a no-args constructor",
                restrictorClass), err);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException err) {
            throw new IllegalStateException(String.format(
                "could not instantiate restrictor class %s",
                restrictorClass), err);
        }

        try {
            restrictor.validate(subcircuit);
        } catch (AssertionError err) {
            throw new AssertionError(String.format(
                "validation error with subcircuit `%s': %s",
                subcircuit.getName(), err.getMessage()), err);
        }
    }

    private List<FieldInjection> generateFieldInjections(Class<?> testClass) {
        Map<Class<?>, Function<Field, FieldInjection>> fieldInjectors = new HashMap<>();
        fieldInjectors.put(InputPin.class,     this::generatePinFieldInjection);
        fieldInjectors.put(OutputPin.class,    this::generatePinFieldInjection);
        fieldInjectors.put(Ram.class,          this::generateMemoryFieldInjection);
        fieldInjectors.put(Rom.class,          this::generateMemoryFieldInjection);
        fieldInjectors.put(Register.class,     this::generateRegFieldInjection);
        fieldInjectors.put(MockRegister.class, this::generateRegFieldInjection);
        fieldInjectors.put(Clock.class,        this::generatePulserFieldInjection);
        fieldInjectors.put(Button.class,       this::generatePulserFieldInjection);

        // TODO: Detect duplicate pins (`Pin a' and `Pin A' are distinct
        //       fields in Java but not here). Easy solution: sort the
        //       stream by canonicalName(field.getName()) and iterate
        //       over the resulting List, comparing each with the
        //       next
        return Arrays.stream(testClass.getDeclaredFields())
                     .filter(field -> field.isAnnotationPresent(SubcircuitComponent.class))
                     .map(field -> Optional.ofNullable(fieldInjectors.get(field.getType()))
                                           .orElseThrow(() -> new IllegalArgumentException(
                                               "Test class field " + field.getName() + " in " +
                                               testClass.getCanonicalName() + " annotated with " +
                                               "@SubcircuitComponent has unknown type " + field.getType()))
                                           .apply(field))
                     .collect(Collectors.toList());
    }

    private FieldInjection generatePinFieldInjection(Field field) {
        SubcircuitComponent pinAnnotation = field.getDeclaredAnnotation(SubcircuitComponent.class);

        if (pinAnnotation.bits() <= 0) {
            throw new IllegalArgumentException(field.getName() +
                " is an Input/Output Pins, so @SubcircuitComponent needs a positive bits parameter");
        }

        BasePin pinWrapper;
        if (pinAnnotation.type() == TUNNEL) {
            if (pinAnnotation.onlyInstance()) {
                throw new IllegalArgumentException(
                    field.getName() + " is a tunnel, please don't specify " +
                    "onlyInstance on @SubcircuitComponent, you're better than this");
            }
            if (pinAnnotation.recursiveSearch()) {
                throw new IllegalArgumentException(
                    field.getName() + " is a tunnel, recursiveSearch on " +
                    "@SubcircuitComponent is not implemented");
            }

            String label = pinAnnotation.label().isEmpty()? field.getName() : pinAnnotation.label();
            pinWrapper = subcircuit.snitchTunnel(label, pinAnnotation.bits());
        } else { // INFER
            boolean wantInputPin = InputPin.class.equals(field.getType());
            String pinLabel = pinAnnotation.onlyInstance()? null :
                              pinAnnotation.label().isEmpty()? field.getName() : pinAnnotation.label();
            pinWrapper = subcircuit.lookupPin(pinLabel, wantInputPin, pinAnnotation.bits(), pinAnnotation.recursiveSearch());
        }

        return new FieldInjection(field, pinWrapper);
    }

    private FieldInjection generateRegFieldInjection(Field field) {
        SubcircuitComponent regAnnotation = field.getDeclaredAnnotation(SubcircuitComponent.class);

        if (regAnnotation.bits() <= 0) {
            throw new IllegalArgumentException(field.getName() +
                    " is an Register, so @SubcircuitComponent needs a positive bits parameter");
        }

        String regLabel = regAnnotation.onlyInstance()? null :
                          regAnnotation.label().isEmpty()? field.getName() : regAnnotation.label();
        Register reg = subcircuit.lookupRegister(regLabel, regAnnotation.bits(),
                                                 regAnnotation.recursiveSearch());

        boolean wantMockRegister = MockRegister.class.isAssignableFrom(field.getType());
        if (wantMockRegister) {
            MockRegister mockRegister = reg.mock();
            return new FieldInjection(field, mockRegister);
        } else {
            return new FieldInjection(field, reg);
        }
    }

    private FieldInjection generateMemoryFieldInjection(Field field) {
        SubcircuitComponent componentAnnotation = field.getDeclaredAnnotation(SubcircuitComponent.class);

        if (componentAnnotation.bits() <= 0) {
            throw new IllegalArgumentException(field.getName() +
                    " is RAM/ROM, so @SubcircuitComponent needs a positive bits parameter");
        }

        String label = componentAnnotation.onlyInstance()? null :
                       componentAnnotation.label().isEmpty()? field.getName() : componentAnnotation.label();
        Subcircuit.MemoryType type = field.getType().equals(Ram.class)? RAM : ROM;
        BaseMemory wrapper = subcircuit.lookupMemory(label, componentAnnotation.bits(),
                                                     componentAnnotation.recursiveSearch(), type);
        return new FieldInjection(field, wrapper);
    }

    private FieldInjection generatePulserFieldInjection(Field field) {
        SubcircuitComponent componentAnnotation = field.getDeclaredAnnotation(SubcircuitComponent.class);

        if (componentAnnotation.bits() >= 0) {
            throw new IllegalArgumentException(field.getName() +
                    " is a Clock/Button, so @SubcircuitComponent does not need a bits parameter");
        }

        String label = componentAnnotation.onlyInstance()? null :
                       componentAnnotation.label().isEmpty()? field.getName() : componentAnnotation.label();
        Subcircuit.PulserType type = field.getType().equals(Button.class)? BUTTON : CLOCK;
        MockPulser mock = subcircuit.mockPulser(label,  componentAnnotation.bits(),  componentAnnotation.recursiveSearch(), type);
        return new FieldInjection(field, mock);
    }

    private static class FieldInjection {
        private Field field;
        private Object valueToInject;

        public FieldInjection(Field field, Object valueToInject) {
            this.field = field;
            this.valueToInject = valueToInject;
        }

        public void inject(Object obj) throws IllegalAccessException {
            field.setAccessible(true);
            field.set(obj, valueToInject);
        }
    }
}
