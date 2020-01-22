package io.zucchini.circuitsimtester.api;

import io.zucchini.circuitsimtester.extension.CircuitSimExtension;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

/**
 * Instructs {@link CircuitSimExtension}
 * to find and inject the Pin component into an {@link InputPin} or
 * {@link OutputPin} field in a test class.
 * <p>
 * When you annotate an {@link InputPin} or {@link OutputPin} field in a
 * test class with this, {@link CircuitSimExtension}
 * will search the subcircuit for a Pin component with direction
 * (input/output) matching the type of the field, bitsize {@code
 * bits()}, and label {@code label()}. If {@code label()} is {@code ""}
 * (the default), it will search for a label matching the name of the
 * field instead.
 *
 * @see Subcircuit#lookupPin(String,boolean,int)
 * @see InputPin
 * @see OutputPin
 */
@Retention(RUNTIME)
public @interface SubcircuitComponent {
    /**
     * The label of a matching Pin. If empty (the default), {@link
     * CircuitSimExtension} will
     * use the name of the field.
     *
     * @return The label of a matching Pin.
     */
    String label() default ""; // label of pin (default: name of variable)

    /**
     * The bit size of a matching Pin.
     *
     * @return The bit size of a matching Pin.
     */
    // TODO FIXME document when this is requried
    int bits() default -1;

    boolean onlyInstance() default false;

    // TODO FIXME document
    boolean recursiveSearch() default false;

    /**
     * The type of "pin". The default is a literal existing Pin
     * component, but you can set this to {@code TUNNEL} to spy on the
     * value of a tunnel by attaching an OutputPin to it. (The label is
     * then the label of the tunnel you want to spy instead of the label
     * of an existing pin.)
     *
     * @return An enum value determining if this should spy on a tunnel
     *         instead of controlling a Pin component
     */
    Type type() default Type.INFER;

    enum Type {
        INFER,
        TUNNEL,
    }
}
