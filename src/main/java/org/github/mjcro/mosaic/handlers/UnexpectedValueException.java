package org.github.mjcro.mosaic.handlers;

import java.sql.SQLException;

/**
 * Exception thrown by type handlers when unexpected value
 * is received.
 */
public class UnexpectedValueException extends SQLException {
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
