package org.github.mjcro.mosaic.exceptions;

/**
 * Exception thrown by type handlers when unexpected value
 * is received.
 */
public class UnexpectedValueException extends MosaicException {
    /**
     * Constructs exception.
     *
     * @param value Unexpected value, can be null.
     */
    public UnexpectedValueException(final Object value) {
        super(
                value == null
                        ? "Unexpected null value"
                        : "Unexpected value of class " + value.getClass().getName()
        );
    }
}
